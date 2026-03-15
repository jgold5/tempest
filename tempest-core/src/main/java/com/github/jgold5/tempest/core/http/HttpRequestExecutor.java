package com.github.jgold5.tempest.core.http;

import okhttp3.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class HttpRequestExecutor {
    private final OkHttpClient client;

    public HttpRequestExecutor() {
        client = new OkHttpClient();
    }

    /*
     * TODO: Use Builder to construct the Request. Then finish handling the respone
     *  Successful vs not successful
     *
     * public HttpRequestResult execute(HttpRequestConfig config) {
     *    Request.Builder request = new Request.Builder();
     *    Request request = new Request(HttpUrl.parse(config.getUrl()), Headers.of(config.getHeaders()), config.getMethod().name(), RequestBody.create(config.getBody().getBytes(StandardCharsets.UTF_8)));
     *    long now = System.nanoTime();
     *    try (Response response = client.newCall(request).execute()) {
     *        long done = System.nanoTime();
     *        response.isSuccessful();
     *    } catch (IOException e) {
     *        long done = System.nanoTime();
     *    }
     *    return null; //TODO: return actual result
     *}
     */

}