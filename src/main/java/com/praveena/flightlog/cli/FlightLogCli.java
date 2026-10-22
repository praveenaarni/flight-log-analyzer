package com.praveena.flightlog.cli;

import com.praveena.flightlog.parse.CsvFlightLogReader;
import com.praveena.flightlog.parse.ParseIssue;
import com.praveena.flightlog.stats.StatsCalculator;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.file.Path;
import java.util.TreeSet;
import java.util.concurrent.Callable;

/**
 * Entry point. For now it only parses a log and prints a summary; stats and anomaly
 * detection plug in here as they are written.
 */
@Command(name = "flightlog",
        mixinStandardHelpOptions = true,
        version = "flightlog 0.1.0",
        description = "Analyzes a CSV flight log for statistics and anomalies.")
public final class FlightLogCli implements Callable<Integer> {

    @Parameters(index = "0", description = "CSV flight log to analyze.")
    private Path logFile;

    public static void main(String[] args) {
        System.exit(new CommandLine(new FlightLogCli()).execute(args));
    }

    @Override
    public Integer call() throws IOException {
        CsvFlightLogReader.Result result = CsvFlightLogReader.read(logFile);

        if (result.samples().isEmpty()) {
            System.err.println("No usable samples in " + logFile);
            return 1;
        }

        var channels = new TreeSet<String>();
        result.samples().forEach(s -> channels.addAll(s.channels().keySet()));
        double start = result.samples().getFirst().timestampSeconds();
        double end = result.samples().getLast().timestampSeconds();

        System.out.printf("File:      %s%n", logFile);
        System.out.printf("Samples:   %d%n", result.samples().size());
        System.out.printf("Duration:  %.2f s (%.2f s to %.2f s)%n", end - start, start, end);
        System.out.printf("Channels:  %s%n", String.join(", ", channels));
        System.out.printf("Issues:    %d%n", result.issues().size());
        for (ParseIssue issue : result.issues()) {
            System.out.printf("  line %d: %s%n", issue.lineNumber(), issue.reason());
        }

        System.out.println();
        System.out.printf("%-20s %6s %10s %10s %10s %10s %10s%n",
                "channel", "count", "min", "max", "mean", "stddev", "p99");
        StatsCalculator.compute(result.samples()).forEach((channel, s) ->
                System.out.printf("%-20s %6d %10.3f %10.3f %10.3f %10.3f %10.3f%n",
                        channel, s.count(), s.min(), s.max(), s.mean(), s.stdDev(), s.p99()));
        return 0;
    }
}
