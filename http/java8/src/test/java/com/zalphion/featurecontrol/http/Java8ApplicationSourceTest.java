package com.zalphion.featurecontrol.http;

import com.zalphion.featurecontrol.TestFixtures;
import com.zalphion.featurecontrol.bundle.ApplicationBundle;
import com.zalphion.featurecontrol.lib.Result;
import com.zalphion.featurecontrol.source.ApplicationSourceContract;
import lombok.val;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

public class Java8ApplicationSourceTest extends ApplicationSourceContract {

    @Test
    public void get_eTagExpired() {
        val bundle2 = ApplicationBundle.builder()
                .property("foo", "bar")
                .build();

        assertThat(client.toFeatureSource("key1").get()).isEqualTo(Result.success(TestFixtures.bundle1));

        clock.updateAndGet(now -> now.plus(server.getMaxAge().plusSeconds(1)));
        server.withBundle("key1", bundle2);

        assertThat(client.toFeatureSource("key1").get()).isEqualTo(Result.success(bundle2));

        assertThat(server.getResponses()).containsExactly(
                new AbstractMap.SimpleEntry<>("key1", 200),
                new AbstractMap.SimpleEntry<>("key1", 200)
        );
    }

    @Test
    public void get_cached_byETag() {
        assertThat(client.toFeatureSource("key1").get()).isEqualTo(Result.success(TestFixtures.bundle1));
        clock.updateAndGet(now -> now.plus(server.getMaxAge().plusSeconds(1)));
        assertThat(client.toFeatureSource("key1").get()).isEqualTo(Result.success(TestFixtures.bundle1));

        assertThat(server.getResponses()).containsExactly(
                new AbstractMap.SimpleEntry<>("key1", 200),
                new AbstractMap.SimpleEntry<>("key1", 304)
        );
    }

    @Override
    protected @NonNull HttpFunction createHttpFunction(@NonNull Supplier<Instant> clock) {
        return Java8HttpFunction.create(
                Duration.ofSeconds(10),
                Duration.ofSeconds(10),
                clock
        );
    }
}
