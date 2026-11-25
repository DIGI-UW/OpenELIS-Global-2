package org.openelisglobal.analyzer.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingTest;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingTestPK;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class AnalyzerMappingTestDAOImpl extends BaseDAOImpl<AnalyzerMappingTest, AnalyzerMappingTestPK>
        implements AnalyzerMappingTestDAO {

    public AnalyzerMappingTestDAOImpl() {
        super(AnalyzerMappingTest.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerMappingTest> findByMappingId(String mappingId) {
        String hql = "FROM AnalyzerMappingTest t WHERE t.mapping.id = :mappingId " + "ORDER BY t.id.sourceRowKey";
        Query<AnalyzerMappingTest> query = entityManager.unwrap(Session.class).createQuery(hql,
                AnalyzerMappingTest.class);
        query.setParameter("mappingId", mappingId);
        return query.getResultList();
    }
}
