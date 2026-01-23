package org.openelisglobal.qaevent.daoimpl;

import java.util.ArrayList;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.qaevent.dao.NceActionLogDAO;
import org.openelisglobal.qaevent.valueholder.NceActionLog;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class NceActionLogDAOImpl extends BaseDAOImpl<NceActionLog, String> implements NceActionLogDAO {

    public NceActionLogDAOImpl() {
        super(NceActionLog.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NceActionLog> getNceActionLogByNceId(String nceId) throws LIMSRuntimeException {
        List<NceActionLog> list = new ArrayList<>();
        try {
            String sqlString = "from NceActionLog nc where nc.ncEventId = :param";

            Query<NceActionLog> query = entityManager.unwrap(Session.class).createQuery(sqlString, NceActionLog.class);
            query.setParameter("param", Integer.parseInt(nceId));

            list = query.list();
            return list;
        } catch (RuntimeException exception) {
            LogEvent.logError(exception);
            throw new LIMSRuntimeException("Error in NceActionLog getNceActionLogByNceId(String nceId)", exception);
        }
    }
}
