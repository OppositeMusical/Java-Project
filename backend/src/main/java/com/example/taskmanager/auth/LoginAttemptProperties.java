package com.example.taskmanager.auth;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Limits on failed sign-in attempts, bound from {@code app.security.login-attempts.*}.
 *
 * @param maxPerAccountAndIp failures allowed for one email from one IP address within the window
 * @param maxPerIp failures allowed from one IP address across all emails within the window
 * @param window how long failures are remembered; also the longest a client has to wait
 */
@ConfigurationProperties("app.security.login-attempts")
public record LoginAttemptProperties(int maxPerAccountAndIp, int maxPerIp, Duration window) {
}
