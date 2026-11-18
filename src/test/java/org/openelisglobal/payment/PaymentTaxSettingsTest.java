package org.openelisglobal.payment;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PaymentTaxSettingsTest {

    @Test
    public void gstOnAmountUsesConfiguredRate() {
        PaymentTaxSettings settings = new PaymentTaxSettings(18);
        // Rs 1000.00 = 100000 paise; 18% = Rs 180.00 = 18000 paise
        assertEquals(18000L, settings.gstPaise(100000L));
    }

    @Test
    public void gstRoundsHalfUpToWholePaise() {
        PaymentTaxSettings settings = new PaymentTaxSettings(18);
        // 18% of 25 paise = 4.5 paise, rounds up to 5
        assertEquals(5L, settings.gstPaise(25L));
    }

    @Test
    public void zeroRateGivesZeroGst() {
        PaymentTaxSettings settings = new PaymentTaxSettings(0);
        assertEquals(0L, settings.gstPaise(100000L));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rateAboveHundredIsRejected() {
        new PaymentTaxSettings(101);
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeRateIsRejected() {
        new PaymentTaxSettings(-1);
    }
}
