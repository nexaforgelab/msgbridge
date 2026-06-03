package com.msgbridge.security;

import java.util.Arrays;
import org.springframework.util.StringUtils;

public final class IpWhitelist {
    private IpWhitelist() {
    }

    public static boolean allows(String whitelist, String ip) {
        if (!StringUtils.hasText(whitelist)) {
            return true;
        }
        return Arrays.stream(whitelist.split("[,\\n]"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .anyMatch(rule -> "*".equals(rule) || rule.equals(ip) || cidrMatches(rule, ip));
    }

    private static boolean cidrMatches(String rule, String ip) {
        if (!rule.contains("/") || ip == null || ip.contains(":")) {
            return false;
        }
        try {
            String[] parts = rule.split("/");
            long network = ipv4ToLong(parts[0]);
            int prefix = Integer.parseInt(parts[1]);
            long mask = prefix == 0 ? 0 : (0xffffffffL << (32 - prefix)) & 0xffffffffL;
            return (ipv4ToLong(ip) & mask) == (network & mask);
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static long ipv4ToLong(String ip) {
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            throw new IllegalArgumentException("not IPv4");
        }
        long value = 0;
        for (String part : parts) {
            value = (value << 8) + Integer.parseInt(part);
        }
        return value & 0xffffffffL;
    }
}
