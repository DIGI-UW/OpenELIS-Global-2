package org.openelisglobal.microbiology.daoimpl;

import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroReportingRuleHistoryDAO;
import org.openelisglobal.microbiology.valueholder.MicroReportingRuleHistory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroReportingRuleHistoryDAOImpl extends BaseDAOImpl<MicroReportingRuleHistory, String>
        implements MicroReportingRuleHistoryDAO {
    public MicroReportingRuleHistoryDAOImpl() {
        super(MicroReportingRuleHistory.class);
    }
}
