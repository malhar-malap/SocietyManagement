package com.society.jaswanti.security;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Single-instance brute-force protection for sign-in.
 *
 * Accounts lock after {@link #MAX_ATTEMPTS} failures within the window. Client IPs have a much higher limit
 * ({@link #MAX_IP_ATTEMPTS}) because many residents share one address (building Wi-Fi, mobile carrier NAT): a
 * single person's typos must not lock everyone else out, while password-spraying from one address is still stopped.
 */
@Service
public class InMemoryLoginRateLimiterService implements LoginRateLimiterService {

    public static final int MAX_ATTEMPTS = 5;
    public static final int MAX_IP_ATTEMPTS = 20;
    public static final long WINDOW_MS = 15 * 60 * 1000L; // 15 minutes
    public static final long LOCKOUT_DURATION_MS = 15 * 60 * 1000L; // 15 minutes

    /** Above this many tracked keys, stale entries are pruned on each write so memory stays bounded. */
    static final int PRUNE_THRESHOLD = 1_000;

    private static class AttemptTracker {
        private final int maxAttempts;
        private final List<Long> failureTimestamps = new ArrayList<>();
        private long lockedUntil = 0;

        AttemptTracker(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

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

            if (failureTimestamps.size() >= maxAttempts && lockedUntil <= now) {
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
            return Math.max(0, maxAttempts - failureTimestamps.size());
        }

        /** True when the entry holds no lock and no failure inside the window, so it can be forgotten. */
        synchronized boolean isStale(long now) {
            if (lockedUntil > now) {
                return false;
            }
            long windowStart = now - WINDOW_MS;
            return failureTimestamps.stream().noneMatch(ts -> ts >= windowStart);
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
        pruneIfLarge(now);

        AttemptTracker idTracker = trackers.computeIfAbsent(normalizeIdentifier(identifier), k -> new AttemptTracker(MAX_ATTEMPTS));
        AttemptTracker ipTracker = trackers.computeIfAbsent(normalizeIp(ipAddress), k -> new AttemptTracker(MAX_IP_ATTEMPTS));

        boolean idJustLocked = idTracker.recordFailure(now);
        boolean ipJustLocked = ipTracker.recordFailure(now);
        return idJustLocked || ipJustLocked;
    }

    @Override
    public void recordSuccessfulLogin(String identifier, String ipAddress) {
        // Only the account's counter is cleared: otherwise anyone holding one valid account could reset their IP's
        // counter between guesses at other accounts
        if (identifier != null) {
            trackers.remove(normalizeIdentifier(identifier));
        }
    }

    @Override
    public int getRemainingAttempts(String identifier, String ipAddress) {
        long now = System.currentTimeMillis();
        AttemptTracker id = trackers.get(normalizeIdentifier(identifier));
        AttemptTracker ip = trackers.get(normalizeIp(ipAddress));
        int idRemaining = id != null ? id.getRemainingAttempts(now) : MAX_ATTEMPTS;
        int ipRemaining = ip != null ? ip.getRemainingAttempts(now) : MAX_IP_ATTEMPTS;
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

    int trackedKeyCount() {
        return trackers.size();
    }

    private void pruneIfLarge(long now) {
        if (trackers.size() > PRUNE_THRESHOLD) {
            trackers.entrySet().removeIf(e -> e.getValue().isStale(now));
        }
    }
}
