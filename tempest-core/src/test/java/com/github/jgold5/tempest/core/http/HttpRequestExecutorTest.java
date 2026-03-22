package com.github.jgold5.tempest.core.http;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class HttpRequestExecutorTest {

  @RegisterExtension
  static WireMockExtension wireMock =
      WireMockExtension.newInstance()
          .options(WireMockConfiguration.wireMockConfig().dynamicPort())
          .build();

  @Test
  void shouldReturnSuccessOnOkResponse() {
    // arrange
    wireMock.stubFor(get("/test").willReturn(aResponse().withStatus(200)));
    HttpRequestExecutor executor = new HttpRequestExecutor();
    HttpRequestConfig config = HttpRequestConfig.get(wireMock.baseUrl() + "/test", null);
    // act
    HttpRequestResult result = executor.execute(config);
    // assert
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getStatusCode()).isEqualTo(200);
  }

  @Test
  void shouldReturnHTTPErrorOn500Response() {
    // arrange
    wireMock.stubFor(get("/test").willReturn(aResponse().withStatus(500)));
    HttpRequestExecutor executor = new HttpRequestExecutor();
    HttpRequestConfig config = HttpRequestConfig.get(wireMock.baseUrl() + "/test", null);
    // act
    HttpRequestResult result = executor.execute(config);
    // assert
    assertThat(result.isSuccess()).isFalse();
    assertThat(result.getStatusCode()).isEqualTo(500);
    assertThat(result.getErrorType()).isEqualTo(HttpRequestResult.ErrorType.HTTP_ERROR);
  }

  @Test
  void shouldReturnConnectionRefusedWhenNoServerListening() {
    HttpRequestExecutor executor = new HttpRequestExecutor();
    HttpRequestConfig config = HttpRequestConfig.get("http://localhost:19999/test", null);
    HttpRequestResult result = executor.execute(config);
    assertThat(result.isSuccess()).isFalse();
    assertThat(result.getErrorType()).isEqualTo(HttpRequestResult.ErrorType.CONNECTION_REFUSED);
  }

  @Test
  void shouldReturnReadTimeoutWhenServerIsUnresponsive() {
    // arrange
    wireMock.stubFor(get("/timeout").willReturn(aResponse().withFixedDelay(1100)));
    HttpRequestExecutor executor = new HttpRequestExecutor(1, 1, 1);
    HttpRequestConfig config = HttpRequestConfig.get(wireMock.baseUrl() + "/timeout", null);
    // act
    HttpRequestResult result = executor.execute(config);
    // assert
    assertThat(result.isSuccess()).isFalse();
    assertThat(result.getErrorType()).isEqualTo(HttpRequestResult.ErrorType.READ_TIMEOUT);
  }
}
