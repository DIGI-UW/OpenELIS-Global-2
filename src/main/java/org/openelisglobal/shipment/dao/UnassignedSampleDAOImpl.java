package org.openelisglobal.shipment.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.shipment.valueholder.UnassignedSample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class UnassignedSampleDAOImpl extends BaseDAOImpl<UnassignedSample, Integer> implements UnassignedSampleDAO {

    private static final Logger logger = LoggerFactory.getLogger(UnassignedSampleDAOImpl.class);

    public UnassignedSampleDAOImpl() {
        super(UnassignedSample.class);
    }

    @Override
    @Transactional(readOnly = true)
    public UnassignedSample findBySampleId(Integer sampleId) {
        try {
            String hql = "FROM UnassignedSample us WHERE us.sample.id = :sampleId";
            Query<UnassignedSample> query = entityManager.unwrap(Session.class).createQuery(hql,
                    UnassignedSample.class);
            query.setParameter("sampleId", sampleId);
            query.setMaxResults(1);
            List<UnassignedSample> results = query.list();
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            logger.error("Error finding UnassignedSample by sample ID", e);
            throw new LIMSRuntimeException("Error finding UnassignedSample by sample ID", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<UnassignedSample> findAllUnassigned() {
        try {
            String hql = "FROM UnassignedSample us WHERE us.assignedDate IS NULL ORDER BY us.createdDate DESC";
            Query<UnassignedSample> query = entityManager.unwrap(Session.class).createQuery(hql,
                    UnassignedSample.class);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding all unassigned samples", e);
            throw new LIMSRuntimeException("Error finding all unassigned samples", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<UnassignedSample> findByDestinationFacilityId(Integer facilityId) {
        try {
            String hql = "FROM UnassignedSample us WHERE us.destinationFacility.id = :facilityId AND us.assignedDate IS NULL ORDER BY us.createdDate DESC";
            Query<UnassignedSample> query = entityManager.unwrap(Session.class).createQuery(hql,
                    UnassignedSample.class);
            query.setParameter("facilityId", facilityId);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding UnassignedSamples by destination facility", e);
            throw new LIMSRuntimeException("Error finding UnassignedSamples by destination facility", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<UnassignedSample> findByPriority(String priority) {
        try {
            String hql = "FROM UnassignedSample us WHERE us.priority = :priority AND us.assignedDate IS NULL ORDER BY us.createdDate DESC";
            Query<UnassignedSample> query = entityManager.unwrap(Session.class).createQuery(hql,
                    UnassignedSample.class);
            query.setParameter("priority", priority);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding UnassignedSamples by priority", e);
            throw new LIMSRuntimeException("Error finding UnassignedSamples by priority", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<UnassignedSample> findByReferralTestId(Integer testId) {
        try {
            String hql = "FROM UnassignedSample us WHERE us.referralTest.id = :testId AND us.assignedDate IS NULL ORDER BY us.createdDate DESC";
            Query<UnassignedSample> query = entityManager.unwrap(Session.class).createQuery(hql,
                    UnassignedSample.class);
            query.setParameter("testId", testId);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding UnassignedSamples by referral test", e);
            throw new LIMSRuntimeException("Error finding UnassignedSamples by referral test", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public int countByDestinationFacilityId(Integer facilityId) {
        try {
            String hql = "SELECT COUNT(*) FROM UnassignedSample us WHERE us.destinationFacility.id = :facilityId AND us.assignedDate IS NULL";
            Query<Long> query = entityManager.unwrap(Session.class).createQuery(hql, Long.class);
            query.setParameter("facilityId", facilityId);
            Long count = query.uniqueResult();
            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            logger.error("Error counting UnassignedSamples by destination facility", e);
            throw new LIMSRuntimeException("Error counting UnassignedSamples by destination facility", e);
        }
    }
}
