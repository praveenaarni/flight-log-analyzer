package com.praveena.flightlog.stats;

import com.praveena.flightlog.model.SensorSample;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Computes {@link ChannelStats} for every channel in a flight.
 *
 * <p>Missing cells never reach this class: a channel absent from a {@link SensorSample} simply
 * contributes nothing to that channel's group, so counts reflect only rows that had a value.
 */
public final class StatsCalculator {

    private StatsCalculator() {
    }

    /**
     * One {@link ChannelStats} per channel that had at least one value, keyed by channel name
     * and sorted alphabetically.
     */
    public static Map<String, ChannelStats> compute(List<SensorSample> samples) {
        return samples.stream()
                // Flatten rows into a single stream of (channel, value) pairs.
                .flatMap(sample -> sample.channels().entrySet().stream())
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        TreeMap::new,
                        Collectors.mapping(
                                Map.Entry::getValue,
                                Collectors.collectingAndThen(Collectors.toList(), StatsCalculator::fromValues))));
    }

    /**
     * Reduces one channel's values to its stats. Package-private so tests can exercise the math
     * directly without building a flight log.
     *
     * @param values at least one value; groupingBy never creates an empty group
     */
    static ChannelStats fromValues(List<Double> values) {
        List<Double> sorted = values.stream().sorted().toList();
        int n = sorted.size();
        double mean = sorted.stream().mapToDouble(Double::doubleValue).sum() / n;
        double variance = sorted.stream()
                .mapToDouble(v -> (v - mean) * (v - mean))
                .sum() / n;
        return new ChannelStats(
                n,
                sorted.getFirst(),
                sorted.getLast(),
                mean,
                Math.sqrt(variance),
                percentile(sorted, 50),
                percentile(sorted, 99));
    }

    /** Nearest-rank percentile over an already sorted, non-empty list. */
    static double percentile(List<Double> sorted, double p) {
        int rank = (int) Math.ceil(p / 100.0 * sorted.size());
        int index = Math.clamp(rank - 1, 0, sorted.size() - 1);
        return sorted.get(index);
    }
}
