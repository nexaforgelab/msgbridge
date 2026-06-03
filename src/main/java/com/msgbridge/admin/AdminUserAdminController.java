package com.msgbridge.admin;

import com.msgbridge.dto.AdminUserRequest;
import com.msgbridge.dto.AdminUserResponse;
import com.msgbridge.dto.ApiResponse;
import com.msgbridge.dto.PasswordResetRequest;
import com.msgbridge.dto.ToggleStatusRequest;
import com.msgbridge.service.AdminUserService;
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
@RequestMapping("/admin/users")
public class AdminUserAdminController {
    private final AdminUserService adminUserService;

    public AdminUserAdminController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public ApiResponse<List<AdminUserResponse>> list() {
        return ApiResponse.ok(adminUserService.list());
    }

    @PostMapping
    public ApiResponse<AdminUserResponse> create(@Valid @RequestBody AdminUserRequest request) {
        return ApiResponse.ok(adminUserService.create(request));
    }

    @PutMapping("/{username}")
    public ApiResponse<AdminUserResponse> update(
            @PathVariable String username,
            @Valid @RequestBody AdminUserRequest request) {
        return ApiResponse.ok(adminUserService.update(username, request));
    }

    @PostMapping("/{username}/status")
    public ApiResponse<AdminUserResponse> status(
            @PathVariable String username,
            @RequestBody ToggleStatusRequest request) {
        return ApiResponse.ok(adminUserService.setStatus(username, request.normalized()));
    }

    @PostMapping("/{username}/reset-password")
    public ApiResponse<Void> resetPassword(
            @PathVariable String username,
            @Valid @RequestBody PasswordResetRequest request) {
        adminUserService.resetPassword(username, request);
        return ApiResponse.ok(null);
    }
}
