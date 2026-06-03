package com.msgbridge.dto;

public record ToggleStatusRequest(Integer status) {
    public int normalized() {
        return status != null && status == 1 ? 1 : 0;
    }
}
