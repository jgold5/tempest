package com.github.jgold5.tempest.core;

import com.github.jgold5.tempest.core.http.HttpRequestConfig;
import com.github.jgold5.tempest.core.http.HttpRequestExecutor;
import com.github.jgold5.tempest.core.http.HttpRequestResult;
import com.github.jgold5.tempest.core.metrics.MetricsRecorder;
import com.github.jgold5.tempest.core.ratelimit.TokenBucket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

public class LoadGenerator {

  private final HttpRequestConfig requestConfig;
  private final LoadTestConfig loadTestConfig;
  private final MetricsRecorder metricsRecorder;

  //  private static final Logger log = LoggerFactory.getLogger(LoadGenerator.class);

  public LoadGenerator(
      HttpRequestConfig requestConfig,
      LoadTestConfig loadTestConfig,
      MetricsRecorder metricsRecorder) {
    this.requestConfig = requestConfig;
    this.loadTestConfig = loadTestConfig;
    this.metricsRecorder = metricsRecorder;
  }

  public void run() {
    //    log.info(
    //        "Starting load test: {} RPS for {}s",
    //        loadTestConfig.getTargetRps(),
    //        loadTestConfig.getDuration().toSeconds());
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
      //      log.error("Load test interrupted", e);
    }
    //    log.info("Load test complete");
  }
}
