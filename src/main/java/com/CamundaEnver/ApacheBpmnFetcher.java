package com.CamundaEnver;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Fetches BPMN XML from a remote HTTP endpoint.
 * This class implements Runnable and Closeable interfaces to allow
 * for execution in a separate thread and proper resource management.
 */
public class ApacheBpmnFetcher implements Runnable, Closeable {
    private final CloseableHttpClient client; // HTTP client for making requests
    private final String url; // URL of the remote endpoint to fetch BPMN XML from
    private final AtomicBoolean closed = new AtomicBoolean(false); // Flag to track if the client is closed

    /**
     * Constructs an ApacheBpmnFetcher with the specified URL, timeout, and maximum retries.
     *
     * @param url        the URL to fetch the BPMN XML from
     * @param timeoutMs  the timeout in milliseconds for the HTTP requests
     * @param maxRetries the maximum number of retries for failed requests
     */
    public ApacheBpmnFetcher(String url, int timeoutMs, int maxRetries) {
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(timeoutMs)
                .setSocketTimeout(timeoutMs)
                .setConnectionRequestTimeout(timeoutMs)
                .build();
        HttpRequestRetryHandler retryHandler = (ex, count, ctx) -> count <= maxRetries && ex instanceof IOException;
        this.client = HttpClients.custom()
                .setDefaultRequestConfig(config)
                .setRetryHandler(retryHandler)
                .build();
        this.url = url;
    }

    /**
     * Fetches the BPMN XML from the specified URL.
     *
     * @return the BPMN XML as a String
     * @throws IOException if an error occurs during the HTTP request or if the response is invalid
     */
    public String fetchXml() throws IOException {
        HttpGet get = new HttpGet(url);
        get.addHeader("Accept", "application/json");
        HttpResponse resp = client.execute(get);
        int code = resp.getStatusLine().getStatusCode();
        if (code != 200) throw new IOException("HTTP " + code);
        HttpEntity entity = resp.getEntity();
        if (entity == null) throw new IOException("Empty response");
        try (InputStream is = entity.getContent()) {
            JsonNode root = JsonUtil.MAPPER.readTree(is);
            JsonNode xmlNode = root.get("bpmn20Xml");
            if (xmlNode == null) throw new IOException("Missing bpmn20Xml field");
            return xmlNode.asText();
        }
    }

    /**
     * Runs the close method to release resources.
     */
    @Override
    public void run() {
        close();
    }

    /**
     * Closes the HTTP client and releases resources.
     * Ensures that the client is only closed once.
     */
    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            try {
                client.close();
            } catch (IOException ignored) {
            }
        }
    }
}