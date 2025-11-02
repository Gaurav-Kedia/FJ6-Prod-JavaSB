package com.foreverjava.Dto;

import com.foreverjava.execution.SupportedJavaVersion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CodeExecutionRequest {

    @NotBlank(message = "Code must not be blank")
    private String code;

    private String input = "";

    @NotNull(message = "Java version must not be null")
    private SupportedJavaVersion javaVersion;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public SupportedJavaVersion getJavaVersion() {
        return javaVersion;
    }

    public void setJavaVersion(SupportedJavaVersion javaVersion) {
        this.javaVersion = javaVersion;
    }
}
