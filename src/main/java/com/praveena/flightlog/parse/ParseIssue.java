package com.praveena.flightlog.parse;

/**
 * A row the reader could not fully use. The reader never throws on bad data; it reports the
 * problem here and keeps going, because one corrupt line should not discard a whole flight.
 *
 * @param lineNumber 1-based line number in the file (the header is line 1)
 * @param reason     human-readable explanation
 */
public record ParseIssue(long lineNumber, String reason) {
}
