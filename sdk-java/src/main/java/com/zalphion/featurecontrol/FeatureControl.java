package com.zalphion.featurecontrol;

import com.squareup.moshi.Moshi;
import com.zalphion.featurecontrol.bundle.ApplicationBundleDto;
import com.zalphion.featurecontrol.dto.SdkMetricsDto;
import com.zalphion.featurecontrol.lib.Result;
import com.zalphion.featurecontrol.dto.SdkLivenessDataDto;
import com.zalphion.featurecontrol.source.ApplicationSource;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import okhttp3.*;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.*;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Function;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class FeatureControl {
    private static final MediaType JSON = MediaType.get("application/json");

    private final @NonNull URI baseUri;
    private final @NonNull OkHttpClient client;
    private final @NonNull Moshi moshi;

    @SneakyThrows
    public FeatureControl(@NonNull @lombok.NonNull URI baseUri) {
        this.baseUri = baseUri;
        client = new OkHttpClient.Builder()
                .followRedirects(false)
                .callTimeout(Duration.ofSeconds(10))
                .cache(new Cache(Files.createTempDirectory("feature-control-http").toFile(), 1000L))
                .build();
        moshi = new Moshi.Builder().build();
    }


    public static @NonNull FeatureControl canada() {
        return new FeatureControl(URI.create("https://ca.featurecontrol.app"));
    }

    public static @NonNull FeatureControl ireland() {
        return new FeatureControl(URI.create("https://ie.featurecontrol.app"));
    }

    public static @NonNull FeatureControl australia() {
        return new FeatureControl(URI.create("https://au.featurecontrol.app"));
    }

    private @NonNull <T> Result<T> exchange(@NonNull Request request, @NonNull Function<Response, Result<T>> block) {
        try (val response = client.newCall(request).execute()) {
            return block.apply(response);
        } catch (ConnectException e) {
            return Result.failure("Connection Refused");
        } catch (UnknownHostException | NoRouteToHostException e) {
            return Result.failure("Unknown Host");
        } catch (InterruptedIOException e) {
            return Result.failure("Client Timeout");
        } catch (IOException e) {
            return Result.failure("Service Unavailable");
        }
    }

    @SneakyThrows
    public @NonNull Result<ApplicationBundleDto> getBundle(@NonNull @lombok.NonNull String sdkKey) {
        val request = new Request.Builder()
                .get()
                .url(baseUri.resolve("/sdkapi/v1/bundle").toURL())
                .header("Authorization", "Bearer " + sdkKey)
                .build();

        return exchange(request, response -> {
            switch(response.code()) {
                case 200:
                    try {
                        val body = response.body();
                        if (body == null) {
                            return Result.failure("Unexpected null response body");
                        }
                        return Result.success(Objects.requireNonNull(moshi.adapter(ApplicationBundleDto.class).fromJson(body.source())));
                    } catch (IOException e) {
                        return Result.failure("Failed to parse response body: " + e.getMessage());
                    }
                case 401: return Result.failure("Invalid SDK key");
                case 403: return Result.failure("Application has been banned for abuse");
                case 429: return Result.failure("Rate limit temporarily exceeded");
                default: return unexpectedError(response);
            }
        });
    }

    @SneakyThrows
    public @NonNull Result<Object> updateLiveness(
            @NonNull @lombok.NonNull String sdkKey,
            @NonNull @lombok.NonNull SdkLivenessDataDto data
    ) {
        val json = moshi.adapter(SdkLivenessDataDto.class).toJson(data);

        val request = new Request.Builder()
                .post(RequestBody.create(JSON, json))
                .url(baseUri.resolve("/sdkapi/v1/liveness").toURL())
                .header("Authorization", "Bearer " + sdkKey)
                .build();

        return exchange(request, resp -> resp.code() == 200 ? Result.success(new Object()) : unexpectedError(resp));
    }

    @SneakyThrows
    public @NonNull Result<Object> pushMetrics(
            @NonNull @lombok.NonNull String sdkKey,
            @NonNull @lombok.NonNull SdkMetricsDto data
    ) {
        val json = moshi.adapter(SdkMetricsDto.class).toJson(data);

        val request = new Request.Builder()
                .post(RequestBody.create(JSON, json))
                .url(baseUri.resolve("/sdkapi/v1/metrics").toURL())
                .header("Authorization", "Bearer " + sdkKey)
                .build();

        return exchange(request, resp -> resp.code() == 200 ? Result.success(new Object()) : unexpectedError(resp));
    }

    public @NonNull ApplicationSource toFeatureSource(@NonNull @lombok.NonNull String sdkKey) {
        return new ApplicationSource() {
            @Override
            protected @NonNull @lombok.NonNull Result<ApplicationBundleDto> getInternal() {
                return getBundle(sdkKey);
            }
        };
    }

    private <T> Result<T> unexpectedError(Response response) {
        return Result.failure("Unexpected status code " + response.code() + ": " + response.message());
    }
}