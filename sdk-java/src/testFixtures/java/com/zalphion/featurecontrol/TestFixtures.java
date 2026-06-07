package com.zalphion.featurecontrol;

import com.zalphion.featurecontrol.dto.ApplicationBundleDto;
import com.zalphion.featurecontrol.source.StaticApplicationSource;

import java.io.IOException;

public class TestFixtures {

    private TestFixtures() {}

    public static final ApplicationBundleDto bundle1;

    static {
        try {
            bundle1 = StaticApplicationSource.fromClasspath("com/zalphion/featurecontrol/bundle1.json");
        } catch (IOException e) {
            throw new RuntimeException("Failed to load test bundle", e);
        }
    }
}
