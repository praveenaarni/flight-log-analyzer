# Flight data

Real logs go here and are git-ignored (they are large). The committed sample used by the
tests lives in `src/test/resources/sample_flight.csv`.

## Getting real PX4 logs

1. Open https://review.px4.io and pick any public flight.
2. Near the top of the log page, use the download menu to export as CSV. Each export is one
   uORB topic; `vehicle_local_position`, `sensor_combined`, and `battery_status` are good
   starting points.
3. Save the file here, then run:

   ./gradlew run --args="data/your_log.csv"

The reader expects a header row with a `timestamp` column in microseconds, which is what
PX4 exports.
