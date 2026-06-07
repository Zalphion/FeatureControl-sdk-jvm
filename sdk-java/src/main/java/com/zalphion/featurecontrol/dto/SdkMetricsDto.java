package com.zalphion.featurecontrol.dto;

import lombok.Data;

import java.util.List;

@Data
public class SdkMetricsDto {
    private final int missingFlagEvaluations;
    private final List<FlagMetricsDto> flags;
}