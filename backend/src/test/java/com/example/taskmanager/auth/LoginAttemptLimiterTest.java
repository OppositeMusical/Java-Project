package com.example.taskmanager.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.example.taskmanager.common.TooManyRequestsException;

class LoginAttemptLimiterTest {

    private static final String IP = "203.0.113.7";
    private static final String EMAIL = "alice@example.com";

    private final MutableClock clock = new MutableClock(Instant.parse("2030-01-01T10:00:00Z"));
    private final LoginAttemptLimiter limiter =
            new LoginAttemptLimiter(new LoginAttemptProperties(3, 5, Duration.ofMinutes(15)), clock);

    @Test
    void blocksAccountFromIpAfterMaxFailures() {
        fail(IP, EMAIL, 2);
        assertThatCode(() -> limiter.checkAllowed(IP, EMAIL)).doesNotThrowAnyException();

        fail(IP, EMAIL, 1);
        clock.advance(Duration.ofMinutes(5));
        assertThatThrownBy(() -> limiter.checkAllowed(IP, EMAIL))
                .isInstanceOfSatisfying(TooManyRequestsException.class,
                        ex -> assertThat(ex.getRetryAfter()).isEqualTo(Duration.ofMinutes(10)));
    }

    @Test
    void sameAccountFromAnotherIpIsNotLockedOut() {
        fail(IP, EMAIL, 3);
        assertThatCode(() -> limiter.checkAllowed("198.51.100.1", EMAIL)).doesNotThrowAnyException();
    }

    @Test
    void blocksIpTryingManyAccounts() {
        for (int i = 0; i < 5; i++) {
            fail(IP, "user" + i + "@example.com", 1);
        }
        assertThatThrownBy(() -> limiter.checkAllowed(IP, "someone-new@example.com"))
                .isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    void unblocksWhenWindowEnds() {
        fail(IP, EMAIL, 3);
        clock.advance(Duration.ofMinutes(15));
        assertThatCode(() -> limiter.checkAllowed(IP, EMAIL)).doesNotThrowAnyException();

        // The count starts over after the window.
        fail(IP, EMAIL, 2);
        assertThatCode(() -> limiter.checkAllowed(IP, EMAIL)).doesNotThrowAnyException();
    }

    @Test
    void successResetsAccountCounterButNotIpCounter() {
        fail(IP, EMAIL, 2);
        limiter.recordSuccess(IP, EMAIL);
        fail(IP, EMAIL, 2);
        assertThatCode(() -> limiter.checkAllowed(IP, EMAIL)).doesNotThrowAnyException();

        // 4 failures from this IP so far; one more reaches the per-IP limit of 5 despite the success.
        fail(IP, "other@example.com", 1);
        assertThatThrownBy(() -> limiter.checkAllowed(IP, EMAIL)).isInstanceOf(TooManyRequestsException.class);
    }

    private void fail(String ip, String email, int times) {
        for (int i = 0; i < times; i++) {
            limiter.recordFailure(ip, email);
        }
    }

    private static final class MutableClock extends Clock {

        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
