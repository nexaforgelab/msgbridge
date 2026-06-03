package com.msgbridge.service;

import com.msgbridge.config.MsgBridgeProperties;
import com.msgbridge.core.AdminRole;
import com.msgbridge.core.BusinessException;
import com.msgbridge.core.Constants;
import com.msgbridge.domain.MbAdminUser;
import com.msgbridge.dto.AdminLoginRequest;
import com.msgbridge.dto.AdminLoginResponse;
import com.msgbridge.dto.AdminUserRequest;
import com.msgbridge.dto.AdminUserResponse;
import com.msgbridge.dto.PasswordResetRequest;
import com.msgbridge.repository.AdminUserRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminUserService {
    private final AdminUserRepository adminUserRepository;
    private final PasswordService passwordService;
    private final AdminTokenService adminTokenService;
    private final AuditLogService auditLogService;
    private final MsgBridgeProperties properties;

    public AdminUserService(
            AdminUserRepository adminUserRepository,
            PasswordService passwordService,
            AdminTokenService adminTokenService,
            AuditLogService auditLogService,
            MsgBridgeProperties properties) {
        this.adminUserRepository = adminUserRepository;
        this.passwordService = passwordService;
        this.adminTokenService = adminTokenService;
        this.auditLogService = auditLogService;
        this.properties = properties;
    }

    @Transactional
    public AdminLoginResponse login(AdminLoginRequest request) {
        MbAdminUser user = adminUserRepository.findByUsername(request.username())
                .orElseThrow(() -> BusinessException.unauthorized("invalid username or password"));
        if (user.getStatus() == null || user.getStatus() != Constants.STATUS_ENABLED
                || !passwordService.verify(request.password(), user.getPasswordHash())) {
            throw BusinessException.unauthorized("invalid username or password");
        }
        user.setLastLoginAt(Instant.now());
        AdminTokenService.TokenIssue issue = adminTokenService.issue(user.getUsername(), user.getDisplayName(), user.getRole());
        auditLogService.record(user.getUsername(), "ADMIN_LOGIN", "ADMIN_USER", user.getUsername(), null);
        return new AdminLoginResponse(issue.token(), issue.expiresAt(), user.getUsername(), user.getDisplayName(), user.getRole());
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> list() {
        return adminUserRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public AdminUserResponse create(AdminUserRequest request) {
        if (adminUserRepository.existsByUsername(request.username())) {
            throw BusinessException.conflict("username already exists");
        }
        if (!StringUtils.hasText(request.password())) {
            throw BusinessException.badRequest("password is required when creating user");
        }
        MbAdminUser user = new MbAdminUser();
        user.setUsername(request.username());
        user.setDisplayName(request.displayName());
        user.setPasswordHash(passwordService.hash(request.password()));
        user.setRole(request.role());
        user.setStatus(request.status() == null ? Constants.STATUS_ENABLED : request.status());
        adminUserRepository.save(user);
        auditLogService.record("CREATE_ADMIN_USER", "ADMIN_USER", request.username());
        return toResponse(user);
    }

    @Transactional
    public AdminUserResponse update(String username, AdminUserRequest request) {
        MbAdminUser user = adminUserRepository.findByUsername(username)
                .orElseThrow(() -> BusinessException.notFound("admin user not found"));
        user.setDisplayName(request.displayName());
        user.setRole(request.role());
        user.setStatus(request.status() == null ? user.getStatus() : request.status());
        if (StringUtils.hasText(request.password())) {
            user.setPasswordHash(passwordService.hash(request.password()));
        }
        auditLogService.record("UPDATE_ADMIN_USER", "ADMIN_USER", username);
        return toResponse(user);
    }

    @Transactional
    public AdminUserResponse setStatus(String username, int status) {
        MbAdminUser user = adminUserRepository.findByUsername(username)
                .orElseThrow(() -> BusinessException.notFound("admin user not found"));
        user.setStatus(status == Constants.STATUS_ENABLED ? Constants.STATUS_ENABLED : Constants.STATUS_DISABLED);
        auditLogService.record("SET_ADMIN_USER_STATUS", "ADMIN_USER", username);
        return toResponse(user);
    }

    @Transactional
    public void resetPassword(String username, PasswordResetRequest request) {
        MbAdminUser user = adminUserRepository.findByUsername(username)
                .orElseThrow(() -> BusinessException.notFound("admin user not found"));
        user.setPasswordHash(passwordService.hash(request.password()));
        auditLogService.record("RESET_ADMIN_PASSWORD", "ADMIN_USER", username);
    }

    @Transactional
    public void ensureDefaultAdmin() {
        String username = properties.getSecurity().getDefaultAdminUsername();
        if (!StringUtils.hasText(username) || adminUserRepository.existsByUsername(username)) {
            return;
        }
        MbAdminUser user = new MbAdminUser();
        user.setUsername(username);
        user.setDisplayName("Super Admin");
        user.setPasswordHash(passwordService.hash(properties.getSecurity().getDefaultAdminPassword()));
        user.setRole(AdminRole.SUPER_ADMIN);
        user.setStatus(Constants.STATUS_ENABLED);
        adminUserRepository.save(user);
    }

    public AdminUserResponse toResponse(MbAdminUser user) {
        return new AdminUserResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getRole(),
                user.getStatus(),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
