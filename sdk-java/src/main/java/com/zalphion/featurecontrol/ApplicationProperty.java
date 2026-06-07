package com.zalphion.featurecontrol;

import org.jspecify.annotations.NonNull;

import java.util.function.Supplier;

@FunctionalInterface
public interface ApplicationProperty<T> {
    @NonNull T getValue();

    static @NonNull @lombok.NonNull <T> ApplicationProperty<T> create(@NonNull @lombok.NonNull T value) {
        return () -> value;
    }

    static @NonNull @lombok.NonNull <T> ApplicationProperty<T> create(
            @NonNull @lombok.NonNull Supplier<@NonNull @lombok.NonNull T> supplier
    ) {
        return supplier::get;
    }
}
