package org.openelisglobal.payment;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.payment.gateway.CustomerContact;
import org.openelisglobal.payment.gateway.RazorpayPaymentGatewayClient;
import org.openelisglobal.payment.service.PaymentContactService;
import org.openelisglobal.payment.service.PaymentLinkService;
import org.openelisglobal.payment.service.PaymentRefundService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Regression (found in the Razorpay test-mode run, 2026-11-17): the real
 * services' get() throws for a missing row instead of returning null, so an
 * unknown order or payment surfaced as HTTP 500. Each lookup must report "not
 * found" (IllegalArgumentException, which the controller maps to 404) or, for
 * the contact, simply no contact. Runs against the real database because the
 * unit tests' mocks return null and could not see this.
 */
public class PaymentLookupIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private PaymentLinkService paymentLinkService;

    @Autowired
    private PaymentRefundService paymentRefundService;

    @Autowired
    private PaymentContactService paymentContactService;

    @Autowired
    private RazorpayPaymentGatewayClient razorpay;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        // Link creation and refunds check the gateway is on before looking anything up.
        ReflectionTestUtils.setField(razorpay, "enabled", true);
        ReflectionTestUtils.setField(razorpay, "keyId", "rzp_test_lookup");
        ReflectionTestUtils.setField(razorpay, "keySecret", "lookup_secret");
    }

    @After
    public void switchRazorpayOff() {
        ReflectionTestUtils.setField(razorpay, "enabled", false);
        ReflectionTestUtils.setField(razorpay, "keyId", "");
        ReflectionTestUtils.setField(razorpay, "keySecret", "");
    }

    @Test
    public void linkForAnUnknownOrderIsNotFound() {
        assertThrows(IllegalArgumentException.class, () -> paymentLinkService.requestLink("999999", TEST_SYS_USER_ID));
    }

    @Test
    public void linkForANonNumericOrderIdIsNotFound() {
        assertThrows(IllegalArgumentException.class, () -> paymentLinkService.requestLink("abc", TEST_SYS_USER_ID));
    }

    @Test
    public void refundOfAnUnknownPaymentIsNotFound() {
        assertThrows(IllegalArgumentException.class,
                () -> paymentRefundService.refund("no-such-payment", TEST_SYS_USER_ID));
    }

    @Test
    public void contactForAnUnknownOrderIsNone() {
        assertSame(CustomerContact.NONE, paymentContactService.contactForSample("999999"));
    }
}
