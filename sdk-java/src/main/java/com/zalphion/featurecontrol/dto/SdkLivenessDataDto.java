package com.zalphion.featurecontrol.dto;

import lombok.Data;

@Data
public class SdkLivenessDataDto {
    private final String sdkId;
    private final String platform;
    private final String version;
    private final String repositoryUrl;
}
