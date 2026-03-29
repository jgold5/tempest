package com.github.jgold5.tempest.core.http;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import okhttp3.*;

/**
 * Executes HTTP requests and produces {@link HttpRequestResult results} containing response time
 * and status information.
 *
 * <p>Default timeouts are 10 seconds each for connect, read, and write. Use {@link
 * #HttpRequestExecutor(int, int, int)} to override.
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * HttpRequestExecutor executor = new HttpRequestExecutor();
 * HttpRequestConfig config = HttpRequestConfig.get("http://localhost:8080/api", null);
 * HttpRequestResult result = executor.execute(config);
 * }</pre>
 */
public class HttpRequestExecutor {
  private final OkHttpClient client;

  /** Creates a default executor. */
  public HttpRequestExecutor() {
    this(10, 10, 10);
  }

  /**
   * Creates an executor with non-default values for connect timeout, read timeout, and write
   * timeout.
   *
   * @param connectTimeoutSeconds timeout for establishing a connection, in seconds
   * @param readTimeoutSeconds timeout for reading a response, in seconds
   * @param writeTimeoutSeconds timeout for writing a request, in seconds
   */
  public HttpRequestExecutor(
      int connectTimeoutSeconds, int readTimeoutSeconds, int writeTimeoutSeconds) {
    client =
        new OkHttpClient.Builder()
            .connectTimeout(connectTimeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(writeTimeoutSeconds, TimeUnit.SECONDS)
            .build();
  }

  /**
   * Executes the request based on the {@link HttpRequestConfig config}.
   *
   * @param config the configuration of the request
   * @return the {@link HttpRequestResult result} of submitting the request
   */
  public HttpRequestResult execute(HttpRequestConfig config) {
    RequestBody requestBody =
        config.getBody() == null
            ? null
            : RequestBody.create(config.getBody().getBytes(StandardCharsets.UTF_8));
    Headers headers =
        config.getHeaders() == null
            ? new Headers.Builder().build()
            : Headers.of(config.getHeaders());
    Request request =
        new Request.Builder()
            .headers(headers)
            .url(config.getUrl())
            .method(config.getMethod().toString(), requestBody)
            .build();
    Call call = client.newCall(request);
    long start = System.nanoTime();
    try (Response response = call.execute()) {
      long end = System.nanoTime();
      if (response.isSuccessful()) {
        return HttpRequestResult.success(response.code(), end - start);
      } else {
        return HttpRequestResult.failure(
            response.code(), end - start, HttpRequestResult.ErrorType.HTTP_ERROR);
      }
    } catch (IOException e) {
      long end = System.nanoTime();
      HttpRequestResult.ErrorType errorType = classifyException(e);
      if (errorType == HttpRequestResult.ErrorType.UNKNOWN) {
        return HttpRequestResult.failureUnknown(end - start, e.getMessage());
      }
      return HttpRequestResult.failureNoResponse(end - start, errorType);
    }
  }

  private HttpRequestResult.ErrorType classifyException(IOException e) {
    if (e instanceof java.net.ConnectException) {
      return HttpRequestResult.ErrorType.CONNECTION_REFUSED; // nothing listening on the port
    } else if (e instanceof java.net.SocketTimeoutException) {
      String message = e.getMessage();
      if (message != null && message.contains("connect")) {
        return HttpRequestResult.ErrorType
            .CONNECTION_TIMEOUT; // request doesn't get response at all
      } else {
        return HttpRequestResult.ErrorType
            .READ_TIMEOUT; // server accepted handshake but didn't send response in time
      }
    }
    return HttpRequestResult.ErrorType.UNKNOWN;
  }
}
