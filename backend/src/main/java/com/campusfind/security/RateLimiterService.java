package com.campusfind.security;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class RateLimiterService {

    private static final int MAX_REQUESTS_PER_MINUTE = 10;
    private static final long WINDOW_MS = 60_000L; // 1 minute

    private final Map<String, WindowData> ipWindows = new ConcurrentHashMap<>();

    private static class WindowData {
        long windowStart;
        AtomicInteger count;

        WindowData(long start) {
            this.windowStart = start;
            this.count = new AtomicInteger(1);
        }
    }

    public boolean allowRequest(String ipKey) {
        long now = System.currentTimeMillis();

        ipWindows.compute(ipKey, (key, existing) -> {
            if (existing == null || (now - existing.windowStart) > WINDOW_MS) {
                return new WindowData(now);
            }
            existing.count.incrementAndGet();
            return existing;
        });

        WindowData current = ipWindows.get(ipKey);
        return current != null && current.count.get() <= MAX_REQUESTS_PER_MINUTE;
    }

    public void reset(String ipKey) {
        ipWindows.remove(ipKey);
    }
}
