package org.openelisglobal.testreflex.daoimpl;

import jakarta.transaction.Transactional;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.testreflex.action.bean.ReflexRule;
import org.openelisglobal.testreflex.dao.ReflexRuleDAO;
import org.springframework.stereotype.Component;

@Component
@Transactional
public class ReflexRuleDAOImpl extends BaseDAOImpl<ReflexRule, Integer> implements ReflexRuleDAO {

    public ReflexRuleDAOImpl() {
        super(ReflexRule.class);
    }

    @Override
    public ReflexRule getReflexRuleByAnalyteId(String analyteId) throws LIMSRuntimeException {
        try {
            String sql = "from ReflexRule r WHERE r.analyteId = :analyteId";
            Query<ReflexRule> query = entityManager.unwrap(Session.class).createQuery(sql, ReflexRule.class);
            query.setParameter("analyteId", Integer.parseInt(analyteId));
            List<ReflexRule> results = query.list();
            if (results.size() > 0) {
                return results.get(0);
            }
        } catch (RuntimeException e) {
            handleException(e, "getReflexRuleByAnalyteId()");
        }
        return null;
    }
}
