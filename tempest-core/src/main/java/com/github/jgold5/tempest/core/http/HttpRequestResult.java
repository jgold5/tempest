package com.github.jgold5.tempest.core.http;

/**
 * A representation of the result of submitting an HTTP request.
 *
 * <p>Results are produced by {@link HttpRequestExecutor#execute(HttpRequestConfig)} and contain the
 * response status, latency, and error information.
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * HttpRequestResult result = executor.execute(config);
 * if (result.isSuccess()) {
 *     long latency = result.getResponseTimeNanos();
 * } else {
 *     HttpRequestResult.ErrorType error = result.getErrorType();
 * }
 * }</pre>
 */
public class HttpRequestResult {

  /**
   * Potential errors observed from the request submission. Covers instances with no errors, HTTP
   * specific errors, connection related errors, and any other error that may occur
   */
  public enum ErrorType {
    NONE,
    CONNECTION_REFUSED,
    CONNECTION_TIMEOUT,
    READ_TIMEOUT,
    UNKNOWN,
    HTTP_ERROR
  }

  private final int statusCode;
  private final long responseTimeNanos;
  private final boolean success;
  private final ErrorType errorType;
  private final String errorMessage;

  private HttpRequestResult(
      int statusCode,
      long responseTimeNanos,
      boolean success,
      ErrorType errorType,
      String errorMessage) {
    this.statusCode = statusCode;
    this.responseTimeNanos = responseTimeNanos;
    this.success = success;
    this.errorType = errorType;
    this.errorMessage = errorMessage;
  }

  /**
   * An instance of a successful response
   *
   * @param statusCode the response status
   * @param responseTimeNanos the time of the response in nanoseconds
   * @return result with the status and time of the response
   */
  public static HttpRequestResult success(int statusCode, long responseTimeNanos) {
    return new HttpRequestResult(statusCode, responseTimeNanos, true, ErrorType.NONE, null);
  }

  /**
   * An instance of a response where either an HTTP error or a connection failure has occurred
   *
   * @param statusCode the response status
   * @param responseTimeNanos the time of the response in nanoseconds
   * @param errorType the type of error observed
   * @return result with the status, time, and type of error observed
   */
  public static HttpRequestResult failure(
      int statusCode, long responseTimeNanos, ErrorType errorType) {
    return new HttpRequestResult(statusCode, responseTimeNanos, false, errorType, null);
  }

  /**
   * An instance of a response where an unknown error has occurred
   *
   * @param responseTimeNanos the time of the response in nanoseconds
   * @param errorMessage the error message from the exception
   * @return result with the status, time, and message describing the failure
   */
  public static HttpRequestResult failureUnknown(long responseTimeNanos, String errorMessage) {
    return new HttpRequestResult(0, responseTimeNanos, false, ErrorType.UNKNOWN, errorMessage);
  }

  /**
   * An instance of a response where no response was received
   *
   * @param responseTimeNanos the time of the response in nanoseconds
   * @param errorType the type of error observed
   * @return result with the status, time, and type of error observed
   */
  public static HttpRequestResult failureNoResponse(long responseTimeNanos, ErrorType errorType) {
    return new HttpRequestResult(0, responseTimeNanos, false, errorType, null);
  }

  /**
   * @return true if the request completed with a 2xx response, false otherwise
   */
  public boolean isSuccess() {
    return success;
  }

  public long getResponseTimeNanos() {
    return responseTimeNanos;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public ErrorType getErrorType() {
    return errorType;
  }

  /**
   * @return the error message from the exception, or null if the error type is not {@link
   *     ErrorType#UNKNOWN}
   */
  public String getErrorMessage() {
    return errorMessage;
  }
}
