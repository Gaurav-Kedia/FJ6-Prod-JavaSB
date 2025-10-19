package com.foreverjava.Dto;

public class CodeExecutionResponse {

    private final String javaVersion;
    private final boolean compileSuccessful;
    private final long compileTimeMillis;
    private final String compileStdout;
    private final String compileStderr;
    private final boolean executionAttempted;
    private final boolean executionSuccessful;
    private final Long executionTimeMillis;
    private final Integer exitCode;
    private final String executionStdout;
    private final String executionStderr;
    private final Long memoryUsageKilobytes;
    private final boolean outputTruncated;
    private final boolean errorTruncated;

    public CodeExecutionResponse(
            String javaVersion,
            boolean compileSuccessful,
            long compileTimeMillis,
            String compileStdout,
            String compileStderr,
            boolean executionAttempted,
            boolean executionSuccessful,
            Long executionTimeMillis,
            Integer exitCode,
            String executionStdout,
            String executionStderr,
            Long memoryUsageKilobytes,
            boolean outputTruncated,
            boolean errorTruncated) {
        this.javaVersion = javaVersion;
        this.compileSuccessful = compileSuccessful;
        this.compileTimeMillis = compileTimeMillis;
        this.compileStdout = compileStdout;
        this.compileStderr = compileStderr;
        this.executionAttempted = executionAttempted;
        this.executionSuccessful = executionSuccessful;
        this.executionTimeMillis = executionTimeMillis;
        this.exitCode = exitCode;
        this.executionStdout = executionStdout;
        this.executionStderr = executionStderr;
        this.memoryUsageKilobytes = memoryUsageKilobytes;
        this.outputTruncated = outputTruncated;
        this.errorTruncated = errorTruncated;
    }

    public String getJavaVersion() {
        return javaVersion;
    }

    public boolean isCompileSuccessful() {
        return compileSuccessful;
    }

    public long getCompileTimeMillis() {
        return compileTimeMillis;
    }

    public String getCompileStdout() {
        return compileStdout;
    }

    public String getCompileStderr() {
        return compileStderr;
    }

    public boolean isExecutionAttempted() {
        return executionAttempted;
    }

    public boolean isExecutionSuccessful() {
        return executionSuccessful;
    }

    public Long getExecutionTimeMillis() {
        return executionTimeMillis;
    }

    public Integer getExitCode() {
        return exitCode;
    }

    public String getExecutionStdout() {
        return executionStdout;
    }

    public String getExecutionStderr() {
        return executionStderr;
    }

    public Long getMemoryUsageKilobytes() {
        return memoryUsageKilobytes;
    }

    public boolean isOutputTruncated() {
        return outputTruncated;
    }

    public boolean isErrorTruncated() {
        return errorTruncated;
    }
}
