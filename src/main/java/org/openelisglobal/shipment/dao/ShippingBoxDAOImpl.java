package org.openelisglobal.shipment.dao;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.shipment.valueholder.BoxState;
import org.openelisglobal.shipment.valueholder.ShippingBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class ShippingBoxDAOImpl extends BaseDAOImpl<ShippingBox, Integer> implements ShippingBoxDAO {

    private static final Logger logger = LoggerFactory.getLogger(ShippingBoxDAOImpl.class);

    public ShippingBoxDAOImpl() {
        super(ShippingBox.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShippingBox> getAll() {
        try {
            String hql = "FROM ShippingBox b ORDER BY b.createdDate DESC";
            Query<ShippingBox> query = entityManager.unwrap(Session.class).createQuery(hql, ShippingBox.class);
            return query.list();
        } catch (Exception e) {
            logger.error("Error getting all ShippingBoxes", e);
            throw new LIMSRuntimeException("Error getting all ShippingBoxes", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ShippingBox findByBoxId(String boxId) {
        try {
            String hql = "FROM ShippingBox b WHERE b.boxId = :boxId";
            Query<ShippingBox> query = entityManager.unwrap(Session.class).createQuery(hql, ShippingBox.class);
            query.setParameter("boxId", boxId);
            query.setMaxResults(1);
            List<ShippingBox> results = query.list();
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            logger.error("Error finding ShippingBox by boxId", e);
            throw new LIMSRuntimeException("Error finding ShippingBox by boxId", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ShippingBox findByFhirUuid(UUID fhirUuid) {
        try {
            String hql = "FROM ShippingBox b WHERE b.fhirUuid = :fhirUuid";
            Query<ShippingBox> query = entityManager.unwrap(Session.class).createQuery(hql, ShippingBox.class);
            query.setParameter("fhirUuid", fhirUuid);
            query.setMaxResults(1);
            List<ShippingBox> results = query.list();
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            logger.error("Error finding ShippingBox by fhirUuid", e);
            throw new LIMSRuntimeException("Error finding ShippingBox by fhirUuid", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShippingBox> findByState(BoxState state) {
        try {
            String hql = "FROM ShippingBox b WHERE b.state = :state ORDER BY b.createdDate DESC";
            Query<ShippingBox> query = entityManager.unwrap(Session.class).createQuery(hql, ShippingBox.class);
            query.setParameter("state", state);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding ShippingBoxes by state", e);
            throw new LIMSRuntimeException("Error finding ShippingBoxes by state", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShippingBox> findByDestinationFacilityId(Integer facilityId) {
        try {
            String hql = "FROM ShippingBox b WHERE b.destinationFacility.id = :facilityId ORDER BY b.createdDate DESC";
            Query<ShippingBox> query = entityManager.unwrap(Session.class).createQuery(hql, ShippingBox.class);
            query.setParameter("facilityId", facilityId);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding ShippingBoxes by destination facility", e);
            throw new LIMSRuntimeException("Error finding ShippingBoxes by destination facility", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShippingBox> findByCreatedDateRange(Timestamp startDate, Timestamp endDate) {
        try {
            String hql = "FROM ShippingBox b WHERE b.createdDate BETWEEN :startDate AND :endDate ORDER BY b.createdDate DESC";
            Query<ShippingBox> query = entityManager.unwrap(Session.class).createQuery(hql, ShippingBox.class);
            query.setParameter("startDate", startDate);
            query.setParameter("endDate", endDate);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding ShippingBoxes by created date range", e);
            throw new LIMSRuntimeException("Error finding ShippingBoxes by created date range", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShippingBox> findAllActive() {
        try {
            String hql = "FROM ShippingBox b WHERE b.archived = false ORDER BY b.createdDate DESC";
            Query<ShippingBox> query = entityManager.unwrap(Session.class).createQuery(hql, ShippingBox.class);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding active ShippingBoxes", e);
            throw new LIMSRuntimeException("Error finding active ShippingBoxes", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public int countByState(BoxState state) {
        try {
            String hql = "SELECT COUNT(*) FROM ShippingBox b WHERE b.state = :state";
            Query<Long> query = entityManager.unwrap(Session.class).createQuery(hql, Long.class);
            query.setParameter("state", state);
            Long count = query.uniqueResult();
            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            logger.error("Error counting ShippingBoxes by state", e);
            throw new LIMSRuntimeException("Error counting ShippingBoxes by state", e);
        }
    }
}
