package com.github.jgold5.tempest.core.http;

import java.util.Map;

public class HttpRequestConfig {

    private final Method method;
    private final String url;
    private final Map<String, String> headers;
    private final String body;

    private HttpRequestConfig(Method method, String url, Map<String, String> headers, String body) {
        this.method = method;
        this.url = url;
        this.headers = headers;
        this.body = body;
    }

    public static HttpRequestConfig get(String url, Map<String, String> headers) {
        return new HttpRequestConfig(Method.GET, url, headers, null);
    }

    public static HttpRequestConfig post(String url, Map<String, String> headers, String body) {
        return new HttpRequestConfig(Method.POST, url, headers, body);
    }

    public static HttpRequestConfig put(String url, Map<String, String> headers, String body) {
        return new HttpRequestConfig(Method.PUT, url, headers, body);
    }

    public static HttpRequestConfig patch(String url, Map<String, String> headers, String body) {
        return new HttpRequestConfig(Method.PATCH, url, headers, body);
    }

    public static HttpRequestConfig delete(String url, Map<String, String> headers, String body) {
        return new HttpRequestConfig(Method.DELETE, url, headers, body);
    }

    public Method getMethod() {
        return method;
    }

    public String getUrl() {
        return url;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }

    public enum Method {
        GET,
        POST,
        PUT,
        PATCH,
        DELETE
    }
}
