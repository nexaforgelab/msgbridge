package com.msgbridge.service;

import com.msgbridge.core.MessageType;
import com.msgbridge.domain.MbTemplate;
import com.msgbridge.dto.UnifiedMessageDto;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class TemplateRenderer {
    private static final Pattern VARIABLE = Pattern.compile("\\$\\{([a-zA-Z0-9_.-]+)}");

    public PreparedMessage render(MbTemplate template, UnifiedMessageDto message) {
        Map<String, Object> variables = variables(message);
        MessageType type = parseType(message == null ? null : message.type(), MessageType.TEXT);
        String title = message == null ? null : message.title();
        String content = message == null ? null : message.content();
        if (template != null) {
            type = template.getMsgType();
            content = replace(template.getContentTemplate(), variables);
        }
        if (content == null || content.isBlank()) {
            content = fallbackContent(message);
        }
        return new PreparedMessage(
                type,
                title == null || title.isBlank() ? "MsgBridge 通知" : title,
                content,
                message == null ? null : message.url(),
                message == null ? null : message.at(),
                variables);
    }

    public Map<String, Object> variables(UnifiedMessageDto message) {
        Map<String, Object> variables = new LinkedHashMap<>();
        if (message == null) {
            return variables;
        }
        if (message.data() != null) {
            variables.putAll(message.data());
        }
        variables.put("title", message.title());
        variables.put("summary", message.summary());
        variables.put("content", message.content());
        variables.put("level", message.level());
        variables.put("url", message.url());
        return variables;
    }

    private String replace(String template, Map<String, Object> variables) {
        Matcher matcher = VARIABLE.matcher(template == null ? "" : template);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            Object value = variables.get(matcher.group(1));
            matcher.appendReplacement(sb, Matcher.quoteReplacement(value == null ? "" : String.valueOf(value)));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String fallbackContent(UnifiedMessageDto message) {
        if (message == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        if (message.title() != null && !message.title().isBlank()) {
            sb.append(message.title()).append('\n');
        }
        if (message.content() != null) {
            sb.append(message.content());
        }
        if (message.url() != null && !message.url().isBlank()) {
            sb.append("\n").append(message.url());
        }
        return sb.toString().trim();
    }

    private MessageType parseType(String value, MessageType fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return MessageType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    public record PreparedMessage(
            MessageType type,
            String title,
            String content,
            String url,
            com.msgbridge.dto.AtDto at,
            Map<String, Object> variables
    ) {
    }
}
