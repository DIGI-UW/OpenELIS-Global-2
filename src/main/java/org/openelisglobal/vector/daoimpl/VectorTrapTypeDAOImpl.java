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
    public List<VectorTrapType> getBySampleTypeId(String sampleTypeId) throws LIMSRuntimeException {
        try {
            TypedQuery<VectorTrapType> query = entityManager.createQuery(
                    "select distinct t from VectorTrapType t join t.sampleTypeIds s where s = :sampleTypeId and t.active = true order by t.name",
                    VectorTrapType.class);
            query.setParameter("sampleTypeId", Long.valueOf(sampleTypeId));
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in VectorTrapTypeDAOImpl.getBySampleTypeId()", e);
        }
    }
}
