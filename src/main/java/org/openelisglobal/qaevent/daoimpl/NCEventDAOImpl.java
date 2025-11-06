package org.openelisglobal.qaevent.daoimpl;

import java.util.ArrayList;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.qaevent.dao.NCEventDAO;
import org.openelisglobal.qaevent.valueholder.NcEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class NCEventDAOImpl extends BaseDAOImpl<NcEvent, String> implements NCEventDAO {

    public NCEventDAOImpl() {
        super(NcEvent.class);
    }

    @Override
    @Transactional
    public List<NcEvent> findByNCENumberOrLabOrderId(String nceNumber, String labOrderId) {
        List<NcEvent> list = new ArrayList();
        try {
            String sql = "from NcEvent where nceNumber = :nceNumber or labOrderNumber = :labOrderNumber";
            Query<NcEvent> query = entityManager.unwrap(Session.class).createQuery(sql, NcEvent.class);
            query.setParameter("nceNumber", nceNumber);
            query.setParameter("labOrderNumber", labOrderId);
            list = query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in NceCategory getAllNceCategory()", e);
        }
        return list;
    }

    @Override
    @Transactional
    public NcEvent getNCEvent(String id) throws LIMSRuntimeException {
        return null;
    }
}
