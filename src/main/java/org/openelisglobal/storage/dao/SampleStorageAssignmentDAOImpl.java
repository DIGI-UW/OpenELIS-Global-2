package org.openelisglobal.storage.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.storage.valueholder.SampleStorageAssignment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class SampleStorageAssignmentDAOImpl extends BaseDAOImpl<SampleStorageAssignment, String>
        implements SampleStorageAssignmentDAO {

    public SampleStorageAssignmentDAOImpl() {
        super(SampleStorageAssignment.class);
    }

    @Override
    @Transactional(readOnly = true)
    public SampleStorageAssignment findBySampleId(String sampleId) {
        try {
            String hql = "FROM SampleStorageAssignment WHERE sample.id = :sampleId";
            Query<SampleStorageAssignment> query = entityManager.unwrap(Session.class).createQuery(hql,
                    SampleStorageAssignment.class);
            query.setParameter("sampleId", sampleId);
            List<SampleStorageAssignment> results = query.list();
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding SampleStorageAssignment by sample ID", e);
        }
    }
}
