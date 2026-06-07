package com.zalphion.featurecontrol;

import java.io.IOException;

public class TestFixtures {

    private TestFixtures() {}

    public static final com.zalphion.featurecontrol.bundle.ApplicationBundleDto bundle1;

    static {
        try {
            bundle1 = com.zalphion.featurecontrol.bundle.ApplicationBundleDto.fromClasspath("com/zalphion/featurecontrol/bundle1.json");
        } catch (IOException e) {
            throw new RuntimeException("Failed to load test bundle", e);
        }
    }
}
