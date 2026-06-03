package com.msgbridge.dto;

public record SendMessageResponse(
        String requestId,
        String messageId,
        String status,
        boolean duplicate
) {
}
