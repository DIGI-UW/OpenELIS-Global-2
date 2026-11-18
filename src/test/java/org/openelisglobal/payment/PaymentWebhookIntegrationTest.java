package org.openelisglobal.payment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.payment.controller.rest.PaymentRestController;
import org.openelisglobal.payment.gateway.RazorpayPaymentGatewayClient;
import org.openelisglobal.payment.service.PaymentService;
import org.openelisglobal.payment.valueholder.Payment;
import org.openelisglobal.payment.valueholder.PaymentStatus;
import org.openelisglobal.sample.service.SampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * The Razorpay webhook end to end: real controller, services and database,
 * signed payloads. Signatures are computed here independently of
 * RazorpayWebhookSignature, so a bug there cannot make the test agree with it.
 *
 * <p>
 * The controller is excluded from the shared test context (AppTestConfig), so
 * it is built and autowired here. Razorpay is switched on for these tests only,
 * on the shared client bean, and switched back off in {@code @After}.
 */
public class PaymentWebhookIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String ACCESSION = "PAY-WH-A";
    private static final String SECRET = "whsec_integration_test";
    private static final String URL = "/rest/payments/razorpay/webhook";

    @Autowired
    private DataSource dataSource;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private RazorpayPaymentGatewayClient razorpay;

    private JdbcTemplate jdbc;
    private MockMvc webhook;
    private String paymentId;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        cleanUp();

        ReflectionTestUtils.setField(razorpay, "enabled", true);
        ReflectionTestUtils.setField(razorpay, "keyId", "rzp_test_integration");
        ReflectionTestUtils.setField(razorpay, "keySecret", "integration_secret");
        ReflectionTestUtils.setField(razorpay, "webhookSecret", SECRET);

        PaymentRestController controller = new PaymentRestController();
        webApplicationContext.getAutowireCapableBeanFactory().autowireBean(controller);
        webhook = MockMvcBuilders.standaloneSetup(controller).build();

        String sampleId = String.valueOf(jdbc.queryForObject("SELECT nextval('sample_seq')", Long.class));
        jdbc.update(
                "INSERT INTO sample (id, accession_number, entered_date, received_date, lastupdated, sys_user_id) "
                        + "VALUES (?, ?, CURRENT_DATE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1)",
                Long.valueOf(sampleId), ACCESSION);
        Payment payment = new Payment();
        payment.setSample(sampleService.get(sampleId));
        payment.setAmountPaise(11800L);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setGatewayLinkId("plink_wh");
        payment.setSysUserId(TEST_SYS_USER_ID);
        paymentId = paymentService.save(payment).getId();
    }

    @After
    public void cleanUp() {
        ReflectionTestUtils.setField(razorpay, "enabled", false);
        ReflectionTestUtils.setField(razorpay, "keyId", "");
        ReflectionTestUtils.setField(razorpay, "keySecret", "");
        ReflectionTestUtils.setField(razorpay, "webhookSecret", "");
        jdbc.update("DELETE FROM clinlims.payment WHERE sample_id IN "
                + "(SELECT id FROM clinlims.sample WHERE accession_number = ?)", ACCESSION);
        jdbc.update("DELETE FROM clinlims.sample WHERE accession_number = ?", ACCESSION);
    }

    private static String sign(String body, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        StringBuilder hex = new StringBuilder();
        for (byte b : mac.doFinal(body.getBytes(StandardCharsets.UTF_8))) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    private static String linkEvent(String event, String linkId, String gatewayPaymentId) {
        return "{\"event\":\"" + event + "\",\"payload\":{\"payment_link\":{\"entity\":{\"id\":\"" + linkId
                + "\"}},\"payment\":{\"entity\":{\"id\":\"" + gatewayPaymentId + "\"}}}}";
    }

    private ResultActions send(String body, String signature, String eventId) throws Exception {
        return webhook.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body)
                .header("X-Razorpay-Signature", signature).header("X-Razorpay-Event-Id", eventId));
    }

    private Payment current() {
        return paymentService.get(paymentId);
    }

    @Test
    public void wronglySignedEventIsRefusedAndChangesNothing() throws Exception {
        String body = linkEvent("payment_link.paid", "plink_wh", "pay_1");

        send(body, sign(body, "not_the_secret"), "evt_bad").andExpect(status().isBadRequest());

        assertEquals(PaymentStatus.PENDING, current().getStatus());
        assertNull(current().getGatewayPaymentId());
    }

    @Test
    public void paidEventMarksThePaymentPaid() throws Exception {
        String body = linkEvent("payment_link.paid", "plink_wh", "pay_1");

        send(body, sign(body, SECRET), "evt_paid").andExpect(status().isOk());

        assertEquals(PaymentStatus.PAID, current().getStatus());
        assertEquals("pay_1", current().getGatewayPaymentId());
        assertEquals("evt_paid", current().getGatewayEventId());
    }

    @Test
    public void replayedEventIsAcceptedButAppliedOnlyOnce() throws Exception {
        String paid = linkEvent("payment_link.paid", "plink_wh", "pay_1");
        send(paid, sign(paid, SECRET), "evt_paid").andExpect(status().isOk());

        // Same event id again, with a body that would otherwise change the payment.
        String replay = linkEvent("payment_link.paid", "plink_wh", "pay_2");
        send(replay, sign(replay, SECRET), "evt_paid").andExpect(status().isOk());

        assertEquals("pay_1", current().getGatewayPaymentId());
    }

    @Test
    public void lateExpiryAfterPaymentDoesNotUndoIt() throws Exception {
        String paid = linkEvent("payment_link.paid", "plink_wh", "pay_1");
        send(paid, sign(paid, SECRET), "evt_paid").andExpect(status().isOk());

        String expired = linkEvent("payment_link.expired", "plink_wh", "pay_1");
        send(expired, sign(expired, SECRET), "evt_expired").andExpect(status().isOk());

        assertEquals(PaymentStatus.PAID, current().getStatus());
    }

    @Test
    public void refundProcessedMarksAPaidPaymentRefunded() throws Exception {
        String paid = linkEvent("payment_link.paid", "plink_wh", "pay_1");
        send(paid, sign(paid, SECRET), "evt_paid").andExpect(status().isOk());

        String refund = "{\"event\":\"refund.processed\",\"payload\":{\"refund\":{\"entity\":"
                + "{\"id\":\"rfnd_1\",\"payment_id\":\"pay_1\"}}}}";
        send(refund, sign(refund, SECRET), "evt_refund").andExpect(status().isOk());

        assertEquals(PaymentStatus.REFUNDED, current().getStatus());
    }

    @Test
    public void eventForAnotherLinkLeavesThisPaymentAlone() throws Exception {
        String body = linkEvent("payment_link.paid", "plink_other", "pay_9");

        send(body, sign(body, SECRET), "evt_other").andExpect(status().isOk());

        assertEquals(PaymentStatus.PENDING, current().getStatus());
    }

    @Test
    public void eventWithoutAnIdIsRefused() throws Exception {
        String body = linkEvent("payment_link.paid", "plink_wh", "pay_1");

        webhook.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body).header("X-Razorpay-Signature",
                sign(body, SECRET))).andExpect(status().isBadRequest());

        assertEquals(PaymentStatus.PENDING, current().getStatus());
    }
}
