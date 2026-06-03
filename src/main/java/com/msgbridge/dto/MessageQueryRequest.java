package com.msgbridge.dto;

import java.time.Instant;

public record MessageQueryRequest(
        String appId,
        String sceneCode,
        String routeKey,
        String status,
        String requestId,
        String messageId,
        Instant createdFrom,
        Instant createdTo
) {
}
