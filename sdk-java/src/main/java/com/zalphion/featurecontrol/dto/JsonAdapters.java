package com.zalphion.featurecontrol.dto;

import com.squareup.moshi.FromJson;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.ToJson;
import org.jspecify.annotations.Nullable;

import java.time.Duration;

public class JsonAdapters {

    private JsonAdapters() {}

    private static final Moshi moshi = new Moshi.Builder()
            .add(new DurationAdapter())
            .build();

    public static final JsonAdapter<ApplicationBundleDto> applicationBundle = moshi.adapter(ApplicationBundleDto.class);
    public static final JsonAdapter<SdkLivenessDataDto> sdkLivenessData = moshi.adapter(SdkLivenessDataDto.class);
    public static final JsonAdapter<SdkMetricsDto> sdkMetrics = moshi.adapter(SdkMetricsDto.class);


    private static class DurationAdapter {
        @ToJson public String toJson(@Nullable Duration duration) {
            return duration == null ? null : duration.toString();
        }

        @FromJson public Duration fromJson(String duration) {
            return duration == null ? null : Duration.parse(duration);
        }
    }
}
