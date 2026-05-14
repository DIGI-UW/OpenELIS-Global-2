package org.openelisglobal.analyzer.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.analyzer.valueholder.AnalyzerQcRule;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class AnalyzerQcRuleDAOImpl extends BaseDAOImpl<AnalyzerQcRule, String> implements AnalyzerQcRuleDAO {

    public AnalyzerQcRuleDAOImpl() {
        super(AnalyzerQcRule.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerQcRule> findByAnalyzerId(String analyzerId) {
        if (analyzerId == null || analyzerId.trim().isEmpty()) {
            return List.of();
        }
        String hql = "FROM AnalyzerQcRule r WHERE r.analyzerId = :analyzerId ORDER BY r.displayOrder";
        Query<AnalyzerQcRule> query = entityManager.unwrap(Session.class).createQuery(hql, AnalyzerQcRule.class);
        query.setParameter("analyzerId", analyzerId.trim());
        return query.getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerQcRule> findActiveByAnalyzerId(String analyzerId) {
        if (analyzerId == null || analyzerId.trim().isEmpty()) {
            return List.of();
        }
        String hql = "FROM AnalyzerQcRule r WHERE r.analyzerId = :analyzerId"
                + " AND r.active = true ORDER BY r.displayOrder";
        Query<AnalyzerQcRule> query = entityManager.unwrap(Session.class).createQuery(hql, AnalyzerQcRule.class);
        query.setParameter("analyzerId", analyzerId.trim());
        return query.getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countActiveByAnalyzerId(String analyzerId) {
        if (analyzerId == null || analyzerId.trim().isEmpty()) {
            return 0;
        }
        String hql = "SELECT COUNT(r) FROM AnalyzerQcRule r WHERE r.analyzerId = :analyzerId" + " AND r.active = true";
        Query<Long> query = entityManager.unwrap(Session.class).createQuery(hql, Long.class);
        query.setParameter("analyzerId", analyzerId.trim());
        Long count = query.uniqueResult();
        return count == null ? 0 : count;
    }

    @Override
    public void deleteByAnalyzerIdAndRuleId(String analyzerId, String ruleId) {
        if (analyzerId == null || ruleId == null) {
            return;
        }
        try {
            AnalyzerQcRule rule = entityManager.find(AnalyzerQcRule.class, ruleId);
            if (rule != null && analyzerId.equals(rule.getAnalyzerId())) {
                entityManager.remove(rule);
            }
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error deleting QC rule " + ruleId + " for analyzer " + analyzerId, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByAnalyzerIdAndRule(String analyzerId, String ruleType, String targetField, String operand) {
        if (analyzerId == null || ruleType == null || operand == null) {
            return false;
        }
        String hql = "SELECT COUNT(r) FROM AnalyzerQcRule r"
                + " WHERE r.analyzerId = :analyzerId AND r.ruleType = :ruleType AND r.operand = :operand"
                + (targetField != null ? " AND r.targetField = :targetField" : " AND r.targetField IS NULL");
        Query<Long> query = entityManager.unwrap(Session.class).createQuery(hql, Long.class);
        query.setParameter("analyzerId", analyzerId.trim());
        query.setParameter("ruleType", AnalyzerQcRule.RuleType.valueOf(ruleType));
        query.setParameter("operand", operand);
        if (targetField != null) {
            query.setParameter("targetField", targetField);
        }
        Long count = query.uniqueResult();
        return count != null && count > 0;
    }
}
