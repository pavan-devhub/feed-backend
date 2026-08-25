package com.feedstartup.dto;

import java.time.LocalDateTime;

public class ActiveSessionDto {

    private Long id;
    private String deviceName;
    private String browser;
    private String os;
    private String ipAddress;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime lastActiveAt;
    private boolean current;

    public ActiveSessionDto() {}

    public ActiveSessionDto(Long id, String deviceName, String browser, String os, String ipAddress,
                             LocalDateTime createdAt, LocalDateTime lastActiveAt, boolean current) {
        this.id = id;
        this.deviceName = deviceName;
        this.browser = browser;
        this.os = os;
        this.ipAddress = ipAddress;
        this.createdAt = createdAt;
        this.lastActiveAt = lastActiveAt;
        this.current = current;
        this.status = current ? "current" : "active";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getBrowser() {
        return browser;
    }

    public void setBrowser(String browser) {
        this.browser = browser;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastActiveAt() {
        return lastActiveAt;
    }

    public void setLastActiveAt(LocalDateTime lastActiveAt) {
        this.lastActiveAt = lastActiveAt;
    }

    public boolean isCurrent() {
        return current;
    }

    public void setCurrent(boolean current) {
        this.current = current;
        this.status = current ? "current" : "active";
    }
}
