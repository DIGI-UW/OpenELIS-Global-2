package org.openelisglobal.payment;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.Test;
import org.openelisglobal.payment.gateway.RazorpayWebhookSignature;

public class RazorpayWebhookSignatureTest {

    private static final String SECRET = "whsec_test_secret";
    private static final String BODY = "{\"event\":\"payment_link.paid\",\"payload\":{}}";

    /** Reference HMAC-SHA256 hex, as Razorpay computes it over the raw body. */
    private static String sign(String body, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] digest = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : digest) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    @Test
    public void validSignatureIsAccepted() throws Exception {
        assertTrue(RazorpayWebhookSignature.isValid(BODY, sign(BODY, SECRET), SECRET));
    }

    @Test
    public void tamperedBodyIsRejected() throws Exception {
        String signature = sign(BODY, SECRET);
        String tampered = BODY.replace("payment_link.paid", "payment_link.cancelled");
        assertFalse(RazorpayWebhookSignature.isValid(tampered, signature, SECRET));
    }

    @Test
    public void wrongSecretIsRejected() throws Exception {
        assertFalse(RazorpayWebhookSignature.isValid(BODY, sign(BODY, "other"), SECRET));
    }

    @Test
    public void missingOrBlankSignatureIsRejected() {
        assertFalse(RazorpayWebhookSignature.isValid(BODY, null, SECRET));
        assertFalse(RazorpayWebhookSignature.isValid(BODY, "", SECRET));
    }

    @Test
    public void missingSecretRejectsEverything() throws Exception {
        // A blank secret is refused before any HMAC is computed.
        assertFalse(RazorpayWebhookSignature.isValid(BODY, "deadbeef", ""));
        assertFalse(RazorpayWebhookSignature.isValid(BODY, sign(BODY, SECRET), null));
    }
}
