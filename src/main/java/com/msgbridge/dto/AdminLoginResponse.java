package com.msgbridge.dto;

import com.msgbridge.core.AdminRole;
import java.time.Instant;

public record AdminLoginResponse(
        String token,
        Instant expiresAt,
        String username,
        String displayName,
        AdminRole role
) {
}
