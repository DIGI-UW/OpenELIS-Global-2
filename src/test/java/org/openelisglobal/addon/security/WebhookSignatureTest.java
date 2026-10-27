package org.openelisglobal.addon.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class WebhookSignatureTest {

    /** The example from the Standard Webhooks specification. */
    @Test
    public void matchesTheStandardWebhooksReferenceSignature() {
        String signature = WebhookSignature.sign("whsec_MfKQ9r8GKYqrTwjUPD8ILPZIo2LaLaSw",
                "msg_p5jXN8AQM9LWM0D4loKWxJek", 1614265330L, "{\"test\": 2432232314}");
        assertEquals("v1,g0hM9SsE+OTPJTGt/tmIKtSyZlE3uFJELVlNIOLJ1OE=", signature);
    }

    @Test
    public void theSignatureCoversIdTimestampAndBody() {
        String secret = WebhookSignature.newSecret();
        String base = WebhookSignature.sign(secret, "1", 100, "{}");
        assertNotEquals(base, WebhookSignature.sign(secret, "2", 100, "{}"));
        assertNotEquals(base, WebhookSignature.sign(secret, "1", 101, "{}"));
        assertNotEquals(base, WebhookSignature.sign(secret, "1", 100, "{ }"));
        assertNotEquals(base, WebhookSignature.sign(WebhookSignature.newSecret(), "1", 100, "{}"));
    }

    @Test
    public void newSecretsHaveTheStandardPrefix() {
        assertTrue(WebhookSignature.newSecret().startsWith("whsec_"));
    }
}
