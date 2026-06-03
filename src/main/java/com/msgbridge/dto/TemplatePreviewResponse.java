package com.msgbridge.dto;

import com.msgbridge.core.ChannelType;
import com.msgbridge.core.MessageType;
import java.util.Map;

public record TemplatePreviewResponse(
        String templateCode,
        String sceneCode,
        ChannelType channelType,
        MessageType msgType,
        String title,
        String content,
        String url,
        Map<String, Object> variables
) {
}
