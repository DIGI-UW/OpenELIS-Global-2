package org.openelisglobal.qaevent.criticalcallback.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.qaevent.criticalcallback.dao.CriticalCallbackDAO;
import org.openelisglobal.qaevent.criticalcallback.valueholder.CriticalCallback;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class CriticalCallbackDAOImpl extends BaseDAOImpl<CriticalCallback, String> implements CriticalCallbackDAO {

    public CriticalCallbackDAOImpl() {
        super(CriticalCallback.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CriticalCallback> getByAnalysisId(String analysisId) {
        String hql = "from CriticalCallback c where c.analysisId = :analysisId order by c.loggedAt desc";
        Query<CriticalCallback> query = entityManager.unwrap(Session.class).createQuery(hql, CriticalCallback.class);
        query.setParameter("analysisId", analysisId);
        return query.list();
    }
}
