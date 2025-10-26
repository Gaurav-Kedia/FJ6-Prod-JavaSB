package com.foreverjava.execution;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Enumerates the JDK releases that the sandbox is configured to run.
 */
public enum SupportedJavaVersion {
    JAVA_8("8"),
    JAVA_11("11"),
    JAVA_17("17"),
    JAVA_21("21");

    private final String featureVersion;

    SupportedJavaVersion(String featureVersion) {
        this.featureVersion = featureVersion;
    }

    /**
     * Accept JSON payloads such as "17", "jdk17", or "JAVA_17" and map them to the enum.
     */
    @JsonCreator
    public static SupportedJavaVersion fromValue(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Java version must not be blank");
        }
        String normalised = trimmed.toUpperCase(Locale.ROOT).replace("JDK", "JAVA").replace('-', '_');
        if (!normalised.startsWith("JAVA_")) {
            normalised = "JAVA_" + normalised;
        }
        for (SupportedJavaVersion candidate : values()) {
            if (candidate.name().equals(normalised) || candidate.featureVersion.equals(trimmed)) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("Unsupported Java version: " + value);
    }

    /**
     * Expose the feature version (e.g. "17") to API consumers.
     */
    @JsonValue
    public String getFeatureVersion() {
        return featureVersion;
    }

    public String getDisplayName() {
        return "JDK " + featureVersion;
    }

    public String getEnvironmentVariableName() {
        return "JDK_" + featureVersion + "_HOME";
    }

    public static Optional<SupportedJavaVersion> fromFeature(int feature) {
        String asString = Integer.toString(feature);
        return Arrays.stream(values())
                .filter(version -> version.featureVersion.equals(asString))
                .findFirst();
    }
}
