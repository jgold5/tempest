package com.github.jgold5.tempest.core.http;

public class HttpRequestResult {

    public enum ErrorType {
        NONE,
        CONNECTION_REFUSED,
        CONNECTION_TIMEOUT,
        READ_TIMEOUT,
        UNKNOWN
    }

    private final int statusCode;
    private final long responseTimeMs;
    private final boolean success;
    private final ErrorType errorType;

    public HttpRequestResult(int statusCode, long responseTimeMs, boolean success, ErrorType errorType) {
        this.statusCode = statusCode;
        this.responseTimeMs = responseTimeMs;
        this.success = success;
        this.errorType = errorType;
    }

    public static HttpRequestResult success(int statusCode, long responseTimeMs) {
        return new HttpRequestResult(statusCode, responseTimeMs, true, ErrorType.NONE);
    }

    public static HttpRequestResult failure(int statusCode, long responseTimeMs, ErrorType errorType) {
        return new HttpRequestResult(statusCode, responseTimeMs, false, errorType);
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
}
