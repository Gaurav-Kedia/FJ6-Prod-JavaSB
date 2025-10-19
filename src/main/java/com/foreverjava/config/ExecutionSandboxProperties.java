package com.foreverjava.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Validated
@ConfigurationProperties(prefix = "sandbox")
public class ExecutionSandboxProperties {

    private Path baseDir = Paths.get(System.getProperty("java.io.tmpdir"), "code-exec");

    @NotNull
    private Duration compileTimeout = Duration.ofSeconds(5);

    @NotNull
    private Duration executionTimeout = Duration.ofSeconds(5);

    @Min(64)
    private int maxMemoryMb = 256;

    @Min(1)
    private int poolSize = Math.max(4, Runtime.getRuntime().availableProcessors());

    @Min(1)
    private int queueCapacity = 200;

    private boolean cleanup = true;

    private String fallbackClassName = "Main";

    private Map<String, Path> jdkPaths = new HashMap<>();

    private String defaultVersion;

    @Min(1024)
    private long maxOutputBytes = 1_048_576L;

    public Path getBaseDir() {
        return baseDir;
    }

    public void setBaseDir(Path baseDir) {
        this.baseDir = baseDir;
    }

    public Duration getCompileTimeout() {
        return compileTimeout;
    }

    public void setCompileTimeout(Duration compileTimeout) {
        this.compileTimeout = compileTimeout;
    }

    public Duration getExecutionTimeout() {
        return executionTimeout;
    }

    public void setExecutionTimeout(Duration executionTimeout) {
        this.executionTimeout = executionTimeout;
    }

    public int getMaxMemoryMb() {
        return maxMemoryMb;
    }

    public void setMaxMemoryMb(int maxMemoryMb) {
        this.maxMemoryMb = maxMemoryMb;
    }

    public int getPoolSize() {
        return poolSize;
    }

    public void setPoolSize(int poolSize) {
        this.poolSize = poolSize;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    public boolean isCleanup() {
        return cleanup;
    }

    public void setCleanup(boolean cleanup) {
        this.cleanup = cleanup;
    }

    public String getFallbackClassName() {
        return fallbackClassName;
    }

    public void setFallbackClassName(String fallbackClassName) {
        this.fallbackClassName = fallbackClassName;
    }

    public Map<String, Path> getJdkPaths() {
        return jdkPaths;
    }

    public void setJdkPaths(Map<String, Path> jdkPaths) {
        this.jdkPaths = jdkPaths;
    }

    public String getDefaultVersion() {
        return defaultVersion;
    }

    public void setDefaultVersion(String defaultVersion) {
        this.defaultVersion = defaultVersion;
    }

    public long getMaxOutputBytes() {
        return maxOutputBytes;
    }

    public void setMaxOutputBytes(long maxOutputBytes) {
        this.maxOutputBytes = maxOutputBytes;
    }
}
