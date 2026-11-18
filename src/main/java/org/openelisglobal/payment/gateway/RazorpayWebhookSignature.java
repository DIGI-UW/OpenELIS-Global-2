package org.openelisglobal.payment.gateway;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Verifies the X-Razorpay-Signature header: hex HMAC-SHA256 of the raw request
 * body, keyed with the webhook secret.
 */
public final class RazorpayWebhookSignature {

    private RazorpayWebhookSignature() {
    }

    public static boolean isValid(String rawBody, String signatureHeader, String secret) {
        if (rawBody == null || signatureHeader == null || signatureHeader.isBlank() || secret == null
                || secret.isBlank()) {
            return false;
        }
        String expected = hmacSha256Hex(rawBody, secret);
        // Constant-time comparison so the check does not leak timing information.
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                signatureHeader.trim().getBytes(StandardCharsets.UTF_8));
    }

    private static String hmacSha256Hex(String body, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 unavailable", e);
        }
    }
}
