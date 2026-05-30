package com.zalphion.featurecontrol.lib;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import org.jspecify.annotations.NonNull;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode
public class Result<T> {
    private final T value;
    private final String failure;


    public <O> @NonNull Result<O> flatMap(
            @NonNull @lombok.NonNull Function<@NonNull T, @NonNull Result<O>> function
    ) {
        return failure == null ? function.apply(value) : new Result<>(null, failure);
    }

    public @NonNull Result<T> flatMapFailure(
            @NonNull @lombok.NonNull Function<@NonNull String, @NonNull Result<T>> function
    ) {
        return failure == null ? this : function.apply(failure);
    }

    public @NonNull T recover(
            @NonNull @lombok.NonNull Function<@NonNull String, @NonNull T> function
    ) {
        return failure == null ? value : function.apply(failure);
    }

    public @NonNull Result<T> peek(
            @NonNull @lombok.NonNull Consumer<@NonNull T> consumer
    ) {
        if (value != null) consumer.accept(value);
        return this;
    }

    public @NonNull Result<T> peekFailure(
            @NonNull @lombok.NonNull Consumer<@NonNull String> consumer
    ) {
        if (failure != null) consumer.accept(failure);
        return this;
    }

    public @NonNull <E extends Exception> T orElseThrow(
            @NonNull @lombok.NonNull Function<@NonNull String, @NonNull E> function
    ) throws E {
        if (value != null) return value;
        throw function.apply(failure);
    }

    public @NonNull <O> Result<@NonNull O> map(
            @NonNull @lombok.NonNull Function<@NonNull T, @NonNull O> function
    ) {
        return flatMap( value -> new Result<>(function.apply(value), null));
    }

    public @NonNull Result<@NonNull T> mapFailure(
            @NonNull @lombok.NonNull Function<@NonNull String, @NonNull String> function
    ) {
        return flatMapFailure(message -> new Result<>(null, function.apply(message)));
    }

    public static @NonNull <T> Result<T> success(@NonNull @lombok.NonNull T value) {
        return new Result<>(value, null);
    }

    public static @NonNull <T> Result<T> failure(@NonNull @lombok.NonNull String failure) {
        return new Result<>(null, failure);
    }

    public static @NonNull <T> Result<T> successOr(
            T value,
            @NonNull @lombok.NonNull Supplier<String> errorFunction
    ) {
        return value == null ? new Result<>(null, errorFunction.get()) : new Result<>(value, null);
    }
}