package com.zalphion.featurecontrol.source;

import com.zalphion.featurecontrol.dto.ApplicationBundleDto;
import com.zalphion.featurecontrol.dto.JsonAdapters;
import com.zalphion.featurecontrol.dto.SdkLivenessDataDto;
import com.zalphion.featurecontrol.dto.SdkMetricsDto;
import com.zalphion.featurecontrol.lib.Pair;
import io.javalin.Javalin;
import io.javalin.http.Context;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import lombok.val;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@RequiredArgsConstructor
public class FakeServer {

    private final @Getter List<Pair<String, Integer>> responses = new CopyOnWriteArrayList<>();
    private final @Getter List<SdkLivenessDataDto> liveness = new CopyOnWriteArrayList<>();
    private final @Getter List<SdkMetricsDto> metrics = new CopyOnWriteArrayList<>();
    private final Map<String, ApplicationBundleDto> bundles = new ConcurrentHashMap<>();
    private final @NonNull Javalin app = Javalin.create(config -> config.showJavalinBanner = false);

    private final @NonNull @Getter Duration maxAge;

    public FakeServer withBundle(String sdkKey, ApplicationBundleDto bundle) {
        bundles.put(sdkKey, bundle);
        return this;
    }

    public int start() {
        app.get("/sdkapi/v1/bundle", this::getBundle);
        app.post("/sdkapi/v1/liveness", this::updateLiveness);
        app.post("/sdkapi/v1/metrics", this::pushMetrics);

        app.after(ctx -> {
            val sdkKey = getSdkKey(ctx.req).orElse("");
            responses.add(new Pair<>(sdkKey, ctx.status()));
        });

        return app.start().port();
    }

    public void stop() {
        app.stop();
    }

    private void getBundle(@NonNull Context ctx) {
        ctx.header("Cache-Control", "max-age=" + maxAge.getSeconds());

        val sdkKey = getSdkKey(ctx.req).orElse("");
        val bundle = bundles.entrySet().stream()
                .filter(entry -> entry.getKey().equals(sdkKey))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);

        if (bundle == null) {
            ctx.status(401);
            return;
        }

        val eTag = "W/\"" + bundle.hashCode() + "\"";
        ctx.header("ETag", eTag);
        if (eTag.equals(ctx.header("If-None-Match"))) {
            ctx.status(304);
            return;
        }

        ctx.result(JsonAdapters.applicationBundle.toJson(bundle));
    }

    private void updateLiveness(@NonNull Context ctx) throws IOException {
        val data = JsonAdapters.sdkLivenessData.fromJson(ctx.body());
        liveness.add(data);
        ctx.status(200);
    }

    private void pushMetrics(@NonNull Context ctx) throws IOException {
        val data = JsonAdapters.sdkMetrics.fromJson(ctx.body());
        metrics.add(data);
        ctx.status(200);
    }

    private static @NonNull Optional<String> getSdkKey(@NonNull HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader("Authorization"))
                .map(value -> value.substring(value.indexOf(' ') + 1));
    }
}