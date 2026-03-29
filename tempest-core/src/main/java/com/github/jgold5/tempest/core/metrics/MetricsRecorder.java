package com.github.jgold5.tempest.core.metrics;

import com.github.jgold5.tempest.core.http.HttpRequestResult;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.HdrHistogram.Recorder;

/**
 * Accumulates request results from concurrent virtual threads and produces interval {@link
 * MetricsSnapshot snapshots} on demand.
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * MetricsRecorder recorder = new MetricsRecorder();
 * HttpRequestResult result = HttpRequestResult.success(200, 100L);
 * recorder.recordResult(result);
 * MetricsSnapshot snapshot = recorder.getSnapshot();
 * }</pre>
 */
public class MetricsRecorder {
  private final Recorder recorder;
  private final AtomicLong totalCount;
  private final AtomicLong errorCount;

  public MetricsRecorder() {
    this.recorder = new Recorder(TimeUnit.SECONDS.toNanos(60), 3);
    totalCount = new AtomicLong(0);
    errorCount = new AtomicLong(0);
  }

  /** Records the {@link HttpRequestResult result} of a single HTTP request. */
  public void recordResult(HttpRequestResult result) {
    recorder.recordValue(result.getResponseTimeNanos());
    if (!result.isSuccess()) {
      errorCount.incrementAndGet();
    }
    totalCount.incrementAndGet();
  }

  /**
   * Returns a snapshot of the metrics accumulated since the last call to this method, then resets
   * the interval histogram for the next interval.
   *
   * @return an immutable snapshot of the current metrics
   */
  public MetricsSnapshot getSnapshot() {
    return MetricsSnapshot.snapshot(
        totalCount.get(), errorCount.get(), recorder.getIntervalHistogram());
  }
}
