package com.praveena.flightlog.model;

/**
 * Something a detector found worth flagging.
 *
 * <p>This is a sealed interface so that report writers can switch over every kind of anomaly
 * exhaustively; adding a new variant here forces every switch to handle it at compile time.
 * Add one record per detector you write (Spike, Flatline, Dropout, RateOfChange, ...).
 */
public sealed interface Anomaly permits Anomaly.LimitViolation {

    /** Channel the anomaly was found on, or {@code "timestamp"} for timing problems. */
    String channel();

    /** Timestamp of the sample where the anomaly was detected. */
    long timestampMicros();

    /** A value went outside the configured [min, max] bounds for its channel. */
    record LimitViolation(String channel, long timestampMicros, double value, double min, double max)
            implements Anomaly {
    }
}
