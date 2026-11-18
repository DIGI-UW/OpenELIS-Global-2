package org.openelisglobal.payment.service;

public interface PaymentWebhookService {

    /**
     * Verifies and applies one gateway webhook. Checks the signature before reading
     * anything, and treats repeated events as no-ops.
     *
     * @param rawBody   the exact request body, as signed by the gateway
     * @param signature the signature header
     * @param eventId   the gateway's unique id for this event
     * @param sysUserId the user recorded on any change
     */
    WebhookOutcome process(String rawBody, String signature, String eventId, String sysUserId);
}
