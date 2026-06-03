package com.msgbridge.admin;

import com.msgbridge.dto.ApiResponse;
import com.msgbridge.service.SystemStatusService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/system")
public class SystemAdminController {
    private final SystemStatusService systemStatusService;

    public SystemAdminController(SystemStatusService systemStatusService) {
        this.systemStatusService = systemStatusService;
    }

    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> status() {
        return ApiResponse.ok(systemStatusService.status());
    }
}
