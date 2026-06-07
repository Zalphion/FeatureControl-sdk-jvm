package com.zalphion.featurecontrol.source;

import com.zalphion.featurecontrol.FeatureFlag;
import com.zalphion.featurecontrol.dto.ApplicationBundleDto;
import com.zalphion.featurecontrol.lib.Result;
import com.zalphion.featurecontrol.ApplicationProperty;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
public abstract class ApplicationSource implements AutoCloseable {

    protected abstract @NonNull @lombok.NonNull Result<ApplicationBundleDto> getInternal() throws Exception;
    protected abstract void recordFlagEvaluation(@NonNull String flagName, @NonNull String variant);
    protected abstract void recordFailedEvaluation(@NonNull String flagName);
    protected abstract void recordMissingFlag();

    public @NonNull @lombok.NonNull Result<ApplicationBundleDto> get() {
        try {
            return getInternal();
        } catch (Exception e) {
            return Result.failure(Optional.ofNullable(e.getMessage()).orElse("Unknown error"));
        }
    }

    /*
     * Properties
     */

    public <T> @NonNull ApplicationProperty<T> property(
            @NonNull @lombok.NonNull String name,
            @NonNull @lombok.NonNull Function<@NonNull @lombok.NonNull String, @lombok.NonNull @NonNull T> parseValue,
            @NonNull @lombok.NonNull Supplier<@NonNull T> defaultValue
    ) {
        return () -> get()
                .flatMap(source -> Result.successOr(source.getProperties().get(name), () -> "Property '" + name + "' not found in source"))
                .map(parseValue)
                .peekFailure(log::trace)
                .recover(err -> defaultValue.get());
    }

    public <T> @NonNull ApplicationProperty<T> property(
            @NonNull @lombok.NonNull String name,
            @NonNull @lombok.NonNull Function<@NonNull @lombok.NonNull String, @lombok.NonNull @NonNull T> parseValue,
            @NonNull @lombok.NonNull T defaultValue
    ) {
        return property(name, parseValue, (Supplier<T>) () -> defaultValue);
    }

    public @NonNull ApplicationProperty<String> stringProperty(
            @NonNull @lombok.NonNull String name,
            @NonNull @lombok.NonNull Supplier<String> defaultValue
    ) {
        return property(name, Function.identity(), defaultValue);
    }

    public @NonNull ApplicationProperty<String> stringProperty(
            @NonNull @lombok.NonNull String name,
            @NonNull @lombok.NonNull String defaultValue
    ) {
        return stringProperty(name, () -> defaultValue);
    }

    /*
     * Flags
     */

    public @NonNull FeatureFlag flag(
            @NonNull @lombok.NonNull String name,
            @NonNull @lombok.NonNull String defaultVariant
    ) {
        return flag(name, recipient -> defaultVariant);
    }

    public @NonNull FeatureFlag flag(
            @NonNull @lombok.NonNull String name,
            @NonNull @lombok.NonNull Function<@NonNull @lombok.NonNull String, @NonNull @lombok.NonNull String> getDefaultVariant
    ) {
        return recipient -> get()
                .flatMap(source -> Result.successOr(source.getFlags().get(name), () -> "Flag '" + name + "' not found in source"))
                .peekFailure(e -> recordMissingFlag())
                .flatMap(def -> def.evaluate(recipient).peekFailure(e -> recordFailedEvaluation(name)))
                .peek(variant -> recordFlagEvaluation(name, variant))
                .peekFailure(log::trace)
                .recover(getDefaultVariant);
    }

    /*
     * Pre-fetching
     */

    public @NonNull PreFetchingApplicationSource preFetching() {
        return new PreFetchingApplicationSource(this);
    }

    public @NonNull PreFetchingApplicationSource preFetching(
            @NonNull @lombok.NonNull Duration refreshInterval,
            @NonNull @lombok.NonNull Duration retryInterval,
            @NonNull @lombok.NonNull ScheduledExecutorService scheduler
    ) {
        return new PreFetchingApplicationSource(this, refreshInterval, retryInterval, scheduler);
    }
}