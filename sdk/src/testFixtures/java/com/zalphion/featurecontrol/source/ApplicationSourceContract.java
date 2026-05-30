package com.zalphion.featurecontrol.source;

import com.zalphion.featurecontrol.FeatureControl;
import com.zalphion.featurecontrol.TestFixtures;
import com.zalphion.featurecontrol.bundle.ApplicationBundle;
import com.zalphion.featurecontrol.http.HttpFunction;
import com.zalphion.featurecontrol.lib.Result;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class ApplicationSourceContract {

    protected final AtomicReference<Instant> clock = new AtomicReference<>(Instant.parse("2026-01-01T12:00:00Z"));
    protected final FakeServer server = new FakeServer(Duration.ofMinutes(30))
            .withBundle("key1", TestFixtures.bundle1);

    protected @NonNull FeatureControl client;

    @BeforeEach
    public void setup() throws Exception {
        client = new FeatureControl(URI.create("http://localhost:" + server.start()), createHttpFunction(clock::get));
    }

    @AfterEach
    public void cleanup() {
        server.stop();
    }

    protected abstract @NonNull HttpFunction createHttpFunction(@NonNull Supplier<Instant> clock) throws Exception;

    @Test
    public void get_unauthorized() {
        assertThat(client.toFeatureSource("key2").get()).isEqualTo(Result.failure("Invalid SDK key"));
        assertThat(server.getResponses()).containsExactly(
                new AbstractMap.SimpleEntry<>("key2", 401)
        );
    }

    @Test
    public void get_present() {
        assertThat(client.toFeatureSource("key1").get()).isEqualTo(Result.success(TestFixtures.bundle1));
        assertThat(server.getResponses()).containsExactly(
                new AbstractMap.SimpleEntry<>("key1", 200)
        );
    }

    @Test
    public void get_cached() {
        assertThat(client.toFeatureSource("key1").get()).isEqualTo(Result.success(TestFixtures.bundle1));
        server.withBundle("key1", ApplicationBundle.builder().build());
        assertThat(client.toFeatureSource("key1").get()).isEqualTo(Result.success(TestFixtures.bundle1));

        assertThat(server.getResponses()).containsExactly(new AbstractMap.SimpleEntry<>("key1", 200));
    }
}
