package com.zalphion.featurecontrol.source;

import com.zalphion.featurecontrol.dto.ApplicationBundleDto;
import com.zalphion.featurecontrol.dto.JsonAdapters;
import com.zalphion.featurecontrol.lib.Result;
import lombok.AllArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Supplier;

@AllArgsConstructor
public class StaticApplicationSource extends ApplicationSource {
    private final @lombok.NonNull Supplier<Result<ApplicationBundleDto>> supplier;

    public StaticApplicationSource(@NonNull @lombok.NonNull Result<ApplicationBundleDto> result) {
        supplier = () -> result;
    }

    public StaticApplicationSource(@NonNull @lombok.NonNull ApplicationBundleDto bundle) {
        supplier = () -> Result.success(bundle);
    }

    private static final int CHUNK_SIZE = 4096;

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
                return Objects.requireNonNull(JsonAdapters.applicationBundle.fromJson(json));
            }
        }
    }

    public static @NonNull ApplicationBundleDto fromClasspath(
            @NonNull @lombok.NonNull String absolutePath
    ) throws IOException {
        return fromClasspath(absolutePath, Thread.currentThread().getContextClassLoader());
    }

    @Override
    protected @NonNull @lombok.NonNull Result<ApplicationBundleDto> getInternal() {
        return supplier.get();
    }

    @Override protected void recordFlagEvaluation(@NonNull String flagName, @NonNull String variant) {}
    @Override protected void recordFailedEvaluation(@NonNull String flagName) {}
    @Override protected void recordMissingFlag() {}
    @Override public void close() {}
}
