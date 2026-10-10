package org.openelisglobal.microbiology.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroCaseAnalysisDAO;
import org.openelisglobal.microbiology.valueholder.MicroCaseAnalysis;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroCaseAnalysisDAOImpl extends BaseDAOImpl<MicroCaseAnalysis, String> implements MicroCaseAnalysisDAO {

    public MicroCaseAnalysisDAOImpl() {
        super(MicroCaseAnalysis.class);
    }

    @Override
    public List<org.openelisglobal.analysis.valueholder.Analysis> getAnalyses(String caseId) {
        return entityManager.unwrap(Session.class).createQuery(
                "select a from Analysis a join fetch a.test join fetch a.sampleItem si join fetch si.sample left join fetch si.typeOfSample left join fetch a.testSection where a.id in (select m.analysisId from MicroCaseAnalysis m where m.caseId = :id and m.cancelledAt is null) order by a.id",
                org.openelisglobal.analysis.valueholder.Analysis.class).setParameter("id", caseId).list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroCaseAnalysis> getByCaseId(String caseId) {
        Query<MicroCaseAnalysis> query = entityManager.unwrap(Session.class).createQuery(
                "from MicroCaseAnalysis c where c.caseId = :caseId and c.cancelledAt is null order by c.analysisId",
                MicroCaseAnalysis.class);
        query.setParameter("caseId", caseId);
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public MicroCaseAnalysis getByCaseAndAnalysis(String caseId, String analysisId) {
        Query<MicroCaseAnalysis> query = entityManager.unwrap(Session.class).createQuery(
                "from MicroCaseAnalysis c where c.caseId = :caseId and c.analysisId = :analysisId and c.cancelledAt is null",
                MicroCaseAnalysis.class);
        query.setParameter("caseId", caseId);
        query.setParameter("analysisId", analysisId);
        return query.uniqueResultOptional().orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasOwnership(String analysisId) {
        return entityManager.unwrap(Session.class)
                .createQuery("select count(c.id) from MicroCaseAnalysis c where c.analysisId = :analysisId", Long.class)
                .setParameter("analysisId", analysisId).uniqueResult() > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public MicroCaseAnalysis getActiveByAnalysisId(String analysisId) {
        return entityManager.unwrap(Session.class)
                .createQuery("from MicroCaseAnalysis c where c.analysisId = :analysisId and c.cancelledAt is null",
                        MicroCaseAnalysis.class)
                .setParameter("analysisId", analysisId).uniqueResultOptional().orElse(null);
    }
}
