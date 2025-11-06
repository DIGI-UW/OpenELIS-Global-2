package org.openelisglobal.storage.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.storage.valueholder.SampleStorageAssignment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class SampleStorageAssignmentDAOImpl extends BaseDAOImpl<SampleStorageAssignment, Integer>
        implements SampleStorageAssignmentDAO {

    private static final Logger logger = LoggerFactory.getLogger(SampleStorageAssignmentDAOImpl.class);

    public SampleStorageAssignmentDAOImpl() {
        super(SampleStorageAssignment.class);
    }

    @Override
    @Transactional(readOnly = true)
    public SampleStorageAssignment findBySampleId(String sampleId) {
        try {
            // Note: Sample.id is String in entity but stored as numeric in database
            // Pattern used throughout codebase: parse String sampleId to Integer for
            // database queries
            // This matches the approach in SampleItemDAOImpl, SampleAdditionalFieldDAOImpl,
            // etc.
            String hql = "SELECT ssa FROM SampleStorageAssignment ssa JOIN ssa.sample s WHERE s.id = :sampleId";
            Query<SampleStorageAssignment> query = entityManager.unwrap(Session.class).createQuery(hql,
                    SampleStorageAssignment.class);
            // Parse String to Integer to match database column type (numeric)
            query.setParameter("sampleId", Integer.parseInt(sampleId));
            List<SampleStorageAssignment> results = query.list();
            return results.isEmpty() ? null : results.get(0);
        } catch (NumberFormatException e) {
            logger.error("Invalid sample ID format (not numeric): " + sampleId, e);
            throw new LIMSRuntimeException("Invalid sample ID format: " + sampleId, e);
        } catch (Exception e) {
            logger.error("Error finding SampleStorageAssignment by sample ID: " + sampleId, e);
            throw new LIMSRuntimeException("Error finding SampleStorageAssignment by sample ID: " + sampleId, e);
        }
    }

    // No override needed - BaseDAOImpl.getAll() uses entity fetch strategies
    // All relationships are EAGER at entity level, so they load automatically
}
