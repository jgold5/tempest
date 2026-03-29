package com.github.jgold5.tempest.core;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.within;

import com.github.jgold5.tempest.core.http.HttpRequestConfig;
import com.github.jgold5.tempest.core.metrics.MetricsRecorder;
import com.github.jgold5.tempest.core.metrics.MetricsSnapshot;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class LoadGeneratorTest {

  @RegisterExtension
  static WireMockExtension wireMock =
      WireMockExtension.newInstance()
          .options(WireMockConfiguration.wireMockConfig().dynamicPort())
          .build();

  @Test
  void shouldSubmitExpectedRequests() {
    // arrange
    wireMock.stubFor(get("/test").willReturn(aResponse().withStatus(200)));
    HttpRequestConfig requestConfig = HttpRequestConfig.get(wireMock.baseUrl() + "/test", null);
    LoadTestConfig loadTestConfig =
        new LoadTestConfig.Builder().targetRps(10).duration(Duration.ofSeconds(2)).build();
    MetricsRecorder metricsRecorder = new MetricsRecorder();
    LoadGenerator loadGenerator = new LoadGenerator(requestConfig, loadTestConfig, metricsRecorder);
    // act
    loadGenerator.run();
    // assert
    wireMock.verify(moreThanOrExactly(18), getRequestedFor(urlEqualTo("/test")));
    wireMock.verify(lessThanOrExactly(22), getRequestedFor(urlEqualTo("/test")));
  }

  @Test
  void shouldIgnoreRequestsDuringWarmupPeriod() {
    // arrange
    wireMock.stubFor(get("/test").willReturn(aResponse().withStatus(200)));
    HttpRequestConfig requestConfig = HttpRequestConfig.get(wireMock.baseUrl() + "/test", null);
    LoadTestConfig loadTestConfig =
        new LoadTestConfig.Builder()
            .targetRps(10)
            .duration(Duration.ofSeconds(2))
            .warmupDuration(Duration.ofSeconds(1))
            .build();
    MetricsRecorder metricsRecorder = new MetricsRecorder();
    LoadGenerator loadGenerator = new LoadGenerator(requestConfig, loadTestConfig, metricsRecorder);
    // act
    loadGenerator.run();
    // assert

    MetricsSnapshot snapshot = metricsRecorder.getSnapshot();
    assertThat(snapshot.getTotalCount()).isCloseTo(10, within(2L));
  }

  @Test
  void shouldHandleInterruption() throws InterruptedException {
    // arrange
    wireMock.stubFor(get("/test").willReturn(aResponse().withStatus(200)));
    HttpRequestConfig requestConfig = HttpRequestConfig.get(wireMock.baseUrl() + "/test", null);
    LoadTestConfig loadTestConfig =
        new LoadTestConfig.Builder()
            .targetRps(10)
            .duration(Duration.ofSeconds(30)) // long enough that interrupt fires first
            .build();
    MetricsRecorder metricsRecorder = new MetricsRecorder();
    LoadGenerator loadGenerator = new LoadGenerator(requestConfig, loadTestConfig, metricsRecorder);

    // act
    Thread thread = Thread.ofPlatform().start(loadGenerator::run);
    Thread.sleep(200); // let it get going
    thread.interrupt();
    thread.join(); // wait for it to finish

    // assert
    assertThat(thread.isInterrupted()).isTrue();
  }
}
