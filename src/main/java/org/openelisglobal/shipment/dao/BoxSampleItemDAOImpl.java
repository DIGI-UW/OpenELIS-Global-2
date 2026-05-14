package org.openelisglobal.shipment.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.shipment.valueholder.BoxSampleItem;
import org.openelisglobal.shipment.valueholder.ReceptionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class BoxSampleItemDAOImpl extends BaseDAOImpl<BoxSampleItem, Integer> implements BoxSampleItemDAO {

    private static final Logger logger = LoggerFactory.getLogger(BoxSampleItemDAOImpl.class);

    public BoxSampleItemDAOImpl() {
        super(BoxSampleItem.class);
    }

    @Override
    public List<BoxSampleItem> findByShippingBoxId(Integer shippingBoxId) {
        try {
            String hql = "FROM BoxSampleItem bsi WHERE bsi.shippingBox.id = :shippingBoxId ORDER BY bsi.positionInBox";
            Query<BoxSampleItem> query = entityManager.unwrap(Session.class).createQuery(hql, BoxSampleItem.class);
            query.setParameter("shippingBoxId", shippingBoxId);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding BoxSampleItems by shipping box ID", e);
            throw new LIMSRuntimeException("Error finding BoxSampleItems by shipping box ID", e);
        }
    }

    @Override
    public BoxSampleItem findBySampleItemId(String sampleItemId) {
        try {
            String hql = "FROM BoxSampleItem bsi WHERE bsi.sampleItem.id = :sampleItemId";
            Query<BoxSampleItem> query = entityManager.unwrap(Session.class).createQuery(hql, BoxSampleItem.class);
            query.setParameter("sampleItemId", sampleItemId);
            query.setMaxResults(1);
            List<BoxSampleItem> results = query.list();
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            logger.error("Error finding BoxSampleItem by sample item ID", e);
            throw new LIMSRuntimeException("Error finding BoxSampleItem by sample item ID", e);
        }
    }

    @Override
    public BoxSampleItem findByShippingBoxIdAndSampleItemId(Integer shippingBoxId, String sampleItemId) {
        try {
            String hql = "FROM BoxSampleItem bsi WHERE bsi.shippingBox.id = :shippingBoxId AND bsi.sampleItem.id = :sampleItemId";
            Query<BoxSampleItem> query = entityManager.unwrap(Session.class).createQuery(hql, BoxSampleItem.class);
            query.setParameter("shippingBoxId", shippingBoxId);
            query.setParameter("sampleItemId", sampleItemId);
            query.setMaxResults(1);
            List<BoxSampleItem> results = query.list();
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            logger.error("Error finding BoxSampleItem by shipping box ID and sample item ID", e);
            throw new LIMSRuntimeException("Error finding BoxSampleItem by shipping box ID and sample item ID", e);
        }
    }

    @Override
    public List<BoxSampleItem> findByShippingBoxIdAndReceptionStatus(Integer shippingBoxId,
            ReceptionStatus receptionStatus) {
        try {
            String hql = "FROM BoxSampleItem bsi WHERE bsi.shippingBox.id = :shippingBoxId AND bsi.receptionStatus = :receptionStatus";
            Query<BoxSampleItem> query = entityManager.unwrap(Session.class).createQuery(hql, BoxSampleItem.class);
            query.setParameter("shippingBoxId", shippingBoxId);
            query.setParameter("receptionStatus", receptionStatus);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding BoxSampleItems by shipping box ID and reception status", e);
            throw new LIMSRuntimeException("Error finding BoxSampleItems by shipping box ID and reception status", e);
        }
    }

    @Override
    public int countByShippingBoxId(Integer shippingBoxId) {
        try {
            String hql = "SELECT COUNT(*) FROM BoxSampleItem bsi WHERE bsi.shippingBox.id = :shippingBoxId";
            Query<Long> query = entityManager.unwrap(Session.class).createQuery(hql, Long.class);
            query.setParameter("shippingBoxId", shippingBoxId);
            Long count = query.uniqueResult();
            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            logger.error("Error counting BoxSampleItems by shipping box ID", e);
            throw new LIMSRuntimeException("Error counting BoxSampleItems by shipping box ID", e);
        }
    }

    @Override
    public boolean existsBySampleItemId(String sampleItemId) {
        try {
            String hql = "SELECT COUNT(*) FROM BoxSampleItem bsi WHERE bsi.sampleItem.id = :sampleItemId";
            Query<Long> query = entityManager.unwrap(Session.class).createQuery(hql, Long.class);
            query.setParameter("sampleItemId", sampleItemId);
            Long count = query.uniqueResult();
            return count != null && count > 0;
        } catch (Exception e) {
            logger.error("Error checking if sample item exists in box", e);
            throw new LIMSRuntimeException("Error checking if sample item exists in box", e);
        }
    }

    @Override
    public List<String> getAllAssignedSampleItemIds() {
        try {
            String hql = "SELECT DISTINCT bsi.sampleItem.id FROM BoxSampleItem bsi";
            Query<String> query = entityManager.unwrap(Session.class).createQuery(hql, String.class);
            return query.list();
        } catch (Exception e) {
            logger.error("Error getting all assigned sample item IDs", e);
            throw new LIMSRuntimeException("Error getting all assigned sample item IDs", e);
        }
    }
}
