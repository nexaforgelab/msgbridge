package com.msgbridge.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public record RouteSimulateRequest(
        @NotBlank String sceneCode,
        String routeKey,
        Map<String, Object> data
) {
}
