package org.openelisglobal.shipment.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.shipment.valueholder.BoxSample;
import org.openelisglobal.shipment.valueholder.ReceptionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class BoxSampleDAOImpl extends BaseDAOImpl<BoxSample, Integer> implements BoxSampleDAO {

    private static final Logger logger = LoggerFactory.getLogger(BoxSampleDAOImpl.class);

    public BoxSampleDAOImpl() {
        super(BoxSample.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BoxSample> findByShippingBoxId(Integer shippingBoxId) {
        try {
            String hql = "FROM BoxSample bs WHERE bs.shippingBox.id = :shippingBoxId ORDER BY bs.positionInBox";
            Query<BoxSample> query = entityManager.unwrap(Session.class).createQuery(hql, BoxSample.class);
            query.setParameter("shippingBoxId", shippingBoxId);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding BoxSamples by shipping box ID", e);
            throw new LIMSRuntimeException("Error finding BoxSamples by shipping box ID", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BoxSample findBySampleId(Integer sampleId) {
        try {
            String hql = "FROM BoxSample bs WHERE bs.sample.id = :sampleId";
            Query<BoxSample> query = entityManager.unwrap(Session.class).createQuery(hql, BoxSample.class);
            query.setParameter("sampleId", sampleId);
            query.setMaxResults(1);
            List<BoxSample> results = query.list();
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            logger.error("Error finding BoxSample by sample ID", e);
            throw new LIMSRuntimeException("Error finding BoxSample by sample ID", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BoxSample findByShippingBoxIdAndSampleId(Integer shippingBoxId, Integer sampleId) {
        try {
            String hql = "FROM BoxSample bs WHERE bs.shippingBox.id = :shippingBoxId AND bs.sample.id = :sampleId";
            Query<BoxSample> query = entityManager.unwrap(Session.class).createQuery(hql, BoxSample.class);
            query.setParameter("shippingBoxId", shippingBoxId);
            query.setParameter("sampleId", sampleId);
            query.setMaxResults(1);
            List<BoxSample> results = query.list();
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            logger.error("Error finding BoxSample by shipping box ID and sample ID", e);
            throw new LIMSRuntimeException("Error finding BoxSample by shipping box ID and sample ID", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<BoxSample> findByShippingBoxIdAndReceptionStatus(Integer shippingBoxId,
            ReceptionStatus receptionStatus) {
        try {
            String hql = "FROM BoxSample bs WHERE bs.shippingBox.id = :shippingBoxId AND bs.receptionStatus = :receptionStatus";
            Query<BoxSample> query = entityManager.unwrap(Session.class).createQuery(hql, BoxSample.class);
            query.setParameter("shippingBoxId", shippingBoxId);
            query.setParameter("receptionStatus", receptionStatus);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding BoxSamples by shipping box ID and reception status", e);
            throw new LIMSRuntimeException("Error finding BoxSamples by shipping box ID and reception status", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public int countByShippingBoxId(Integer shippingBoxId) {
        try {
            String hql = "SELECT COUNT(*) FROM BoxSample bs WHERE bs.shippingBox.id = :shippingBoxId";
            Query<Long> query = entityManager.unwrap(Session.class).createQuery(hql, Long.class);
            query.setParameter("shippingBoxId", shippingBoxId);
            Long count = query.uniqueResult();
            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            logger.error("Error counting BoxSamples by shipping box ID", e);
            throw new LIMSRuntimeException("Error counting BoxSamples by shipping box ID", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsBySampleId(Integer sampleId) {
        try {
            String hql = "SELECT COUNT(*) FROM BoxSample bs WHERE bs.sample.id = :sampleId";
            Query<Long> query = entityManager.unwrap(Session.class).createQuery(hql, Long.class);
            query.setParameter("sampleId", sampleId);
            Long count = query.uniqueResult();
            return count != null && count > 0;
        } catch (Exception e) {
            logger.error("Error checking if sample exists in box", e);
            throw new LIMSRuntimeException("Error checking if sample exists in box", e);
        }
    }
}
