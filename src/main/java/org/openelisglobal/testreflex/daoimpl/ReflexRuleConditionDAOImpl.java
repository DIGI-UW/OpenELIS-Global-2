package org.openelisglobal.testreflex.daoimpl;

import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.testreflex.action.bean.ReflexRuleCondition;
import org.openelisglobal.testreflex.dao.ReflexRuleConditionDAO;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class ReflexRuleConditionDAOImpl extends BaseDAOImpl<ReflexRuleCondition, Integer>
        implements ReflexRuleConditionDAO {
    public ReflexRuleConditionDAOImpl() {
        super(ReflexRuleCondition.class);
    }
}
