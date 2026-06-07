package com.zalphion.featurecontrol.dto;

import com.zalphion.featurecontrol.source.ApplicationSource;
import com.zalphion.featurecontrol.source.StaticApplicationSource;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import org.jspecify.annotations.NonNull;

import java.time.Duration;
import java.util.Map;

@Data
@Builder
public class ApplicationBundleDto {
    private final @Singular @NonNull Map<@NonNull String, String> properties;
    private final @Singular Map<@NonNull @lombok.NonNull String, FlagDefinition> flags;
    private final @Builder.Default @NonNull @lombok.NonNull Duration livenessInterval = Duration.parse("PT1M");
    private final @Builder.Default @NonNull @lombok.NonNull Duration metricsInterval = Duration.parse("PT10M");

    public @NonNull ApplicationSource toSource() {
        return new StaticApplicationSource(this);
    }
}