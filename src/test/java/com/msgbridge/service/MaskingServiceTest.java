package com.msgbridge.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MaskingServiceTest {

    private final MaskingService maskingService = new MaskingService();

    @Test
    void masksWebhookTokensAndSecretPairs() {
        String raw = """
                {"webhook_url":"https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=abc","secret":"SEC123"}
                https://oapi.dingtalk.com/robot/send?access_token=token123&timestamp=1
                """;

        String masked = maskingService.mask(raw);

        assertThat(masked).doesNotContain("abc", "SEC123", "token123");
        assertThat(masked).contains("******");
    }
}
