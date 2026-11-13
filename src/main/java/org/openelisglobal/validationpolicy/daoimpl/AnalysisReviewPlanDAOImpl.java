package org.openelisglobal.validationpolicy.daoimpl;

import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.validationpolicy.dao.AnalysisReviewPlanDAO;
import org.openelisglobal.validationpolicy.valueholder.AnalysisReviewPlan;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class AnalysisReviewPlanDAOImpl extends BaseDAOImpl<AnalysisReviewPlan, Integer>
        implements AnalysisReviewPlanDAO {

    public AnalysisReviewPlanDAOImpl() {
        super(AnalysisReviewPlan.class);
    }
}
