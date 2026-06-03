package com.msgbridge.adapter;

import com.msgbridge.core.ChannelType;
import com.msgbridge.core.MessageType;
import com.msgbridge.service.JsonService;
import com.msgbridge.service.TemplateRenderer.PreparedMessage;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class WeComRobotAdapter extends AbstractRobotAdapter {

    public WeComRobotAdapter(WebClient.Builder webClientBuilder, JsonService jsonService) {
        super(webClientBuilder, jsonService);
    }

    @Override
    public ChannelType channelType() {
        return ChannelType.WE_COM_ROBOT;
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
        return postJson(channelType(), context.channel().getId(), url, body(context.message()), context.timeoutSeconds());
    }

    private Map<String, Object> body(PreparedMessage message) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (message.type() == MessageType.TEXT) {
            body.put("msgtype", "text");
            Map<String, Object> text = new LinkedHashMap<>();
            text.put("content", withUrl(message.content(), message.url()));
            if (message.at() != null && message.at().atAll()) {
                text.put("mentioned_list", List.of("@all"));
            } else if (message.at() != null && message.at().userIds() != null) {
                text.put("mentioned_list", message.at().userIds());
            }
            if (message.at() != null && message.at().mobiles() != null) {
                text.put("mentioned_mobile_list", message.at().mobiles());
            }
            body.put("text", text);
            return body;
        }
        body.put("msgtype", "markdown");
        body.put("markdown", Map.of("content", withUrl(message.content(), message.url())));
        return body;
    }

    @Override
    protected SendResult parsePlatformResponse(
            ChannelType platform, Long channelId, String requestJson, String rawResponse, int costMs) {
        Map<String, Object> map = jsonService.readMap(rawResponse);
        String code = String.valueOf(map.getOrDefault("errcode", ""));
        String msg = String.valueOf(map.getOrDefault("errmsg", ""));
        boolean success = "0".equals(code);
        boolean retryable = !success && ("45009".equals(code) || "50001".equals(code) || code.isBlank());
        return new SendResult(success, platform, channelId, code, msg, requestJson, rawResponse, costMs, retryable);
    }

    private String withUrl(String content, String url) {
        if (url == null || url.isBlank() || (content != null && content.contains(url))) {
            return content;
        }
        return (content == null ? "" : content) + "\n" + url;
    }
}
