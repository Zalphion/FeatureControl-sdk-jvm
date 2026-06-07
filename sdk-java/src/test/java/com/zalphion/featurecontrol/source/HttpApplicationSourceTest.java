package com.zalphion.featurecontrol.source;

import com.zalphion.featurecontrol.FeatureControl;
import com.zalphion.featurecontrol.TestFixtures;
import com.zalphion.featurecontrol.lib.Result;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.util.AbstractMap;

import static org.assertj.core.api.Assertions.assertThat;

public class HttpApplicationSourceTest {

    protected final FakeServer server = new FakeServer(Duration.ofMinutes(30))
            .withBundle("key1", TestFixtures.bundle1);

    protected @NonNull FeatureControl client;

    @BeforeEach
    public void setup() {
        client = new FeatureControl(URI.create("http://localhost:" + server.start()));
    }

    @AfterEach
    public void cleanup() {
        server.stop();
    }

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
        server.withBundle("key1", com.zalphion.featurecontrol.bundle.ApplicationBundleDto.builder().build());
        assertThat(client.toFeatureSource("key1").get()).isEqualTo(Result.success(TestFixtures.bundle1));

        assertThat(server.getResponses()).containsExactly(new AbstractMap.SimpleEntry<>("key1", 200));
    }
}
