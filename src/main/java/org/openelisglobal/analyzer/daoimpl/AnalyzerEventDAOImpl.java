package org.openelisglobal.analyzer.daoimpl;

import java.util.List;
import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.analyzer.dao.AnalyzerEventDAO;
import org.openelisglobal.analyzer.valueholder.AnalyzerEvent;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class AnalyzerEventDAOImpl extends BaseDAOImpl<AnalyzerEvent, String> implements AnalyzerEventDAO {

    public AnalyzerEventDAOImpl() {
        super(AnalyzerEvent.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AnalyzerEvent> getByExternalEventId(String externalEventId) {
        Query<AnalyzerEvent> query = entityManager.unwrap(Session.class)
                .createQuery("from AnalyzerEvent e where e.externalEventId = :externalEventId", AnalyzerEvent.class);
        query.setParameter("externalEventId", externalEventId);
        return query.uniqueResultOptional();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerEvent> getFailed(int limit) {
        Query<AnalyzerEvent> query = entityManager.unwrap(Session.class).createQuery(
                "from AnalyzerEvent e where e.status = 'FAILED' order by e.receivedAt desc", AnalyzerEvent.class);
        query.setMaxResults(limit);
        return query.list();
    }
}
