package com.msgbridge.dto;

import java.time.Instant;

public record AppResponse(
        Long id,
        String appId,
        String appName,
        String signType,
        String ipWhitelist,
        Integer rateLimitPerMin,
        Integer status,
        String ownerName,
        String ownerContact,
        String remark,
        Instant createdAt,
        Instant updatedAt
) {
}
