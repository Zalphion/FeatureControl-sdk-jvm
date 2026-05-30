package com.zalphion.featurecontrol;

import com.zalphion.featurecontrol.bundle.ApplicationBundle;
import com.zalphion.featurecontrol.bundle.ApplicationBundleJson;
import com.zalphion.featurecontrol.http.HttpFunction;
import com.zalphion.featurecontrol.http.HttpMethod;
import com.zalphion.featurecontrol.http.HttpRequest;
import com.zalphion.featurecontrol.lib.Result;
import com.zalphion.featurecontrol.source.ApplicationSource;
import lombok.AllArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Optional;

@AllArgsConstructor
public class FeatureControl {
    private final @NonNull URI baseUri;
    private final @NonNull HttpFunction http;

    public static @lombok.NonNull FeatureControl canada(@NonNull HttpFunction http) {
        return new FeatureControl(URI.create("https://ca.featurecontrol.app"), http);
    }

    public static @lombok.NonNull FeatureControl ireland(HttpFunction http) {
        return new FeatureControl(URI.create("https://ie.featurecontrol.app"), http);
    }

    public static @lombok.NonNull FeatureControl australia(HttpFunction http) {
        return new FeatureControl(URI.create("https://au.featurecontrol.app"), http);
    }

    public @NonNull Result<ApplicationBundle> getBundle(@NonNull @lombok.NonNull String sdkKey) {
        try {
            val request = HttpRequest.builder()
                    .method(HttpMethod.GET)
                    .url(baseUri.resolve("/sdkapi/v1/bundle").toURL())
                    .header("Authorization", Collections.singletonList("Bearer " + sdkKey))
                    .build();

            val response = http.exchange(request);

            switch(response.getStatusCode()) {
                case 200:
                    val json = new String(response.getBody(), StandardCharsets.UTF_8);
                    return ApplicationBundleJson.fromJson(json);
                case 401: return Result.failure("Invalid SDK key");
                case 403: return Result.failure("Application has been banned for abuse");
                case 429: return Result.failure("Rate limit temporarily exceeded");
                default: return Result.failure("Unexpected status code " + response.getStatusCode() + ": " + response.getStatusReason());
            }
        } catch (IOException e) {
            return Result.failure(Optional.ofNullable(e.getMessage()).orElse("Unknown error"));
        }
    }

    public @NonNull ApplicationSource toFeatureSource(@NonNull @lombok.NonNull String sdkKey) {
        return new ApplicationSource() {
            @Override
            protected @NonNull @lombok.NonNull Result<ApplicationBundle> getInternal() {
                return getBundle(sdkKey);
            }
        };
    }
}