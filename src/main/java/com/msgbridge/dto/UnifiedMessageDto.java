package com.msgbridge.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UnifiedMessageDto(
        String type,
        String title,
        String summary,
        String content,
        String level,
        String url,
        AtDto at,
        Map<String, Object> data
) {
}
