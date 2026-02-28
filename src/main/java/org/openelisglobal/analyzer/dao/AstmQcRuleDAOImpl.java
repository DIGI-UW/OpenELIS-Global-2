package org.openelisglobal.analyzer.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.analyzer.valueholder.AstmQcRule;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class AstmQcRuleDAOImpl extends BaseDAOImpl<AstmQcRule, String> implements AstmQcRuleDAO {

    public AstmQcRuleDAOImpl() {
        super(AstmQcRule.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AstmQcRule> findByAnalyzerId(String analyzerId) {
        try {
            var cb = entityManager.getCriteriaBuilder();
            var cq = cb.createQuery(AstmQcRule.class);
            var root = cq.from(AstmQcRule.class);
            cq.select(root).where(cb.equal(root.get("analyzer").get("id"), analyzerId))
                    .orderBy(cb.asc(root.get("sortOrder")), cb.asc(root.get("id")));
            Query<AstmQcRule> query = entityManager.unwrap(Session.class).createQuery(cq);
            return query.list();
        } catch (Exception e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in AstmQcRuleDAOImpl.findByAnalyzerId", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AstmQcRule> findActiveByAnalyzerId(String analyzerId) {
        try {
            var cb = entityManager.getCriteriaBuilder();
            var cq = cb.createQuery(AstmQcRule.class);
            var root = cq.from(AstmQcRule.class);
            cq.select(root)
                    .where(cb.and(cb.equal(root.get("analyzer").get("id"), analyzerId),
                            cb.isTrue(root.get("isActive"))))
                    .orderBy(cb.asc(root.get("sortOrder")), cb.asc(root.get("id")));
            Query<AstmQcRule> query = entityManager.unwrap(Session.class).createQuery(cq);
            return query.list();
        } catch (Exception e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in AstmQcRuleDAOImpl.findActiveByAnalyzerId", e);
        }
    }
}
