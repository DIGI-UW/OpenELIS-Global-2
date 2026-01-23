package org.openelisglobal.biorepository.daoimpl;

import java.sql.Timestamp;
import java.util.List;
import org.hibernate.Session;
import org.openelisglobal.biorepository.dao.ShipmentDAO;
import org.openelisglobal.biorepository.valueholder.Shipment;
import org.openelisglobal.biorepository.valueholder.Shipment.ShipmentStatus;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.springframework.stereotype.Component;

/**
 * DAO implementation for Shipment entity operations.
 */
@Component
public class ShipmentDAOImpl extends BaseDAOImpl<Shipment, Integer> implements ShipmentDAO {

    public ShipmentDAOImpl() {
        super(Shipment.class);
    }

    @Override
    public Shipment getByDeliveryReference(String deliveryReference) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM Shipment s WHERE s.deliveryReference = :deliveryReference";
        List<Shipment> results = session.createQuery(hql, Shipment.class)
                .setParameter("deliveryReference", deliveryReference).getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public List<Shipment> getByStatus(ShipmentStatus status) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM Shipment s WHERE s.status = :status ORDER BY s.receptionTimestamp DESC";
        return session.createQuery(hql, Shipment.class).setParameter("status", status.name()).getResultList();
    }

    @Override
    public List<Shipment> getByDateRange(Timestamp startDate, Timestamp endDate) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM Shipment s WHERE s.receptionTimestamp >= :startDate "
                + "AND s.receptionTimestamp <= :endDate " + "ORDER BY s.receptionTimestamp DESC";
        return session.createQuery(hql, Shipment.class).setParameter("startDate", startDate)
                .setParameter("endDate", endDate).getResultList();
    }

    @Override
    public List<Shipment> getByReceiverId(Integer receiverUserId) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM Shipment s WHERE s.receiver.id = :receiverUserId ORDER BY s.receptionTimestamp DESC";
        return session.createQuery(hql, Shipment.class).setParameter("receiverUserId", receiverUserId).getResultList();
    }

    @Override
    public List<Shipment> getRecentShipments(int offset, int limit) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM Shipment s ORDER BY s.receptionTimestamp DESC";
        return session.createQuery(hql, Shipment.class).setFirstResult(offset).setMaxResults(limit).getResultList();
    }

    @Override
    public long countByStatus(ShipmentStatus status) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "SELECT COUNT(s) FROM Shipment s WHERE s.status = :status";
        return session.createQuery(hql, Long.class).setParameter("status", status.name()).getSingleResult();
    }

    @Override
    public List<Shipment> search(String searchTerm, int limit) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM Shipment s WHERE LOWER(s.senderName) LIKE :searchTerm "
                + "OR LOWER(s.deliveryReference) LIKE :searchTerm " + "OR LOWER(s.senderOrganization) LIKE :searchTerm "
                + "ORDER BY s.receptionTimestamp DESC";
        return session.createQuery(hql, Shipment.class).setParameter("searchTerm", "%" + searchTerm.toLowerCase() + "%")
                .setMaxResults(limit).getResultList();
    }
}
