package org.openelisglobal.analyzer.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingResult;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingResultPK;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class AnalyzerSiteBindingResultDAOImpl extends
        BaseDAOImpl<AnalyzerSiteBindingResult, AnalyzerSiteBindingResultPK> implements AnalyzerSiteBindingResultDAO {

    public AnalyzerSiteBindingResultDAOImpl() {
        super(AnalyzerSiteBindingResult.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerSiteBindingResult> findByRevisionId(String revisionId) {
        String hql = "FROM AnalyzerSiteBindingResult r WHERE r.siteBindingRevision.id = :revisionId "
                + "ORDER BY r.id.sourceRowKey, r.id.rawValue";
        Query<AnalyzerSiteBindingResult> query = entityManager.unwrap(Session.class).createQuery(hql,
                AnalyzerSiteBindingResult.class);
        query.setParameter("revisionId", revisionId);
        return query.getResultList();
    }
}
