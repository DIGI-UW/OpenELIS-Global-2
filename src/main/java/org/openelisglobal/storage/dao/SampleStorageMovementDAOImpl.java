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
public class SampleStorageMovementDAOImpl extends BaseDAOImpl<SampleStorageMovement, Integer>
        implements SampleStorageMovementDAO {

    public SampleStorageMovementDAOImpl() {
        super(SampleStorageMovement.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleStorageMovement> findBySampleItemId(String sampleItemId) {
        if (sampleItemId == null || sampleItemId.trim().isEmpty()) {
            return new java.util.ArrayList<>();
        }

        // Parse String to Integer since DB column is numeric
        Integer sampleItemIdInt;
        try {
            sampleItemIdInt = Integer.parseInt(sampleItemId.trim());
        } catch (NumberFormatException e) {
            throw new LIMSRuntimeException("Invalid SampleItem ID format (must be numeric): " + sampleItemId, e);
        }

        try {
            // Query directly using sampleItemId column (no join to HBM-mapped entity)
            String hql = "FROM SampleStorageMovement ssm WHERE ssm.sampleItemId = :sampleItemId ORDER BY ssm.movementDate DESC";
            Query<SampleStorageMovement> query = entityManager.unwrap(Session.class).createQuery(hql,
                    SampleStorageMovement.class);
            query.setParameter("sampleItemId", sampleItemIdInt);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding SampleStorageMovements by SampleItem ID: " + sampleItemId, e);
        }
    }
}
