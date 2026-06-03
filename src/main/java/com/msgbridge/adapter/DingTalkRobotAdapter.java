package com.msgbridge.adapter;

import com.msgbridge.core.ChannelType;
import com.msgbridge.core.MessageType;
import com.msgbridge.crypto.HmacSigner;
import com.msgbridge.service.JsonService;
import com.msgbridge.service.TemplateRenderer.PreparedMessage;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class DingTalkRobotAdapter extends AbstractRobotAdapter {
    private final HmacSigner hmacSigner;

    public DingTalkRobotAdapter(WebClient.Builder webClientBuilder, JsonService jsonService, HmacSigner hmacSigner) {
        super(webClientBuilder, jsonService);
        this.hmacSigner = hmacSigner;
    }

    @Override
    public ChannelType channelType() {
        return ChannelType.DINGTALK_ROBOT;
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
        if (AdapterConfig.bool(context.config(), "sign_enabled", "signEnabled")) {
            url = signedUrl(url, AdapterConfig.string(context.config(), "secret", "sign_secret", "signSecret"));
        }
        return postJson(channelType(), context.channel().getId(), url, body(context.message()), context.timeoutSeconds());
    }

    private String signedUrl(String url, String secret) {
        if (url == null || url.isBlank() || secret == null || secret.isBlank()) {
            return url;
        }
        long timestamp = Instant.now().toEpochMilli();
        String sign = hmacSigner.hmacSha256Base64(secret, timestamp + "\n" + secret);
        String separator = url.contains("?") ? "&" : "?";
        return url + separator + "timestamp=" + timestamp + "&sign="
                + URLEncoder.encode(sign, StandardCharsets.UTF_8);
    }

    private Map<String, Object> body(PreparedMessage message) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (message.type() == MessageType.TEXT) {
            body.put("msgtype", "text");
            body.put("text", Map.of("content", withUrl(message.content(), message.url())));
        } else if (message.type() == MessageType.LINK) {
            body.put("msgtype", "link");
            body.put("link", Map.of(
                    "title", message.title(),
                    "text", message.content(),
                    "messageUrl", message.url() == null ? "" : message.url()));
        } else {
            body.put("msgtype", "markdown");
            body.put("markdown", Map.of("title", message.title(), "text", withUrl(message.content(), message.url())));
        }
        if (message.at() != null) {
            Map<String, Object> at = new LinkedHashMap<>();
            at.put("isAtAll", message.at().atAll());
            if (message.at().mobiles() != null) {
                at.put("atMobiles", message.at().mobiles());
            }
            body.put("at", at);
        }
        return body;
    }

    @Override
    protected SendResult parsePlatformResponse(
            ChannelType platform, Long channelId, String requestJson, String rawResponse, int costMs) {
        Map<String, Object> map = jsonService.readMap(rawResponse);
        String code = String.valueOf(map.getOrDefault("errcode", ""));
        String msg = String.valueOf(map.getOrDefault("errmsg", ""));
        boolean success = "0".equals(code);
        boolean retryable = !success && ("310000".equals(code) || code.isBlank());
        return new SendResult(success, platform, channelId, code, msg, requestJson, rawResponse, costMs, retryable);
    }

    private String withUrl(String content, String url) {
        if (url == null || url.isBlank() || (content != null && content.contains(url))) {
            return content;
        }
        return (content == null ? "" : content) + "\n" + url;
    }
}
