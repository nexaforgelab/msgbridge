package com.msgbridge.api;

import com.msgbridge.core.Constants;
import com.msgbridge.dto.ApiResponse;
import com.msgbridge.dto.BatchSendRequest;
import com.msgbridge.dto.MessageDetailResponse;
import com.msgbridge.dto.SendMessageRequest;
import com.msgbridge.dto.SendMessageResponse;
import com.msgbridge.service.MessageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/messages")
public class MessageController {
    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping("/send")
    public ApiResponse<SendMessageResponse> send(
            @Valid @RequestBody SendMessageRequest request,
            HttpServletRequest servletRequest) {
        return ApiResponse.accepted(messageService.accept(request, currentAppId(servletRequest)));
    }

    @PostMapping("/batch-send")
    public ApiResponse<List<SendMessageResponse>> batchSend(
            @Valid @RequestBody BatchSendRequest request,
            HttpServletRequest servletRequest) {
        String appId = currentAppId(servletRequest);
        return ApiResponse.accepted(request.messages().stream()
                .map(message -> messageService.accept(message, appId))
                .toList());
    }

    @GetMapping("/{messageId}")
    public ApiResponse<MessageDetailResponse> byMessageId(
            @PathVariable String messageId,
            HttpServletRequest servletRequest) {
        return ApiResponse.ok(messageService.detailByMessageId(messageId, currentAppId(servletRequest)));
    }

    @GetMapping("/by-request-id")
    public ApiResponse<MessageDetailResponse> byRequestId(
            @RequestParam String appId,
            @RequestParam String requestId,
            HttpServletRequest servletRequest) {
        return ApiResponse.ok(messageService.detailByRequestId(appId, requestId, currentAppId(servletRequest)));
    }

    @PostMapping("/{messageId}/retry")
    public ApiResponse<Void> retry(
            @PathVariable String messageId,
            HttpServletRequest servletRequest) {
        messageService.markManualRetry(messageId, currentAppId(servletRequest));
        return ApiResponse.ok(null);
    }

    private String currentAppId(HttpServletRequest request) {
        return String.valueOf(request.getAttribute(Constants.CURRENT_APP_ID_ATTR));
    }
}
