package org.openelisglobal.qaevent.criticalcallback.daoimpl;

import java.util.Collection;
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
    public List<String> getLoggedResultIds(Collection<String> resultIds) {
        if (resultIds == null || resultIds.isEmpty()) {
            return List.of();
        }
        String hql = "select distinct c.resultId from CriticalCallback c where c.resultId in (:resultIds)";
        Query<String> query = entityManager.unwrap(Session.class).createQuery(hql, String.class);
        query.setParameterList("resultIds", resultIds);
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CriticalCallback> getCallbacksForAnalyses(Collection<String> analysisIds) {
        if (analysisIds == null || analysisIds.isEmpty()) {
            return List.of();
        }
        String hql = "from CriticalCallback c where c.analysisId in (:analysisIds) order by c.loggedAt desc";
        Query<CriticalCallback> query = entityManager.unwrap(Session.class).createQuery(hql, CriticalCallback.class);
        query.setParameterList("analysisIds", analysisIds);
        return query.list();
    }
}
