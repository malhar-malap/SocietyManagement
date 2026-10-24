package com.society.jaswanti.security;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InMemoryLoginRateLimiterService implements LoginRateLimiterService {

    public static final int MAX_ATTEMPTS = 5;
    public static final long WINDOW_MS = 15 * 60 * 1000L; // 15 minutes
    public static final long LOCKOUT_DURATION_MS = 15 * 60 * 1000L; // 15 minutes

    private static class AttemptTracker {
        private final List<Long> failureTimestamps = new ArrayList<>();
        private long lockedUntil = 0;

        synchronized boolean isLocked(long now) {
            if (lockedUntil > now) {
                return true;
            }
            if (lockedUntil != 0 && lockedUntil <= now) {
                // Lock expired, reset
                lockedUntil = 0;
                failureTimestamps.clear();
            }
            return false;
        }

        synchronized boolean recordFailure(long now) {
            // Clean failures outside rolling window
            long windowStart = now - WINDOW_MS;
            failureTimestamps.removeIf(ts -> ts < windowStart);

            failureTimestamps.add(now);

            if (failureTimestamps.size() >= MAX_ATTEMPTS && lockedUntil <= now) {
                lockedUntil = now + LOCKOUT_DURATION_MS;
                return true; // Just locked
            }
            return false;
        }

        synchronized int getRemainingAttempts(long now) {
            if (isLocked(now)) {
                return 0;
            }
            long windowStart = now - WINDOW_MS;
            failureTimestamps.removeIf(ts -> ts < windowStart);
            return Math.max(0, MAX_ATTEMPTS - failureTimestamps.size());
        }

        synchronized void clear() {
            failureTimestamps.clear();
            lockedUntil = 0;
        }
    }

    private final Map<String, AttemptTracker> trackers = new ConcurrentHashMap<>();

    private String normalizeIdentifier(String identifier) {
        return identifier != null ? "id:" + identifier.trim().toLowerCase() : "id:anonymous";
    }

    private String normalizeIp(String ip) {
        return ip != null ? "ip:" + ip.trim() : "ip:unknown";
    }

    @Override
    public boolean isBlocked(String identifier, String ipAddress) {
        long now = System.currentTimeMillis();
        boolean idBlocked = isKeyBlocked(normalizeIdentifier(identifier), now);
        boolean ipBlocked = isKeyBlocked(normalizeIp(ipAddress), now);
        return idBlocked || ipBlocked;
    }

    private boolean isKeyBlocked(String key, long now) {
        AttemptTracker tracker = trackers.get(key);
        return tracker != null && tracker.isLocked(now);
    }

    @Override
    public boolean recordFailedAttempt(String identifier, String ipAddress) {
        long now = System.currentTimeMillis();
        String idKey = normalizeIdentifier(identifier);
        String ipKey = normalizeIp(ipAddress);

        AttemptTracker idTracker = trackers.computeIfAbsent(idKey, k -> new AttemptTracker());
        AttemptTracker ipTracker = trackers.computeIfAbsent(ipKey, k -> new AttemptTracker());

        boolean idJustLocked = idTracker.recordFailure(now);
        boolean ipJustLocked = ipTracker.recordFailure(now);

        return idJustLocked || ipJustLocked;
    }

    @Override
    public void recordSuccessfulLogin(String identifier, String ipAddress) {
        if (identifier != null) {
            trackers.remove(normalizeIdentifier(identifier));
        }
        if (ipAddress != null) {
            trackers.remove(normalizeIp(ipAddress));
        }
    }

    @Override
    public int getRemainingAttempts(String identifier, String ipAddress) {
        long now = System.currentTimeMillis();
        int idRemaining = trackers.computeIfAbsent(normalizeIdentifier(identifier), k -> new AttemptTracker()).getRemainingAttempts(now);
        int ipRemaining = trackers.computeIfAbsent(normalizeIp(ipAddress), k -> new AttemptTracker()).getRemainingAttempts(now);
        return Math.min(idRemaining, ipRemaining);
    }

    @Override
    public void reset(String identifier, String ipAddress) {
        if (identifier != null) {
            trackers.remove(normalizeIdentifier(identifier));
        }
        if (ipAddress != null) {
            trackers.remove(normalizeIp(ipAddress));
        }
    }

    @Override
    public void resetAll() {
        trackers.clear();
    }
}
