package com.zalphion.featurecontrol.dto;

import com.zalphion.featurecontrol.lib.Result;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.val;
import org.jspecify.annotations.NonNull;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;

@Data
@Builder
public class FlagDefinition {
    private final @Singular @NonNull Map<@NonNull String, @NonNull String> overrides;
    private final @Singular @NonNull List<@NonNull VariantDefinition> buckets;
    private final @NonNull String saltBase64;

    public Result<String> evaluate(@NonNull @lombok.NonNull String recipient) {
        val overrideValue = overrides.get(recipient);
        if (overrideValue != null) return Result.success(overrideValue);

        if (buckets.isEmpty()) return Result.failure("Buckets are empty");

        final long hash;
        {
            val hashFunction = new CRC32();
            hashFunction.update(recipient.getBytes(StandardCharsets.UTF_8));
            hashFunction.update(Base64.getDecoder().decode(saltBase64));

            val modulo = buckets.get(buckets.size() - 1).getThreshold();
            hash = hashFunction.getValue() % modulo;
        }

        return buckets.stream()
                .filter(bucket -> hash < bucket.getThreshold())
                .findAny()
                .map(bucket -> Result.success(bucket.getName()))
                .orElseGet(() -> Result.failure("No matching variant bucket found"));
    }

}
