package com.msgbridge.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SendMessageRequest(
        @NotBlank String requestId,
        @NotBlank String appId,
        @NotBlank String sceneCode,
        String priority,
        @Valid @NotNull ReceiverDto receiver,
        @Valid @NotNull UnifiedMessageDto message,
        MessageOptionsDto options
) {
    public String routeKey() {
        return receiver == null ? null : receiver.routeKey();
    }
}
