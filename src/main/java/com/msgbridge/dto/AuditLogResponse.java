package com.msgbridge.dto;

import java.time.Instant;
import java.util.Map;

public record AuditLogResponse(
        Long id,
        String actor,
        String action,
        String resourceType,
        String resourceId,
        Map<String, Object> detail,
        Instant createdAt
) {
}
