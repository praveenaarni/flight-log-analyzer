package com.praveena.flightlog.model;

import java.util.Map;
import java.util.OptionalDouble;

/**
 * One row of a flight log: a timestamp plus every numeric channel that had a value on that row.
 *
 * <p>Channels with a missing or unparsable cell are simply absent from {@code channels}, so
 * detectors must use {@link #value(String)} rather than assuming every channel is present.
 *
 * @param timestampMicros microseconds since the start of the log (PX4 convention)
 * @param channels        channel name to value, immutable
 */
public record SensorSample(long timestampMicros, Map<String, Double> channels) {

    public SensorSample {
        channels = Map.copyOf(channels);
    }

    /** The value of {@code channel} on this row, or empty if the cell was missing. */
    public OptionalDouble value(String channel) {
        Double v = channels.get(channel);
        return v == null ? OptionalDouble.empty() : OptionalDouble.of(v);
    }

    public double timestampSeconds() {
        return timestampMicros / 1_000_000.0;
    }
}
