package org.openelisglobal.validationpolicy.dao;

import java.util.Collection;
import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.validationpolicy.valueholder.AnalysisAutoRelease;

public interface AnalysisAutoReleaseDAO extends BaseDAO<AnalysisAutoRelease, Long> {

    /** Every automatic release of the analyses, oldest first. */
    List<AnalysisAutoRelease> findByAnalysisIds(Collection<Integer> analysisIds);
}
