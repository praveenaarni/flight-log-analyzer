# Flight Log Analyzer

Reads CSV flight logs from drones and rockets, computes per-channel statistics, and flags
anomalies such as limit violations, sensor spikes, flatlined sensors, and data dropouts.
Written in modern Java (21) with records, sealed interfaces, and the Streams API.

## Status

Scaffold. The streaming CSV reader, core record types, detector interface, and CLI are in
place with tests. Statistics, detectors, flight-phase segmentation, and report writers are
next. See the plan below.

## Quick start

    export JAVA_HOME=/opt/homebrew/opt/openjdk@21   # if `java -version` is not 17+
    ./gradlew build
    ./gradlew run --args="src/test/resources/sample_flight.csv"

## Design

- `model.SensorSample` is one row: a timestamp plus the channels that had values. Missing
  cells are absent rather than NaN, so detectors cannot accidentally do math on a gap.
- `parse.CsvFlightLogReader` streams one line at a time and never throws on bad data. Bad rows
  become `ParseIssue`s so a single corrupt line does not discard a whole flight.
- `model.Anomaly` is a sealed interface with one record per kind of finding. Report writers
  switch over it exhaustively, so adding a detector without updating the report is a compile
  error.
- `detect.AnomalyDetector` is the plug-in point. Detectors are stateless and get the full
  ordered sample list, since spike and dropout checks need neighbouring rows.

## Plan

- [x] Streaming CSV reader with issue reporting
- [ ] Per-channel stats (min, max, mean, stddev, percentiles) via custom collectors
- [ ] Limit detector driven by a per-channel bounds file
- [ ] Spike detector (rolling z-score)
- [ ] Flatline detector
- [ ] Dropout detector (timestamp gaps)
- [ ] Rate-of-change detector
- [ ] Flight phase segmentation (pad, ascent, cruise, descent, landed)
- [ ] Markdown and JSON report writers
- [ ] Run on real PX4 logs; see `data/README.md`

## Requirements

- JDK 17 or newer on `JAVA_HOME` to launch Gradle. The project itself compiles with a JDK 21
  toolchain, which Gradle downloads via the Foojay resolver if none is installed.
- On macOS: `brew install openjdk@21`, then `export JAVA_HOME=/opt/homebrew/opt/openjdk@21`.
- The Gradle wrapper (`./gradlew`) is committed, so no separate Gradle install is needed.
