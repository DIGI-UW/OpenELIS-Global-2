package org.openelisglobal.addon.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.openelisglobal.addon.dao.AddonOutboxDAO.Claim;
import org.openelisglobal.addon.security.WebhookSignature;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Sends queued events to add-ons: a CloudEvents 1.0 JSON body, signed in the
 * Standard Webhooks format, POSTed to the add-on's endpoint. A 2xx answer is a
 * delivery; anything else is retried with backoff and given up after
 * {@link AddonOutboxService#MAX_ATTEMPTS}. Delivery is at least once: add-ons
 * de-duplicate on {@code webhook-id}.
 */
@Component
public class AddonEventDispatcher {

    private static final int BATCH = 20;
    private static final ObjectMapper JSON = new ObjectMapper();

    private final String worker = UUID.randomUUID().toString();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3))
            .followRedirects(HttpClient.Redirect.NEVER).build();
    private volatile long lastPurge;

    @Autowired
    private AddonOutboxService outbox;
    @Autowired
    private AddonSettings settings;

    @Scheduled(fixedDelayString = "${addons.dispatcher.poll-ms:2000}", initialDelayString = "${addons.dispatcher.initial-delay-ms:30000}")
    public void tick() {
        if (settings.dispatcherEnabled()) {
            runOnce();
        }
    }

    /** One pass: fan out, claim, send. Synchronized so passes never overlap. */
    synchronized void runOnce() {
        List<Claim> claims;
        try {
            claims = outbox.claim(worker, BATCH);
        } catch (RuntimeException e) {
            LogEvent.logError(getClass().getSimpleName(), "tick",
                    "could not claim add-on deliveries: " + e.getMessage());
            return;
        }
        for (Claim claim : claims) {
            try {
                deliver(claim);
            } catch (RuntimeException e) {
                LogEvent.logError(getClass().getSimpleName(), "tick",
                        "add-on delivery failed for event " + claim.eventId() + " to " + claim.addonId());
                LogEvent.logError(e);
            }
        }
        if (System.currentTimeMillis() - lastPurge > 3_600_000L) {
            lastPurge = System.currentTimeMillis();
            try {
                outbox.purge();
            } catch (RuntimeException e) {
                LogEvent.logError(e);
            }
        }
    }

    void deliver(Claim claim) {
        Optional<AddonOutboxService.Target> target = outbox.target(claim.addonId());
        if (target.isEmpty()) {
            outbox.dropped(claim, worker);
            return;
        }
        String body = envelope(claim);
        String id = String.valueOf(claim.eventId());
        long timestamp = Instant.now().getEpochSecond();
        Integer code = null;
        String error;
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(target.get().endpoint()))
                    .timeout(Duration.ofSeconds(10)).header("Content-Type", "application/cloudevents+json")
                    .header("webhook-id", id).header("webhook-timestamp", String.valueOf(timestamp))
                    .header("webhook-signature",
                            WebhookSignature.sign(target.get().webhookSecret(), id, timestamp, body))
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build();
            HttpResponse<Void> response = http.send(request, HttpResponse.BodyHandlers.discarding());
            code = response.statusCode();
            if (code >= 200 && code < 300) {
                outbox.delivered(claim, worker, code);
                return;
            }
            error = "HTTP " + code;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            error = "interrupted";
        } catch (Exception e) {
            error = e.getClass().getSimpleName() + (e.getMessage() == null ? "" : ": " + e.getMessage());
        }
        outbox.failed(claim, worker, code, error);
    }

    /** CloudEvents 1.0, structured JSON mode. {@code data} holds codes only. */
    static String envelope(Claim claim) {
        ObjectNode o = JSON.createObjectNode();
        o.put("specversion", "1.0");
        o.put("id", String.valueOf(claim.eventId()));
        o.put("type", claim.type());
        o.put("source", "/openelis-global");
        o.put("time", claim.createdAt().toString());
        o.put("datacontenttype", "application/json");
        try {
            o.set("data", JSON.readTree(claim.data()));
        } catch (Exception e) {
            o.putObject("data");
        }
        return o.toString();
    }

    String workerId() {
        return worker;
    }
}
