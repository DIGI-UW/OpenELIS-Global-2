package org.openelisglobal.payment.daoimpl;

import java.util.Collections;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.payment.dao.PaymentDAO;
import org.openelisglobal.payment.valueholder.Payment;
import org.springframework.stereotype.Repository;

@Repository
public class PaymentDAOImpl extends BaseDAOImpl<Payment, String> implements PaymentDAO {

    public PaymentDAOImpl() {
        super(Payment.class);
    }

    @Override
    public List<Payment> getBySampleId(String sampleId) {
        if (sampleId == null || sampleId.trim().isEmpty()) {
            return Collections.emptyList();
        }
        Session session = entityManager.unwrap(Session.class);
        Query<Payment> query = session
                .createQuery("FROM Payment p WHERE p.sample.id = :sampleId ORDER BY p.lastupdated DESC", Payment.class);
        query.setParameter("sampleId", sampleId.trim());
        return query.list();
    }

    @Override
    public Payment getByGatewayLinkId(String gatewayLinkId) {
        return findUnique("FROM Payment p WHERE p.gatewayLinkId = :value", gatewayLinkId);
    }

    @Override
    public Payment getByGatewayEventId(String gatewayEventId) {
        return findUnique("FROM Payment p WHERE p.gatewayEventId = :value", gatewayEventId);
    }

    private Payment findUnique(String hql, String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        Session session = entityManager.unwrap(Session.class);
        Query<Payment> query = session.createQuery(hql, Payment.class);
        query.setParameter("value", value.trim());
        query.setMaxResults(1);
        return query.uniqueResult();
    }

    @Override
    public Payment getByGatewayPaymentId(String gatewayPaymentId) {
        return findUnique("FROM Payment p WHERE p.gatewayPaymentId = :value", gatewayPaymentId);
    }
}
