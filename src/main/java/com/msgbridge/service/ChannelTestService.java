package com.msgbridge.service;

import com.msgbridge.adapter.AdapterRegistry;
import com.msgbridge.adapter.MessageChannelAdapter;
import com.msgbridge.adapter.SendContext;
import com.msgbridge.adapter.SendResult;
import com.msgbridge.core.MessageType;
import com.msgbridge.domain.MbChannel;
import com.msgbridge.dto.AtDto;
import com.msgbridge.dto.ChannelTestRequest;
import com.msgbridge.service.TemplateRenderer.PreparedMessage;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChannelTestService {
    private final ChannelService channelService;
    private final AdapterRegistry adapterRegistry;

    public ChannelTestService(ChannelService channelService, AdapterRegistry adapterRegistry) {
        this.channelService = channelService;
        this.adapterRegistry = adapterRegistry;
    }

    @Transactional(readOnly = true)
    public SendResult test(String channelCode, ChannelTestRequest request) {
        MbChannel channel = channelService.getEnabled(channelCode);
        MessageType type = parseType(request == null ? null : request.msgType());
        PreparedMessage message = new PreparedMessage(
                type,
                request == null || request.title() == null ? "MsgBridge 测试消息" : request.title(),
                request == null || request.content() == null ? "这是一条来自 MsgBridge 的渠道测试消息。" : request.content(),
                null,
                new AtDto(false, null, null),
                Map.of());
        MessageChannelAdapter adapter = adapterRegistry.get(channel.getChannelType());
        return adapter.send(new SendContext(channel, channelService.mergedConfig(channel), message, 10));
    }

    private MessageType parseType(String value) {
        if (value == null || value.isBlank()) {
            return MessageType.TEXT;
        }
        try {
            return MessageType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return MessageType.TEXT;
        }
    }
}
