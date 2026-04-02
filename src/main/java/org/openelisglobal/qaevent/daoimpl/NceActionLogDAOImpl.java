package org.openelisglobal.qaevent.daoimpl;

import jakarta.persistence.TypedQuery;
import java.util.List;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.qaevent.dao.NceActionLogDAO;
import org.openelisglobal.qaevent.valueholder.NceActionLog;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class NceActionLogDAOImpl extends BaseDAOImpl<NceActionLog, Integer> implements NceActionLogDAO {

    public NceActionLogDAOImpl() {
        super(NceActionLog.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NceActionLog> getNceActionLogByNceId(Integer nceId) throws LIMSRuntimeException {
        try {
            String sql = "from NceActionLog nc where nc.ncEventId = :nceId";
            TypedQuery<NceActionLog> query = entityManager.createQuery(sql, NceActionLog.class);
            query.setParameter("nceId", nceId);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in NceActionLog getNceActionLogByNceId(Integer nceId)", e);
        }
    }
}
