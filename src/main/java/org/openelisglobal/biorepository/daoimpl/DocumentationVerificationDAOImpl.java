package org.openelisglobal.biorepository.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.openelisglobal.biorepository.dao.DocumentationVerificationDAO;
import org.openelisglobal.biorepository.valueholder.DocumentationVerification;
import org.openelisglobal.biorepository.valueholder.DocumentationVerification.OverallStatus;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.springframework.stereotype.Component;

/**
 * DAO implementation for DocumentationVerification entity operations.
 * Documentation verification is now at shipment level (one verification per
 * shipment).
 */
@Component
public class DocumentationVerificationDAOImpl extends BaseDAOImpl<DocumentationVerification, Integer>
        implements DocumentationVerificationDAO {

    public DocumentationVerificationDAOImpl() {
        super(DocumentationVerification.class);
    }

    @Override
    public DocumentationVerification getByShipmentId(Integer shipmentId) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM DocumentationVerification dv WHERE dv.shipment.id = :shipmentId";
        List<DocumentationVerification> results = session.createQuery(hql, DocumentationVerification.class)
                .setParameter("shipmentId", shipmentId).getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public List<DocumentationVerification> getByOverallStatus(OverallStatus status) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM DocumentationVerification dv WHERE dv.overallStatus = :status "
                + "ORDER BY dv.lastupdated DESC";
        return session.createQuery(hql, DocumentationVerification.class).setParameter("status", status.name())
                .getResultList();
    }

    @Override
    public List<DocumentationVerification> getByVerifiedByUserId(Integer verifiedByUserId) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM DocumentationVerification dv WHERE dv.verifiedByUser.id = :verifiedByUserId "
                + "ORDER BY dv.verifiedTimestamp DESC";
        return session.createQuery(hql, DocumentationVerification.class)
                .setParameter("verifiedByUserId", verifiedByUserId).getResultList();
    }

    @Override
    public List<DocumentationVerification> getPendingVerifications(int limit) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM DocumentationVerification dv WHERE dv.overallStatus = :status "
                + "ORDER BY dv.lastupdated ASC";
        return session.createQuery(hql, DocumentationVerification.class)
                .setParameter("status", OverallStatus.PENDING.name()).setMaxResults(limit).getResultList();
    }

    @Override
    public long countByOverallStatus(OverallStatus status) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "SELECT COUNT(dv) FROM DocumentationVerification dv WHERE dv.overallStatus = :status";
        return session.createQuery(hql, Long.class).setParameter("status", status.name()).getSingleResult();
    }

    @Override
    public boolean existsByShipmentId(Integer shipmentId) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "SELECT COUNT(dv) FROM DocumentationVerification dv WHERE dv.shipment.id = :shipmentId";
        Long count = session.createQuery(hql, Long.class).setParameter("shipmentId", shipmentId).getSingleResult();
        return count > 0;
    }
}
