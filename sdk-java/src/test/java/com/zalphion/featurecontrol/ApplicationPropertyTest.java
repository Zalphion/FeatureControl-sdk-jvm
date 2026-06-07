package com.zalphion.featurecontrol;

import com.zalphion.featurecontrol.lib.Result;
import com.zalphion.featurecontrol.source.ApplicationSource;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import lombok.val;

import java.util.concurrent.atomic.AtomicBoolean;

public class ApplicationPropertyTest {

    private final ApplicationSource source = TestFixtures.bundle1.toSource();

    @Test
    public void missingProperty() {
        assertThat(source.stringProperty("missing", "default").getValue())
                .isEqualTo("default");
    }

    @Test
    public void stringProperty() {
        assertThat(source.stringProperty("str", "default").getValue())
                .isEqualTo("foo");
    }

    @Test
    public void intProperty() {
        assertThat(source.property("int", Integer::parseInt, 1).getValue())
                .isEqualTo(42);
    }

    @Test
    public void propertyWithSupplier() {
        val ref = new AtomicBoolean(false);
        val property = ApplicationProperty.create(ref::get);

        assertThat(property.getValue()).isEqualTo(false);

        ref.set(true);
        assertThat(property.getValue()).isEqualTo(true);
    }

    @Test
    public void getValue_sourceFailure() {
        val source = ApplicationSource.createWithResult(Result.failure("foo"));
        assertThat(source.stringProperty("str", "default").getValue())
                .isEqualTo("default");
    }
}
