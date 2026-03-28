package com.github.jgold5.tempest.core.http;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.github.jgold5.tempest.core.metrics.MetricsRecorder;
import com.github.jgold5.tempest.core.metrics.MetricsSnapshot;
import java.util.concurrent.CountDownLatch;
import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.Test;

class MetricsRecorderTest {

  @Test
  void shouldRecordSingleSuccessfulTest() {
    MetricsRecorder recorder = new MetricsRecorder();
    HttpRequestResult result = HttpRequestResult.success(200, 100L);

    recorder.recordResult(result);
    MetricsSnapshot snapshot = recorder.getSnapshot();

    assertThat(snapshot.getTotalCount()).isEqualTo(1);
    assertThat(snapshot.getErrorCount()).isZero();
  }

  @Test
  void shouldKeepTrackOfSuccessesAndFailures() {
    MetricsRecorder recorder = new MetricsRecorder();
    HttpRequestResult success1 = HttpRequestResult.success(200, 100L);
    HttpRequestResult fail1 =
        HttpRequestResult.failure(500, 5L, HttpRequestResult.ErrorType.HTTP_ERROR);

    recorder.recordResult(success1);
    recorder.recordResult(fail1);
    MetricsSnapshot snapshot = recorder.getSnapshot();

    assertThat(snapshot.getTotalCount()).isEqualTo(2);
    assertThat(snapshot.getErrorCount()).isEqualTo(1);
  }

  @Test
  void shouldReflectExpectedLatency() {
    MetricsRecorder recorder = new MetricsRecorder();
    HttpRequestResult success1 = HttpRequestResult.success(200, 0);
    HttpRequestResult failure1 = HttpRequestResult.failureUnknown(100, "Test Error");
    HttpRequestResult failure2 = HttpRequestResult.failureUnknown(900, "Test Error");
    HttpRequestResult success2 = HttpRequestResult.success(200, 50);

    recorder.recordResult(success1);
    recorder.recordResult(failure1);
    recorder.recordResult(failure2);
    recorder.recordResult(success2);
    MetricsSnapshot snapshot = recorder.getSnapshot();

    assertThat(snapshot.getTotalCount()).isEqualTo(4);
    assertThat(snapshot.getErrorCount()).isEqualTo(2);
    assertThat(snapshot.getIntervalHistogram().getValueAtPercentile(99.0))
        .isCloseTo(900L, Percentage.withPercentage(5));
  }

  @Test
  void shouldHandleConcurrentRecording() throws InterruptedException {
    MetricsRecorder recorder = new MetricsRecorder();
    int threadCount = 1000;
    CountDownLatch latch = new CountDownLatch(threadCount);

    for (int i = 0; i < threadCount; i++) {
      Thread.ofVirtual()
          .start(
              () -> {
                recorder.recordResult(HttpRequestResult.success(200, 50));
                latch.countDown();
              });
    }
    latch.await();
    MetricsSnapshot snapshot = recorder.getSnapshot();
    assertThat(snapshot.getTotalCount()).isEqualTo(1000);
    assertThat(snapshot.getErrorCount()).isZero();
  }
}
