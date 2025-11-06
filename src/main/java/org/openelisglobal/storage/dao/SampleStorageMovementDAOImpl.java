package org.openelisglobal.storage.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.storage.valueholder.SampleStorageMovement;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class SampleStorageMovementDAOImpl extends BaseDAOImpl<SampleStorageMovement, Integer>
        implements SampleStorageMovementDAO {

    public SampleStorageMovementDAOImpl() {
        super(SampleStorageMovement.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleStorageMovement> findBySampleId(String sampleId) {
        try {
            // Note: Sample.id is String in entity but stored as numeric in database
            // Pattern used throughout codebase: parse String sampleId to Integer for
            // database queries
            String hql = "FROM SampleStorageMovement WHERE sample.id = :sampleId ORDER BY movementDate DESC";
            Query<SampleStorageMovement> query = entityManager.unwrap(Session.class).createQuery(hql,
                    SampleStorageMovement.class);
            // Parse String to Integer to match database column type (numeric)
            query.setParameter("sampleId", Integer.parseInt(sampleId));
            return query.list();
        } catch (NumberFormatException e) {
            throw new LIMSRuntimeException("Invalid sample ID format (not numeric): " + sampleId, e);
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding SampleStorageMovements by sample ID", e);
        }
    }
}
