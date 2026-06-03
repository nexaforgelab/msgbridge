package com.msgbridge.adapter;

import com.msgbridge.core.ChannelType;

public record SendResult(
        boolean success,
        ChannelType platform,
        Long channelId,
        String platformCode,
        String platformMessage,
        String requestJson,
        String rawResponse,
        int costMs,
        boolean retryable
) {
    public static SendResult failure(ChannelType platform, Long channelId, String code, String message, boolean retryable) {
        return new SendResult(false, platform, channelId, code, message, null, null, 0, retryable);
    }
}
