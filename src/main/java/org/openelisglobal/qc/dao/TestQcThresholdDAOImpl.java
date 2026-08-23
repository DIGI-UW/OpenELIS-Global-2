package org.openelisglobal.qc.dao;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.qc.valueholder.TestQcThreshold;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class TestQcThresholdDAOImpl extends BaseDAOImpl<TestQcThreshold, String> implements TestQcThresholdDAO {

    public TestQcThresholdDAOImpl() {
        super(TestQcThreshold.class);
    }

    @Override
    public Optional<TestQcThreshold> findByTestId(Integer testId) throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<TestQcThreshold> cq = cb.createQuery(TestQcThreshold.class);
            Root<TestQcThreshold> root = cq.from(TestQcThreshold.class);
            cq.where(cb.equal(root.get("testId"), testId));
            List<TestQcThreshold> results = entityManager.createQuery(cq).getResultList();
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving QC threshold by test ID", e);
        }
    }

    @Override
    public Set<Integer> findAllConfiguredTestIds() throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<Integer> cq = cb.createQuery(Integer.class);
            Root<TestQcThreshold> root = cq.from(TestQcThreshold.class);
            cq.select(root.get("testId"));
            return new HashSet<>(entityManager.createQuery(cq).getResultList());
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving configured QC threshold test IDs", e);
        }
    }
}
