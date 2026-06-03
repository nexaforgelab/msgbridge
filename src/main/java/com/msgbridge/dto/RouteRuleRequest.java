package com.msgbridge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record RouteRuleRequest(
        @NotBlank String ruleCode,
        @NotBlank String ruleName,
        @NotBlank String sceneCode,
        String routeKey,
        String conditionExpr,
        @NotEmpty List<String> targetChannels,
        Integer priority,
        Integer status
) {
}
