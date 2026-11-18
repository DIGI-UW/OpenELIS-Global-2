package org.openelisglobal.payment;

/**
 * The three amounts on a payment, in paise: the test subtotal, the GST on it,
 * and the total the patient pays (subtotal plus GST).
 */
public record PaymentAmounts(long subtotalPaise, long gstPaise, long totalPaise) {

    public static PaymentAmounts of(long subtotalPaise, PaymentTaxSettings tax) {
        long gst = tax.gstPaise(subtotalPaise);
        return new PaymentAmounts(subtotalPaise, gst, subtotalPaise + gst);
    }
}
