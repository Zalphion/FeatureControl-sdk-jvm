package com.zalphion.featurecontrol.source;

import com.zalphion.featurecontrol.dto.ApplicationBundleDto;
import com.zalphion.featurecontrol.lib.Result;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class PreFetchingApplicationSource extends ApplicationSource {
    private final @lombok.NonNull ApplicationSource inner;
    private final @lombok.NonNull Duration retryInterval;
    private final @lombok.NonNull ScheduledExecutorService scheduler;

    private final AtomicReference<ApplicationBundleDto> cache = new AtomicReference<>();

    public PreFetchingApplicationSource(@NonNull @lombok.NonNull ApplicationSource inner) {
        this(inner, Duration.ofSeconds(10), Duration.ofSeconds(1), Executors.newSingleThreadScheduledExecutor());
    }

    public PreFetchingApplicationSource(
            @NonNull @lombok.NonNull ApplicationSource inner,
            @NonNull @lombok.NonNull Duration refreshInterval,
            @NonNull @lombok.NonNull Duration retryInterval,
            @NonNull @lombok.NonNull ScheduledExecutorService scheduler
    ) {
        this.inner = inner;
        this.retryInterval = retryInterval;
        this.scheduler = scheduler;

        scheduler.scheduleWithFixedDelay(this::fetchNow, 0, refreshInterval.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    protected void recordFlagEvaluation(@NonNull String flagName, @NonNull String variant) {
        inner.recordFlagEvaluation(flagName, variant);
    }

    @Override
    protected void recordFailedEvaluation(@NonNull String flagName) {
        inner.recordFailedEvaluation(flagName);
    }

    @Override
    protected void recordMissingFlag() {
        inner.recordMissingFlag();
    }

    @Override
    protected @NonNull @lombok.NonNull Result<ApplicationBundleDto> getInternal() {
        return Result.successOr(cache.get(), () -> "A bundle has not yet been successfully fetched");
    }

    @Override
    public void close() throws Exception {
        scheduler.shutdown();
        inner.close();
    }

    private void fetchNow() {
        if (scheduler.isShutdown()) return;

        try {
            inner.get().peek(value -> {
                val previous = cache.getAndSet(value);
                if (previous == null) {
                    log.debug("Ready");
                } else {
                    log.trace("Refreshed");
                }
            }).peekFailure(message -> {
                    log.warn(message);
                    if (cache.get() == null && !scheduler.isShutdown()) {
                        scheduler.schedule(this::fetchNow, retryInterval.toMillis(), TimeUnit.MILLISECONDS);
                    }
            });
        } catch (RuntimeException e) {
            log.warn("Error refreshing SDK Bundle", e);
        }
    }
}