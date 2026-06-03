package com.msgbridge.admin;

import com.msgbridge.dto.ApiResponse;
import com.msgbridge.dto.AppResponse;
import com.msgbridge.dto.AppUpdateRequest;
import com.msgbridge.dto.CreateAppRequest;
import com.msgbridge.dto.CreateAppResponse;
import com.msgbridge.dto.ToggleStatusRequest;
import com.msgbridge.service.AppService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/apps")
public class AppAdminController {
    private final AppService appService;

    public AppAdminController(AppService appService) {
        this.appService = appService;
    }

    @GetMapping
    public ApiResponse<List<AppResponse>> list() {
        return ApiResponse.ok(appService.list());
    }

    @PostMapping
    public ApiResponse<CreateAppResponse> create(@Valid @RequestBody CreateAppRequest request) {
        return ApiResponse.ok(appService.create(request));
    }

    @PutMapping("/{appId}")
    public ApiResponse<AppResponse> update(@PathVariable String appId, @RequestBody AppUpdateRequest request) {
        return ApiResponse.ok(appService.update(appId, request));
    }

    @PostMapping("/{appId}/status")
    public ApiResponse<AppResponse> status(@PathVariable String appId, @RequestBody ToggleStatusRequest request) {
        return ApiResponse.ok(appService.setStatus(appId, request.normalized()));
    }

    @PostMapping("/{appId}/reset-secret")
    public ApiResponse<CreateAppResponse> resetSecret(@PathVariable String appId) {
        return ApiResponse.ok(appService.resetSecret(appId));
    }
}
