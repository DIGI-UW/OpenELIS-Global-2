package org.openelisglobal.payment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.openelisglobal.payment.gateway.PaymentGatewayProvider;
import org.openelisglobal.payment.valueholder.Payment;
import org.openelisglobal.payment.valueholder.PaymentStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentWebhookServiceImpl implements PaymentWebhookService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final PaymentService paymentService;
    private final PaymentGatewayProvider gatewayProvider;

    @Autowired
    public PaymentWebhookServiceImpl(PaymentService paymentService, PaymentGatewayProvider gatewayProvider) {
        this.paymentService = paymentService;
        this.gatewayProvider = gatewayProvider;
    }

    @Override
    @Transactional
    public WebhookOutcome process(String rawBody, String signature, String eventId, String sysUserId) {
        if (!gatewayProvider.current().verifyWebhookSignature(rawBody, signature)) {
            return WebhookOutcome.REJECTED_SIGNATURE;
        }
        if (eventId == null || eventId.isBlank()) {
            return WebhookOutcome.MALFORMED;
        }
        JsonNode root;
        try {
            root = MAPPER.readTree(rawBody);
        } catch (IOException e) {
            return WebhookOutcome.MALFORMED;
        }
        String event = text(root, "event");
        if (event == null) {
            return WebhookOutcome.MALFORMED;
        }
        if (paymentService.getByGatewayEventId(eventId) != null) {
            return WebhookOutcome.DUPLICATE;
        }

        JsonNode payload = root.path("payload");
        switch (event) {
        case "payment_link.paid": {
            Payment payment = paymentService
                    .getByGatewayLinkId(text(payload.path("payment_link").path("entity"), "id"));
            return apply(payment, PaymentStatus.PAID, eventId, text(payload.path("payment").path("entity"), "id"),
                    sysUserId);
        }
        case "payment_link.cancelled":
        case "payment_link.expired": {
            Payment payment = paymentService
                    .getByGatewayLinkId(text(payload.path("payment_link").path("entity"), "id"));
            return apply(payment, PaymentStatus.CANCELLED, eventId, null, sysUserId);
        }
        case "refund.processed": {
            String gatewayPaymentId = text(payload.path("refund").path("entity"), "payment_id");
            Payment payment = paymentService.getByGatewayPaymentId(gatewayPaymentId);
            return apply(payment, PaymentStatus.REFUNDED, eventId, null, sysUserId);
        }
        default:
            return WebhookOutcome.IGNORED;
        }
    }

    /**
     * Records the event and moves the payment. A move the status rules do not allow
     * (for example a late "cancelled" after "paid") changes nothing.
     */
    private WebhookOutcome apply(Payment payment, PaymentStatus target, String eventId, String gatewayPaymentId,
            String sysUserId) {
        if (payment == null || !payment.getStatus().canTransitionTo(target)) {
            return WebhookOutcome.IGNORED;
        }
        if (gatewayPaymentId != null) {
            payment.setGatewayPaymentId(gatewayPaymentId);
        }
        payment.setGatewayEventId(eventId);
        payment.setSysUserId(sysUserId);
        paymentService.update(payment);
        paymentService.transition(payment.getId(), target, sysUserId);
        return WebhookOutcome.APPLIED;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
