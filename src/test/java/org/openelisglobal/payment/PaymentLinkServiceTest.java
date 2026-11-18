package org.openelisglobal.payment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.payment.gateway.CustomerContact;
import org.openelisglobal.payment.gateway.PaymentGatewayClient;
import org.openelisglobal.payment.gateway.PaymentGatewayDisabledException;
import org.openelisglobal.payment.gateway.PaymentGatewayException;
import org.openelisglobal.payment.gateway.PaymentGatewayProvider;
import org.openelisglobal.payment.gateway.PaymentLinkRequest;
import org.openelisglobal.payment.gateway.PaymentLinkResult;
import org.openelisglobal.payment.service.OrderPricingService;
import org.openelisglobal.payment.service.PaymentContactService;
import org.openelisglobal.payment.service.PaymentLinkService;
import org.openelisglobal.payment.service.PaymentLinkServiceImpl;
import org.openelisglobal.payment.service.PaymentService;
import org.openelisglobal.payment.service.PriceQuote;
import org.openelisglobal.payment.valueholder.Payment;
import org.openelisglobal.payment.valueholder.PaymentStatus;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;

public class PaymentLinkServiceTest {

    private static final String SAMPLE_ID = "101";
    private static final String USER = "1";

    private PaymentService paymentService;
    private PaymentGatewayProvider gatewayProvider;
    private PaymentGatewayClient gateway;
    private OrderPricingService pricing;
    private SampleService sampleService;
    private PaymentContactService contactService;
    private PaymentLinkService service;

    @Before
    public void setUp() {
        paymentService = mock(PaymentService.class);
        gatewayProvider = mock(PaymentGatewayProvider.class);
        gateway = mock(PaymentGatewayClient.class);
        pricing = mock(OrderPricingService.class);
        sampleService = mock(SampleService.class);
        contactService = mock(PaymentContactService.class);

        when(gatewayProvider.current()).thenReturn(gateway);
        when(gateway.isEnabled()).thenReturn(true);

        Sample sample = new Sample();
        sample.setId(SAMPLE_ID);
        sample.setAccessionNumber("A-2026-0001");
        when(sampleService.get(SAMPLE_ID)).thenReturn(sample);

        service = new PaymentLinkServiceImpl(paymentService, gatewayProvider, pricing, sampleService, contactService,
                new PaymentTaxSettings(18));
    }

    private static PriceQuote pricedQuote(long paise) {
        return PriceQuote.of(List.of(new PriceQuote.Line("t1", "Hb", paise)));
    }

    @Test(expected = PaymentGatewayDisabledException.class)
    public void disabledGatewayRefusesWithoutTouchingAnything() {
        when(gateway.isEnabled()).thenReturn(false);

        try {
            service.requestLink(SAMPLE_ID, USER);
        } finally {
            verify(gateway, never()).createPaymentLink(any());
            verify(paymentService, never()).save(any());
        }
    }

    @Test
    public void existingPendingPaymentIsReturnedWithoutANewLink() {
        Payment pending = new Payment();
        pending.setId("pay-1");
        pending.setStatus(PaymentStatus.PENDING);
        when(paymentService.getPaymentsForSample(SAMPLE_ID)).thenReturn(List.of(pending));

        assertSame(pending, service.requestLink(SAMPLE_ID, USER));
        verify(gateway, never()).createPaymentLink(any());
    }

    @Test
    public void unpricedTestBlocksTheLinkAndNamesTheTest() {
        when(paymentService.getPaymentsForSample(SAMPLE_ID)).thenReturn(List.of());
        when(pricing.quoteSample(SAMPLE_ID))
                .thenReturn(PriceQuote.of(List.of(new PriceQuote.Line("t9", "Culture", null))));

        try {
            service.requestLink(SAMPLE_ID, USER);
            fail("expected IllegalStateException");
        } catch (IllegalStateException e) {
            assertEquals(true, e.getMessage().contains("Culture"));
        }
        verify(gateway, never()).createPaymentLink(any());
    }

