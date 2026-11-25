package org.openelisglobal.testcatalog.dao;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.util.Collection;
import java.util.List;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.testcatalog.valueholder.TestQcPolicy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class TestQcPolicyDAOImpl extends BaseDAOImpl<TestQcPolicy, Long> implements TestQcPolicyDAO {

    public TestQcPolicyDAOImpl() {
        super(TestQcPolicy.class);
    }

    @Override
    public List<TestQcPolicy> getByTestIds(Collection<Long> testIds) throws LIMSRuntimeException {
        if (testIds == null || testIds.isEmpty()) {
            return List.of();
        }
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<TestQcPolicy> cq = cb.createQuery(TestQcPolicy.class);
            Root<TestQcPolicy> root = cq.from(TestQcPolicy.class);
            cq.where(root.get("id").in(testIds));
            return entityManager.createQuery(cq).getResultList();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving QC policies", e);
        }
    }
}
