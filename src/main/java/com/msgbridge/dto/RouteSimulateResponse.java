package com.msgbridge.dto;

import java.util.List;

public record RouteSimulateResponse(
        List<String> channelCodes,
        List<String> matchedRules
) {
}
