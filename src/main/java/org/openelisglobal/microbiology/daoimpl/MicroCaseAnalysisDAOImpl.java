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
    @Transactional(readOnly = true)
    public List<MicroCaseAnalysis> getByCaseId(String caseId) {
        Query<MicroCaseAnalysis> query = entityManager.unwrap(Session.class).createQuery(
                "from MicroCaseAnalysis c where c.caseId = :caseId order by c.analysisId", MicroCaseAnalysis.class);
        query.setParameter("caseId", caseId);
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public MicroCaseAnalysis getByCaseAndAnalysis(String caseId, String analysisId) {
        Query<MicroCaseAnalysis> query = entityManager.unwrap(Session.class).createQuery(
                "from MicroCaseAnalysis c where c.caseId = :caseId and c.analysisId = :analysisId",
                MicroCaseAnalysis.class);
        query.setParameter("caseId", caseId);
        query.setParameter("analysisId", analysisId);
        return query.uniqueResultOptional().orElse(null);
    }
}
