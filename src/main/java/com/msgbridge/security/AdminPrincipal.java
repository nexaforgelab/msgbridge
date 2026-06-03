package com.msgbridge.security;

import com.msgbridge.core.AdminRole;

public record AdminPrincipal(
        String username,
        String displayName,
        AdminRole role
) {
}
