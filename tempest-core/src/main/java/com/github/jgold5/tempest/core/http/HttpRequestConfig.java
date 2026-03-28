package com.github.jgold5.tempest.core.http;

import java.util.Map;

/**
 * Configuration settings of an HTTP request.
 *
 * <p>Use one of the provided static factory methods to craft the type of request to be sent.
 *
 * <pre>{@code
 * HttpRequestConfig getRequestConfig = HttpRequestConfig.get("http://localhost:8080/api", null);
 * HttpRequestResult result = executor.execute(getRequestConfig);
 * }</pre>
 */
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

  /**
   * Generates a config for a GET request.
   *
   * @param url the target URL of the request
   * @param headers a map of headers provided in the request
   * @return the config for the request
   */
  public static HttpRequestConfig get(String url, Map<String, String> headers) {
    return new HttpRequestConfig(Method.GET, url, headers, null);
  }

  /**
   * Generates a config for a POST request.
   *
   * @param url the target URL of the request
   * @param headers a map of headers provided in the request
   * @param body a string representation of the request body
   * @return the config for the request
   */
  public static HttpRequestConfig post(String url, Map<String, String> headers, String body) {
    return new HttpRequestConfig(Method.POST, url, headers, body);
  }

  /**
   * Generates a config for a PUT request.
   *
   * @param url the target URL of the request
   * @param headers a map of headers provided in the request
   * @param body a string representation of the request body
   * @return the config for the request
   */
  public static HttpRequestConfig put(String url, Map<String, String> headers, String body) {
    return new HttpRequestConfig(Method.PUT, url, headers, body);
  }

  /**
   * Generates a config for a PATCH request.
   *
   * @param url the target URL of the request
   * @param headers a map of headers provided in the request
   * @param body a string representation of the request body
   * @return the config for the request
   */
  public static HttpRequestConfig patch(String url, Map<String, String> headers, String body) {
    return new HttpRequestConfig(Method.PATCH, url, headers, body);
  }

  /**
   * Generates a config for a DELETE request.
   *
   * @param url the target URL of the request
   * @param headers a map of headers provided in the request
   * @param body a string representation of the request body
   * @return the config for the request
   */
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
