package org.openelisglobal.payment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * GST rate applied to payable amounts. Read from
 * {@code org.openelisglobal.payment.gst.rate} (percent, e.g. 18) so it can be
 * changed in configuration without a code change.
 */
@Component
public class PaymentTaxSettings {

    private final int gstRatePercent;

    @Autowired
    public PaymentTaxSettings(@Value("${org.openelisglobal.payment.gst.rate:18}") int gstRatePercent) {
        if (gstRatePercent < 0 || gstRatePercent > 100) {
            throw new IllegalArgumentException("GST rate must be between 0 and 100, was " + gstRatePercent);
        }
        this.gstRatePercent = gstRatePercent;
    }

    public int getGstRatePercent() {
        return gstRatePercent;
    }

    /** GST on an amount in paise, rounded half-up to the nearest paisa. */
    public long gstPaise(long amountPaise) {
        return (amountPaise * gstRatePercent + 50) / 100;
    }
}
