package org.openelisglobal.testalertrule.service;

import java.util.List;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.testalertrule.dao.TestAlertRuleDAO;
import org.openelisglobal.testalertrule.valueholder.TestAlertRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TestAlertRuleServiceImpl extends AuditableBaseObjectServiceImpl<TestAlertRule, String>
        implements TestAlertRuleService {

    @Autowired
    protected TestAlertRuleDAO baseObjectDAO;

    TestAlertRuleServiceImpl() {
        super(TestAlertRule.class);
    }

    @Override
    protected TestAlertRuleDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestAlertRule> getByTestId(String testId) {
        return baseObjectDAO.getByTestId(testId);
    }
}
