package com.zalphion.featurecontrol.source;

import com.zalphion.featurecontrol.ApplicationProperty;
import com.zalphion.featurecontrol.dto.ApplicationBundleDto;
import com.zalphion.featurecontrol.lib.Result;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("resource")
public class PreFetchingApplicationSourceTest {

    private final DeterministicScheduler scheduler = new DeterministicScheduler();
    private final AtomicReference<Result<ApplicationBundleDto>> nextResult = new AtomicReference<>(Result.success(buildFeatures("foo")));
    private int invocations = 0;

    private final PreFetchingApplicationSource source = new StaticApplicationSource(() -> {
        invocations++;
        return nextResult.get();
    }).preFetching(
            Duration.ofMinutes(1),
            Duration.ofSeconds(1),
            scheduler
    );

    private final ApplicationProperty<String> property = source.stringProperty("prop", "default");

    @Test
    public void get_cached() {
        scheduler.tick(Duration.ZERO);

        assertThat(source.get()).isEqualTo(Result.success(buildFeatures("foo")));
        assertThat(source.get()).isEqualTo(Result.success(buildFeatures("foo")));
        assertThat(invocations).isEqualTo(1);
    }

    @Test
    public void get_withRefreshAfterDelay() {
        scheduler.tick(Duration.ZERO);

        assertThat(property.getValue()).isEqualTo("foo");
        assertThat(invocations).isEqualTo(1);

        nextResult.set(Result.success(buildFeatures("bar")));
        assertThat(property.getValue()).isEqualTo("foo");
        assertThat(invocations).isEqualTo(1);

        scheduler.tick(Duration.ofSeconds(40));
        assertThat(property.getValue()).isEqualTo("foo");
        assertThat(invocations).isEqualTo(1);

        scheduler.tick(Duration.ofSeconds(40));
        assertThat(property.getValue()).isEqualTo("bar");
        assertThat(invocations).isEqualTo(2);
    }

    @Test
    public void gracefullyHandleSourceFailure() {
        nextResult.set(Result.failure("foo"));
        scheduler.tick(Duration.ZERO);
        assertThat(invocations).isEqualTo(1);

        assertThat(property.getValue()).isEqualTo("default");
    }

    @Test
    public void retry_beforeBundleReady() {
        nextResult.set(Result.failure("foo"));

        scheduler.tick(Duration.ZERO);
        assertThat(property.getValue()).isEqualTo("default");
        assertThat(invocations).isEqualTo(1);

        scheduler.tick(Duration.ofSeconds(1));
        assertThat(property.getValue()).isEqualTo("default");
        assertThat(invocations).isEqualTo(2);

        nextResult.set(Result.success(buildFeatures("foo")));

        scheduler.tick(Duration.ofSeconds(1));
        assertThat(property.getValue()).isEqualTo("foo");
        assertThat(invocations).isEqualTo(3);
    }

    @Test
    public void doesNotRetry_afterBundleReady() {
        scheduler.tick(Duration.ZERO);
        assertThat(invocations).isEqualTo(1);
        assertThat(source.get()).isEqualTo(Result.success(buildFeatures("foo")));

        nextResult.set(Result.failure("foo"));

        scheduler.tick(Duration.ofMinutes(1));
        assertThat(invocations).isEqualTo(2);
        assertThat(source.get()).isEqualTo(Result.success(buildFeatures("foo")));

        scheduler.tick(Duration.ofSeconds(30));
        assertThat(invocations).isEqualTo(2);
        assertThat(source.get()).isEqualTo(Result.success(buildFeatures("foo")));

        scheduler.tick(Duration.ofSeconds(30));
        assertThat(invocations).isEqualTo(3);
        assertThat(source.get()).isEqualTo(Result.success(buildFeatures("foo")));
    }

    @Test
    public void closeSource() throws Exception {
        scheduler.tick(Duration.ZERO);
        assertThat(property.getValue()).isEqualTo("foo");

        source.close();
        assertThat(property.getValue()).isEqualTo("foo");

        nextResult.set(Result.success(buildFeatures("bar")));
        scheduler.tick(Duration.ofMinutes(5));
        assertThat(property.getValue()).isEqualTo("foo");
    }

    private static ApplicationBundleDto buildFeatures(@NonNull @lombok.NonNull String propValue) {
        return ApplicationBundleDto.builder()
                .property("prop", propValue)
                .build();
    }
}
