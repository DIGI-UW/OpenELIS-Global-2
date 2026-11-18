package org.openelisglobal.payment.form;

import java.util.List;
import org.openelisglobal.payment.Money;
import org.openelisglobal.payment.PaymentAmounts;
import org.openelisglobal.payment.PaymentTaxSettings;
import org.openelisglobal.payment.service.PriceQuote;
import org.openelisglobal.payment.valueholder.Payment;
import org.openelisglobal.payment.valueholder.PaymentStatus;

/**
 * What the order screen shows about payment: the test subtotal, GST, and the
 * total due (subtotal plus GST), the latest payment's status, whether anything
 * is paid, and the link to pay at while one is pending. Amounts are null when
 * the order has unpriced tests.
 */
public record PaymentSummary(String sampleId, String subtotalRupees, String gstRupees, String amountDueRupees,
        String status, boolean paid, String shortUrl, List<String> unpricedTestNames) {

    /** @param paymentsNewestFirst the order's payments, newest first */
    public static PaymentSummary of(String sampleId, PriceQuote quote, List<Payment> paymentsNewestFirst,
            PaymentTaxSettings tax) {
        String subtotal = null;
        String gst = null;
        String total = null;
        if (quote.isComplete() && quote.totalPaise() > 0) {
            PaymentAmounts amounts = PaymentAmounts.of(quote.totalPaise(), tax);
            subtotal = Money.formatPaiseAsRupees(amounts.subtotalPaise());
            gst = Money.formatPaiseAsRupees(amounts.gstPaise());
            total = Money.formatPaiseAsRupees(amounts.totalPaise());
        }
        String status = paymentsNewestFirst.isEmpty() ? "NOT_REQUESTED"
                : statusName(paymentsNewestFirst.get(0).getStatus());
        boolean paid = paymentsNewestFirst.stream().anyMatch(p -> p.getStatus() == PaymentStatus.PAID);
        String shortUrl = paymentsNewestFirst.stream().filter(p -> p.getStatus() == PaymentStatus.PENDING)
                .map(Payment::getShortUrl).findFirst().orElse(null);
        return new PaymentSummary(sampleId, subtotal, gst, total, status, paid, shortUrl, quote.unpricedTestNames());
    }

    private static String statusName(PaymentStatus status) {
        return status == null ? "NOT_REQUESTED" : status.name();
    }
}
