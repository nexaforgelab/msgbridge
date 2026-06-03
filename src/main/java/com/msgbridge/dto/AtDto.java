package com.msgbridge.dto;

import java.util.List;

public record AtDto(
        Boolean all,
        List<String> mobiles,
        List<String> userIds
) {
    public boolean atAll() {
        return Boolean.TRUE.equals(all);
    }
}
