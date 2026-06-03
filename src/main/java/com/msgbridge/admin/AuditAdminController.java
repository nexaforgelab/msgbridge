package com.msgbridge.admin;

import com.msgbridge.dto.ApiResponse;
import com.msgbridge.dto.AuditLogResponse;
import com.msgbridge.dto.AuditQueryRequest;
import com.msgbridge.service.AuditLogService;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/audit-logs")
public class AuditAdminController {
    private final AuditLogService auditLogService;

    public AuditAdminController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ApiResponse<List<AuditLogResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String resourceId,
            @RequestParam(required = false) Instant createdFrom,
            @RequestParam(required = false) Instant createdTo) {
        return ApiResponse.ok(auditLogService.list(
                page,
                size,
                new AuditQueryRequest(actor, action, resourceType, resourceId, createdFrom, createdTo)));
    }
}
