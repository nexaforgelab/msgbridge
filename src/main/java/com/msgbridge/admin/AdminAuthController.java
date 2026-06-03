package com.msgbridge.admin;

import com.msgbridge.dto.AdminLoginRequest;
import com.msgbridge.dto.AdminLoginResponse;
import com.msgbridge.dto.ApiResponse;
import com.msgbridge.service.AdminUserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/auth")
public class AdminAuthController {
    private final AdminUserService adminUserService;

    public AdminAuthController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @PostMapping("/login")
    public ApiResponse<AdminLoginResponse> login(@Valid @RequestBody AdminLoginRequest request) {
        return ApiResponse.ok(adminUserService.login(request));
    }
}
