package com.github.jgold5.tempest.core;

import com.github.jgold5.tempest.core.http.HttpRequestConfig;
import com.github.jgold5.tempest.core.http.HttpRequestExecutor;
import com.github.jgold5.tempest.core.http.HttpRequestResult;
import com.github.jgold5.tempest.core.metrics.MetricsRecorder;
import com.github.jgold5.tempest.core.ratelimit.TokenBucket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates HTTP load against a target endpoint at a configured rate and duration.
 *
 * <p>Each request is dispatched on its own virtual thread via {@link
 * Executors#newVirtualThreadPerTaskExecutor()}, allowing many in-flight requests concurrently
 * without the overhead of a traditional thread pool. Request throughput is controlled by a {@link
 * TokenBucket} rate limiter. Results are recorded into a {@link MetricsRecorder} for later
 * analysis, with requests during the warmup period excluded from recording.
 *
 * <p>Call {@link #run()} to execute the load test. This method blocks until the configured duration
 * has elapsed or the thread is interrupted.
 */
public class LoadGenerator {

  private final HttpRequestConfig requestConfig;
  private final LoadTestConfig loadTestConfig;
  private final MetricsRecorder metricsRecorder;

  private static final Logger log = LoggerFactory.getLogger(LoadGenerator.class);

  /**
   * Initializes a LoadGenerator with desired behavior of the load test.
   *
   * @param requestConfig the type of HTTP request used
   * @param loadTestConfig configuration defining the target RPS, duration, and warmup behavior of
   *     the test
   * @param metricsRecorder where the results of each request is written to
   */
  public LoadGenerator(
      HttpRequestConfig requestConfig,
      LoadTestConfig loadTestConfig,
      MetricsRecorder metricsRecorder) {
    this.requestConfig = requestConfig;
    this.loadTestConfig = loadTestConfig;
    this.metricsRecorder = metricsRecorder;
  }

  /**
   * Executes the load test, blocking until the configured duration has elapsed or the thread is
   * interrupted. Requests fired during the warmup period are excluded from {@link MetricsRecorder}.
   * If interrupted, the executor is shut down immediately and the interrupted flag is restored.
   */
  public void run() {
    log.info(
        "Starting load test: {} RPS for {}s",
        loadTestConfig.getTargetRps(),
        loadTestConfig.getDuration().toSeconds());
    TokenBucket tokenBucket =
        new TokenBucket(loadTestConfig.getTargetRps(), loadTestConfig.getTargetRps());
    long startTime = System.nanoTime();
    long durationNanos = loadTestConfig.getDuration().toNanos();
    ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    try (executor) {
      HttpRequestExecutor httpRequestExecutor = new HttpRequestExecutor();
      while (System.nanoTime() - startTime < durationNanos) {
        tokenBucket.acquire();
        executor.submit(
            () -> {
              HttpRequestResult result = httpRequestExecutor.execute(requestConfig);
              if (System.nanoTime() - startTime > loadTestConfig.getWarmupDuration().toNanos()) {
                metricsRecorder.recordResult(result);
              }
            });
      }
    } catch (InterruptedException e) {
      executor.shutdownNow();
      Thread.currentThread().interrupt(); // re-set the interrupted flag
      log.error("Load test interrupted", e);
    }
    log.info("Load test complete");
  }
}
