package com.github.jgold5.tempest.core.http;

public class HttpRequestResult {

  public enum ErrorType {
    NONE,
    CONNECTION_REFUSED,
    CONNECTION_TIMEOUT,
    READ_TIMEOUT,
    UNKNOWN,
    HTTP_ERROR
  }

  private final int statusCode;
  private final long responseTimeMs;
  private final boolean success;
  private final ErrorType errorType;
  private final String errorMessage;

  private HttpRequestResult(
      int statusCode,
      long responseTimeMs,
      boolean success,
      ErrorType errorType,
      String errorMessage) {
    this.statusCode = statusCode;
    this.responseTimeMs = responseTimeMs;
    this.success = success;
    this.errorType = errorType;
    this.errorMessage = errorMessage;
  }

  public static HttpRequestResult success(int statusCode, long responseTimeMs) {
    return new HttpRequestResult(statusCode, responseTimeMs, true, ErrorType.NONE, null);
  }

  public static HttpRequestResult failure(
      int statusCode, long responseTimeMs, ErrorType errorType) {
    return new HttpRequestResult(statusCode, responseTimeMs, false, errorType, null);
  }

  public static HttpRequestResult failureUnknown(long responseTimeMs, String errorMessage) {
    return new HttpRequestResult(0, responseTimeMs, false, ErrorType.UNKNOWN, errorMessage);
  }

  public static HttpRequestResult failureNoResponse(long responseTimeMs, ErrorType errorType) {
    return new HttpRequestResult(0, responseTimeMs, false, errorType, null);
  }

  public boolean isSuccess() {
    return success;
  }

  public long getResponseTimeMs() {
    return responseTimeMs;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public ErrorType getErrorType() {
    return errorType;
  }

  public String getErrorMessage() {
    return errorMessage;
  }
}
