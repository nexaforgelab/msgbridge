package com.msgbridge.service;

import com.msgbridge.domain.MbAuditLog;
import com.msgbridge.dto.AuditLogResponse;
import com.msgbridge.dto.AuditQueryRequest;
import com.msgbridge.repository.AuditLogRepository;
import com.msgbridge.core.Constants;
import java.util.Map;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class AuditLogService {
    private final AuditLogRepository auditLogRepository;
    private final JsonService jsonService;

    public AuditLogService(AuditLogRepository auditLogRepository, JsonService jsonService) {
        this.auditLogRepository = auditLogRepository;
        this.jsonService = jsonService;
    }

    public void record(String actor, String action, String resourceType, String resourceId, Object detail) {
        MbAuditLog log = new MbAuditLog();
        log.setActor(StringUtils.hasText(actor) ? actor : currentActor());
        log.setAction(action);
        log.setResourceType(resourceType);
        log.setResourceId(resourceId);
        log.setDetailJson(detail == null ? "{}" : jsonService.toJson(detail));
        auditLogRepository.save(log);
    }

    public void record(String action, String resourceType, String resourceId) {
        record(null, action, resourceType, resourceId, Map.of());
    }

    public List<AuditLogResponse> list(int page, int size) {
        return list(page, size, null);
    }

    public List<AuditLogResponse> list(int page, int size, AuditQueryRequest query) {
        return auditLogRepository.findAll(spec(query), PageRequest.of(
                        Math.max(page, 0),
                        Math.max(1, Math.min(size, 200)),
                        Sort.by(Sort.Direction.DESC, "createdAt")))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private AuditLogResponse toResponse(MbAuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getActor(),
                log.getAction(),
                log.getResourceType(),
                log.getResourceId(),
                jsonService.readMap(log.getDetailJson()),
                log.getCreatedAt());
    }

    private Specification<MbAuditLog> spec(AuditQueryRequest query) {
        return (root, criteriaQuery, builder) -> {
            if (query == null) {
                return builder.conjunction();
            }
            var predicate = builder.conjunction();
            if (StringUtils.hasText(query.actor())) {
                predicate = builder.and(predicate, builder.like(root.get("actor"), "%" + query.actor() + "%"));
            }
            if (StringUtils.hasText(query.action())) {
                predicate = builder.and(predicate, builder.equal(root.get("action"), query.action()));
            }
            if (StringUtils.hasText(query.resourceType())) {
                predicate = builder.and(predicate, builder.equal(root.get("resourceType"), query.resourceType()));
            }
            if (StringUtils.hasText(query.resourceId())) {
                predicate = builder.and(predicate, builder.like(root.get("resourceId"), "%" + query.resourceId() + "%"));
            }
            if (query.createdFrom() != null) {
                predicate = builder.and(predicate, builder.greaterThanOrEqualTo(root.get("createdAt"), query.createdFrom()));
            }
            if (query.createdTo() != null) {
                predicate = builder.and(predicate, builder.lessThanOrEqualTo(root.get("createdAt"), query.createdTo()));
            }
            return predicate;
        };
    }

    private String currentActor() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            Object username = attributes.getRequest().getAttribute(Constants.ADMIN_USERNAME_ATTR);
            if (username != null && StringUtils.hasText(String.valueOf(username))) {
                return String.valueOf(username);
            }
        }
        return "system";
    }
}
