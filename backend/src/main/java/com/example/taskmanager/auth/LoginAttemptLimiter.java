package com.example.taskmanager.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

import com.example.taskmanager.common.TooManyRequestsException;

/**
 * Throttles password guessing by counting failed sign-ins in a fixed window per key:
 *
 * <ul>
 *   <li><b>IP + email</b>: a few wrong passwords for one account from one client. Keyed by IP as well
 *       so that an attacker cannot lock the real owner out of their account from somewhere else.</li>
 *   <li><b>IP</b>: a higher limit across all emails, against one client trying many accounts.</li>
 * </ul>
 *
 * A successful sign-in clears the IP + email counter but not the IP counter, so an attacker cannot
 * reset their budget by signing in to an account of their own between guesses.
 *
 * <p>Counters live in memory, so they reset on restart and are per instance. Running several
 * instances needs a shared store (for example Redis) instead.
 */
@Component
public class LoginAttemptLimiter {

    private static final int CLEANUP_EVERY = 1_000;

    private final LoginAttemptProperties properties;
    private final Clock clock;
    private final Map<String, Window> failures = new ConcurrentHashMap<>();
    private final AtomicInteger callsSinceCleanup = new AtomicInteger();

    public LoginAttemptLimiter(LoginAttemptProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    /** Throws if this client must wait before trying again. Call before checking the password. */
    public void checkAllowed(String ip, String email) {
        Instant now = clock.instant();
        Duration wait = max(
                waitFor(accountKey(ip, email), properties.maxPerAccountAndIp(), now),
                waitFor(ipKey(ip), properties.maxPerIp(), now));
        if (!wait.isZero()) {
            throw new TooManyRequestsException("Too many failed sign-in attempts. Try again later.", wait);
        }
    }

    public void recordFailure(String ip, String email) {
        Instant now = clock.instant();
        increment(accountKey(ip, email), now);
        increment(ipKey(ip), now);
        cleanUpOccasionally(now);
    }

    public void recordSuccess(String ip, String email) {
        failures.remove(accountKey(ip, email));
    }

    private Duration waitFor(String key, int max, Instant now) {
        Window window = failures.get(key);
        if (window == null || window.isExpired(now, properties.window()) || window.count() < max) {
            return Duration.ZERO;
        }
        return Duration.between(now, window.start().plus(properties.window()));
    }

    private void increment(String key, Instant now) {
        failures.compute(key, (k, window) -> window == null || window.isExpired(now, properties.window())
                ? new Window(now, 1)
                : new Window(window.start(), window.count() + 1));
    }

    private void cleanUpOccasionally(Instant now) {
        if (callsSinceCleanup.incrementAndGet() >= CLEANUP_EVERY) {
            callsSinceCleanup.set(0);
            failures.values().removeIf(window -> window.isExpired(now, properties.window()));
        }
    }

    private static String accountKey(String ip, String email) {
        return "account:" + ip + ":" + email;
    }

    private static String ipKey(String ip) {
        return "ip:" + ip;
    }

    private static Duration max(Duration a, Duration b) {
        return a.compareTo(b) >= 0 ? a : b;
    }

    private record Window(Instant start, int count) {

        boolean isExpired(Instant now, Duration length) {
            return !now.isBefore(start.plus(length));
        }
    }
}
