package org.openelisglobal.testalertrule.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.testalertrule.valueholder.TestAlertRule;

public interface TestAlertRuleDAO extends BaseDAO<TestAlertRule, String> {

    /** All alert rules for a test, ordered for display. */
    List<TestAlertRule> getByTestId(String testId);
}
