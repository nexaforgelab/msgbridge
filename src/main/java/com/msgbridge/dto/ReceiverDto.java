package com.msgbridge.dto;

import java.util.List;

public record ReceiverDto(
        String type,
        String routeKey,
        List<String> channelCodes
) {
}
