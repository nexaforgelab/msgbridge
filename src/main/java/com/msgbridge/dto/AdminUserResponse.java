package com.msgbridge.dto;

import com.msgbridge.core.AdminRole;
import java.time.Instant;

public record AdminUserResponse(
        Long id,
        String username,
        String displayName,
        AdminRole role,
        Integer status,
        Instant lastLoginAt,
        Instant createdAt,
        Instant updatedAt
) {
}
