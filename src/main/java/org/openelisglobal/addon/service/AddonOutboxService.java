package org.openelisglobal.addon.service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.addon.dao.AddonOutboxDAO.Claim;

/** The database side of event delivery; the dispatcher does the HTTP. */
public interface AddonOutboxService {

    String PENDING = "PENDING";
    String DELIVERED = "DELIVERED";
    /** Gave up after {@link #MAX_ATTEMPTS}. */
    String FAILED = "FAILED";
    /** Not sent: the add-on was disabled or removed. */
    String DROPPED = "DROPPED";

    int MAX_ATTEMPTS = 10;

    /** Where and how to send for an add-on that is enabled now. */
    record Target(String endpoint, String webhookSecret) {
    }

    /** Fans new events out to subscribed add-ons, then leases due deliveries. */
    List<Claim> claim(String worker, int limit);

    /**
     * Read right before each send, so a disable between claim and send is honoured.
     */
    Optional<Target> target(String addonId);

    void delivered(Claim claim, String worker, int statusCode);

    void failed(Claim claim, String worker, Integer statusCode, String error);

    void dropped(Claim claim, String worker);

    int purge();

    /** Wait after the n-th failed attempt: 5 s doubling, at most one hour. */
    static Duration backoff(int attempts) {
        long seconds = 5L << Math.min(Math.max(attempts - 1, 0), 10);
        return Duration.ofSeconds(Math.min(seconds, 3600));
    }
}
