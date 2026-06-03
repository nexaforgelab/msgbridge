package com.msgbridge.adapter;

import com.msgbridge.core.BusinessException;
import com.msgbridge.core.ChannelType;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AdapterRegistry {
    private final Map<ChannelType, MessageChannelAdapter> adapters = new EnumMap<>(ChannelType.class);

    public AdapterRegistry(List<MessageChannelAdapter> adapters) {
        for (MessageChannelAdapter adapter : adapters) {
            this.adapters.put(adapter.channelType(), adapter);
        }
    }

    public MessageChannelAdapter get(ChannelType channelType) {
        MessageChannelAdapter adapter = adapters.get(channelType);
        if (adapter == null) {
            throw BusinessException.badRequest("channel type is not supported in this version: " + channelType);
        }
        return adapter;
    }
}
