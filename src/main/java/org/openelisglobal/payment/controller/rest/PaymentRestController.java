package org.openelisglobal.payment.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ControllerUtills;
import org.openelisglobal.payment.PaymentTaxSettings;
import org.openelisglobal.payment.form.PaymentForm;
import org.openelisglobal.payment.form.PaymentSummary;
import org.openelisglobal.payment.gateway.PaymentGatewayDisabledException;
import org.openelisglobal.payment.gateway.PaymentGatewayException;
import org.openelisglobal.payment.service.OrderPricingService;
import org.openelisglobal.payment.service.PaymentLinkService;
import org.openelisglobal.payment.service.PaymentRefundService;
import org.openelisglobal.payment.service.PaymentService;
import org.openelisglobal.payment.service.PaymentWebhookService;
import org.openelisglobal.payment.service.WebhookOutcome;
import org.openelisglobal.payment.valueholder.Payment;
import org.openelisglobal.security.DaemonContextExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Payment endpoints: list an order's payments and let staff request a payment
 * link, take Razorpay webhooks, and refund.
 */
@RestController
@RequestMapping("/rest/payments")
public class PaymentRestController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentLinkService paymentLinkService;

    @Autowired
    private PaymentWebhookService paymentWebhookService;

    @Autowired
    private OrderPricingService orderPricingService;

    @Autowired
    private PaymentRefundService paymentRefundService;

    @Autowired
    private PaymentTaxSettings taxSettings;

    @Autowired
    private DaemonContextExecutor daemonContextExecutor;

    /**
     * Who may see an order's payment and create its link: Reception (who enter and
     * edit orders) and administrators. The webhook below has no role check;
     * Razorpay is not an OpenELIS user, so it is signature-checked instead.
     */
    private static final String ORDER_STAFF = "hasAnyRole('RECEPTION', 'ADMIN')";

    @PreAuthorize(ORDER_STAFF)
    @GetMapping(value = "/sample/{sampleId}")
    public List<PaymentForm> getPaymentsForSample(@PathVariable String sampleId) {
        return paymentService.getPaymentsForSample(sampleId).stream().map(PaymentForm::from)
                .collect(Collectors.toList());
    }

    /**
     * What the order screen shows: amount due, latest status, and the link to pay
     * at.
     */
    @PreAuthorize(ORDER_STAFF)
    @GetMapping(value = "/sample/{sampleId}/summary")
    public PaymentSummary getSummary(@PathVariable String sampleId) {
        return PaymentSummary.of(sampleId, orderPricingService.quoteSample(sampleId),
                paymentService.getPaymentsForSample(sampleId), taxSettings);
    }

    /**
     * Staff retry: creates the order's payment link if one is not already pending
     * or paid.
     */
    @PreAuthorize(ORDER_STAFF)
    @PostMapping(value = "/sample/{sampleId}/link")
    public ResponseEntity<PaymentForm> requestLink(@PathVariable String sampleId, HttpServletRequest request) {
        try {
            Payment payment = paymentLinkService.requestLink(sampleId, ControllerUtills.getSysUserId(request));
            return ResponseEntity.ok(PaymentForm.from(payment));
        } catch (PaymentGatewayDisabledException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.unprocessableEntity().build();
        } catch (PaymentGatewayException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }

    /**
     * Razorpay webhook. Called without a login, so the signature is the only check:
     * a bad signature gets 400 and changes nothing. Any 2xx tells Razorpay not to
     * resend; non-2xx makes it retry.
     */
    @PostMapping(value = "/razorpay/webhook")
    public ResponseEntity<Void> razorpayWebhook(HttpServletRequest request,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature,
            @RequestHeader(value = "X-Razorpay-Event-Id", required = false) String eventId) {
        // Read the raw stream: Spring's body converters parse application/json bodies
        // (even into byte[] or String), so the signed bytes are taken directly.
        String rawBody;
        try {
            rawBody = new String(request.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            return ResponseEntity.badRequest().build();
        }
        try {
            WebhookOutcome outcome = daemonContextExecutor.executeAsDaemon(
                    () -> paymentWebhookService.process(rawBody, signature, eventId, ControllerUtills.getSysUserId()));
            if (outcome == WebhookOutcome.REJECTED_SIGNATURE || outcome == WebhookOutcome.MALFORMED) {
                LogEvent.logWarn(getClass().getSimpleName(), "razorpayWebhook",
                        "Razorpay webhook event " + safeEventId(eventId) + ": " + outcome);
                return ResponseEntity.badRequest().build();
            }
            LogEvent.logInfo(getClass().getSimpleName(), "razorpayWebhook",
                    "Razorpay webhook event " + safeEventId(eventId) + ": " + outcome);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            // Non-2xx so Razorpay retries; the event is not recorded, so a retry can apply
            // it.
            LogEvent.logError(getClass().getSimpleName(), "razorpayWebhook",
                    "Razorpay webhook event " + safeEventId(eventId) + " failed: " + e.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * The event id header comes from an unauthenticated caller: keep only safe
     * characters and cap the length so it cannot inject or flood log lines.
     */
    private static String safeEventId(String eventId) {
        if (eventId == null) {
            return "<none>";
        }
        String cleaned = eventId.replaceAll("[^A-Za-z0-9_-]", "");
        return cleaned.length() > 64 ? cleaned.substring(0, 64) : cleaned;
    }

    /**
     * Refunds a paid payment in full. Returns 202: the payment becomes REFUNDED
     * when the gateway's refund.processed webhook arrives. Admin only for now.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/{paymentId}/refund")
    public ResponseEntity<String> refund(@PathVariable String paymentId, HttpServletRequest request) {
        try {
            String refundId = paymentRefundService.refund(paymentId, ControllerUtills.getSysUserId(request));
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(refundId);
        } catch (PaymentGatewayDisabledException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.unprocessableEntity().build();
        } catch (PaymentGatewayException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }
}