    @Test(expected = IllegalStateException.class)
    public void zeroPricedOrderIsNotBilled() {
        when(paymentService.getPaymentsForSample(SAMPLE_ID)).thenReturn(List.of());
        when(pricing.quoteSample(SAMPLE_ID)).thenReturn(PriceQuote.of(List.of()));

        service.requestLink(SAMPLE_ID, USER);
    }

    @Test
    public void successStoresLinkAndMovesToPending() {
        when(paymentService.getPaymentsForSample(SAMPLE_ID)).thenReturn(List.of());
        when(pricing.quoteSample(SAMPLE_ID)).thenReturn(pricedQuote(123450L));
        when(paymentService.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId("pay-1");
            return p;
        });
        when(gateway.createPaymentLink(any(PaymentLinkRequest.class)))
                .thenReturn(new PaymentLinkResult("plink_1", "https://rzp.io/i/abc"));
        when(paymentService.transition(eq("pay-1"), eq(PaymentStatus.PENDING), anyString())).thenAnswer(inv -> {
            Payment p = new Payment();
            p.setId("pay-1");
            p.setStatus(PaymentStatus.PENDING);
            return p;
        });

        Payment result = service.requestLink(SAMPLE_ID, USER);

        // Subtotal 123450 paise plus 18% GST (22221) is the amount the patient pays.
        verify(gateway).createPaymentLink(new PaymentLinkRequest("A-2026-0001", 145671L));
        verify(paymentService).update(any(Payment.class));
        assertEquals(PaymentStatus.PENDING, result.getStatus());
    }

    @Test
    public void gatewayFailureMarksPaymentFailedAndRethrows() {
        when(paymentService.getPaymentsForSample(SAMPLE_ID)).thenReturn(List.of());
        when(pricing.quoteSample(SAMPLE_ID)).thenReturn(pricedQuote(5000L));
        when(paymentService.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId("pay-2");
            return p;
        });
        when(gateway.createPaymentLink(any(PaymentLinkRequest.class)))
                .thenThrow(new PaymentGatewayException("Razorpay returned HTTP 500"));

        try {
            service.requestLink(SAMPLE_ID, USER);
            fail("expected PaymentGatewayException");
        } catch (PaymentGatewayException expected) {
            verify(paymentService).transition("pay-2", PaymentStatus.FAILED, USER);
        }
    }

    /** A priced order whose link the gateway creates successfully. */
    private void stubSuccessfulLink() {
        when(paymentService.getPaymentsForSample(SAMPLE_ID)).thenReturn(List.of());
        when(pricing.quoteSample(SAMPLE_ID)).thenReturn(pricedQuote(10000L));
        when(paymentService.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId("pay-3");
            return p;
        });
        when(gateway.createPaymentLink(any(PaymentLinkRequest.class)))
                .thenReturn(new PaymentLinkResult("plink_3", "https://rzp.io/i/xyz"));
    }

    @Test
    public void withNotificationOnThePatientsContactIsSent() {
        stubSuccessfulLink();
        CustomerContact contact = CustomerContact.of("9876543210", "pat@example.org");
        when(gateway.notifiesCustomer()).thenReturn(true);
        when(contactService.contactForSample(SAMPLE_ID)).thenReturn(contact);

        service.requestLink(SAMPLE_ID, USER);

        // 10000 paise plus 18% GST.
        verify(gateway).createPaymentLink(new PaymentLinkRequest("A-2026-0001", 11800L, contact));
    }

    @Test
    public void withNotificationOffThePatientsContactIsNeverRead() {
        stubSuccessfulLink();
        when(gateway.notifiesCustomer()).thenReturn(false);

        service.requestLink(SAMPLE_ID, USER);

        verify(contactService, never()).contactForSample(anyString());
        verify(gateway).createPaymentLink(new PaymentLinkRequest("A-2026-0001", 11800L));
    }
}
