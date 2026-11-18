package org.openelisglobal.payment.service;

public interface PaymentRefundService {

    /**
     * Asks the gateway to refund a paid payment in full, and returns the refund id.
     * The payment stays PAID here; it becomes REFUNDED when Razorpay's
     * refund.processed webhook arrives.
     *
     * @throws org.openelisglobal.payment.gateway.PaymentGatewayDisabledException when
     *                                                                            online
     *                                                                            payment
     *                                                                            is
     *                                                                            off
     * @throws IllegalArgumentException                                           when
     *                                                                            the
     *                                                                            payment
     *                                                                            does
     *                                                                            not
     *                                                                            exist
     * @throws IllegalStateException                                              when
     *                                                                            the
     *                                                                            payment
     *                                                                            is
     *                                                                            not
     *                                                                            paid
     *                                                                            or
     *                                                                            has
     *                                                                            no
     *                                                                            gateway
     *                                                                            payment
     *                                                                            id
     */
    String refund(String paymentId, String sysUserId);
}
