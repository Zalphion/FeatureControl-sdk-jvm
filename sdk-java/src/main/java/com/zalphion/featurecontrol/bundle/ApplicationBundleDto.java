package com.zalphion.featurecontrol.bundle;

import com.squareup.moshi.Moshi;
import com.zalphion.featurecontrol.source.ApplicationSource;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.val;
import org.jspecify.annotations.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

@Data
@Builder
public class ApplicationBundleDto {
    private static final int CHUNK_SIZE = 4096;

    private final @Singular @NonNull Map<@NonNull String, String> properties;
    private final @Singular Map<@NonNull @lombok.NonNull String, FlagDefinition> flags;

    public @NonNull ApplicationSource toSource() {
        return ApplicationSource.create(this);
    }
    public static @NonNull ApplicationBundleDto fromClasspath(
            @NonNull @lombok.NonNull String absolutePath,
            @NonNull @lombok.NonNull ClassLoader classLoader
    ) throws IOException {
        try (val stream = classLoader.getResourceAsStream(absolutePath)){
            if (stream == null) throw new IllegalArgumentException("Could not find bundle at " + absolutePath);
            try (val outputStream = new ByteArrayOutputStream()) {
                byte[] chunk = new byte[CHUNK_SIZE];

                int bytesRead;
                while ((bytesRead = stream.read(chunk)) != -1) {
                    outputStream.write(chunk, 0, bytesRead);
                }

                val json = new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
                val jsonAdapter = new Moshi.Builder().build().adapter(ApplicationBundleDto.class);
                return Objects.requireNonNull(jsonAdapter.fromJson(json));
            }
        }
    }

    public static @NonNull ApplicationBundleDto fromClasspath(
            @NonNull @lombok.NonNull String absolutePath
    ) throws IOException {
        return fromClasspath(absolutePath, Thread.currentThread().getContextClassLoader());
    }
}