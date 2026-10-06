package org.openelisglobal.analyzer.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingResult;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingResultPK;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class AnalyzerMappingResultDAOImpl extends BaseDAOImpl<AnalyzerMappingResult, AnalyzerMappingResultPK>
        implements AnalyzerMappingResultDAO {

    public AnalyzerMappingResultDAOImpl() {
        super(AnalyzerMappingResult.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerMappingResult> findByRevisionId(String revisionId) {
        String hql = "FROM AnalyzerMappingResult r WHERE r.siteBindingRevision.id = :revisionId "
                + "ORDER BY r.id.sourceRowKey, r.id.rawValue";
        Query<AnalyzerMappingResult> query = entityManager.unwrap(Session.class).createQuery(hql,
                AnalyzerMappingResult.class);
        query.setParameter("revisionId", revisionId);
        return query.getResultList();
    }
}
