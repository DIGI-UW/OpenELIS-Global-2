package org.openelisglobal.payment.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.payment.valueholder.Payment;

public interface PaymentDAO extends BaseDAO<Payment, String> {

    /** All payments for an order, newest first. */
    List<Payment> getBySampleId(String sampleId);

    /** The payment with this gateway link id, or null. */
    Payment getByGatewayLinkId(String gatewayLinkId);

    /** The payment already recorded for this gateway event id, or null. */
    Payment getByGatewayEventId(String gatewayEventId);

    /** The payment with this Razorpay payment id, or null. */
    Payment getByGatewayPaymentId(String gatewayPaymentId);
}
