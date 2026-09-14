package org.openelisglobal.testcatalog.dao;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.util.List;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.testcatalog.valueholder.TestQcTarget;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Criteria API rather than HQL, like the QC DAOs: Hibernate 6 lowercases
 * camelCase property names in HQL and misses the {@code @Column} names.
 */
@Component
@Transactional
public class TestQcTargetDAOImpl extends BaseDAOImpl<TestQcTarget, String> implements TestQcTargetDAO {

    public TestQcTargetDAOImpl() {
        super(TestQcTarget.class);
    }

    @Override
    public List<TestQcTarget> getByTestId(String testId) throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<TestQcTarget> cq = cb.createQuery(TestQcTarget.class);
            Root<TestQcTarget> root = cq.from(TestQcTarget.class);
            cq.where(cb.equal(root.get("testId"), testId));
            cq.orderBy(cb.asc(root.get("controlLevel")), cb.asc(cb.coalesce(root.get("qcControlLotId"), "")),
                    cb.asc(root.get("lastupdated")));
            return entityManager.createQuery(cq).getResultList();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving QC targets for test " + testId, e);
        }
    }
}
