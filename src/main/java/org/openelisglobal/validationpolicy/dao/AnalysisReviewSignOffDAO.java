package org.openelisglobal.validationpolicy.dao;

import java.util.Collection;
import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.validationpolicy.valueholder.AnalysisReviewSignOff;

public interface AnalysisReviewSignOffDAO extends BaseDAO<AnalysisReviewSignOff, Long> {

    /** Every signature on the analyses, superseded ones included, oldest first. */
    List<AnalysisReviewSignOff> findByAnalysisIds(Collection<Integer> analysisIds);
}
