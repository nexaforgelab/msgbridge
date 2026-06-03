package com.msgbridge.dto;

public record PurgeResponse(
        long deletedMessages,
        long deletedSendLogs
) {
}
