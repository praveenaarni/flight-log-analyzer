package com.praveena.flightlog.stats;

/**
 * Summary of one channel over a whole flight.
 *
 * <p>Standard deviation is the population form (divide by n), since the log is the whole flight,
 * not a sample of it. Percentiles use nearest-rank.
 *
 * @param count  number of rows where this channel had a value
 * @param min    smallest value seen
 * @param max    largest value seen
 * @param mean   arithmetic mean
 * @param stdDev population standard deviation
 * @param p50    median (50th percentile, nearest-rank)
 * @param p99    99th percentile (nearest-rank)
 */
public record ChannelStats(long count, double min, double max, double mean,
                           double stdDev, double p50, double p99) {
}
