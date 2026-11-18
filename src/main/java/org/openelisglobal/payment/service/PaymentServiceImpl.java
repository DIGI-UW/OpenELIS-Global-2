package org.openelisglobal.payment.service;

import java.util.List;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.payment.dao.PaymentDAO;
import org.openelisglobal.payment.valueholder.Payment;
import org.openelisglobal.payment.valueholder.PaymentStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentServiceImpl extends AuditableBaseObjectServiceImpl<Payment, String> implements PaymentService {

    @Autowired
    private PaymentDAO paymentDAO;

    public PaymentServiceImpl() {
        super(Payment.class);
    }

    @Override
    protected PaymentDAO getBaseObjectDAO() {
        return paymentDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> getPaymentsForSample(String sampleId) {
        return paymentDAO.getBySampleId(sampleId);
    }

    @Override
    @Transactional(readOnly = true)
    public Payment getByGatewayLinkId(String gatewayLinkId) {
        return paymentDAO.getByGatewayLinkId(gatewayLinkId);
    }

    @Override
    @Transactional(readOnly = true)
    public Payment getByGatewayEventId(String gatewayEventId) {
        return paymentDAO.getByGatewayEventId(gatewayEventId);
    }

    @Override
    @Transactional
    public Payment transition(String paymentId, PaymentStatus next, String sysUserId) {
        Payment payment = get(paymentId);
        if (payment == null) {
            throw new IllegalArgumentException("Payment not found: " + paymentId);
        }
        if (!payment.getStatus().canTransitionTo(next)) {
            throw new IllegalStateException("Cannot move payment from " + payment.getStatus() + " to " + next);
        }
        payment.setStatus(next);
        payment.setSysUserId(sysUserId);
        return update(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public Payment getByGatewayPaymentId(String gatewayPaymentId) {
        return paymentDAO.getByGatewayPaymentId(gatewayPaymentId);
    }
}
