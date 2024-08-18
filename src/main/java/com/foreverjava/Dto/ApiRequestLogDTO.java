package com.foreverjava.Dto;

import java.time.LocalDateTime;

public class ApiRequestLogDTO {
    private Long requestId;
    private Long ipv6Id;
    private LocalDateTime requestTime;

    // Getters and Setters
    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }

    public Long getIpv6Id() {
        return ipv6Id;
    }

    public void setIpv6Id(Long ipv6Id) {
        this.ipv6Id = ipv6Id;
    }

    public LocalDateTime getRequestTime() {
        return requestTime;
    }

    public void setRequestTime(LocalDateTime requestTime) {
        this.requestTime = requestTime;
    }
}