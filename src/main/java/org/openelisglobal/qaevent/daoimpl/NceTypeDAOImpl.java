package org.openelisglobal.qaevent.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.qaevent.dao.NceTypeDAO;
import org.openelisglobal.qaevent.valueholder.NceType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class NceTypeDAOImpl extends BaseDAOImpl<NceType, String> implements NceTypeDAO {

    public NceTypeDAOImpl() {
        super(NceType.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NceType> getAllNceType() throws LIMSRuntimeException {
        List<NceType> list;
        try {
            String sql = "from NceType nt order by nt.id";
            Query<NceType> query = entityManager.unwrap(Session.class).createQuery(sql, NceType.class);
            list = query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in NceCategory getAllNceType()", e);
        }
        return list;
    }
}
