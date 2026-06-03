package com.msgbridge.adapter;

import com.msgbridge.domain.MbChannel;
import com.msgbridge.service.TemplateRenderer.PreparedMessage;
import java.util.Map;

public record SendContext(
        MbChannel channel,
        Map<String, Object> config,
        PreparedMessage message,
        int timeoutSeconds
) {
}
