package org.openelisglobal.testreflex.dao;

import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.testreflex.action.bean.ReflexRule;

public interface ReflexRuleDAO extends BaseDAO<ReflexRule, Integer> {

    ReflexRule getReflexRuleByAnalyteId(String analyteId) throws LIMSRuntimeException;
}
