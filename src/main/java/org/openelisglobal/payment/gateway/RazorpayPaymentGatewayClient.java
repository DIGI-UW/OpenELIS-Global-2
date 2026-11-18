package org.openelisglobal.payment.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Razorpay implementation. Uses the JDK HTTP client rather than the shared
 * Apache client, so payment traffic is configured on its own.
 */
@Component
public class RazorpayPaymentGatewayClient implements PaymentGatewayClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Value("${org.openelisglobal.payment.razorpay.enabled:false}")
    private boolean enabled;

    @Value("${org.openelisglobal.payment.razorpay.keyId:}")
    private String keyId;

    @Value("${org.openelisglobal.payment.razorpay.keySecret:}")
    private String keySecret;

    @Value("${org.openelisglobal.payment.razorpay.webhookSecret:}")
    private String webhookSecret;

    @Value("${org.openelisglobal.payment.razorpay.baseUrl:https://api.razorpay.com/v1}")
    private String baseUrl;

    /**
     * Off by default: when on, the patient's phone and email go to Razorpay so it
     * can SMS and email the link.
     */
    @Value("${org.openelisglobal.payment.razorpay.notify:false}")
    private boolean notify;

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    @Override
    public boolean isEnabled() {
        return enabled && notBlank(keyId) && notBlank(keySecret);
    }

    @Override
    public boolean notifiesCustomer() {
        return notify;
    }

    /**
     * Builds the payment-link request body: the accession (as reference_id), the
     * amount and, when the request has one, the patient's phone and/or email as the
     * customer. Razorpay sends the link by SMS when there is a phone and by email
     * when there is an email. Never a name or a description.
     */
    public static String buildPaymentLinkBody(PaymentLinkRequest request) {
        ObjectNode body = MAPPER.createObjectNode();
        body.put("amount", request.amountPaise());
        body.put("currency", "INR");
        body.put("reference_id", request.accessionNumber());
        body.put("accept_partial", false);
        body.put("reminder_enable", false);
        CustomerContact contact = request.customer();
        if (!contact.isEmpty()) {
            ObjectNode customer = body.putObject("customer");
            if (contact.phone() != null) {
                customer.put("contact", contact.phone());
            }
            if (contact.email() != null) {
                customer.put("email", contact.email());
            }
        }
        ObjectNode notify = body.putObject("notify");
        notify.put("sms", contact.phone() != null);
        notify.put("email", contact.email() != null);
        try {
            return MAPPER.writeValueAsString(body);
        } catch (IOException e) {
            throw new IllegalStateException("Could not build payment link body", e);
        }
    }

    @Override
    public PaymentLinkResult createPaymentLink(PaymentLinkRequest request) {
        requireEnabled();
        JsonNode response = post("/payment_links", buildPaymentLinkBody(request));
        return new PaymentLinkResult(text(response, "id"), text(response, "short_url"));
    }

    @Override
    public String refund(String gatewayPaymentId, long amountPaise) {
        requireEnabled();
        ObjectNode body = MAPPER.createObjectNode();
        body.put("amount", amountPaise);
        JsonNode response = post("/payments/" + gatewayPaymentId + "/refund", body.toString());
        return text(response, "id");
    }

    @Override
    public boolean verifyWebhookSignature(String rawBody, String signatureHeader) {
        return RazorpayWebhookSignature.isValid(rawBody, signatureHeader, webhookSecret);
    }

    private JsonNode post(String path, String jsonBody) {
        String credentials = keyId + ":" + keySecret;
        String auth = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path)).timeout(Duration.ofSeconds(20))
                .header("Authorization", "Basic " + auth).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8)).build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                // Status only: the response body is not logged or returned.
                throw new PaymentGatewayException("Razorpay returned HTTP " + response.statusCode());
            }
            return MAPPER.readTree(response.body());
        } catch (IOException e) {
            throw new PaymentGatewayException("Razorpay request failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PaymentGatewayException("Razorpay request interrupted", e);
        }
    }

    private void requireEnabled() {
        if (!isEnabled()) {
            throw new PaymentGatewayDisabledException();
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            throw new PaymentGatewayException("Razorpay response missing " + field);
        }
        return value.asText();
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
