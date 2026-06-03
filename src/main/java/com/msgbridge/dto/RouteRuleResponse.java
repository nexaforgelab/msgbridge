package com.msgbridge.dto;

import java.time.Instant;
import java.util.List;

public record RouteRuleResponse(
        Long id,
        String ruleCode,
        String ruleName,
        String sceneCode,
        String routeKey,
        String conditionExpr,
        List<String> targetChannels,
        Integer priority,
        Integer status,
        Instant createdAt,
        Instant updatedAt
) {
}
