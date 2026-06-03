package com.msgbridge.adapter;

import java.util.Map;

final class AdapterConfig {
    private AdapterConfig() {
    }

    static String string(Map<String, Object> config, String... keys) {
        Object value = value(config, keys);
        return value == null ? null : String.valueOf(value);
    }

    static boolean bool(Map<String, Object> config, String... keys) {
        Object value = value(config, keys);
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private static Object value(Map<String, Object> config, String... keys) {
        if (config == null) {
            return null;
        }
        for (String key : keys) {
            if (config.containsKey(key)) {
                return config.get(key);
            }
        }
        return null;
    }
}
