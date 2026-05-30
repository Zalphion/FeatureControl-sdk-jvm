package com.zalphion.featurecontrol.bundle;

import argo.InvalidSyntaxException;
import argo.JsonGenerator;
import argo.JsonParser;
import argo.jdom.JsonField;
import argo.jdom.JsonNode;
import com.zalphion.featurecontrol.lib.Result;
import org.jspecify.annotations.NonNull;
import java.util.stream.Collectors;
import lombok.val;

import static argo.jdom.JsonNodeFactories.*;
import static argo.jdom.JsonNodeSelectors.*;

public class ApplicationBundleJson {

    private ApplicationBundleJson() {}

    private static final String
            PROPERTIES = "properties",
            FLAGS = "flags",
            OVERRIDES = "overrides",
            BUCKETS = "buckets",
            SALT_BASE_64 = "saltBase64",
            BUCKET_NAME = "name",
            BUCKET_THRESHOLD = "threshold";

    public static @NonNull Result<ApplicationBundle> fromJson(@NonNull @lombok.NonNull String json) {
        try {
            val bundle = new JsonParser().parse(json);
            return Result.success(fromJson(bundle));
        } catch (InvalidSyntaxException e) {
            return Result.failure("Error parsing feature bundle JSON: " + e.getMessage());
        }
    }

    private static @NonNull ApplicationBundle fromJson(@NonNull @lombok.NonNull JsonNode bundle) {
        val properties = anObjectNodeWithField(PROPERTIES).getValue(bundle)
                .getFieldList().stream().collect(Collectors.toMap(
                        JsonField::getNameText,
                        property -> aStringNode().getValue(property.getValue())
                ));

        val flags = anObjectNodeWithField(FLAGS).getValue(bundle)
                .getFieldList().stream().collect(Collectors.toMap(
                        JsonField::getNameText,
                flag -> parseFlag(flag.getValue())
                ));

        return new ApplicationBundle(properties, flags);
    }

    private static @NonNull FlagDefinition parseFlag(@NonNull JsonNode node) {
        return new FlagDefinition(
                anObjectNodeWithField(OVERRIDES).getValue(node)
                        .getFieldList().stream().collect(Collectors.toMap(
                                JsonField::getNameText,
                        override -> aStringNode().getValue(override.getValue())
                        )),
                anArrayNode(BUCKETS).getValue(node).stream().map(bucket ->
                        new VariantDefinition(
                                aStringNode(BUCKET_NAME).getValue(bucket),
                                Integer.parseInt(aNumberNode(BUCKET_THRESHOLD).getValue(bucket))
                        )
                ).collect(Collectors.toList()),
                aStringNode(SALT_BASE_64).getValue(node)
        );
    }

    public static @NonNull String toJson(@NonNull @lombok.NonNull ApplicationBundle bundle) {
        return new JsonGenerator().generate(object(
                field(PROPERTIES, object(
                        bundle.getProperties().entrySet().stream().map(entry ->
                                field(entry.getKey(), string(entry.getValue()))
                        ).collect(Collectors.toList())
                )),
                field(FLAGS, object(
                        bundle.getFlags().entrySet().stream().map(flag ->
                                field(flag.getKey(), object(
                                        field(OVERRIDES, object(
                                                flag.getValue().getOverrides().entrySet().stream().map(override ->
                                                        field(override.getKey(), string(override.getValue()))
                                                ).collect(Collectors.toList())
                                        )),
                                        field(BUCKETS, array(
                                                flag.getValue().getBuckets().stream().map(bucket ->
                                                        object(
                                                                field(BUCKET_NAME, string(bucket.getName())),
                                                                field(BUCKET_THRESHOLD, number(bucket.getThreshold()))
                                                        )
                                                ).collect(Collectors.toList())
                                        )),
                                        field(SALT_BASE_64, string(flag.getValue().getSaltBase64()))
                                ))
                        ).collect(Collectors.toList())
                ))
        ));
    }
}
