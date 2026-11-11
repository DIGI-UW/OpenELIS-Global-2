package org.openelisglobal.qc.dao;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.List;
import java.util.stream.Collectors;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.qc.dto.TestInstrumentPair;
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
    public List<WestgardRuleConfig> findEnabledByInstrument(String instrumentId) throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<WestgardRuleConfig> cq = cb.createQuery(WestgardRuleConfig.class);
            Root<WestgardRuleConfig> root = cq.from(WestgardRuleConfig.class);
            cq.where(cb.equal(root.get("instrumentId"), instrumentId), cb.equal(root.get("enabled"), true));
            return entityManager.createQuery(cq).getResultList();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving enabled Westgard rules by instrument", e);
        }
    }

    @Override
    public List<WestgardRuleConfig> findByTestAndInstrument(String testId, String instrumentId)
            throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<WestgardRuleConfig> cq = cb.createQuery(WestgardRuleConfig.class);
            Root<WestgardRuleConfig> root = cq.from(WestgardRuleConfig.class);
            cq.where(cb.equal(root.get("testId"), testId), cb.equal(root.get("instrumentId"), instrumentId));
            return entityManager.createQuery(cq).getResultList();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving Westgard rules by test and instrument", e);
        }
    }

    @Override
    public WestgardRuleConfig findByTestInstrumentAndRule(String testId, String instrumentId, String ruleCode)
            throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<WestgardRuleConfig> cq = cb.createQuery(WestgardRuleConfig.class);
            Root<WestgardRuleConfig> root = cq.from(WestgardRuleConfig.class);
            Predicate[] predicates = { cb.equal(root.get("testId"), testId),
                    cb.equal(root.get("instrumentId"), instrumentId), cb.equal(root.get("ruleCode"), ruleCode) };
            cq.where(predicates);
            List<WestgardRuleConfig> results = entityManager.createQuery(cq).getResultList();
            return results.isEmpty() ? null : results.get(0);
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving Westgard rule by test, instrument, and rule code", e);
        }
    }

    @Override
    public List<WestgardRuleConfig> findEnabledByTestAndInstrument(String testId, String instrumentId)
            throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<WestgardRuleConfig> cq = cb.createQuery(WestgardRuleConfig.class);
            Root<WestgardRuleConfig> root = cq.from(WestgardRuleConfig.class);
            cq.where(cb.equal(root.get("testId"), testId), cb.equal(root.get("instrumentId"), instrumentId),
                    cb.equal(root.get("enabled"), true));
            return entityManager.createQuery(cq).getResultList();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving enabled Westgard rules by test and instrument", e);
        }
    }

    @Override
    public List<TestInstrumentPair> findDistinctTestInstrumentPairs() throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class);
            Root<WestgardRuleConfig> root = cq.from(WestgardRuleConfig.class);
            cq.multiselect(root.get("testId"), root.get("instrumentId")).distinct(true);
            return entityManager.createQuery(cq).getResultList().stream()
                    .map(row -> new TestInstrumentPair((String) row[0], (String) row[1])).collect(Collectors.toList());
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving distinct test-instrument pairs from rule configs", e);
        }
    }
}
