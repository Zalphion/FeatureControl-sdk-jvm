/*
 * Portions derived from Forkhandles
 * Copyright 2026 Forkhandles Authors
 * Modifications Copyright 2026 Zalphion Systems Inc.
 *
 * Licensed under the Apache License, Version 2.0.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package com.zalphion.featurecontrol.source;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jspecify.annotations.NonNull;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

@RequiredArgsConstructor
public class DeterministicScheduler implements ScheduledExecutorService {

    private final Queue<SimpleScheduleTask<?>> tasks = emptyTaskList();
    private @Getter boolean isShutdown = false;
    private @NonNull @lombok.NonNull Instant clock;

    public DeterministicScheduler() {
        this(Instant.parse("2026-01-01T12:00:00Z"));
    }

    public @NonNull Instant currentTime() {
        return clock;
    }

    @Override
    public @NonNull Future<?> submit(@NonNull @lombok.NonNull Runnable task) {
        return schedule(task, 0, TimeUnit.SECONDS);
    }

    @Override
    public @NonNull <T> Future<T> submit(@NonNull @lombok.NonNull Callable<T> task) {
        return schedule(task, 0, TimeUnit.SECONDS);
    }

    @Override
    public @NonNull <V> ScheduledFuture<V> schedule(@NonNull Callable<V> callable, long delay, @NonNull TimeUnit unit) {
        return enqueue(new SimpleScheduleTask<>(clock, callable, null, clock.plus(asDuration(delay, unit))));
    }

    <T> ScheduledFuture<T> enqueue(SimpleScheduleTask<T> task) {
        if (!isShutdown) tasks.add(task);
        return task;
    }

    @Override
    public @NonNull ScheduledFuture<?> schedule(@NonNull Runnable command, long delay, @NonNull TimeUnit unit) {
        val timeToRun = clock.plus(asDuration(delay, unit));
        return enqueue(new SimpleScheduleTask<>(
                clock,
                () -> {command.run(); return null;},
                null,
                timeToRun
        ));
    }

    @Override
    public @NonNull ScheduledFuture<?> scheduleWithFixedDelay(@NonNull Runnable command, long initialDelay, long delay, @NonNull TimeUnit unit) {
        val period = asDuration(delay, unit);
        val timeToRun = clock.plus(asDuration(initialDelay, unit));

        return enqueue(new SimpleScheduleTask<>(
                clock,
                () -> {command.run(); return null;},
                period,
                timeToRun
        ));
    }

    @Override
    public @NonNull ScheduledFuture<?> scheduleAtFixedRate(@NonNull Runnable command, long initialDelay, long period, @NonNull TimeUnit unit) {
        val periodParsed = asDuration(period, unit);
        val timeToRun = clock.plus(asDuration(initialDelay, unit));

        return enqueue(new SimpleScheduleTask<>(
                clock,
                () -> {command.run(); return null;},
                periodParsed,
                timeToRun
        ));
    }

    @Override
    public void shutdown() {
        isShutdown = true;
    }

    public void clear() {
        tasks.clear();
    }

    public boolean isIdle() {
        return tasks.isEmpty() || tasks.peek().getTimeToRun().isAfter(clock);
    }

    public void runUntilIdle() {
        tick(Duration.ZERO);
    }

    public void tick(@NonNull @lombok.NonNull Duration duration) {
        val endOfPeriod = clock.plus(duration);
        while(true) {
            if (!runNextTask(endOfPeriod)) break;
        }
        clock = endOfPeriod;
    }

    private boolean runNextTask(@NonNull @lombok.NonNull Instant endOfPeriod) {
        val nextTasks = emptyTaskList();
        boolean ranSomething = false;
        boolean execute = true;
        val currentTasks = new ArrayList<>(tasks);

        tasks.clear();

        for (val task: currentTasks) {
            if (task.isCancelled()) continue;
            val executionTimeOfTask = task.getTimeToRun();
            if (execute && !executionTimeOfTask.isAfter(endOfPeriod)) {
                clock = executionTimeOfTask;
                ranSomething = true;
                val success = task.execute();
                if (task.isPeriodic() && success && !task.isCancelled()) {
                    nextTasks.add(task.atNextExecutionTimeAfter(executionTimeOfTask));
                }

                if (!tasks.isEmpty()) {
                    // if a task added another task, then we need to drop out
                    // so that the added task runs in the correct order
                    execute = false;
                }
            } else {
                nextTasks.add(task);
            }
        }

        tasks.addAll(nextTasks);
        return ranSomething;
    }

    @Override
    public @NonNull <T> Future<T> submit(@NonNull @lombok.NonNull Runnable task, T result) {
        return schedule(
                () -> { task.run(); return result; },
                0,
                TimeUnit.SECONDS
        );
    }

    @Override
    public void execute(@NonNull Runnable command) {
        submit(command);
    }

    @Override
    public @NonNull List<Runnable> shutdownNow() {
        shutdown();
        return Collections.emptyList();
    }

    @Override
    public boolean isTerminated() {
        return isShutdown;
    }

    @Override
    public boolean awaitTermination(long timeout, @NonNull TimeUnit unit) {
        if (isShutdown) {
            return true;
        }
        throw blockingOperationsNotSupported();
    }

    @Override
    public @NonNull <T> List<Future<T>> invokeAll(@NonNull Collection<? extends Callable<T>> tasks) {
        throw blockingOperationsNotSupported();
    }

    @Override
    public @NonNull <T> List<Future<T>> invokeAll(@NonNull Collection<? extends Callable<T>> tasks, long timeout, @NonNull TimeUnit unit) {
        throw blockingOperationsNotSupported();
    }

    @Override
    public @NonNull <T> T invokeAny(@NonNull Collection<? extends Callable<T>> tasks) {
        throw blockingOperationsNotSupported();
    }

    @Override
    public <T> T invokeAny(@NonNull Collection<? extends Callable<T>> tasks, long timeout, @NonNull TimeUnit unit) {
        throw blockingOperationsNotSupported();
    }



    private static Queue<SimpleScheduleTask<?>> emptyTaskList() {
        return new PriorityQueue<>(Comparator.comparingLong(tasks -> tasks.getDelay(TimeUnit.MILLISECONDS)));
    }

    private RuntimeException blockingOperationsNotSupported() {
        return new UnsupportedOperationException("cannot perform blocking wait on a task scheduled on a " + getClass().getSimpleName());
    }

    private static @NonNull Duration asDuration(long delay, @NonNull @lombok.NonNull TimeUnit unit) {
        return Duration.ofMillis(TimeUnit.MILLISECONDS.convert(delay, unit));
    }
}