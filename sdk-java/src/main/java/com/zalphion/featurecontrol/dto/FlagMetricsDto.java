package com.zalphion.featurecontrol.dto;

import lombok.Data;

import java.util.Map;

@Data
public class FlagMetricsDto {
    private final String name;
    private final int failedEvaluations;
    private final Map<String, Integer> successfulEvaluations;
}