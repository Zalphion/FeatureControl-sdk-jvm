package com.zalphion.featurecontrol.dto;

import lombok.Data;

import java.util.List;

@Data
public class SdkMetricsDto {
    private final int cacheHits;
    private final int cacheMisses;
    private final int unmodifiedHits;
    private final int missingFlagEvaluations;
    private final List<FlagMetricsDto> flags;
}