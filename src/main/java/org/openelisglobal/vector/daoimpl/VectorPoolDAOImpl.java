package org.openelisglobal.vector.daoimpl;

import jakarta.persistence.TypedQuery;
import java.util.Collections;
import java.util.List;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.vector.dao.VectorPoolDAO;
import org.openelisglobal.vector.valueholder.VectorPool;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class VectorPoolDAOImpl extends BaseDAOImpl<VectorPool, Integer> implements VectorPoolDAO {

    public VectorPoolDAOImpl() {
        super(VectorPool.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VectorPool> getBySampleId(String sampleId) {
        if (sampleId == null || sampleId.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            TypedQuery<VectorPool> query = entityManager.createQuery(
                    "select p from VectorPool p where p.sampleId = :sampleId order by p.id", VectorPool.class);
            query.setParameter("sampleId", sampleId);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<VectorPool> getByParentPoolId(Integer parentPoolId) {
        if (parentPoolId == null) {
            return Collections.emptyList();
        }
        try {
            TypedQuery<VectorPool> query = entityManager.createQuery(
                    "select p from VectorPool p where p.parentPool.id = :parentId order by p.id", VectorPool.class);
            query.setParameter("parentId", parentPoolId);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw e;
        }
    }
}
