package com.msgbridge.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateAppRequest(
        @NotBlank String appId,
        @NotBlank String appName,
        Integer rateLimitPerMin,
        String ipWhitelist,
        String ownerName,
        String ownerContact,
        String remark
) {
}
