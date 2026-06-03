package com.msgbridge.service;

import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MaskingService {
    private static final Pattern SECRET_PAIR = Pattern.compile(
            "(?i)(\"?(?:secret|token|access_token|app_secret|corp_secret|webhook_url|webhook|sign_secret)\"?\\s*[:=]\\s*\")([^\"&\\s]+)(\")");
    private static final Pattern URL_TOKEN = Pattern.compile("(?i)([?&](?:key|access_token|token|sign)=)[^&\\s\"]+");
    private static final Pattern WEBHOOK_URL = Pattern.compile("https://[^\\s\"]+/(?:cgi-bin/webhook/send|robot/send|open-apis/bot/)[^\\s\"]+");

    public String mask(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        String masked = SECRET_PAIR.matcher(value).replaceAll("$1******$3");
        masked = URL_TOKEN.matcher(masked).replaceAll("$1******");
        masked = WEBHOOK_URL.matcher(masked).replaceAll("https://******/webhook");
        return masked;
    }
}
