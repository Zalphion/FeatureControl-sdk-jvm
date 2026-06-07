package com.zalphion.featurecontrol.source;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.*;

@RequiredArgsConstructor
class SimpleScheduleTask<T> implements ScheduledFuture<T> {
    private final @NonNull @lombok.NonNull Instant clock;
    private final @NonNull @lombok.NonNull Callable<T> callable;
    private final Duration period;
    private final @Getter Instant timeToRun;

    private @Getter boolean isCancelled = false, isDone = false;
    private T result = null;
    private Throwable error = null;

    public SimpleScheduleTask(
            @NonNull @lombok.NonNull Instant clock,
            @NonNull @lombok.NonNull Callable<T> callable,
            @NonNull @lombok.NonNull Instant timeToRun,
            Duration period
    ) {
        this.clock = clock;
        this.callable = callable;
        this.period = period;
        this.timeToRun = timeToRun;

        if (period != null && period.compareTo(Duration.ZERO) <= 0) {
            throw new IllegalArgumentException("period/rate must be > 0");
        }
    }

    public boolean isPeriodic() {
        return period != null;
    }

    public long getDelay(@NonNull @lombok.NonNull TimeUnit unit) {
        return unit.convert(timeToRun.toEpochMilli() - clock.toEpochMilli(), TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(@NonNull Delayed o) {
        throw new UnsupportedOperationException("james didn't write");
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (isDone) {
            return false;
        }
        isCancelled = true;
        return true;
    }

    @Override
    public T get() throws ExecutionException {
        if (isCancelled) throw new CancellationException("get() on cancelled task");
        if (error != null) throw new ExecutionException(error);
        if (isDone) return result;
        throw new UnsupportedOperationException("task not scheduled to run for another " + Duration.between(timeToRun, clock));
    }

    @Override
    public T get(long timeout, @NonNull @lombok.NonNull TimeUnit unit) throws ExecutionException {
        return get();
    }

    public boolean execute() {
        try {
            if (!isCancelled) {
                result = callable.call();
            }
            return true;
        } catch (Exception e) {
            error = e;
            return false;
        } finally {
            isDone = true;
        }
    }

    public SimpleScheduleTask<T> atNextExecutionTimeAfter(@NonNull @lombok.NonNull Instant clock) {
        return new SimpleScheduleTask<>(clock, callable, period, clock.plus(period));
    }
}