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
    public List<SampleStorageMovement> findBySampleItemId(String sampleItemId) {
        try {
            // Note: SampleItem.id uses LIMSStringNumberUserType (String in Java, numeric in
            // DB)
            // When querying through relationships, we must parse String to Integer for the
            // parameter
            // This matches the pattern in SampleItemDAOImpl.getSampleItemsBySampleId()
            String hql = "FROM SampleStorageMovement ssm WHERE ssm.sampleItem.id = :sampleItemId ORDER BY ssm.movementDate DESC";
            Query<SampleStorageMovement> query = entityManager.unwrap(Session.class).createQuery(hql,
                    SampleStorageMovement.class);
            query.setParameter("sampleItemId", Integer.parseInt(sampleItemId));
            return query.list();
        } catch (NumberFormatException e) {
            throw new LIMSRuntimeException("Invalid SampleItem ID format (must be numeric): " + sampleItemId, e);
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding SampleStorageMovements by SampleItem ID: " + sampleItemId, e);
        }
    }
}
