package org.openelisglobal.testreflex.service;

import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.testreflex.action.bean.ReflexRule;
import org.openelisglobal.testreflex.dao.ReflexRuleDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ReflexRuleServiceImpl extends AuditableBaseObjectServiceImpl<ReflexRule, Integer>
        implements ReflexRuleService {
    @Autowired
    protected ReflexRuleDAO baseObjectDAO;

    public ReflexRuleServiceImpl() {
        super(ReflexRule.class);
    }

    @Override
    protected BaseDAO<ReflexRule, Integer> getBaseObjectDAO() {
        return baseObjectDAO;
    }
}
