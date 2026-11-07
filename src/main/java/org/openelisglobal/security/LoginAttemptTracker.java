package org.openelisglobal.security;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Counts failed sign-ins per login name and per client address and holds either
 * back once it reaches the limit within the window. The count lives in this JVM
 * and a restart clears it, which slows password guessing without an account
 * lock that an attacker could use to shut a user out.
 */
@Component
public class LoginAttemptTracker {

    private static final int SWEEP_ABOVE = 5000;

    private final int maxAttempts;
    private final Duration window;
    private final Clock clock;
    private final Map<String, Deque<Instant>> failures = new ConcurrentHashMap<>();

    @Autowired
    public LoginAttemptTracker(@Value("${org.openelisglobal.login.throttle.maxAttempts:10}") int maxAttempts,
            @Value("${org.openelisglobal.login.throttle.windowMinutes:15}") int windowMinutes) {
        this(maxAttempts, Duration.ofMinutes(windowMinutes), Clock.systemUTC());
    }

    public LoginAttemptTracker(int maxAttempts, Duration window, Clock clock) {
        this.maxAttempts = maxAttempts;
        this.window = window;
        this.clock = clock;
    }

    public boolean isThrottled(String loginName, String client) {
        return recentFailures(loginKey(loginName)) >= maxAttempts || recentFailures(clientKey(client)) >= maxAttempts;
    }

    public void recordFailure(String loginName, String client) {
        Instant now = clock.instant();
        record(loginKey(loginName), now);
        record(clientKey(client), now);
        if (failures.size() > SWEEP_ABOVE) {
            failures.keySet().forEach(this::recentFailures);
        }
    }

    /**
     * A sign-in proves the login's owner is present; the client keeps its count.
     */
    public void recordSuccess(String loginName) {
        failures.remove(loginKey(loginName));
    }

    public long windowMinutes() {
        return window.toMinutes();
    }

    public void clear() {
        failures.clear();
    }

    /** The first forwarded hop when a proxy is in front, else the socket peer. */
    public static String clientAddress(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void record(String key, Instant now) {
        failures.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (failures.get(key)) {
            failures.get(key).addLast(now);
        }
    }

    private int recentFailures(String key) {
        Deque<Instant> instants = failures.get(key);
        if (instants == null) {
            return 0;
        }
        Instant cutoff = clock.instant().minus(window);
        synchronized (instants) {
            while (!instants.isEmpty() && instants.peekFirst().isBefore(cutoff)) {
                instants.pollFirst();
            }
            if (instants.isEmpty()) {
                failures.remove(key, instants);
            }
            return instants.size();
        }
    }

    private static String loginKey(String loginName) {
        return "login:" + (loginName == null ? "" : loginName.trim().toLowerCase());
    }

    private static String clientKey(String client) {
        return "client:" + (client == null ? "" : client.trim());
    }
}
