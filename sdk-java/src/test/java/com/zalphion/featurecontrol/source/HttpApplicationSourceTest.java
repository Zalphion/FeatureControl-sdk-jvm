package com.zalphion.featurecontrol.source;

import com.zalphion.featurecontrol.FeatureControl;
import com.zalphion.featurecontrol.TestFixtures;
import com.zalphion.featurecontrol.dto.ApplicationBundleDto;
import com.zalphion.featurecontrol.dto.FlagMetricsDto;
import com.zalphion.featurecontrol.dto.SdkLivenessDataDto;
import com.zalphion.featurecontrol.dto.SdkMetricsDto;
import com.zalphion.featurecontrol.lib.Pair;
import com.zalphion.featurecontrol.lib.Result;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import lombok.val;

import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("resource")
public class HttpApplicationSourceTest {

    private final FakeServer server = new FakeServer(Duration.ofMinutes(30))
            .withBundle("key1", TestFixtures.bundle1);

    private final DeterministicScheduler scheduler = new DeterministicScheduler();
    private final FeatureControl client = new FeatureControl(URI.create("http://localhost:" + server.start()));

    private final SdkLivenessDataDto livenessData = new SdkLivenessDataDto(
            "sdkId", "testApp", "v1", "http://app"
    );

    @AfterEach
    public void cleanup() {
        server.stop();
    }

    @Test
    public void get_unauthorized() {
        assertThat(client.toFeatureSource("key2").get()).isEqualTo(Result.failure("Invalid SDK key"));
        assertThat(server.getResponses()).containsExactly(
                new Pair<>("key2", 401)
        );
    }

    @Test
    public void get_present() {
        assertThat(client.toFeatureSource("key1").get()).isEqualTo(Result.success(TestFixtures.bundle1));
        assertThat(server.getResponses()).containsExactly(
                new Pair<>("key1", 200)
        );
    }

    @Test
    public void get_cached() {
        assertThat(client.toFeatureSource("key1").get()).isEqualTo(Result.success(TestFixtures.bundle1));
        server.withBundle("key1", ApplicationBundleDto.builder().build());
        assertThat(client.toFeatureSource("key1").get()).isEqualTo(Result.success(TestFixtures.bundle1));

        assertThat(server.getResponses()).containsExactly(new Pair<>("key1", 200));
    }

    @Test
    public void updateLiveness() {
        val source = new HttpApplicationSource(client, "key1", livenessData, scheduler);

        // won't run without a fetch
        scheduler.tick(Duration.ZERO);
        assertThat(server.getLiveness()).isEmpty();

        // runs immediately after first get
        source.get();
        scheduler.tick(Duration.ZERO);
        assertThat(server.getLiveness()).containsExactly(livenessData);

        // won't run again before the liveness interval elapses
        scheduler.tick(Duration.ofSeconds(45));
        assertThat(server.getLiveness()).containsExactly(livenessData);

        // runs after the liveness interval elapses
        scheduler.tick(Duration.ofSeconds(45));
        assertThat(server.getLiveness()).containsExactly(livenessData, livenessData);
    }

    @Test
    public void updateMetrics() {
        val source = new HttpApplicationSource(client, "key1", livenessData, scheduler);
        val flag = source.flag("lasers", "off");
        val invalidFlag = source.flag("treats", "none");
        val missingFlag = source.flag("naps", "never");

        // won't run until the first get
        scheduler.tick(Duration.ofHours(1));
        assertThat(server.getMetrics()).isEmpty();

        // won't run before metrics interval elapses
        source.get();
        scheduler.tick(Duration.ofMinutes(6));
        assertThat(server.getMetrics()).isEmpty();

        // empty metrics when no activity
        scheduler.tick(Duration.ofMinutes(6));
        assertThat(server.getMetrics()).containsExactly(
                new SdkMetricsDto(0, Collections.emptyList())
        );

        // report activity
        flag.getVariant("user1");
        flag.getVariant("user2");
        flag.getVariant("user1");
        missingFlag.getVariant("user1");
        missingFlag.getVariant("user1");
        invalidFlag.getVariant("user1");
        invalidFlag.getVariant("user1");
        scheduler.tick(Duration.ofMinutes(10));
        assertThat(server.getMetrics()).containsExactly(
                new SdkMetricsDto(0, Collections.emptyList()),
                new SdkMetricsDto(2, Arrays.asList(
                        new FlagMetricsDto("lasers", 0, new HashMap<String, Integer>() {{
                            put("off", 1);
                            put("on", 2);
                        }}),
                        new FlagMetricsDto("treats", 2, Collections.emptyMap())
                ))
        );

        // new activity isn't cumulative
        flag.getVariant("user1");
        scheduler.tick(Duration.ofMinutes(10));
        assertThat(server.getMetrics()).containsExactly(
                new SdkMetricsDto(0, Collections.emptyList()),
                new SdkMetricsDto(2, Arrays.asList(
                        new FlagMetricsDto("lasers", 0, new HashMap<String, Integer>() {{
                            put("off", 1);
                            put("on", 2);
                        }}),
                        new FlagMetricsDto("treats", 2, Collections.emptyMap())
                )),
                new SdkMetricsDto(0, Collections.singletonList(
                        new FlagMetricsDto("lasers", 0, new HashMap<String, Integer>() {{
                            put("on", 1);
                        }})
                ))
        );
    }
}
