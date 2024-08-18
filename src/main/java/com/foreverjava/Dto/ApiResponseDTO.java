package com.foreverjava.Dto;

public class ApiResponseDTO {
    private int requestCount;
    private String status;
    private String message;

    public ApiResponseDTO(int requestCount, String status, String message) {
        this.requestCount = requestCount;
        this.status = status;
        this.message = message;
    }

    // Getters and Setters
    public int getRequestCount() {
        return requestCount;
    }

    public void setRequestCount(int requestCount) {
        this.requestCount = requestCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}