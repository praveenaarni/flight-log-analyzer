package com.praveena.flightlog.parse;

import com.praveena.flightlog.model.SensorSample;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Reads a header-driven CSV flight log into {@link SensorSample}s.
 *
 * <p>Format assumptions, chosen to match the CSV export from PX4 Flight Review:
 * <ul>
 *   <li>First line is a header of column names.</li>
 *   <li>A column named {@code timestamp} (case-insensitive) holds microseconds; if absent, the
 *       first column is used.</li>
 *   <li>Every other column is a numeric channel. Empty or non-numeric cells are omitted from
 *       that sample rather than failing the row.</li>
 *   <li>No quoted fields. PX4 exports never quote, and avoiding a CSV library keeps the parser
 *       small enough to test exhaustively.</li>
 * </ul>
 *
 * <p>{@link #stream} is lazy and reads one line at a time, so a multi-gigabyte log can be
 * processed without loading it into memory. {@link #read} is the convenient eager form.
 */
public final class CsvFlightLogReader {

    private static final String TIMESTAMP_COLUMN = "timestamp";

    private CsvFlightLogReader() {
    }

    /** Everything from one file: the usable samples and every row that had a problem. */
    public record Result(List<SensorSample> samples, List<ParseIssue> issues) {
    }

    /** Reads the whole file eagerly. */
    public static Result read(Path path) throws IOException {
        List<ParseIssue> issues = new ArrayList<>();
        try (Stream<SensorSample> samples = stream(path, issues::add)) {
            return new Result(samples.toList(), List.copyOf(issues));
        }
    }

    /**
     * Streams samples lazily. The caller must close the returned stream (use try-with-resources)
     * to release the underlying file handle.
     *
     * @param onIssue called for each row that could not be fully parsed
     * @throws IOException if the file cannot be opened or has no header line
     */
    public static Stream<SensorSample> stream(Path path, Consumer<ParseIssue> onIssue) throws IOException {
        BufferedReader reader = Files.newBufferedReader(path);
        String headerLine;
        try {
            headerLine = reader.readLine();
        } catch (IOException e) {
            reader.close();
            throw e;
        }
        if (headerLine == null || headerLine.isBlank()) {
            reader.close();
            throw new IOException("Flight log has no header line: " + path);
        }

        Header header = Header.parse(headerLine);
        AtomicLong lineNumber = new AtomicLong(1);

        return reader.lines()
                .onClose(() -> closeQuietly(reader))
                .map(raw -> parseRow(header, raw, lineNumber.incrementAndGet(), onIssue))
                .flatMap(Optional::stream);
    }

    private static Optional<SensorSample> parseRow(Header header, String raw, long lineNumber,
                                                   Consumer<ParseIssue> onIssue) {
        if (raw.isBlank()) {
            return Optional.empty();
        }

        // -1 keeps trailing empty cells, so "1,2," has three fields, not two.
        String[] cells = raw.split(",", -1);
        if (cells.length != header.columns().size()) {
            onIssue.accept(new ParseIssue(lineNumber,
                    "expected " + header.columns().size() + " columns but found " + cells.length));
            return Optional.empty();
        }

        long timestamp;
        try {
            timestamp = Long.parseLong(cells[header.timestampIndex()].trim());
        } catch (NumberFormatException e) {
            onIssue.accept(new ParseIssue(lineNumber,
                    "unparsable timestamp '" + cells[header.timestampIndex()] + "'"));
            return Optional.empty();
        }

        Map<String, Double> channels = new HashMap<>();
        for (int i = 0; i < cells.length; i++) {
            if (i == header.timestampIndex()) {
                continue;
            }
            String cell = cells[i].trim();
            if (cell.isEmpty()) {
                continue;
            }
            try {
                channels.put(header.columns().get(i), Double.parseDouble(cell));
            } catch (NumberFormatException e) {
                onIssue.accept(new ParseIssue(lineNumber,
                        "non-numeric value '" + cell + "' in channel " + header.columns().get(i)));
            }
        }
        return Optional.of(new SensorSample(timestamp, channels));
    }

    private static void closeQuietly(BufferedReader reader) {
        try {
            reader.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Column names in file order plus which one holds the timestamp. */
    private record Header(List<String> columns, int timestampIndex) {

        static Header parse(String line) {
            List<String> columns = Stream.of(line.split(",", -1)).map(String::trim).toList();
            int timestampIndex = 0;
            for (int i = 0; i < columns.size(); i++) {
                if (columns.get(i).equalsIgnoreCase(TIMESTAMP_COLUMN)) {
                    timestampIndex = i;
                    break;
                }
            }
            return new Header(columns, timestampIndex);
        }
    }
}
