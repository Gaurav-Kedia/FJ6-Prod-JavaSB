package com.foreverjava.Dto;

public class UserIPv6DTO {
    private Long ipv6Id;
    private String ipv6Value;
    private Integer roleId;

    // Getters and Setters
    public Long getIpv6Id() {
        return ipv6Id;
    }

    public void setIpv6Id(Long ipv6Id) {
        this.ipv6Id = ipv6Id;
    }

    public String getIpv6Value() {
        return ipv6Value;
    }

    public void setIpv6Value(String ipv6Value) {
        this.ipv6Value = ipv6Value;
    }

    public Integer getRoleId() {
        return roleId;
    }

    public void setRoleId(Integer roleId) {
        this.roleId = roleId;
    }
}