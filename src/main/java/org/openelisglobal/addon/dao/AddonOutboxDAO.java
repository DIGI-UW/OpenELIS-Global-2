package org.openelisglobal.addon.dao;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * The event outbox ({@code addon_event}) and the per-add-on delivery queue
 * ({@code addon_delivery}). Rows are written by database triggers and read by
 * the dispatcher; claims are leased so two backends can share one database.
 */
public interface AddonOutboxDAO {

    /** A delivery taken by a worker: what to send and where. */
    record Claim(long eventId, String addonId, int attempts, String type, String data, Instant createdAt) {
    }

    /**
     * Gives each event not yet fanned out one delivery row per enabled add-on that
     * subscribes to its type, then marks exactly those events fanned.
     *
     * @return delivery rows created
     */
    int fanOut(int limit);

    List<Claim> claimDue(String worker, int limit, int leaseSeconds);

    /**
     * Records an attempt and releases the lease. {@code status}: see AddonDelivery
     * statuses.
     */
    void recordAttempt(long eventId, String addonId, String worker, String status, Integer statusCode, String error,
            Instant nextAttemptAt);

    /** Drops every pending delivery of an add-on (it was disabled or removed). */
    int dropPending(String addonId);

    /** Deletes events (and their deliveries) older than the given age. */
    int purgeOlderThan(int days);

    /** Delivery counts by status. */
    Map<String, Integer> countsByStatus(String addonId);

    /** The latest deliveries of an add-on, newest first. */
    List<Map<String, Object>> recent(String addonId, int limit);

    /** Whether the add-on was sent (or is being sent) an event about this order. */
    boolean wasNotified(String addonId, String accessionNumber);

    /** Writes an event through the same function the triggers use. */
    void emit(String type, String accessionNumber, String dataJson);
}
