package org.openelisglobal.qaevent.daoimpl;

import jakarta.persistence.TypedQuery;
import java.util.List;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.qaevent.dao.NceTypeDAO;
import org.openelisglobal.qaevent.valueholder.NceType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class NceTypeDAOImpl extends BaseDAOImpl<NceType, Integer> implements NceTypeDAO {

    public NceTypeDAOImpl() {
        super(NceType.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NceType> getAllNceType() throws LIMSRuntimeException {
        try {
            String sql = "from NceType nt order by nt.id";
            TypedQuery<NceType> query = entityManager.createQuery(sql, NceType.class);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in NceType getAllNceType()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<NceType> getNceTypesByCategoryId(Integer categoryId) throws LIMSRuntimeException {
        try {
            String sql = "from NceType nt where nt.categoryId = :categoryId order by nt.id";
            TypedQuery<NceType> query = entityManager.createQuery(sql, NceType.class);
            query.setParameter("categoryId", categoryId);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in NceType getNceTypesByCategoryId()", e);
        }
    }
}
