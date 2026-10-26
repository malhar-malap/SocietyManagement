package com.society.jaswanti.security;

public interface LoginRateLimiterService {

    /**
     * Checks if either the account identifier or client IP address is currently locked out.
     */
    boolean isBlocked(String identifier, String ipAddress);

    /**
     * Records a failed authentication attempt.
     * @return true if this failure triggered a new lockout (e.g. reached 5 failures).
     */
    boolean recordFailedAttempt(String identifier, String ipAddress);

    /**
     * Clears failed attempts and lockouts upon successful login.
     */
    void recordSuccessfulLogin(String identifier, String ipAddress);

    /**
     * Returns remaining attempts before lockout for this identifier and IP.
     */
    int getRemainingAttempts(String identifier, String ipAddress);

    /**
     * Resets rate-limiting state for an identifier and IP (for testing and administrative unlocks).
     */
    void reset(String identifier, String ipAddress);

    /**
     * Clears all tracked state across all identifiers and IPs.
     */
    void resetAll();
}
