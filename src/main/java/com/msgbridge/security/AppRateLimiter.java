package com.msgbridge.security;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class AppRateLimiter {
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public boolean allow(String appId, int limitPerMinute) {
        if (limitPerMinute <= 0) {
            return false;
        }
        long minute = Instant.now().getEpochSecond() / 60;
        Bucket bucket = buckets.computeIfAbsent(appId, ignored -> new Bucket(minute));
        synchronized (bucket) {
            if (bucket.minute != minute) {
                bucket.minute = minute;
                bucket.count = 0;
            }
            if (bucket.count >= limitPerMinute) {
                return false;
            }
            bucket.count++;
            return true;
        }
    }

    private static class Bucket {
        private long minute;
        private int count;

        private Bucket(long minute) {
            this.minute = minute;
        }
    }
}
