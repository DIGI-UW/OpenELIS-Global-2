package org.openelisglobal.payment.service;

import org.openelisglobal.payment.valueholder.Payment;

public interface PaymentLinkService {

    /**
     * Returns the order's payment, creating a gateway payment link if none is
     * pending or paid yet. An existing pending or paid payment is returned as is.
     *
     * <p>
     * Fails with a {@code PaymentGatewayDisabledException} when online payment is
     * off, an {@code IllegalArgumentException} when the sample does not exist, an
     * {@code IllegalStateException} when the order has unpriced tests or nothing to
     * charge, and a {@code PaymentGatewayException} when the gateway call fails.
     */
    Payment requestLink(String sampleId, String sysUserId);
}
