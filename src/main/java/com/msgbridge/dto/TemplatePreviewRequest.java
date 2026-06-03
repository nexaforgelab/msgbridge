package com.msgbridge.dto;

import com.msgbridge.core.ChannelType;
import com.msgbridge.core.MessageType;
import java.util.Map;

public record TemplatePreviewRequest(
        String templateCode,
        String sceneCode,
        ChannelType channelType,
        MessageType msgType,
        String contentTemplate,
        UnifiedMessageDto message,
        Map<String, Object> data
) {
}
