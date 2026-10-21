package com.praveena.flightlog.parse;

import com.praveena.flightlog.model.SensorSample;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvFlightLogReaderTest {

    @Test
    void readsEveryWellFormedRowFromSampleLog() throws Exception {
        var result = CsvFlightLogReader.read(sampleLog());

        // 11 data rows, 1 of them has the wrong column count and is dropped.
        assertEquals(10, result.samples().size());
        assertEquals(0L, result.samples().getFirst().timestampMicros());
        assertEquals(1_000_000L, result.samples().getLast().timestampMicros());
    }

    @Test
    void usesHeaderNamesAsChannelNames() throws Exception {
        var result = CsvFlightLogReader.read(sampleLog());

        SensorSample first = result.samples().getFirst();
        assertEquals(Set.of("altitude_m", "vertical_speed_mps", "accel_z_mps2", "battery_v"),
                first.channels().keySet());
        assertEquals(9.81, first.value("accel_z_mps2").orElseThrow());
    }

    @Test
    void omitsEmptyCellInsteadOfFailingRow() throws Exception {
        var result = CsvFlightLogReader.read(sampleLog());

        SensorSample at600ms = sampleAt(result.samples(), 600_000L);
        assertTrue(at600ms.value("vertical_speed_mps").isEmpty());
        assertEquals(3.9, at600ms.value("altitude_m").orElseThrow());
    }

    @Test
    void reportsNonNumericCellAndKeepsRestOfRow() throws Exception {
        var result = CsvFlightLogReader.read(sampleLog());

        SensorSample at700ms = sampleAt(result.samples(), 700_000L);
        assertTrue(at700ms.value("battery_v").isEmpty());
        assertEquals(4.8, at700ms.value("altitude_m").orElseThrow());

        ParseIssue issue = issueAtLine(result.issues(), 9);
        assertTrue(issue.reason().contains("battery_v"), issue.reason());
    }

    @Test
    void dropsRowWithWrongColumnCountAndReportsLineNumber() throws Exception {
        var result = CsvFlightLogReader.read(sampleLog());

        assertTrue(result.samples().stream().noneMatch(s -> s.timestampMicros() == 800_000L));
        ParseIssue issue = issueAtLine(result.issues(), 10);
        assertTrue(issue.reason().contains("columns"), issue.reason());
    }

    @Test
    void dropsRowWithUnparsableTimestamp(@TempDir Path dir) throws IOException {
        Path log = dir.resolve("bad_timestamp.csv");
        Files.writeString(log, """
                timestamp,alt
                0,1.0
                oops,2.0
                200,3.0
                """);

        var result = CsvFlightLogReader.read(log);

        assertEquals(List.of(0L, 200L), result.samples().stream().map(SensorSample::timestampMicros).toList());
        assertEquals(1, result.issues().size());
        assertEquals(3, result.issues().getFirst().lineNumber());
    }

    @Test
    void fallsBackToFirstColumnWhenNoTimestampHeader(@TempDir Path dir) throws IOException {
        Path log = dir.resolve("no_ts_header.csv");
        Files.writeString(log, """
                t_us,alt
                500,1.0
                """);

        var result = CsvFlightLogReader.read(log);

        assertEquals(500L, result.samples().getFirst().timestampMicros());
        assertEquals(Set.of("alt"), result.samples().getFirst().channels().keySet());
    }

    @Test
    void skipsBlankLines(@TempDir Path dir) throws IOException {
        Path log = dir.resolve("blank_lines.csv");
        Files.writeString(log, """
                timestamp,alt
                0,1.0

                100,2.0
                """);

        var result = CsvFlightLogReader.read(log);

        assertEquals(2, result.samples().size());
        assertTrue(result.issues().isEmpty());
    }

    @Test
    void rejectsEmptyFile(@TempDir Path dir) throws IOException {
        Path log = dir.resolve("empty.csv");
        Files.writeString(log, "");

        assertThrows(IOException.class, () -> CsvFlightLogReader.read(log));
    }

    @Test
    void sampleIsImmutable() {
        var sample = new SensorSample(0L, java.util.Map.of("alt", 1.0));
        assertThrows(UnsupportedOperationException.class, () -> sample.channels().put("x", 2.0));
    }

    private static Path sampleLog() throws URISyntaxException {
        return Path.of(CsvFlightLogReaderTest.class.getResource("/sample_flight.csv").toURI());
    }

    private static SensorSample sampleAt(List<SensorSample> samples, long timestampMicros) {
        return samples.stream()
                .filter(s -> s.timestampMicros() == timestampMicros)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no sample at " + timestampMicros));
    }

    private static ParseIssue issueAtLine(List<ParseIssue> issues, long line) {
        return issues.stream()
                .filter(i -> i.lineNumber() == line)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no issue at line " + line + ", got " + issues));
    }
}
