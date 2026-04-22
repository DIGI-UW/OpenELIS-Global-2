package org.openelisglobal.vector.daoimpl;

import jakarta.persistence.TypedQuery;
import java.util.List;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.vector.dao.VectorTrapTypeDAO;
import org.openelisglobal.vector.valueholder.VectorTrapType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class VectorTrapTypeDAOImpl extends BaseDAOImpl<VectorTrapType, Integer> implements VectorTrapTypeDAO {

    public VectorTrapTypeDAOImpl() {
        super(VectorTrapType.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VectorTrapType> getByGroupId(Integer groupId) throws LIMSRuntimeException {
        try {
            TypedQuery<VectorTrapType> query = entityManager.createQuery(
                    "select t from VectorTrapType t join t.groups g where g.id = :groupId and t.active = true order by t.name",
                    VectorTrapType.class);
            query.setParameter("groupId", groupId);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in VectorTrapTypeDAOImpl.getByGroupId()", e);
        }
    }
}
