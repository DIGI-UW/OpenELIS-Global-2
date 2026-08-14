package org.openelisglobal.analyzer.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingTest;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingTestPK;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class AnalyzerSiteBindingTestDAOImpl extends BaseDAOImpl<AnalyzerSiteBindingTest, AnalyzerSiteBindingTestPK>
        implements AnalyzerSiteBindingTestDAO {

    public AnalyzerSiteBindingTestDAOImpl() {
        super(AnalyzerSiteBindingTest.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerSiteBindingTest> findByRevisionId(String revisionId) {
        if (revisionId == null || revisionId.isBlank()) {
            return List.of();
        }
        Query<AnalyzerSiteBindingTest> query = entityManager.unwrap(Session.class).createQuery(
                "SELECT row FROM AnalyzerSiteBindingTest row JOIN FETCH row.siteBindingRevision revision"
                        + " WHERE revision.id = :revisionId ORDER BY row.id.sourceRowKey",
                AnalyzerSiteBindingTest.class);
        query.setParameter("revisionId", revisionId.trim());
        return query.getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerSiteBindingTest> findByRevisionIds(List<String> revisionIds) {
        return List.of();
    }
}
