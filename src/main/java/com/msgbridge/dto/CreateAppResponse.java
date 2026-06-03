package com.msgbridge.dto;

public record CreateAppResponse(
        AppResponse app,
        String appSecret
) {
}
