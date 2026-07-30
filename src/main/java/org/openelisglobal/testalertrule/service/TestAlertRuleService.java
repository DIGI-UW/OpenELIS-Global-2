package org.openelisglobal.testalertrule.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.testalertrule.valueholder.TestAlertRule;

public interface TestAlertRuleService extends BaseObjectService<TestAlertRule, String> {

    List<TestAlertRule> getByTestId(String testId);
}
