package com.msgbridge.admin;

import com.msgbridge.dto.ApiResponse;
import com.msgbridge.dto.RouteRuleRequest;
import com.msgbridge.dto.RouteRuleResponse;
import com.msgbridge.dto.RouteSimulateRequest;
import com.msgbridge.dto.RouteSimulateResponse;
import com.msgbridge.dto.ToggleStatusRequest;
import com.msgbridge.service.RouteService;
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
@RequestMapping("/admin/routes")
public class RouteAdminController {
    private final RouteService routeService;

    public RouteAdminController(RouteService routeService) {
        this.routeService = routeService;
    }

    @GetMapping
    public ApiResponse<List<RouteRuleResponse>> list() {
        return ApiResponse.ok(routeService.list());
    }

    @PostMapping
    public ApiResponse<RouteRuleResponse> create(@Valid @RequestBody RouteRuleRequest request) {
        return ApiResponse.ok(routeService.create(request));
    }

    @PutMapping("/{ruleCode}")
    public ApiResponse<RouteRuleResponse> update(
            @PathVariable String ruleCode,
            @Valid @RequestBody RouteRuleRequest request) {
        return ApiResponse.ok(routeService.update(ruleCode, request));
    }

    @PostMapping("/{ruleCode}/status")
    public ApiResponse<RouteRuleResponse> status(
            @PathVariable String ruleCode,
            @RequestBody ToggleStatusRequest request) {
        return ApiResponse.ok(routeService.setStatus(ruleCode, request.normalized()));
    }

    @PostMapping("/simulate")
    public ApiResponse<RouteSimulateResponse> simulate(@Valid @RequestBody RouteSimulateRequest request) {
        return ApiResponse.ok(routeService.simulate(request));
    }
}
