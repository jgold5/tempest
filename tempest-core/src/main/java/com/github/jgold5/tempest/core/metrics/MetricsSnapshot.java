package com.github.jgold5.tempest.core.metrics;

import org.HdrHistogram.Histogram;

/**
 * An immutable snapshot of load test metrics captured at a point in time.
 *
 * <p>Snapshots are produced by {@link MetricsRecorder#getSnapshot()} and represent the metrics
 * collected during a single reporting interval. Each snapshot contains request counts, error
 * counts, and a latency histogram that can be queried for percentiles.
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * MetricsSnapshot snapshot = recorder.getSnapshot();
 * long p99 = snapshot.getIntervalHistogram().getValueAtPercentile(99.0);
 * }</pre>
 */
public class MetricsSnapshot {

  private final long timestamp;
  private final long totalCount;
  private final long errorCount;
  private final Histogram intervalHistogram;

  private MetricsSnapshot(
      long totalCount, long errorCount, long timestamp, Histogram intervalHistogram) {
    this.totalCount = totalCount;
    this.errorCount = errorCount;
    this.timestamp = timestamp;
    this.intervalHistogram = intervalHistogram;
  }

  /**
   * Creates a new metrics snapshot with the current timestamp.
   *
   * @param totalCount the total requests submitted during the snapshot interval
   * @param errorCount the number of errors observed during the snapshot interval
   * @param intervalHistogram the latency histogram
   * @return the snapshot
   */
  public static MetricsSnapshot snapshot(
      long totalCount, long errorCount, Histogram intervalHistogram) {
    long timestamp = System.currentTimeMillis();
    return new MetricsSnapshot(totalCount, errorCount, timestamp, intervalHistogram);
  }

  /**
   * @return the epoch milliseconds when this snapshot was taken
   */
  public long getTimestamp() {
    return timestamp;
  }

  public long getTotalCount() {
    return totalCount;
  }

  public long getErrorCount() {
    return errorCount;
  }

  /**
   * @return the latency histogram for this interval, queryable for percentiles
   */
  public Histogram getIntervalHistogram() {
    return intervalHistogram;
  }
}
