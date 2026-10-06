package com.pricepulse.auth.service;

import com.pricepulse.auth.exception.RateLimitException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory, per-IP rate limiter for failed login attempts (Req 2.5).
 *
 * <p>Rules:
 * <ul>
 *   <li>Track failure count per IP within a 10-minute sliding window.</li>
 *   <li>Once count reaches 5, the IP is blocked for 15 minutes from the first failure
 *       in the window (or from the time the 5th failure occurred — using windowStart for simplicity).</li>
 *   <li>On successful login the failure record is removed.</li>
 * </ul>
 *
 * <p>Limitation: state is lost on restart and is not shared across instances.
 * This is a known, documented MVP tradeoff — upgrading to Redis requires changing only this class.
 */
@Component
public class LoginRateLimiter {

    /** How many failures are allowed before blocking. */
    private static final int MAX_FAILURES = 5;

    /** Sliding window in which failures are counted (10 minutes). */
    private static final long WINDOW_SECONDS = 10 * 60;

    /** How long a blocked IP stays blocked after exceeding MAX_FAILURES (15 minutes). */
    private static final long BLOCK_SECONDS = 15 * 60;

    private record FailureBucket(int count, Instant windowStart) {}

    private final ConcurrentHashMap<String, FailureBucket> buckets = new ConcurrentHashMap<>();

    /**
     * Returns {@code true} if the given IP is currently blocked.
     * An IP is blocked when it has accumulated {@value #MAX_FAILURES} or more failures
     * and the block window (15 minutes from {@code windowStart}) has not yet elapsed.
     */
    public boolean isBlocked(String ip) {
        FailureBucket bucket = buckets.get(ip);
        if (bucket == null) {
            return false;
        }
        if (bucket.count() < MAX_FAILURES) {
            return false;
        }
        // Count has reached the threshold — check if the block window has expired
        Instant blockExpiry = bucket.windowStart().plusSeconds(BLOCK_SECONDS);
        return Instant.now().isBefore(blockExpiry);
    }

    /**
     * Records a failed login attempt for the given IP.
     * If the current failure falls outside the 10-minute counting window, the window
     * is reset so the count starts from 1 again.
     */
    public void recordFailure(String ip) {
        buckets.compute(ip, (key, existing) -> {
            Instant now = Instant.now();
            if (existing == null || isWindowExpired(existing, now)) {
                // First failure or window has expired — start a new window
                return new FailureBucket(1, now);
            }
            return new FailureBucket(existing.count() + 1, existing.windowStart());
        });
    }

    /**
     * Clears the failure record for the given IP after a successful login.
     */
    public void resetFailures(String ip) {
        buckets.remove(ip);
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private boolean isWindowExpired(FailureBucket bucket, Instant now) {
        Instant windowExpiry = bucket.windowStart().plusSeconds(WINDOW_SECONDS);
        return now.isAfter(windowExpiry);
    }
}
