package com.msgbridge.dto;

import com.msgbridge.core.ChannelType;
import java.time.Instant;
import java.util.Map;

public record ChannelResponse(
        Long id,
        String channelCode,
        String channelName,
        ChannelType channelType,
        Map<String, Object> config,
        Map<String, Object> secrets,
        Integer status,
        String remark,
        Instant createdAt,
        Instant updatedAt
) {
}
