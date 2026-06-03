package com.msgbridge.adapter;

import com.msgbridge.core.ChannelType;
import com.msgbridge.core.MessageType;

public interface MessageChannelAdapter {
    ChannelType channelType();

    boolean supports(MessageType messageType);

    SendResult send(SendContext context);
}
