package org.openelisglobal.qc.dao;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.qc.valueholder.SampleItemQcProfile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DAO implementation for SampleItemQcProfile entity.
 *
 * Uses JPA Criteria API (consistent with QCResultDAOImpl).
 */
@Component
@Transactional
public class SampleItemQcProfileDAOImpl extends BaseDAOImpl<SampleItemQcProfile, String>
        implements SampleItemQcProfileDAO {

    public SampleItemQcProfileDAOImpl() {
        super(SampleItemQcProfile.class);
    }

    @Override
    public Optional<SampleItemQcProfile> findBySampleItemId(Integer sampleItemId) throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<SampleItemQcProfile> cq = cb.createQuery(SampleItemQcProfile.class);
            Root<SampleItemQcProfile> root = cq.from(SampleItemQcProfile.class);
            cq.where(cb.equal(root.get("sampleItemId"), sampleItemId));
            List<SampleItemQcProfile> results = entityManager.createQuery(cq).getResultList();
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving QC profile by sample item ID", e);
        }
    }

    @Override
    public List<SampleItemQcProfile> findBySampleItemIds(List<Integer> sampleItemIds) throws LIMSRuntimeException {
        if (sampleItemIds == null || sampleItemIds.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<SampleItemQcProfile> cq = cb.createQuery(SampleItemQcProfile.class);
            Root<SampleItemQcProfile> root = cq.from(SampleItemQcProfile.class);
            cq.where(root.get("sampleItemId").in(sampleItemIds));
            return entityManager.createQuery(cq).getResultList();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving QC profiles by sample item IDs", e);
        }
    }
}
