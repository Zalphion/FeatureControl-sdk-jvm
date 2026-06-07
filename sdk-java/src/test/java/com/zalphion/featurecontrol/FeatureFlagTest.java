package com.zalphion.featurecontrol;

import com.zalphion.featurecontrol.dto.ApplicationBundleDto;
import com.zalphion.featurecontrol.dto.FlagDefinition;
import com.zalphion.featurecontrol.dto.VariantDefinition;
import com.zalphion.featurecontrol.lib.Pair;
import com.zalphion.featurecontrol.lib.Result;
import com.zalphion.featurecontrol.source.ApplicationSource;
import com.zalphion.featurecontrol.source.StaticApplicationSource;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import lombok.val;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("resource")
public class FeatureFlagTest {

    private final ApplicationSource source = TestFixtures.bundle1.toSource();
    private final FeatureFlag flag = source.flag("lasers", "off");

    @Test
    public void getVariant_fromOverride() {
        assertThat(flag.getVariant("user1")).isEqualTo("on");
        assertThat(flag.getVariant("user2")).isEqualTo("off");
    }

    @Test
    public void getVariant_notFound() {
        assertThat(source.flag("missing", "default").getVariant("user1"))
                .isEqualTo("default");

    }

    @Test
    public void getVariant_sourceFailure() {
        val source = new StaticApplicationSource(Result.failure("foo"));
        assertThat(source.flag("lasers", "default").getVariant("user1"))
                .isEqualTo("default");
    }

    @Test
    public void getVariant_fromBucketing() {
        assertThat(flag.getVariant("user3")).isEqualTo("on");
        assertThat(flag.getVariant("user6")).isEqualTo("off");
    }

    @Test
    public void getVariant_emptyBuckets() {
        assertThat(source.flag("treats", "none").getVariant("toggles"))
                .isEqualTo("none");
    }

    @Test
    public void getVariant_stickyBuckets() {
        val offThreshold = new AtomicInteger(2);

        val flag = new StaticApplicationSource(() -> {
            val flagBundle = FlagDefinition.builder()
                    .bucket(new VariantDefinition("off", offThreshold.get()))
                    .bucket(new VariantDefinition("on", 8))
                    .saltBase64("bGFzZXJz")
                    .build();

            return Result.success(ApplicationBundleDto.builder().flag("lasers", flagBundle).build());
        }).flag("lasers", "off");

        val offSubjectsBefore = getManyVariants(flag)
                .stream()
                .filter(e -> e.getValue().equals("off"))
                .map(Pair::getKey)
                .collect(Collectors.toSet());

        offThreshold.set(4);

        val offSubjectsAfter = getManyVariants(flag)
                .stream()
                .filter(e -> e.getValue().equals("off"))
                .map(Pair::getKey)
                .collect(Collectors.toList());

        assertThat(offSubjectsAfter).hasSizeGreaterThan(offSubjectsBefore.size());
        assertThat(offSubjectsAfter).containsAll(offSubjectsBefore);
    }

    private static @NonNull List<Pair<String, String>> getManyVariants(FeatureFlag flag) {
        val results = new ArrayList<Pair<String, String>>();

        for (int i = 0; i < 1000; i++) {
            val recipient = "user" + i;
            val variant = flag.getVariant(recipient);
            results.add(new Pair<>(recipient, variant));
        }

        return results;
    }
}
