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
        String hql = "FROM AnalyzerSiteBindingTest t WHERE t.siteBindingRevision.id = :revisionId "
                + "ORDER BY t.id.sourceRowKey";
        Query<AnalyzerSiteBindingTest> query = entityManager.unwrap(Session.class).createQuery(hql,
                AnalyzerSiteBindingTest.class);
        query.setParameter("revisionId", revisionId);
        return query.getResultList();
    }
}
