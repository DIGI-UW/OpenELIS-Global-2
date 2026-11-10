package org.openelisglobal.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

public class LoginAttemptTrackerTest {

    private static final int LIMIT = 3;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private final SteppingClock clock = new SteppingClock(Instant.parse("2026-11-07T10:00:00Z"));
    private final LoginAttemptTracker tracker = new LoginAttemptTracker(LIMIT, WINDOW, clock);

    @Test
    public void failuresBelowTheLimit_doNotThrottle() {
        fail("jdoe", "10.0.0.1", LIMIT - 1);

        assertFalse(tracker.isThrottled("jdoe", "10.0.0.1"));
    }

    @Test
    public void theLimit_throttlesTheLogin_fromAnyClient() {
        fail("jdoe", "10.0.0.1", LIMIT);

        assertTrue(tracker.isThrottled("jdoe", "10.0.0.1"));
        assertTrue(tracker.isThrottled("JDoe ", "10.0.0.9"));
    }

    @Test
    public void theLimit_throttlesTheClient_forAnyLogin() {
        tracker.recordFailure("a", "10.0.0.1");
        tracker.recordFailure("b", "10.0.0.1");
        tracker.recordFailure("c", "10.0.0.1");

        assertTrue(tracker.isThrottled("d", "10.0.0.1"));
        assertFalse(tracker.isThrottled("d", "10.0.0.2"));
    }

    @Test
    public void aSignIn_clearsTheLogin_butNotTheClient() {
        fail("jdoe", "10.0.0.1", LIMIT);

        tracker.recordSuccess("jdoe");

        assertFalse(tracker.isThrottled("jdoe", "10.0.0.2"));
        assertTrue(tracker.isThrottled("jdoe", "10.0.0.1"));
    }

    @Test
    public void failuresOlderThanTheWindow_noLongerCount() {
        fail("jdoe", "10.0.0.1", LIMIT);

        clock.advance(WINDOW.plusSeconds(1));

        assertFalse(tracker.isThrottled("jdoe", "10.0.0.1"));
    }

    @Test
    public void untrustedPeer_cannotChooseItsAddressWithAHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.40");
        request.addHeader("X-Forwarded-For", "203.0.113.7");
        assertEquals("192.168.1.40", tracker.clientAddress(request));
    }

    @Test
    public void trustedProxy_usesTheNearestUntrustedHop_includingPrivateClients() {
        LoginAttemptTracker proxied = new LoginAttemptTracker(LIMIT, WINDOW, clock, "172.16.0.5/32,172.16.0.6/32");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("172.16.0.5");
        request.addHeader("X-Forwarded-For", "203.0.113.7, 192.168.1.40, 172.16.0.6");
        assertEquals("192.168.1.40", proxied.clientAddress(request));
    }

    @Test
    public void multipleHeaderValues_areReadInOrder() {
        LoginAttemptTracker proxied = new LoginAttemptTracker(LIMIT, WINDOW, clock, "172.16.0.5");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("172.16.0.5");
        request.addHeader("X-Forwarded-For", "203.0.113.7");
        request.addHeader("X-Forwarded-For", "192.168.1.40");
        assertEquals("192.168.1.40", proxied.clientAddress(request));
    }

    @Test
    public void malformedProxyHeader_fallsBackToSocketPeer() {
        LoginAttemptTracker proxied = new LoginAttemptTracker(LIMIT, WINDOW, clock, "172.16.0.5");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("172.16.0.5");
        request.addHeader("X-Forwarded-For", "forged, unknown");
        assertEquals("172.16.0.5", proxied.clientAddress(request));
    }

    @Test
    public void concurrentFailureSuccessAndExpiry_doNotLoseTheClientFailures() throws Exception {
        int attempts = 2000;
        LoginAttemptTracker concurrent = new LoginAttemptTracker(attempts, WINDOW, clock);
        java.util.concurrent.ExecutorService workers = java.util.concurrent.Executors.newFixedThreadPool(3);
        java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
        try {
            java.util.List<java.util.concurrent.Future<?>> futures = new java.util.ArrayList<>();
            futures.add(workers.submit(() -> {
                start.await();
                for (int i = 0; i < attempts; i++)
                    concurrent.recordFailure("user", "192.0.2.1");
                return null;
            }));
            futures.add(workers.submit(() -> {
                start.await();
                for (int i = 0; i < attempts; i++)
                    concurrent.recordSuccess("user");
                return null;
            }));
            futures.add(workers.submit(() -> {
                start.await();
                for (int i = 0; i < attempts; i++)
                    concurrent.isThrottled("user", "192.0.2.1");
                return null;
            }));
            start.countDown();
            for (java.util.concurrent.Future<?> future : futures)
                future.get(10, java.util.concurrent.TimeUnit.SECONDS);
            assertTrue(concurrent.isThrottled("another-user", "192.0.2.1"));
        } finally {
            workers.shutdownNow();
        }
    }

    private void fail(String login, String client, int times) {
        for (int i = 0; i < times; i++) {
            tracker.recordFailure(login, client);
        }
    }

    private static class SteppingClock extends Clock {
        private Instant now;

        SteppingClock(Instant start) {
            now = start;
        }

        void advance(Duration by) {
            now = now.plus(by);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
