package com.msgbridge.dto;

import com.msgbridge.core.ChannelType;
import com.msgbridge.core.MessageType;
import java.time.Instant;
import java.util.Map;

public record TemplateResponse(
        Long id,
        String templateCode,
        String templateName,
        String sceneCode,
        ChannelType channelType,
        MessageType msgType,
        String contentTemplate,
        Map<String, Object> variables,
        Integer version,
        Integer status,
        Instant createdAt,
        Instant updatedAt
) {
}
