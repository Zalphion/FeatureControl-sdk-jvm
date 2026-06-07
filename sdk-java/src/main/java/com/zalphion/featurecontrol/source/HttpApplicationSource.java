package com.zalphion.featurecontrol.source;

import com.zalphion.featurecontrol.BuildManifest;
import com.zalphion.featurecontrol.FeatureControl;
import com.zalphion.featurecontrol.dto.ApplicationBundleDto;
import com.zalphion.featurecontrol.dto.FlagMetricsDto;
import com.zalphion.featurecontrol.dto.SdkLivenessDataDto;
import com.zalphion.featurecontrol.dto.SdkMetricsDto;
import com.zalphion.featurecontrol.lib.Pair;
import com.zalphion.featurecontrol.lib.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jspecify.annotations.NonNull;

import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
public class HttpApplicationSource extends ApplicationSource {
    private final @lombok.NonNull FeatureControl client;
    private final @lombok.NonNull String sdkKey;
    private final @lombok.NonNull SdkLivenessDataDto livenessData;
    private final @lombok.NonNull ScheduledExecutorService scheduler;
    private final @NonNull AtomicBoolean schedulerInit = new AtomicBoolean(false);

    private final AtomicInteger missingFlagEvaluations = new AtomicInteger();
    private final ConcurrentHashMap<String, FlagMetrics> flagMetrics = new ConcurrentHashMap<>();

    public HttpApplicationSource(
            @NonNull @lombok.NonNull FeatureControl client,
            @NonNull @lombok.NonNull String sdkKey
    ) {
        this(client, sdkKey, new SdkLivenessDataDto(
                UUID.randomUUID().toString(),
                BuildManifest.platform,
                BuildManifest.version,
                BuildManifest.repositoryUrl
        ), Executors.newSingleThreadScheduledExecutor());
    }

    @Override
    protected @NonNull @lombok.NonNull Result<ApplicationBundleDto> getInternal() {
        return client.getBundle(sdkKey).peek(bundle -> {
            if (!schedulerInit.getAndSet(true)) {
                // if the pre-fetching wrapper is not applied,
                scheduler.scheduleAtFixedRate(
                        this::updateLiveness,
                        0, bundle.getLivenessInterval().toMillis(),
                        TimeUnit.MILLISECONDS
                );
                scheduler.scheduleAtFixedRate(
                        this::pushMetrics,
                        bundle.getMetricsInterval().toMillis(), bundle.getMetricsInterval().toMillis(),
                        TimeUnit.MILLISECONDS
                );
            }
        });
    }

    @Override
    protected void recordMissingFlag() {
        missingFlagEvaluations.incrementAndGet();
    }

    @Override
    protected void recordFlagEvaluation(
            @NonNull @lombok.NonNull String flagName,
            @NonNull @lombok.NonNull String variant
    ) {
        val metrics = flagMetrics.computeIfAbsent(flagName, k -> new FlagMetrics());
        metrics.successfulEvaluations.computeIfAbsent(variant, v -> new AtomicInteger()).incrementAndGet();
    }

    @Override
    protected void recordFailedEvaluation(@NonNull String flagName) {
        flagMetrics.computeIfAbsent(flagName, k -> new FlagMetrics()).failedEvaluations.incrementAndGet();
    }

    private void updateLiveness() {
        client.updateLiveness(sdkKey, livenessData).peekFailure(e ->
                log.trace("Failed to update liveness for SDK: {}", e)
        );
    }

    private void pushMetrics() {
        val metrics = new SdkMetricsDto(
                missingFlagEvaluations.getAndSet(0),
                flagMetrics.entrySet().stream().map(entry ->
                    new FlagMetricsDto(
                            entry.getKey(),
                            entry.getValue().failedEvaluations.getAndSet(0),
                            entry.getValue().successfulEvaluations.entrySet().stream()
                                    .map(e -> new Pair<>(e.getKey(), e.getValue().getAndSet(0)))
                                    .filter(e -> e.getValue() > 0)
                                    .collect(Pair.toMap())
                    )
                ).filter(m -> !m.isEmpty()).collect(Collectors.toList())
        );

        client.pushMetrics(sdkKey, metrics).peekFailure(e ->
                log.trace("Failed to push metrics for SDK: {}", e)
        );
    }

    @Override
    public void close() {
        scheduler.shutdown();
    }

    private static class FlagMetrics {
        private final AtomicInteger failedEvaluations = new AtomicInteger();
        private final ConcurrentHashMap<String, AtomicInteger> successfulEvaluations = new ConcurrentHashMap<>();
    }
}
