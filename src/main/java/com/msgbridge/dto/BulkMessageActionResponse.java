package com.msgbridge.dto;

import java.util.List;

public record BulkMessageActionResponse(
        int requested,
        int succeeded,
        int failed,
        List<ItemResult> results
) {
    public record ItemResult(
            String messageId,
            boolean success,
            String message
    ) {
    }
}
