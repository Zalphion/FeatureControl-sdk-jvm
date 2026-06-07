package com.zalphion.featurecontrol.lib;

import lombok.Data;
import lombok.NonNull;

import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Data
public class Pair<K, V> {
    private final @NonNull K key;
    private final @NonNull V value;

    public static <K, V> Collector<Pair<K, V>, ?, Map<K,V>> toMap() {
        return Collectors.toMap(Pair::getKey, Pair::getValue);
    }
}
