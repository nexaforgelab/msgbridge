package com.msgbridge.admin;

import com.msgbridge.dto.ApiResponse;
import com.msgbridge.dto.TemplatePreviewRequest;
import com.msgbridge.dto.TemplatePreviewResponse;
import com.msgbridge.dto.TemplateRequest;
import com.msgbridge.dto.TemplateResponse;
import com.msgbridge.dto.ToggleStatusRequest;
import com.msgbridge.service.TemplateService;
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
@RequestMapping("/admin/templates")
public class TemplateAdminController {
    private final TemplateService templateService;

    public TemplateAdminController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping
    public ApiResponse<List<TemplateResponse>> list() {
        return ApiResponse.ok(templateService.list());
    }

    @PostMapping
    public ApiResponse<TemplateResponse> create(@Valid @RequestBody TemplateRequest request) {
        return ApiResponse.ok(templateService.create(request));
    }

    @PutMapping("/{templateCode}")
    public ApiResponse<TemplateResponse> update(
            @PathVariable String templateCode,
            @Valid @RequestBody TemplateRequest request) {
        return ApiResponse.ok(templateService.update(templateCode, request));
    }

    @PostMapping("/{templateCode}/status")
    public ApiResponse<TemplateResponse> status(
            @PathVariable String templateCode,
            @RequestBody ToggleStatusRequest request) {
        return ApiResponse.ok(templateService.setStatus(templateCode, request.normalized()));
    }

    @PostMapping("/preview")
    public ApiResponse<TemplatePreviewResponse> preview(@RequestBody TemplatePreviewRequest request) {
        return ApiResponse.ok(templateService.preview(request));
    }
}
