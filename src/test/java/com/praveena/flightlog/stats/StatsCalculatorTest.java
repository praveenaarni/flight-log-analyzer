package com.praveena.flightlog.stats;

import com.praveena.flightlog.parse.CsvFlightLogReader;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StatsCalculatorTest {

    @Test
    void computesAltitudeStatsFromSampleLog() throws Exception {
        var samples = CsvFlightLogReader.read(sampleLog()).samples();

        ChannelStats alt = StatsCalculator.compute(samples).get("altitude_m");

        // Ten values after the parser drops the malformed row: 0, 0, 0.5, 1.2, 2.1, 3.0, 3.9, 4.8, 6.6, 7.5
        assertEquals(10, alt.count());
        assertEquals(0.0, alt.min());
        assertEquals(7.5, alt.max());
        assertEquals(2.96, alt.mean(), 1e-9);
    }

    @Test
    void countsOnlyRowsWhereChannelHadAValue() throws Exception {
        var samples = CsvFlightLogReader.read(sampleLog()).samples();

        Map<String, ChannelStats> stats = StatsCalculator.compute(samples);

        // 10 rows survive parsing. One has a blank vertical speed, one has a non-numeric battery.
        assertEquals(9, stats.get("vertical_speed_mps").count(), "one blank cell");
        assertEquals(9, stats.get("battery_v").count(), "one non-numeric cell");
        assertEquals(10, stats.get("accel_z_mps2").count(), "every surviving row has a value");
    }

    @Test
    void returnsChannelsInAlphabeticalOrder() throws Exception {
        var samples = CsvFlightLogReader.read(sampleLog()).samples();

        List<String> channels = List.copyOf(StatsCalculator.compute(samples).keySet());

        assertEquals(List.of("accel_z_mps2", "altitude_m", "battery_v", "vertical_speed_mps"), channels);
    }

    @Test
    void constantChannelHasZeroStdDev() {
        ChannelStats s = StatsCalculator.fromValues(List.of(5.0, 5.0, 5.0));

        assertEquals(0.0, s.stdDev());
        assertEquals(5.0, s.p50());
    }

    @Test
    void singleValueIsItsOwnEverything() {
        ChannelStats s = StatsCalculator.fromValues(List.of(42.0));

        assertEquals(1, s.count());
        assertEquals(42.0, s.min());
        assertEquals(42.0, s.max());
        assertEquals(42.0, s.mean());
        assertEquals(0.0, s.stdDev());
        assertEquals(42.0, s.p50());
        assertEquals(42.0, s.p99());
    }

    @Test
    void stdDevIsPopulationForm() {
        // Values 2, 4, 4, 4, 5, 5, 7, 9: mean 5, population variance 4, stddev 2.
        ChannelStats s = StatsCalculator.fromValues(List.of(2.0, 4.0, 4.0, 4.0, 5.0, 5.0, 7.0, 9.0));

        assertEquals(5.0, s.mean(), 1e-9);
        assertEquals(2.0, s.stdDev(), 1e-9);
    }

    @Test
    void percentileUsesNearestRank() {
        List<Double> sorted = List.of(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0);

        assertEquals(5.0, StatsCalculator.percentile(sorted, 50));
        assertEquals(10.0, StatsCalculator.percentile(sorted, 99));
        assertEquals(1.0, StatsCalculator.percentile(sorted, 0));
        assertEquals(10.0, StatsCalculator.percentile(sorted, 100));
    }

    private static Path sampleLog() throws Exception {
        return Path.of(StatsCalculatorTest.class.getResource("/sample_flight.csv").toURI());
    }
}
