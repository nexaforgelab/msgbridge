package com.msgbridge.webhook;

import com.msgbridge.core.Constants;
import com.msgbridge.dto.ApiResponse;
import com.msgbridge.dto.ReceiverDto;
import com.msgbridge.dto.SendMessageRequest;
import com.msgbridge.dto.SendMessageResponse;
import com.msgbridge.dto.UnifiedMessageDto;
import com.msgbridge.service.JsonService;
import com.msgbridge.service.MessageService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WebhookController {
    private final MessageService messageService;
    private final JsonService jsonService;

    public WebhookController(MessageService messageService, JsonService jsonService) {
        this.messageService = messageService;
        this.jsonService = jsonService;
    }

    @PostMapping("/webhook/{appId}/{sceneCode}")
    public ApiResponse<SendMessageResponse> inbound(
            @PathVariable String appId,
            @PathVariable String sceneCode,
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        String requestId = string(body.get("requestId"), "webhook-" + UUID.randomUUID());
        String routeKey = string(first(body, "routeKey", "route_key"), null);
        UnifiedMessageDto message = message(body);
        SendMessageRequest sendRequest = new SendMessageRequest(
                requestId,
                appId,
                sceneCode,
                string(body.get("priority"), "NORMAL"),
                new ReceiverDto("ROUTE", routeKey, null),
                message,
                null);
        return ApiResponse.accepted(messageService.accept(
                sendRequest,
                String.valueOf(request.getAttribute(Constants.CURRENT_APP_ID_ATTR))));
    }

    private UnifiedMessageDto message(Map<String, Object> body) {
        Object nested = body.get("message");
        if (nested instanceof Map<?, ?>) {
            return jsonService.convert(nested, UnifiedMessageDto.class);
        }
        Map<String, Object> data = new LinkedHashMap<>(body);
        data.remove("requestId");
        data.remove("routeKey");
        data.remove("route_key");
        data.remove("priority");
        String title = string(first(body, "title", "summary"), "Webhook 通知");
        String content = string(first(body, "content", "text", "message"), jsonService.toJson(data));
        String url = string(first(body, "url", "link"), null);
        return new UnifiedMessageDto("TEXT", title, null, content, null, url, null, data);
    }

    private Object first(Map<String, Object> body, String... keys) {
        for (String key : keys) {
            if (body.containsKey(key)) {
                return body.get(key);
            }
        }
        return null;
    }

    private String string(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }
}
