package org.openelisglobal.validationpolicy.daoimpl;

import java.util.Collection;
import java.util.List;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.validationpolicy.dao.AnalysisReviewSignOffDAO;
import org.openelisglobal.validationpolicy.valueholder.AnalysisReviewSignOff;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class AnalysisReviewSignOffDAOImpl extends BaseDAOImpl<AnalysisReviewSignOff, Long>
        implements AnalysisReviewSignOffDAO {

    public AnalysisReviewSignOffDAOImpl() {
        super(AnalysisReviewSignOff.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalysisReviewSignOff> findByAnalysisIds(Collection<Integer> analysisIds) {
        if (analysisIds == null || analysisIds.isEmpty()) {
            return List.of();
        }
        return entityManager
                .createQuery("FROM AnalysisReviewSignOff s WHERE s.analysisId IN (:ids) ORDER BY s.signedAt, s.id",
                        AnalysisReviewSignOff.class)
                .setParameter("ids", analysisIds).getResultList();
    }
}
