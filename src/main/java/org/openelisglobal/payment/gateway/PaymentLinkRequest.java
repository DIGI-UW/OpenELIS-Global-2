package org.openelisglobal.payment.gateway;

/**
 * What is sent to the gateway for a payment link: the order's accession number,
 * the amount in paise, and optionally the patient's phone and email so the
 * gateway can send the link. Never the patient's name or test details.
 */
public record PaymentLinkRequest(String accessionNumber, long amountPaise, CustomerContact customer) {

    public PaymentLinkRequest {
        if (accessionNumber == null || accessionNumber.isBlank()) {
            throw new IllegalArgumentException("accessionNumber is required");
        }
        if (amountPaise <= 0) {
            throw new IllegalArgumentException("amountPaise must be positive");
        }
        if (customer == null) {
            customer = CustomerContact.NONE;
        }
    }

    /** A link the gateway does not send to anyone. */
    public PaymentLinkRequest(String accessionNumber, long amountPaise) {
        this(accessionNumber, amountPaise, CustomerContact.NONE);
    }
}
