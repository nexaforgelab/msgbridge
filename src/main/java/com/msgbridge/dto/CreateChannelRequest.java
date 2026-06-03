package com.msgbridge.dto;

import com.msgbridge.core.ChannelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record CreateChannelRequest(
        @NotBlank String channelCode,
        @NotBlank String channelName,
        @NotNull ChannelType channelType,
        Map<String, Object> config,
        Map<String, Object> secrets,
        String remark
) {
}
