package org.openelisglobal.validationpolicy.daoimpl;

import java.util.Collection;
import java.util.List;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.validationpolicy.dao.AnalysisAutoReleaseDAO;
import org.openelisglobal.validationpolicy.valueholder.AnalysisAutoRelease;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class AnalysisAutoReleaseDAOImpl extends BaseDAOImpl<AnalysisAutoRelease, Long>
        implements AnalysisAutoReleaseDAO {

    public AnalysisAutoReleaseDAOImpl() {
        super(AnalysisAutoRelease.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalysisAutoRelease> findByAnalysisIds(Collection<Integer> analysisIds) {
        if (analysisIds == null || analysisIds.isEmpty()) {
            return List.of();
        }
        return entityManager
                .createQuery("FROM AnalysisAutoRelease r WHERE r.analysisId IN (:ids) ORDER BY r.releasedAt, r.id",
                        AnalysisAutoRelease.class)
                .setParameter("ids", analysisIds).getResultList();
    }
}
