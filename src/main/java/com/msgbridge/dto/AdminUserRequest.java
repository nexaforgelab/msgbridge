package com.msgbridge.dto;

import com.msgbridge.core.AdminRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminUserRequest(
        @NotBlank String username,
        @NotBlank String displayName,
        String password,
        @NotNull AdminRole role,
        Integer status
) {
}
