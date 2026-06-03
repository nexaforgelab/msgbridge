package com.msgbridge.adapter;

import com.msgbridge.core.ChannelType;
import com.msgbridge.core.MessageType;
import com.msgbridge.crypto.HmacSigner;
import com.msgbridge.service.JsonService;
import com.msgbridge.service.TemplateRenderer.PreparedMessage;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class FeishuRobotAdapter extends AbstractRobotAdapter {
    private final HmacSigner hmacSigner;

    public FeishuRobotAdapter(WebClient.Builder webClientBuilder, JsonService jsonService, HmacSigner hmacSigner) {
        super(webClientBuilder, jsonService);
        this.hmacSigner = hmacSigner;
    }

    @Override
    public ChannelType channelType() {
        return ChannelType.FEISHU_ROBOT;
    }

    @Override
    public boolean supports(MessageType messageType) {
        return messageType == MessageType.TEXT
                || messageType == MessageType.MARKDOWN
                || messageType == MessageType.LINK
                || messageType == MessageType.ALERT;
    }

    @Override
    public SendResult send(SendContext context) {
        String url = AdapterConfig.string(context.config(), "webhook_url", "webhookUrl", "webhook");
        Map<String, Object> body = body(context.message());
        if (AdapterConfig.bool(context.config(), "sign_enabled", "signEnabled")) {
            sign(body, AdapterConfig.string(context.config(), "secret", "sign_secret", "signSecret"));
        }
        return postJson(channelType(), context.channel().getId(), url, body, context.timeoutSeconds());
    }

    private void sign(Map<String, Object> body, String secret) {
        if (secret == null || secret.isBlank()) {
            return;
        }
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String sign = hmacSigner.hmacSha256Base64(timestamp + "\n" + secret, "");
        body.put("timestamp", timestamp);
        body.put("sign", sign);
    }

    private Map<String, Object> body(PreparedMessage message) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (message.type() == MessageType.TEXT) {
            body.put("msg_type", "text");
            body.put("content", Map.of("text", withUrl(message.content(), message.url())));
            return body;
        }
        body.put("msg_type", "post");
        body.put("content", Map.of("post", Map.of("zh_cn", Map.of(
                "title", message.title(),
                "content", List.of(List.of(Map.of("tag", "text", "text", withUrl(message.content(), message.url()))))))));
        return body;
    }

    @Override
    protected SendResult parsePlatformResponse(
            ChannelType platform, Long channelId, String requestJson, String rawResponse, int costMs) {
        Map<String, Object> map = jsonService.readMap(rawResponse);
        Object codeValue = map.containsKey("StatusCode") ? map.get("StatusCode") : map.get("code");
        Object msgValue = map.containsKey("StatusMessage") ? map.get("StatusMessage") : map.get("msg");
        String code = codeValue == null ? "" : String.valueOf(codeValue);
        String msg = msgValue == null ? "" : String.valueOf(msgValue);
        boolean success = "0".equals(code);
        boolean retryable = !success && ("99991663".equals(code) || "99991400".equals(code) || code.isBlank());
        return new SendResult(success, platform, channelId, code, msg, requestJson, rawResponse, costMs, retryable);
    }

    private String withUrl(String content, String url) {
        if (url == null || url.isBlank() || (content != null && content.contains(url))) {
            return content;
        }
        return (content == null ? "" : content) + "\n" + url;
    }
}
