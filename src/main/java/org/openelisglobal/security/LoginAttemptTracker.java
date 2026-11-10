package org.openelisglobal.security;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.http.conn.util.InetAddressUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
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
    private final List<IpAddressMatcher> trustedProxies;
    private final Map<String, Deque<Instant>> failures = new ConcurrentHashMap<>();

    @Autowired
    public LoginAttemptTracker(@Value("${org.openelisglobal.login.throttle.maxAttempts:10}") int maxAttempts,
            @Value("${org.openelisglobal.login.throttle.windowMinutes:15}") int windowMinutes,
            @Value("${org.openelisglobal.login.throttle.trustedProxies:}") String trustedProxies) {
        this(maxAttempts, Duration.ofMinutes(windowMinutes), Clock.systemUTC(), trustedProxies);
    }

    public LoginAttemptTracker(int maxAttempts, Duration window, Clock clock) {
        this(maxAttempts, window, clock, "");
    }

    public LoginAttemptTracker(int maxAttempts, Duration window, Clock clock, String trustedProxies) {
        this.maxAttempts = maxAttempts;
        this.window = window;
        this.clock = clock;
        this.trustedProxies = java.util.Arrays.stream(trustedProxies.split(",")).map(String::trim)
                .filter(value -> !value.isEmpty()).map(IpAddressMatcher::new).toList();
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

    /** Only explicitly configured proxies may attest to a client address. */
    public String clientAddress(HttpServletRequest request) {
        String peer = request.getRemoteAddr();
        if (!isTrustedProxy(peer)) {
            return peer;
        }
        List<String> hops = new ArrayList<>();
        for (String header : Collections.list(request.getHeaders("X-Forwarded-For"))) {
            Collections.addAll(hops, header.split(",", -1));
        }
        String client = peer;
        for (int index = hops.size() - 1; index >= 0 && isTrustedProxy(client); index--) {
            String hop = hops.get(index).trim();
            if (!InetAddressUtils.isIPv4Address(hop) && !InetAddressUtils.isIPv6Address(hop)) {
                return peer;
            }
            client = hop;
        }
        return client;
    }

    private boolean isTrustedProxy(String address) {
        return address != null && trustedProxies.stream().anyMatch(proxy -> proxy.matches(address));
    }

    private void record(String key, Instant now) {
        failures.compute(key, (ignored, instants) -> {
            if (instants == null) {
                instants = new ArrayDeque<>();
            }
            expire(instants, now.minus(window));
            instants.addLast(now);
            return instants;
        });
    }

    private int recentFailures(String key) {
        AtomicInteger count = new AtomicInteger();
        Instant cutoff = clock.instant().minus(window);
        failures.computeIfPresent(key, (ignored, instants) -> {
            expire(instants, cutoff);
            count.set(instants.size());
            return instants.isEmpty() ? null : instants;
        });
        return count.get();
    }

    private static void expire(Deque<Instant> instants, Instant cutoff) {
        while (!instants.isEmpty() && instants.peekFirst().isBefore(cutoff)) {
            instants.pollFirst();
        }
    }

    private static String loginKey(String loginName) {
        return "login:" + (loginName == null ? "" : loginName.trim().toLowerCase(Locale.ROOT));
    }

    private static String clientKey(String client) {
        return "client:" + (client == null ? "" : client.trim());
    }
}
