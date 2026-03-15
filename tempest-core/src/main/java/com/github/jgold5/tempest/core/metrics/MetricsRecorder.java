package com.github.jgold5.tempest.core.metrics;

import com.github.jgold5.tempest.core.http.HttpRequestResult;
import org.HdrHistogram.Recorder;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class MetricsRecorder {
    private final Recorder recorder;
    private final AtomicLong totalCount;
    private final AtomicLong errorCount;

    public MetricsRecorder() {
        this.recorder = new Recorder(TimeUnit.SECONDS.toNanos(60), 3);
        totalCount = new AtomicLong(0);
        errorCount = new AtomicLong(0);
    }

    public void record(HttpRequestResult result) {
        recorder.recordValue(result.getResponseTimeMs());
        if (!result.isSuccess()) {
           errorCount.incrementAndGet();
        }
        totalCount.incrementAndGet();
    }

    public MetricsSnapshot getSnapshot() {
        return MetricsSnapshot.snapshot(totalCount.get(), errorCount.get(), recorder.getIntervalHistogram());
    }
}