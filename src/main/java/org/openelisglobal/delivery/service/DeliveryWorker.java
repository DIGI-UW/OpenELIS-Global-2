package org.openelisglobal.delivery.service;

import java.util.List;
import java.util.UUID;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.delivery.dao.DeliveryQueueDAO.Claim;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Sends what the outbox holds. Every tick claims the channel rows that are due
 * (queued, next attempt time reached, not leased by another worker) and sends
 * them one by one; a restart or an outage only delays a message, it never loses
 * one. Two workers can run against one database: claims are leased.
 */
@Component
public class DeliveryWorker {

    private static final int BATCH = 5;

    private final String worker = UUID.randomUUID().toString();

    @Autowired
    private DeliveryOutboxService outbox;

    @Value("${delivery.enabled:true}")
    private boolean enabled;

    @Scheduled(fixedDelayString = "${delivery.poll-ms:5000}", initialDelayString = "${delivery.initial-delay-ms:15000}")
    public synchronized void tick() {
        if (!enabled) {
            return;
        }
        List<Claim> claims;
        try {
            claims = outbox.claim(worker, BATCH);
        } catch (RuntimeException e) {
            LogEvent.logError(getClass().getSimpleName(), "tick", "could not claim delivery rows: " + e.getMessage());
            return;
        }
        for (Claim claim : claims) {
            try {
                outbox.attempt(claim, worker);
            } catch (RuntimeException e) {
                LogEvent.logError(getClass().getSimpleName(), "tick",
                        "delivery attempt failed for message " + claim.logId() + " on " + claim.channel());
                LogEvent.logError(e);
            }
        }
    }

    public String workerId() {
        return worker;
    }
}
