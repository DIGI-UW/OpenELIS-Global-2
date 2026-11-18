package org.openelisglobal.payment;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.payment.gateway.PaymentGatewayClient;
import org.openelisglobal.payment.gateway.PaymentGatewayProvider;
import org.openelisglobal.payment.service.PaymentService;
import org.openelisglobal.payment.service.PaymentWebhookService;
import org.openelisglobal.payment.service.PaymentWebhookServiceImpl;
import org.openelisglobal.payment.service.WebhookOutcome;
import org.openelisglobal.payment.valueholder.Payment;
import org.openelisglobal.payment.valueholder.PaymentStatus;

public class PaymentWebhookServiceTest {

    private static final String USER = "7";
    private static final String SIG = "sig-ok";

    private static final String PAID = "{\"event\":\"payment_link.paid\",\"payload\":{"
            + "\"payment_link\":{\"entity\":{\"id\":\"plink_1\",\"status\":\"paid\"}},"
            + "\"payment\":{\"entity\":{\"id\":\"pay_9\",\"status\":\"captured\"}}}}";
    private static final String CANCELLED = "{\"event\":\"payment_link.cancelled\",\"payload\":{"
            + "\"payment_link\":{\"entity\":{\"id\":\"plink_1\",\"status\":\"cancelled\"}}}}";
    private static final String REFUND = "{\"event\":\"refund.processed\",\"payload\":{"
            + "\"refund\":{\"entity\":{\"id\":\"rfnd_1\",\"payment_id\":\"pay_9\"}}}}";

    private PaymentService paymentService;
    private PaymentGatewayClient gateway;
    private PaymentWebhookService service;

    @Before
    public void setUp() {
        paymentService = mock(PaymentService.class);
        gateway = mock(PaymentGatewayClient.class);
        PaymentGatewayProvider provider = mock(PaymentGatewayProvider.class);
        when(provider.current()).thenReturn(gateway);
        when(gateway.verifyWebhookSignature(anyString(), eq(SIG))).thenReturn(true);
        service = new PaymentWebhookServiceImpl(paymentService, provider);
    }

    private static Payment payment(String id, PaymentStatus status) {
        Payment p = new Payment();
        p.setId(id);
        p.setStatus(status);
        return p;
    }

    @Test
    public void badSignatureIsRejectedBeforeAnythingIsRead() {
        when(gateway.verifyWebhookSignature(anyString(), anyString())).thenReturn(false);

        assertEquals(WebhookOutcome.REJECTED_SIGNATURE, service.process(PAID, "forged", "evt_1", USER));

        verify(paymentService, never()).getByGatewayEventId(anyString());
        verify(paymentService, never()).transition(anyString(), any(), anyString());
    }

    @Test
    public void unparseableBodyWithValidSignatureIsMalformed() {
        assertEquals(WebhookOutcome.MALFORMED, service.process("not json", SIG, "evt_1", USER));
    }

    @Test
    public void missingEventIdIsMalformed() {
        assertEquals(WebhookOutcome.MALFORMED, service.process(PAID, SIG, null, USER));
    }

    @Test
    public void replayedEventIsDuplicateAndChangesNothing() {
        when(paymentService.getByGatewayEventId("evt_1")).thenReturn(payment("pay-1", PaymentStatus.PAID));

        assertEquals(WebhookOutcome.DUPLICATE, service.process(PAID, SIG, "evt_1", USER));
        verify(paymentService, never()).transition(anyString(), any(), anyString());
    }

    @Test
    public void paidEventMarksThePendingPaymentPaidAndRecordsTheIds() {
        Payment pending = payment("pay-1", PaymentStatus.PENDING);
        when(paymentService.getByGatewayLinkId("plink_1")).thenReturn(pending);

        assertEquals(WebhookOutcome.APPLIED, service.process(PAID, SIG, "evt_1", USER));

        assertEquals("pay_9", pending.getGatewayPaymentId());
        assertEquals("evt_1", pending.getGatewayEventId());
        verify(paymentService).update(pending);
        verify(paymentService).transition("pay-1", PaymentStatus.PAID, USER);
    }

    @Test
    public void cancelledEventAfterPaidIsIgnored() {
        when(paymentService.getByGatewayLinkId("plink_1")).thenReturn(payment("pay-1", PaymentStatus.PAID));

        assertEquals(WebhookOutcome.IGNORED, service.process(CANCELLED, SIG, "evt_2", USER));
        verify(paymentService, never()).transition(anyString(), any(), anyString());
    }

    @Test
    public void eventForAnUnknownLinkIsIgnored() {
        when(paymentService.getByGatewayLinkId("plink_1")).thenReturn(null);

        assertEquals(WebhookOutcome.IGNORED, service.process(PAID, SIG, "evt_1", USER));
        verify(paymentService, never()).transition(anyString(), any(), anyString());
    }

    @Test
    public void refundMarksThePaidPaymentRefunded() {
        when(paymentService.getByGatewayPaymentId("pay_9")).thenReturn(payment("pay-1", PaymentStatus.PAID));

        assertEquals(WebhookOutcome.APPLIED, service.process(REFUND, SIG, "evt_3", USER));
        verify(paymentService).transition("pay-1", PaymentStatus.REFUNDED, USER);
    }

    @Test
    public void unknownEventTypeIsIgnored() {
        String other = "{\"event\":\"order.paid\",\"payload\":{}}";

        assertEquals(WebhookOutcome.IGNORED, service.process(other, SIG, "evt_4", USER));
    }
}
