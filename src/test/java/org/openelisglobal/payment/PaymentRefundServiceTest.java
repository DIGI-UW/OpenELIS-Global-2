package org.openelisglobal.payment;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.payment.gateway.PaymentGatewayClient;
import org.openelisglobal.payment.gateway.PaymentGatewayDisabledException;
import org.openelisglobal.payment.gateway.PaymentGatewayProvider;
import org.openelisglobal.payment.service.PaymentRefundService;
import org.openelisglobal.payment.service.PaymentRefundServiceImpl;
import org.openelisglobal.payment.service.PaymentService;
import org.openelisglobal.payment.valueholder.Payment;
import org.openelisglobal.payment.valueholder.PaymentStatus;

public class PaymentRefundServiceTest {

    private PaymentService paymentService;
    private PaymentGatewayClient gateway;
    private PaymentRefundService service;

    @Before
    public void setUp() {
        paymentService = mock(PaymentService.class);
        gateway = mock(PaymentGatewayClient.class);
        PaymentGatewayProvider provider = mock(PaymentGatewayProvider.class);
        when(provider.current()).thenReturn(gateway);
        when(gateway.isEnabled()).thenReturn(true);
        service = new PaymentRefundServiceImpl(paymentService, provider);
    }

    private static Payment paid(String gatewayPaymentId) {
        Payment p = new Payment();
        p.setId("pay-1");
        p.setStatus(PaymentStatus.PAID);
        p.setAmountPaise(145671L);
        p.setGatewayPaymentId(gatewayPaymentId);
        return p;
    }

    @Test(expected = PaymentGatewayDisabledException.class)
    public void disabledGatewayRefusesWithoutTouchingPayments() {
        when(gateway.isEnabled()).thenReturn(false);
        try {
            service.refund("pay-1", "A-1");
        } finally {
            verify(gateway, never()).refund(anyString(), anyLong());
        }
    }

    @Test(expected = IllegalStateException.class)
    public void onlyAPaidPaymentCanBeRefunded() {
        Payment pending = paid("pay_9");
        pending.setStatus(PaymentStatus.PENDING);
        when(paymentService.get("pay-1")).thenReturn(pending);

        service.refund("pay-1", "A-1");
    }

    @Test(expected = IllegalStateException.class)
    public void paidPaymentWithoutGatewayIdCannotBeRefunded() {
        when(paymentService.get("pay-1")).thenReturn(paid(null));

        service.refund("pay-1", "A-1");
    }

    @Test
    public void refundSendsTheFullPaidAmountAndLeavesStatusToTheWebhook() {
        when(paymentService.get("pay-1")).thenReturn(paid("pay_9"));
        when(gateway.refund("pay_9", 145671L)).thenReturn("rfnd_1");

        assertEquals("rfnd_1", service.refund("pay-1", "A-1"));
        // The payment becomes REFUNDED only when Razorpay's refund.processed event arrives.
        verify(paymentService, never()).transition(anyString(), org.mockito.ArgumentMatchers.any(), anyString());
    }
}
