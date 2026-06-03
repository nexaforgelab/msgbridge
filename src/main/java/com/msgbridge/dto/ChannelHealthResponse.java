package com.msgbridge.dto;

import com.msgbridge.core.ChannelType;
import java.time.Instant;

public record ChannelHealthResponse(
        String channelCode,
        String channelName,
        ChannelType channelType,
        Integer status,
        boolean configured,
        boolean supported,
        String health,
        String lastSendStatus,
        String lastPlatformCode,
        String lastPlatformMessage,
        Instant lastSentAt
) {
}
