package com.msgbridge.dto;

public record AppUpdateRequest(
        String appName,
        Integer rateLimitPerMin,
        String ipWhitelist,
        Integer status,
        String ownerName,
        String ownerContact,
        String remark
) {
}
