package org.openelisglobal.analyzer.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.analyzer.valueholder.AnalyzerActivationCandidate;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class AnalyzerActivationCandidateDAOImpl extends BaseDAOImpl<AnalyzerActivationCandidate, String>
        implements AnalyzerActivationCandidateDAO {

    public AnalyzerActivationCandidateDAOImpl() {
        super(AnalyzerActivationCandidate.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerActivationCandidate> findByAnalyzerId(String analyzerId) {
        if (analyzerId == null || analyzerId.trim().isEmpty()) {
            return List.of();
        }
        String hql = "SELECT candidate FROM AnalyzerActivationCandidate candidate "
                + "JOIN FETCH candidate.analyzer analyzer " + "JOIN FETCH candidate.siteBindingRevision revision "
                + "JOIN FETCH candidate.verificationConfirmation confirmation "
                + "WHERE analyzer.id = :analyzerId ORDER BY candidate.id";
        Query<AnalyzerActivationCandidate> query = entityManager.unwrap(Session.class).createQuery(hql,
                AnalyzerActivationCandidate.class);
        query.setParameter("analyzerId", analyzerId.trim());
        return query.getResultList();
    }
}
