package org.openelisglobal.security;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
    private Instant nextSweep = Instant.MIN;

    private final int maxAttempts;
    private final int maxClientAttempts;
    private final Duration window;
    private final Clock clock;
    private final List<IpAddressMatcher> trustedProxies;
    private final Map<String, Bucket> failures = new HashMap<>();

    private static final class Bucket {
        final Deque<Instant> failures = new ArrayDeque<>();
        int active;
    }

    /**
     * A reservation covers password verification without holding the tracker lock.
     */
    public final class Attempt implements AutoCloseable {
        private final String login;
        private final String client;
        private boolean closed;

        private Attempt(String login, String client) {
            this.login = login;
            this.client = client;
        }

        @Override
        public void close() {
            synchronized (LoginAttemptTracker.this) {
                if (!closed) {
                    closed = true;
                    release(login);
                    release(client);
                }
            }
        }
    }

    public synchronized Attempt tryAcquire(String loginName, String client) {
        sweepExpired();
        if (isThrottled(loginName, client)) {
            return null;
        }
        String login = loginKey(loginName);
        String address = clientKey(client);
        failures.computeIfAbsent(login, ignored -> new Bucket()).active++;
        failures.computeIfAbsent(address, ignored -> new Bucket()).active++;
        return new Attempt(login, address);
    }

    private void release(String key) {
        Bucket bucket = failures.get(key);
        if (bucket != null) {
            bucket.active--;
            if (bucket.active == 0 && bucket.failures.isEmpty()) {
                failures.remove(key);
            }
        }
    }

    @Autowired
    public LoginAttemptTracker(@Value("${org.openelisglobal.login.throttle.maxAttempts:10}") int maxAttempts,
            @Value("${org.openelisglobal.login.throttle.maxClientAttempts:100}") int maxClientAttempts,
            @Value("${org.openelisglobal.login.throttle.windowMinutes:15}") int windowMinutes,
            @Value("${org.openelisglobal.login.throttle.trustedProxies:}") String trustedProxies) {
        this(maxAttempts, maxClientAttempts, Duration.ofMinutes(windowMinutes), Clock.systemUTC(), trustedProxies);
    }

    public LoginAttemptTracker(int maxAttempts, int maxClientAttempts, Duration window, Clock clock,
            String trustedProxies) {
        if (maxAttempts < 1 || maxClientAttempts < 1 || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("Login throttle limits and window must be positive");
        }
        this.maxAttempts = maxAttempts;
        this.maxClientAttempts = maxClientAttempts;
        this.window = window;
        this.clock = clock;
        this.trustedProxies = java.util.Arrays.stream(trustedProxies.split(",")).map(String::trim)
                .filter(value -> !value.isEmpty()).map(IpAddressMatcher::new).toList();
    }

    public synchronized boolean isThrottled(String loginName, String client) {
        return recentFailures(loginKey(loginName)) >= maxAttempts
                || recentFailures(clientKey(client)) >= maxClientAttempts;
    }

    public synchronized void recordFailure(String loginName, String client) {
        Instant now = clock.instant();
        record(loginKey(loginName), now);
        record(clientKey(client), now);
        sweepExpired();
    }

    /**
     * A sign-in proves the login's owner is present; the client keeps its count.
     */
    public synchronized void recordSuccess(String loginName) {
        Bucket bucket = failures.get(loginKey(loginName));
        if (bucket != null) {
            bucket.failures.clear();
            if (bucket.active == 0)
                failures.remove(loginKey(loginName));
        }
    }

    public long windowMinutes() {
        return window.toMinutes();
    }

    public synchronized void clear() {
        failures.entrySet().removeIf(entry -> {
            entry.getValue().failures.clear();
            return entry.getValue().active == 0;
        });
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
        Bucket bucket = failures.computeIfAbsent(key, ignored -> new Bucket());
        expire(bucket.failures, now.minus(window));
        bucket.failures.addLast(now);
    }

    private int recentFailures(String key) {
        Bucket bucket = failures.get(key);
        if (bucket == null)
            return 0;
        expire(bucket.failures, clock.instant().minus(window));
        int count = bucket.failures.size() + bucket.active;
        if (count == 0)
            failures.remove(key);
        return count;
    }

    private void sweepExpired() {
        Instant now = clock.instant();
        if (failures.size() <= SWEEP_ABOVE || now.isBefore(nextSweep))
            return;
        nextSweep = now.plusSeconds(60);
        Instant cutoff = now.minus(window);
        failures.entrySet().removeIf(entry -> {
            Bucket bucket = entry.getValue();
            expire(bucket.failures, cutoff);
            return bucket.active == 0 && bucket.failures.isEmpty();
        });
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
