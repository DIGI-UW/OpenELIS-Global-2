package org.openelisglobal.shipment.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.shipment.valueholder.Shipment;
import org.openelisglobal.shipment.valueholder.ShipmentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class ShipmentDAOImpl extends BaseDAOImpl<Shipment, Integer> implements ShipmentDAO {

    private static final Logger logger = LoggerFactory.getLogger(ShipmentDAOImpl.class);

    public ShipmentDAOImpl() {
        super(Shipment.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Shipment findByShippingBoxId(Integer shippingBoxId) {
        try {
            String hql = "FROM Shipment s WHERE s.shippingBox.id = :shippingBoxId";
            Query<Shipment> query = entityManager.unwrap(Session.class).createQuery(hql, Shipment.class);
            query.setParameter("shippingBoxId", shippingBoxId);
            query.setMaxResults(1);
            List<Shipment> results = query.list();
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            logger.error("Error finding Shipment by shipping box ID", e);
            throw new LIMSRuntimeException("Error finding Shipment by shipping box ID", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Shipment findByTrackingNumber(String trackingNumber) {
        try {
            String hql = "FROM Shipment s WHERE s.trackingNumber = :trackingNumber";
            Query<Shipment> query = entityManager.unwrap(Session.class).createQuery(hql, Shipment.class);
            query.setParameter("trackingNumber", trackingNumber);
            query.setMaxResults(1);
            List<Shipment> results = query.list();
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            logger.error("Error finding Shipment by tracking number", e);
            throw new LIMSRuntimeException("Error finding Shipment by tracking number", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Shipment> findByStatus(ShipmentStatus status) {
        try {
            String hql = "FROM Shipment s WHERE s.status = :status";
            Query<Shipment> query = entityManager.unwrap(Session.class).createQuery(hql, Shipment.class);
            query.setParameter("status", status);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding Shipments by status", e);
            throw new LIMSRuntimeException("Error finding Shipments by status", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Shipment> findByCourier(String courier) {
        try {
            String hql = "FROM Shipment s WHERE s.courier = :courier";
            Query<Shipment> query = entityManager.unwrap(Session.class).createQuery(hql, Shipment.class);
            query.setParameter("courier", courier);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding Shipments by courier", e);
            throw new LIMSRuntimeException("Error finding Shipments by courier", e);
        }
    }
}
