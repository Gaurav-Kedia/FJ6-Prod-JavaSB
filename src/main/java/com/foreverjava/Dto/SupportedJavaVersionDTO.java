package com.foreverjava.Dto;

import com.foreverjava.execution.SupportedJavaVersion;

/**
 * Simple DTO that exposes the supported JDK options to API clients so UIs can
 * populate version selectors without hardcoding the list.
 */
public record SupportedJavaVersionDTO(String value, String displayName, String environmentVariable) {

    public static SupportedJavaVersionDTO from(SupportedJavaVersion version) {
        return new SupportedJavaVersionDTO(
                version.getFeatureVersion(),
                version.getDisplayName(),
                version.getEnvironmentVariableName()
        );
    }
}
