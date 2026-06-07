package com.zalphion.featurecontrol.dto;

import lombok.Data;
import org.jspecify.annotations.NonNull;

@Data
public class VariantDefinition {
    private final @NonNull String name;
    private final int threshold;
}
