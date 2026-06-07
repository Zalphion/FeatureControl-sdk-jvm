package com.zalphion.featurecontrol.source;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import lombok.val;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class FakeServer {
    private static final int EMPTY_BODY_LENGTH = -1;

    private final @Getter List<Map.Entry<String, Integer>> responses = new CopyOnWriteArrayList<>();
    private final Map<String, com.zalphion.featurecontrol.bundle.ApplicationBundleDto> bundles = new ConcurrentHashMap<>();
    private final JsonAdapter<com.zalphion.featurecontrol.bundle.ApplicationBundleDto> bundleAdapter = new Moshi.Builder().build().adapter(com.zalphion.featurecontrol.bundle.ApplicationBundleDto.class);

    private final @NonNull @Getter Duration maxAge;
    private final @NonNull HttpServer server;

    public FakeServer(@NonNull @lombok.NonNull Duration maxAge) {
        this.maxAge = maxAge;

        try {
            this.server = HttpServer.create(new InetSocketAddress(0), 0);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create HTTP server", e);
        }

        server.createContext("/", exchange -> {
            if (exchange.getRequestMethod().equals("GET") && exchange.getRequestURI().getPath().equals("/sdkapi/v1/bundle")) {
                getBundle(exchange);
            } else {
                exchange.sendResponseHeaders(404, EMPTY_BODY_LENGTH);
            }
        });
    }

    public FakeServer withBundle(String sdkKey, com.zalphion.featurecontrol.bundle.ApplicationBundleDto bundle) {
        bundles.put(sdkKey, bundle);
        return this;
    }

    public int start() {
        server.start();
        return server.getAddress().getPort();
    }

    public void stop() {
        server.stop(0);
    }

    private void getBundle(@NonNull @lombok.NonNull HttpExchange exchange) throws IOException {
        val ifNoneMatch = Optional.ofNullable(exchange.getRequestHeaders().get("If-None-Match"))
                .orElseGet(Collections::emptyList)
                .stream().findFirst().orElse(null);

        val sdkKey = Optional.ofNullable(exchange.getRequestHeaders().get("Authorization"))
                .orElseGet(Collections::emptyList)
                .stream().findFirst().map(value -> value.substring(value.indexOf(' ') + 1))
                .orElse("");

        val bundle = bundles.entrySet().stream()
                .filter(entry -> entry.getKey().equals(sdkKey))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);

        if (bundle == null) {
            responses.add(new AbstractMap.SimpleEntry<>(sdkKey, 401));
            exchange.sendResponseHeaders(401, EMPTY_BODY_LENGTH);
            return;
        }

        val eTag = "W/\"" + bundle.hashCode() + "\"";
        exchange.getResponseHeaders().set("ETag", eTag);
        exchange.getResponseHeaders().set("Cache-Control", "max-age=" + maxAge.getSeconds());

        if (eTag.equals(ifNoneMatch)) {
            responses.add(new AbstractMap.SimpleEntry<>(sdkKey, 304));
            exchange.sendResponseHeaders(304, EMPTY_BODY_LENGTH);
            return;
        }

        responses.add(new AbstractMap.SimpleEntry<>(sdkKey, 200));
        val jsonBinary = bundleAdapter.toJson(bundle).getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, jsonBinary.length);
        try (val out = exchange.getResponseBody()) {
            out.write(jsonBinary);
        }
    }
}