package org.openelisglobal.testreflex.service;

import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.testreflex.action.bean.ReflexRuleCondition;
import org.openelisglobal.testreflex.dao.ReflexRuleConditionDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ReflexRuleConditionServiceImpl extends AuditableBaseObjectServiceImpl<ReflexRuleCondition, Integer>
        implements ReflexRuleConditionService {
    @Autowired
    protected ReflexRuleConditionDAO baseObjectDAO;

    public ReflexRuleConditionServiceImpl() {
        super(ReflexRuleCondition.class);
    }

    @Override
    protected ReflexRuleConditionDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }
}
