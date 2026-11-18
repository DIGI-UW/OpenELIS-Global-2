package org.openelisglobal.payment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.payment.service.PaymentService;
import org.openelisglobal.payment.valueholder.Payment;
import org.openelisglobal.payment.valueholder.PaymentStatus;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Payment persistence against the real test database (Testcontainers Postgres
 * with the full Liquibase changelog, so 085/086/087 must apply cleanly). Real
 * service and DAO, no mocks. Rows are discriminating: two orders with payments
 * that differ in every looked-up field, so a finder that ignored its WHERE
 * clause, or matched the wrong column, fails.
 *
 * <p>
 * The base class does not roll back, so {@code @After} deletes in FK order.
 */
public class PaymentPersistenceIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String ACCESSION_A = "PAY-IT-A";
    private static final String ACCESSION_B = "PAY-IT-B";

    @Autowired
    private DataSource dataSource;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private SampleService sampleService;

    private JdbcTemplate jdbc;
    private String sampleA;
    private String sampleB;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        cleanUp();
        sampleA = insertSample(ACCESSION_A);
        sampleB = insertSample(ACCESSION_B);
    }

    @After
    public void cleanUp() {
        jdbc.update(
                "DELETE FROM clinlims.payment WHERE sample_id IN "
                        + "(SELECT id FROM clinlims.sample WHERE accession_number IN (?, ?))",
                ACCESSION_A, ACCESSION_B);
        jdbc.update("DELETE FROM clinlims.sample WHERE accession_number IN (?, ?)", ACCESSION_A, ACCESSION_B);
    }

    private String insertSample(String accession) {
        String id = String.valueOf(jdbc.queryForObject("SELECT nextval('sample_seq')", Long.class));
        jdbc.update(
                "INSERT INTO sample (id, accession_number, entered_date, received_date, lastupdated, sys_user_id) "
                        + "VALUES (?, ?, CURRENT_DATE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1)",
                Long.valueOf(id), accession);
        return id;
    }

    private Payment save(String sampleId, PaymentStatus status, String linkId, String gatewayPaymentId,
            String eventId) {
        // Loaded like PaymentLinkServiceImpl does, so Hibernate sees a persisted
        // sample.
        Sample sample = sampleService.get(sampleId);
        Payment payment = new Payment();
        payment.setSample(sample);
        payment.setSubtotalPaise(10000L);
        payment.setGstPaise(1800L);
        payment.setAmountPaise(11800L);
        payment.setStatus(status);
        payment.setGatewayLinkId(linkId);
        payment.setGatewayPaymentId(gatewayPaymentId);
        payment.setGatewayEventId(eventId);
        payment.setSysUserId(TEST_SYS_USER_ID);
        return paymentService.save(payment);
    }

    private void setLastUpdatedMinutesAgo(String paymentId, int minutes) {
        jdbc.update("UPDATE clinlims.payment SET last_updated = now() - make_interval(mins => ?) WHERE id = ?", minutes,
                paymentId);
    }

    @Test
    public void paymentsForAnOrderAreItsOwnAndNewestFirst() {
        Payment older = save(sampleA, PaymentStatus.CANCELLED, "plink_a1", null, null);
        Payment newer = save(sampleA, PaymentStatus.PENDING, "plink_a2", null, null);
        save(sampleB, PaymentStatus.PENDING, "plink_b1", null, null);
        setLastUpdatedMinutesAgo(older.getId(), 30);
        setLastUpdatedMinutesAgo(newer.getId(), 1);

        List<String> ids = paymentService.getPaymentsForSample(sampleA).stream().map(Payment::getId)
                .collect(Collectors.toList());

        assertEquals(List.of(newer.getId(), older.getId()), ids);
    }

    @Test
    public void eachGatewayIdFindsOnlyItsOwnPayment() {
        Payment a = save(sampleA, PaymentStatus.PAID, "plink_a", "pay_a", "evt_a");
        Payment b = save(sampleB, PaymentStatus.PAID, "plink_b", "pay_b", "evt_b");

        assertEquals(a.getId(), paymentService.getByGatewayLinkId("plink_a").getId());
        assertEquals(b.getId(), paymentService.getByGatewayLinkId("plink_b").getId());
        assertEquals(a.getId(), paymentService.getByGatewayPaymentId("pay_a").getId());
        assertEquals(b.getId(), paymentService.getByGatewayPaymentId("pay_b").getId());
        assertEquals(a.getId(), paymentService.getByGatewayEventId("evt_a").getId());
        assertEquals(b.getId(), paymentService.getByGatewayEventId("evt_b").getId());
    }

    @Test
    public void unknownGatewayIdsFindNothing() {
        save(sampleA, PaymentStatus.PAID, "plink_a", "pay_a", "evt_a");

        assertNull(paymentService.getByGatewayLinkId("plink_missing"));
        assertNull(paymentService.getByGatewayPaymentId("pay_missing"));
        assertNull(paymentService.getByGatewayEventId("evt_missing"));
        assertTrue(paymentService.getPaymentsForSample(sampleB).isEmpty());
    }

    @Test
    public void theSameGatewayEventCannotBeRecordedTwice() {
        save(sampleA, PaymentStatus.PAID, "plink_a", "pay_a", "evt_dup");

        // Backs up the webhook's duplicate check: the database refuses it too.
        assertThrows(RuntimeException.class, () -> save(sampleB, PaymentStatus.PAID, "plink_b", "pay_b", "evt_dup"));
    }

    @Test
    public void amountsAndStatusSurviveARoundTrip() {
        Payment saved = save(sampleA, PaymentStatus.PENDING, "plink_a", null, null);

        Payment loaded = paymentService.get(saved.getId());

        assertNotNull(loaded);
        assertEquals(Long.valueOf(10000L), loaded.getSubtotalPaise());
        assertEquals(Long.valueOf(1800L), loaded.getGstPaise());
        assertEquals(Long.valueOf(11800L), loaded.getAmountPaise());
        assertEquals("INR", loaded.getCurrency());
        assertEquals(PaymentStatus.PENDING, loaded.getStatus());
    }

    @Test
    public void allowedTransitionIsStoredAndABackwardOneIsRefused() {
        Payment payment = save(sampleA, PaymentStatus.PENDING, "plink_a", null, null);

        paymentService.transition(payment.getId(), PaymentStatus.PAID, TEST_SYS_USER_ID);
        assertEquals(PaymentStatus.PAID, paymentService.get(payment.getId()).getStatus());

        assertThrows(IllegalStateException.class,
                () -> paymentService.transition(payment.getId(), PaymentStatus.PENDING, TEST_SYS_USER_ID));
        assertEquals(PaymentStatus.PAID, paymentService.get(payment.getId()).getStatus());
    }

    @Test
    public void priceColumnsExistOnTestAndPanel() {
        // 085: nullable price per test and panel, in paise.
        assertEquals("bigint", columnType("test", "price_paise"));
        assertEquals("bigint", columnType("panel", "price_paise"));
        // 087: subtotal and GST kept apart on each payment.
        assertEquals("bigint", columnType("payment", "subtotal_paise"));
        assertEquals("bigint", columnType("payment", "gst_paise"));
    }

    private String columnType(String table, String column) {
        return jdbc.queryForObject(
                "SELECT data_type FROM information_schema.columns "
                        + "WHERE table_schema = 'clinlims' AND table_name = ? AND column_name = ?",
                String.class, table, column);
    }
}
