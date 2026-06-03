package com.msgbridge.admin;

import com.msgbridge.adapter.SendResult;
import com.msgbridge.dto.ApiResponse;
import com.msgbridge.dto.ChannelHealthResponse;
import com.msgbridge.dto.ChannelResponse;
import com.msgbridge.dto.ChannelTestRequest;
import com.msgbridge.dto.CreateChannelRequest;
import com.msgbridge.dto.ToggleStatusRequest;
import com.msgbridge.service.ChannelService;
import com.msgbridge.service.ChannelTestService;
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
@RequestMapping("/admin/channels")
public class ChannelAdminController {
    private final ChannelService channelService;
    private final ChannelTestService channelTestService;

    public ChannelAdminController(ChannelService channelService, ChannelTestService channelTestService) {
        this.channelService = channelService;
        this.channelTestService = channelTestService;
    }

    @GetMapping
    public ApiResponse<List<ChannelResponse>> list() {
        return ApiResponse.ok(channelService.list());
    }

    @GetMapping("/health")
    public ApiResponse<List<ChannelHealthResponse>> health() {
        return ApiResponse.ok(channelService.health());
    }

    @GetMapping("/{channelCode}")
    public ApiResponse<ChannelResponse> get(@PathVariable String channelCode) {
        return ApiResponse.ok(channelService.get(channelCode));
    }

    @PostMapping
    public ApiResponse<ChannelResponse> create(@Valid @RequestBody CreateChannelRequest request) {
        return ApiResponse.ok(channelService.create(request));
    }

    @PutMapping("/{channelCode}")
    public ApiResponse<ChannelResponse> update(
            @PathVariable String channelCode,
            @Valid @RequestBody CreateChannelRequest request) {
        return ApiResponse.ok(channelService.update(channelCode, request));
    }

    @PostMapping("/{channelCode}/status")
    public ApiResponse<ChannelResponse> status(
            @PathVariable String channelCode,
            @RequestBody ToggleStatusRequest request) {
        return ApiResponse.ok(channelService.setStatus(channelCode, request.normalized()));
    }

    @PostMapping("/{channelCode}/test")
    public ApiResponse<SendResult> test(
            @PathVariable String channelCode,
            @RequestBody(required = false) ChannelTestRequest request) {
        return ApiResponse.ok(channelTestService.test(channelCode, request));
    }
}
