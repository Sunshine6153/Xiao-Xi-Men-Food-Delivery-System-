package com.sky.utils;

import org.apache.http.HttpHeaders;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.http.client.utils.URIBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class HttpClientUtils {

    private static final int CONNECT_TIMEOUT = 5_000;
    private static final int SOCKET_TIMEOUT = 10_000;

    private HttpClientUtils() {
    }

    public static String doGet(String url) {
        return doGet(url, Collections.emptyMap(), Collections.emptyMap());
    }

    public static String doGet(String url, Map<String, String> params) {
        return doGet(url, params, Collections.emptyMap());
    }

    public static String doGet(String url, Map<String, String> params,
                               Map<String, String> headers) {
        try {
            URIBuilder uriBuilder = new URIBuilder(url);
            addParameters(uriBuilder, params);
            HttpGet request = new HttpGet(uriBuilder.build());
            addHeaders(request, headers);
            return execute(request);
        } catch (Exception e) {
            throw new IllegalStateException("GET 请求失败: " + url, e);
        }
    }

    public static String doPost(String url) {
        return doPost(url, Collections.emptyMap(), Collections.emptyMap());
    }

    public static String doPost(String url, Map<String, String> params) {
        return doPost(url, params, Collections.emptyMap());
    }

    public static String doPost(String url, Map<String, String> params,
                                Map<String, String> headers) {
        try {
            HttpPost request = new HttpPost(url);
            List<NameValuePair> pairs = new ArrayList<>();
            params.forEach((key, value) -> pairs.add(new BasicNameValuePair(key, value)));
            request.setEntity(new UrlEncodedFormEntity(pairs, StandardCharsets.UTF_8));
            addHeaders(request, headers);
            return execute(request);
        } catch (Exception e) {
            throw new IllegalStateException("POST 表单请求失败: " + url, e);
        }
    }

    public static String doPostJson(String url, String json) {
        return doPostJson(url, json, Collections.emptyMap());
    }

    public static String doPostJson(String url, String json,
                                    Map<String, String> headers) {
        try {
            HttpPost request = new HttpPost(url);
            request.setEntity(new StringEntity(json == null ? "" : json,
                    ContentType.APPLICATION_JSON));
            addHeaders(request, headers);
            return execute(request);
        } catch (Exception e) {
            throw new IllegalStateException("POST JSON 请求失败: " + url, e);
        }
    }

    private static String execute(HttpUriRequest request) throws IOException {
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(CONNECT_TIMEOUT)
                .setConnectionRequestTimeout(CONNECT_TIMEOUT)
                .setSocketTimeout(SOCKET_TIMEOUT)
                .build();
        ((HttpRequestBase) request).setConfig(config);

        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(request)) {
            int statusCode = response.getStatusLine().getStatusCode();
            String body = response.getEntity() == null
                    ? ""
                    : EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            if (statusCode < 200 || statusCode >= 300) {
                throw new IllegalStateException("HTTP " + statusCode + ": " + body);
            }
            return body;
        }
    }

    private static void addParameters(URIBuilder builder, Map<String, String> params) {
        params.forEach((key, value) -> builder.addParameter(key, value));
    }

    private static void addHeaders(HttpUriRequest request, Map<String, String> headers) {
        headers.forEach(request::setHeader);
        if (!request.containsHeader(HttpHeaders.ACCEPT)) {
            request.setHeader(HttpHeaders.ACCEPT, "application/json, text/plain, */*");
        }
    }
}
