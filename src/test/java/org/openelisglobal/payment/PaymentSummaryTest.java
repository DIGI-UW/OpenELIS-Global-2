package org.openelisglobal.payment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;
import org.openelisglobal.payment.form.PaymentSummary;
import org.openelisglobal.payment.service.PriceQuote;
import org.openelisglobal.payment.valueholder.Payment;
import org.openelisglobal.payment.valueholder.PaymentStatus;

public class PaymentSummaryTest {

    private static final PaymentTaxSettings TAX = new PaymentTaxSettings(18);

    private static PriceQuote priced(long paise) {
        return PriceQuote.of(List.of(new PriceQuote.Line("t1", "Hb", paise)));
    }

    private static Payment payment(PaymentStatus status, String shortUrl) {
        Payment p = new Payment();
        p.setStatus(status);
        p.setShortUrl(shortUrl);
        p.setAmountPaise(145671L);
        return p;
    }

    @Test
    public void noPaymentYetShowsSubtotalGstAndTotalDue() {
        PaymentSummary s = PaymentSummary.of("101", priced(123450L), List.of(), TAX);

        assertEquals("NOT_REQUESTED", s.status());
        assertFalse(s.paid());
        assertEquals("1234.50", s.subtotalRupees());
        assertEquals("222.21", s.gstRupees());
        assertEquals("1456.71", s.amountDueRupees());
        assertNull(s.shortUrl());
    }

    @Test
    public void pendingPaymentExposesTheLinkAndIsNotPaid() {
        PaymentSummary s = PaymentSummary.of("101", priced(123450L),
                List.of(payment(PaymentStatus.PENDING, "https://rzp.io/i/abc")), TAX);

        assertEquals("PENDING", s.status());
        assertFalse(s.paid());
        assertEquals("https://rzp.io/i/abc", s.shortUrl());
    }

    @Test
    public void paidPaymentIsPaidAndHasNoLinkToShow() {
        PaymentSummary s = PaymentSummary.of("101", priced(123450L), List.of(payment(PaymentStatus.PAID, null)), TAX);

        assertTrue(s.paid());
        assertEquals("PAID", s.status());
        assertNull(s.shortUrl());
    }

    @Test
    public void unpricedOrderHasNoAmountAndNamesTheTests() {
        PriceQuote quote = PriceQuote.of(List.of(new PriceQuote.Line("t9", "Culture", null)));

        PaymentSummary s = PaymentSummary.of("101", quote, List.of(), TAX);

        assertNull(s.amountDueRupees());
        assertNull(s.gstRupees());
        assertEquals(List.of("Culture"), s.unpricedTestNames());
    }
}
