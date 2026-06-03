package com.msgbridge.admin;

import com.msgbridge.dto.ApiResponse;
import com.msgbridge.dto.BulkMessageActionRequest;
import com.msgbridge.dto.BulkMessageActionResponse;
import com.msgbridge.dto.MessageDetailResponse;
import com.msgbridge.dto.MessageQueryRequest;
import com.msgbridge.dto.PurgeResponse;
import com.msgbridge.dto.PurgePreviewResponse;
import com.msgbridge.service.MessageService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class MessageAdminController {
    private final MessageService messageService;

    public MessageAdminController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Object>> dashboard() {
        return ApiResponse.ok(messageService.dashboard());
    }

    @GetMapping("/messages")
    public ApiResponse<List<Map<String, Object>>> messages(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String appId,
            @RequestParam(required = false) String sceneCode,
            @RequestParam(required = false) String routeKey,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String requestId,
            @RequestParam(required = false) String messageId,
            @RequestParam(required = false) Instant createdFrom,
            @RequestParam(required = false) Instant createdTo) {
        return ApiResponse.ok(messageService.adminList(
                page,
                size,
                new MessageQueryRequest(appId, sceneCode, routeKey, status, requestId, messageId, createdFrom, createdTo)));
    }

    @GetMapping("/messages/export")
    public ResponseEntity<String> export(
            @RequestParam(required = false) String appId,
            @RequestParam(required = false) String sceneCode,
            @RequestParam(required = false) String routeKey,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String requestId,
            @RequestParam(required = false) String messageId,
            @RequestParam(required = false) Instant createdFrom,
            @RequestParam(required = false) Instant createdTo) {
        String csv = messageService.exportCsv(
                new MessageQueryRequest(appId, sceneCode, routeKey, status, requestId, messageId, createdFrom, createdTo));
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=msgbridge-messages.csv")
                .body(csv);
    }

    @GetMapping("/messages/{messageId}")
    public ApiResponse<MessageDetailResponse> detail(@PathVariable String messageId) {
        return ApiResponse.ok(messageService.adminDetail(messageId));
    }

    @PostMapping("/messages/{messageId}/retry")
    public ApiResponse<Void> retry(@PathVariable String messageId) {
        messageService.markManualRetry(messageId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/messages/{messageId}/terminate")
    public ApiResponse<Void> terminate(@PathVariable String messageId) {
        messageService.terminate(messageId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/messages/bulk-retry")
    public ApiResponse<BulkMessageActionResponse> bulkRetry(@Valid @RequestBody BulkMessageActionRequest request) {
        return ApiResponse.ok(messageService.bulkRetry(request.messageIds()));
    }

    @PostMapping("/messages/bulk-terminate")
    public ApiResponse<BulkMessageActionResponse> bulkTerminate(@Valid @RequestBody BulkMessageActionRequest request) {
        return ApiResponse.ok(messageService.bulkTerminate(request.messageIds()));
    }

    @GetMapping("/messages/purge-preview")
    public ApiResponse<PurgePreviewResponse> purgePreview(@RequestParam Instant before) {
        return ApiResponse.ok(messageService.purgePreview(before));
    }

    @PostMapping("/messages/purge")
    public ApiResponse<PurgeResponse> purge(@RequestParam Instant before) {
        return ApiResponse.ok(messageService.purgeBefore(before));
    }
}
