package org.openelisglobal.payment.form;

import org.openelisglobal.payment.Money;
import org.openelisglobal.payment.valueholder.Payment;
import org.openelisglobal.payment.valueholder.PaymentStatus;

/** Read model for a payment, returned by the payment REST endpoints. */
public class PaymentForm {

    private String id;
    private String sampleId;
    private Long amountPaise;
    private String amountRupees;
    private String currency;
    private PaymentStatus status;
    private String shortUrl;
    private String gatewayPaymentId;

    public static PaymentForm from(Payment payment) {
        PaymentForm form = new PaymentForm();
        form.id = payment.getId();
        form.sampleId = payment.getSample() == null ? null : payment.getSample().getId();
        form.amountPaise = payment.getAmountPaise();
        form.amountRupees = Money.formatPaiseAsRupees(payment.getAmountPaise());
        form.currency = payment.getCurrency();
        form.status = payment.getStatus();
        form.shortUrl = payment.getShortUrl();
        form.gatewayPaymentId = payment.getGatewayPaymentId();
        return form;
    }

    public String getId() {
        return id;
    }

    public String getSampleId() {
        return sampleId;
    }

    public Long getAmountPaise() {
        return amountPaise;
    }

    public String getAmountRupees() {
        return amountRupees;
    }

    public String getCurrency() {
        return currency;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getShortUrl() {
        return shortUrl;
    }

    public String getGatewayPaymentId() {
        return gatewayPaymentId;
    }
}
