package com.msgbridge.service;

import com.msgbridge.core.BusinessException;
import com.msgbridge.core.Constants;
import com.msgbridge.crypto.CryptoService;
import com.msgbridge.domain.MbApp;
import com.msgbridge.dto.AppResponse;
import com.msgbridge.dto.AppUpdateRequest;
import com.msgbridge.dto.CreateAppRequest;
import com.msgbridge.dto.CreateAppResponse;
import com.msgbridge.repository.AppRepository;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppService {
    private final AppRepository appRepository;
    private final CryptoService cryptoService;
    private final AuditLogService auditLogService;
    private final SecureRandom secureRandom = new SecureRandom();

    public AppService(AppRepository appRepository, CryptoService cryptoService, AuditLogService auditLogService) {
        this.appRepository = appRepository;
        this.cryptoService = cryptoService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public CreateAppResponse create(CreateAppRequest request) {
        if (appRepository.existsByAppId(request.appId())) {
            throw BusinessException.conflict("appId already exists");
        }
        String secret = generateSecret();
        MbApp app = new MbApp();
        app.setAppId(request.appId());
        app.setAppName(request.appName());
        app.setAppSecretEncrypted(cryptoService.encrypt(secret));
        app.setRateLimitPerMin(request.rateLimitPerMin() == null ? 600 : request.rateLimitPerMin());
        app.setIpWhitelist(request.ipWhitelist());
        app.setOwnerName(request.ownerName());
        app.setOwnerContact(request.ownerContact());
        app.setRemark(request.remark());
        appRepository.save(app);
        auditLogService.record("CREATE_APP", "APP", request.appId());
        return new CreateAppResponse(toResponse(app), secret);
    }

    @Transactional(readOnly = true)
    public List<AppResponse> list() {
        return appRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public AppResponse update(String appId, AppUpdateRequest request) {
        MbApp app = appRepository.findByAppId(appId)
                .orElseThrow(() -> BusinessException.notFound("app not found"));
        if (request.appName() != null) {
            app.setAppName(request.appName());
        }
        if (request.rateLimitPerMin() != null) {
            app.setRateLimitPerMin(Math.max(1, request.rateLimitPerMin()));
        }
        if (request.ipWhitelist() != null) {
            app.setIpWhitelist(request.ipWhitelist());
        }
        if (request.status() != null) {
            app.setStatus(request.status() == Constants.STATUS_ENABLED ? Constants.STATUS_ENABLED : Constants.STATUS_DISABLED);
        }
        if (request.ownerName() != null) {
            app.setOwnerName(request.ownerName());
        }
        if (request.ownerContact() != null) {
            app.setOwnerContact(request.ownerContact());
        }
        if (request.remark() != null) {
            app.setRemark(request.remark());
        }
        auditLogService.record("UPDATE_APP", "APP", appId);
        return toResponse(app);
    }

    @Transactional
    public AppResponse setStatus(String appId, int status) {
        MbApp app = appRepository.findByAppId(appId)
                .orElseThrow(() -> BusinessException.notFound("app not found"));
        app.setStatus(status == Constants.STATUS_ENABLED ? Constants.STATUS_ENABLED : Constants.STATUS_DISABLED);
        auditLogService.record("SET_APP_STATUS", "APP", appId);
        return toResponse(app);
    }

    @Transactional
    public CreateAppResponse resetSecret(String appId) {
        MbApp app = appRepository.findByAppId(appId)
                .orElseThrow(() -> BusinessException.notFound("app not found"));
        String secret = generateSecret();
        app.setAppSecretEncrypted(cryptoService.encrypt(secret));
        auditLogService.record("RESET_APP_SECRET", "APP", appId);
        return new CreateAppResponse(toResponse(app), secret);
    }

    @Transactional
    public void ensureDemoApp() {
        if (appRepository.existsByAppId("demo")) {
            return;
        }
        MbApp app = new MbApp();
        app.setAppId("demo");
        app.setAppName("Demo Business System");
        app.setAppSecretEncrypted(cryptoService.encrypt("demo-secret"));
        app.setRateLimitPerMin(600);
        app.setRemark("Seeded local development app. Reset before production use.");
        appRepository.save(app);
    }

    public AppResponse toResponse(MbApp app) {
        return new AppResponse(
                app.getId(),
                app.getAppId(),
                app.getAppName(),
                app.getSignType(),
                app.getIpWhitelist(),
                app.getRateLimitPerMin(),
                app.getStatus(),
                app.getOwnerName(),
                app.getOwnerContact(),
                app.getRemark(),
                app.getCreatedAt(),
                app.getUpdatedAt());
    }

    private String generateSecret() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
