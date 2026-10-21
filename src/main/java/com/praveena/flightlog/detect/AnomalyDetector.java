package com.praveena.flightlog.detect;

import com.praveena.flightlog.model.Anomaly;
import com.praveena.flightlog.model.SensorSample;

import java.util.List;

/**
 * A single kind of check over a whole flight. Detectors are stateless and pluggable: the CLI
 * builds a list of them from configuration and runs each over the same samples.
 *
 * <p>Samples are passed as a {@link List} rather than a {@code Stream} because several detectors
 * (spike, dropout) need to look at neighbouring rows, which a one-pass stream makes awkward.
 */
public interface AnomalyDetector {

    /** Short name used in reports, e.g. {@code "limit"}. */
    String name();

    /** Runs the check over samples ordered by timestamp. Must not mutate the list. */
    List<Anomaly> detect(List<SensorSample> samples);
}
