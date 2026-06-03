package com.msgbridge.dto;

import com.msgbridge.core.ChannelType;
import com.msgbridge.core.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record TemplateRequest(
        @NotBlank String templateCode,
        @NotBlank String templateName,
        @NotBlank String sceneCode,
        @NotNull ChannelType channelType,
        @NotNull MessageType msgType,
        @NotBlank String contentTemplate,
        Map<String, Object> variables,
        Integer version,
        Integer status
) {
}
