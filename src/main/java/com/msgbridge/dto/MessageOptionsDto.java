package com.msgbridge.dto;

public record MessageOptionsDto(
        Boolean deduplicate,
        Integer timeoutSeconds,
        Boolean retry,
        Integer maxRetryCount
) {
    public boolean retryEnabled() {
        return retry == null || Boolean.TRUE.equals(retry);
    }

    public int effectiveMaxRetryCount() {
        if (!retryEnabled()) {
            return 0;
        }
        if (maxRetryCount == null) {
            return 3;
        }
        return Math.max(0, Math.min(maxRetryCount, 10));
    }
}
