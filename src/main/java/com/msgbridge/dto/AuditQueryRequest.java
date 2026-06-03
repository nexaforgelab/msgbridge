package com.msgbridge.dto;

import java.time.Instant;

public record AuditQueryRequest(
        String actor,
        String action,
        String resourceType,
        String resourceId,
        Instant createdFrom,
        Instant createdTo
) {
}
