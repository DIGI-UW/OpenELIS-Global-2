package org.openelisglobal.payment;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PaymentAmountsTest {

    @Test
    public void totalIsSubtotalPlusGst() {
        PaymentAmounts amounts = PaymentAmounts.of(100000L, new PaymentTaxSettings(18));

        assertEquals(100000L, amounts.subtotalPaise());
        assertEquals(18000L, amounts.gstPaise());
        assertEquals(118000L, amounts.totalPaise());
    }

    @Test
    public void gstRoundsLikeTheTaxSettings() {
        PaymentAmounts amounts = PaymentAmounts.of(25L, new PaymentTaxSettings(18));

        assertEquals(5L, amounts.gstPaise());
        assertEquals(30L, amounts.totalPaise());
    }

    @Test
    public void zeroRateLeavesTheSubtotalAsTheTotal() {
        PaymentAmounts amounts = PaymentAmounts.of(100000L, new PaymentTaxSettings(0));

        assertEquals(100000L, amounts.totalPaise());
    }
}
