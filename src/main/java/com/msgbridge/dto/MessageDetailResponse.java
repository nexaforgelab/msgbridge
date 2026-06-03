package com.msgbridge.dto;

import java.util.List;
import java.util.Map;

public record MessageDetailResponse(
        Map<String, Object> task,
        List<Map<String, Object>> logs
) {
}
