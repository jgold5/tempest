package com.github.jgold5.tempest.core.metrics;

import org.HdrHistogram.Histogram;

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

  public static MetricsSnapshot snapshot(
      long totalCount, long errorCount, Histogram intervalHistogram) {
    long timestamp = System.currentTimeMillis();
    return new MetricsSnapshot(totalCount, errorCount, timestamp, intervalHistogram);
  }

  public long getTimestamp() {
    return timestamp;
  }

  public long getTotalCount() {
    return totalCount;
  }

  public long getErrorCount() {
    return errorCount;
  }

  public Histogram getIntervalHistogram() {
    return intervalHistogram;
  }
}
