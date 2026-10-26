package org.openelisglobal.microbiology.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroReportingRuleHistory;

public interface MicroReportingRuleHistoryDAO extends BaseDAO<MicroReportingRuleHistory, String> {
    List<MicroReportingRuleHistory> getByRuleId(String ruleId);
}
