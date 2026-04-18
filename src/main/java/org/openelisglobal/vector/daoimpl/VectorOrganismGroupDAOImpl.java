package org.openelisglobal.vector.daoimpl;

import jakarta.persistence.TypedQuery;
import java.util.List;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.vector.dao.VectorOrganismGroupDAO;
import org.openelisglobal.vector.valueholder.VectorOrganismGroup;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class VectorOrganismGroupDAOImpl extends BaseDAOImpl<VectorOrganismGroup, String>
        implements VectorOrganismGroupDAO {

    public VectorOrganismGroupDAOImpl() {
        super(VectorOrganismGroup.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VectorOrganismGroup> getActiveGroups() throws LIMSRuntimeException {
        try {
            TypedQuery<VectorOrganismGroup> query = entityManager.createQuery(
                    "from VectorOrganismGroup where active = true order by label", VectorOrganismGroup.class);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in VectorOrganismGroupDAOImpl.getActiveGroups()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public VectorOrganismGroup getByCode(String code) throws LIMSRuntimeException {
        try {
            TypedQuery<VectorOrganismGroup> query = entityManager.createQuery(
                    "from VectorOrganismGroup where code = :code", VectorOrganismGroup.class);
            query.setParameter("code", code);
            List<VectorOrganismGroup> list = query.getResultList();
            return list.isEmpty() ? null : list.get(0);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in VectorOrganismGroupDAOImpl.getByCode()", e);
        }
    }
}
