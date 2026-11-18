package org.openelisglobal.payment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.openelisglobal.payment.gateway.CustomerContact;
import org.openelisglobal.payment.gateway.PaymentLinkRequest;
import org.openelisglobal.payment.gateway.RazorpayPaymentGatewayClient;

public class RazorpayPaymentLinkBodyTest {

    @Test
    public void bodyCarriesAmountInPaiseAndAccessionAsReference() throws Exception {
        PaymentLinkRequest request = new PaymentLinkRequest("A-2026-0001", 123450L);
        JsonNode body = new ObjectMapper().readTree(RazorpayPaymentGatewayClient.buildPaymentLinkBody(request));

        assertEquals(123450L, body.get("amount").asLong());
        assertEquals("INR", body.get("currency").asText());
        assertEquals("A-2026-0001", body.get("reference_id").asText());
    }

    @Test
    public void bodyNeverCarriesPatientOrTestDetails() throws Exception {
        PaymentLinkRequest request = new PaymentLinkRequest("A-2026-0001", 123450L);
        String json = RazorpayPaymentGatewayClient.buildPaymentLinkBody(request);

        // Only the accession and amount go to the gateway: no customer block and
        // no free-text description that could carry test names or patient data.
        JsonNode body = new ObjectMapper().readTree(json);
        assertFalse(body.has("customer"));
        assertFalse(body.has("description"));
        assertTrue(body.has("amount"));
    }

    @Test
    public void withoutContactRazorpaySendsNothingToThePatient() throws Exception {
        JsonNode body = new ObjectMapper().readTree(
                RazorpayPaymentGatewayClient.buildPaymentLinkBody(new PaymentLinkRequest("A-2026-0001", 100L)));

        assertFalse(body.get("notify").get("sms").asBoolean());
        assertFalse(body.get("notify").get("email").asBoolean());
    }

    @Test
    public void contactGoesAsCustomerAndTurnsOnMatchingNotifications() throws Exception {
        PaymentLinkRequest request = new PaymentLinkRequest("A-2026-0001", 100L,
                CustomerContact.of("+919876543210", "pat@example.org"));
        JsonNode body = new ObjectMapper().readTree(RazorpayPaymentGatewayClient.buildPaymentLinkBody(request));

        assertEquals("+919876543210", body.get("customer").get("contact").asText());
        assertEquals("pat@example.org", body.get("customer").get("email").asText());
        assertTrue(body.get("notify").get("sms").asBoolean());
        assertTrue(body.get("notify").get("email").asBoolean());
    }

    @Test
    public void phoneOnlyContactSendsSmsButNoEmail() throws Exception {
        PaymentLinkRequest request = new PaymentLinkRequest("A-2026-0001", 100L,
                CustomerContact.of("9876543210", null));
        JsonNode body = new ObjectMapper().readTree(RazorpayPaymentGatewayClient.buildPaymentLinkBody(request));

        assertTrue(body.get("notify").get("sms").asBoolean());
        assertFalse(body.get("notify").get("email").asBoolean());
        assertFalse(body.get("customer").has("email"));
    }

    @Test
    public void customerNeverCarriesTheNameAndThereIsStillNoDescription() throws Exception {
        PaymentLinkRequest request = new PaymentLinkRequest("A-2026-0001", 100L,
                CustomerContact.of("9876543210", "pat@example.org"));
        JsonNode body = new ObjectMapper().readTree(RazorpayPaymentGatewayClient.buildPaymentLinkBody(request));

        assertFalse(body.get("customer").has("name"));
        assertFalse(body.has("description"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void zeroAmountIsRejected() {
        new PaymentLinkRequest("A-2026-0001", 0L);
    }

    @Test(expected = IllegalArgumentException.class)
    public void blankAccessionIsRejected() {
        new PaymentLinkRequest(" ", 100L);
    }
}
