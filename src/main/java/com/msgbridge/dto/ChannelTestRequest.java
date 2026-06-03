package com.msgbridge.dto;

public record ChannelTestRequest(
        String title,
        String content,
        String msgType
) {
}
