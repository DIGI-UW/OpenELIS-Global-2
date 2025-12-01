package org.openelisglobal.qc.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.qc.valueholder.WestgardRuleConfig;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DAO implementation for WestgardRuleConfig entity.
 */
@Component
@Transactional
public class WestgardRuleConfigDAOImpl extends BaseDAOImpl<WestgardRuleConfig, String>
        implements WestgardRuleConfigDAO {

    public WestgardRuleConfigDAOImpl() {
        super(WestgardRuleConfig.class);
    }

    @Override
    public List<WestgardRuleConfig> findEnabledByInstrument(Integer instrumentId) throws LIMSRuntimeException {
        String hql = "FROM WestgardRuleConfig WHERE instrumentId = :instrumentId AND enabled = true";
        try {
            Session session = entityManager.unwrap(Session.class);
            Query<WestgardRuleConfig> query = session.createQuery(hql, WestgardRuleConfig.class);
            query.setParameter("instrumentId", instrumentId);
            return query.list();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving enabled Westgard rules by instrument", e);
        }
    }

    @Override
    public List<WestgardRuleConfig> findByTestAndInstrument(Integer testId, Integer instrumentId)
            throws LIMSRuntimeException {
        String hql = "FROM WestgardRuleConfig WHERE testId = :testId AND instrumentId = :instrumentId";
        try {
            Session session = entityManager.unwrap(Session.class);
            Query<WestgardRuleConfig> query = session.createQuery(hql, WestgardRuleConfig.class);
            query.setParameter("testId", testId);
            query.setParameter("instrumentId", instrumentId);
            return query.list();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving Westgard rules by test and instrument", e);
        }
    }

    @Override
    public WestgardRuleConfig findByTestInstrumentAndRule(Integer testId, Integer instrumentId, String ruleCode)
            throws LIMSRuntimeException {
        String hql = "FROM WestgardRuleConfig WHERE testId = :testId AND instrumentId = :instrumentId AND ruleCode = :ruleCode";
        try {
            Session session = entityManager.unwrap(Session.class);
            Query<WestgardRuleConfig> query = session.createQuery(hql, WestgardRuleConfig.class);
            query.setParameter("testId", testId);
            query.setParameter("instrumentId", instrumentId);
            query.setParameter("ruleCode", ruleCode);
            return query.uniqueResult();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving Westgard rule by test, instrument, and rule code", e);
        }
    }

    @Override
    public List<WestgardRuleConfig> findEnabledByTestAndInstrument(Integer testId, Integer instrumentId)
            throws LIMSRuntimeException {
        String hql = "FROM WestgardRuleConfig WHERE testId = :testId AND instrumentId = :instrumentId AND enabled = true";
        try {
            Session session = entityManager.unwrap(Session.class);
            Query<WestgardRuleConfig> query = session.createQuery(hql, WestgardRuleConfig.class);
            query.setParameter("testId", testId);
            query.setParameter("instrumentId", instrumentId);
            return query.list();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving enabled Westgard rules by test and instrument", e);
        }
    }
}
