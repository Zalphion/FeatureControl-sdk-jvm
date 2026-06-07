package com.zalphion.featurecontrol;

import org.jspecify.annotations.NonNull;

import java.util.Optional;
import java.util.function.Function;

@FunctionalInterface
public interface FeatureFlag {
    @NonNull String getVariant(@NonNull String recipient);


    static @NonNull FeatureFlag create(@NonNull @lombok.NonNull String variant) {
        return create(s -> variant);
    }

    static @NonNull FeatureFlag create(
            @NonNull @lombok.NonNull Function<@NonNull @lombok.NonNull String, String> getVariantOrNull,
            @NonNull @lombok.NonNull String defaultVariant
    ) {
        return recipient -> Optional.ofNullable(getVariantOrNull.apply(recipient)).orElse(defaultVariant);
    }

    static @NonNull FeatureFlag create(
            @NonNull @lombok.NonNull Function<@NonNull @lombok.NonNull String, @NonNull @lombok.NonNull String> getVariant
    ) {
        return getVariant::apply;
    }
}
