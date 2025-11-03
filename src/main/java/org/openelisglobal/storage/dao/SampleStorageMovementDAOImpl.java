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
public class SampleStorageMovementDAOImpl extends BaseDAOImpl<SampleStorageMovement, String>
        implements SampleStorageMovementDAO {

    public SampleStorageMovementDAOImpl() {
        super(SampleStorageMovement.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleStorageMovement> findBySampleId(String sampleId) {
        try {
            String hql = "FROM SampleStorageMovement WHERE sample.id = :sampleId ORDER BY movementDate DESC";
            Query<SampleStorageMovement> query = entityManager.unwrap(Session.class).createQuery(hql,
                    SampleStorageMovement.class);
            query.setParameter("sampleId", sampleId);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding SampleStorageMovements by sample ID", e);
        }
    }
}
