package com.foreverjava.Dto;

import jakarta.validation.constraints.NotBlank;

public class CodeExecutionRequest {

    @NotBlank(message = "Code must not be blank")
    private String code;

    private String input = "";

    @NotBlank(message = "Java version must not be blank")
    private String javaVersion;

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

    public String getJavaVersion() {
        return javaVersion;
    }

    public void setJavaVersion(String javaVersion) {
        this.javaVersion = javaVersion;
    }
}
