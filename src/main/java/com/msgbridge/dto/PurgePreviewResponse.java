package com.msgbridge.dto;

import java.time.Instant;

public record PurgePreviewResponse(
        Instant before,
        long messages,
        long sendLogs
) {
}
