package org.openelisglobal.testalertrule.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.testalertrule.valueholder.TestAlertRule;
import org.springframework.security.access.prepost.PreAuthorize;

public interface TestAlertRuleService extends BaseObjectService<TestAlertRule, String> {

    /**
     * Read privilege is result-entry level, not test-configure: alert evaluation
     * (TestAlertEvaluationService) loads a test's rules while a technician saves
     * results, so gating this at PRIV_TEST_CONFIGURE would 403 every result save.
     */
    @PreAuthorize("hasAuthority('PRIV_RESULT_ENTER')")
    List<TestAlertRule> getByTestId(String testId);
}
