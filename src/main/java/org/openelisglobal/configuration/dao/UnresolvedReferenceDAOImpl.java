package org.openelisglobal.configuration.dao;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.util.List;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.configuration.valueholder.UnresolvedReference;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class UnresolvedReferenceDAOImpl extends BaseDAOImpl<UnresolvedReference, String>
        implements UnresolvedReferenceDAO {

    public UnresolvedReferenceDAOImpl() {
        super(UnresolvedReference.class);
    }

    @Override
    public List<UnresolvedReference> getByStatus(String status) throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<UnresolvedReference> cq = cb.createQuery(UnresolvedReference.class);
            Root<UnresolvedReference> root = cq.from(UnresolvedReference.class);
            cq.where(cb.equal(root.get("status"), status));
            cq.orderBy(cb.asc(root.get("referenceType")), cb.asc(root.get("referenceValue")));
            return entityManager.createQuery(cq).getResultList();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving unresolved references with status " + status, e);
        }
    }

    @Override
    public UnresolvedReference getOpen(String referenceType, String referenceValue) throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<UnresolvedReference> cq = cb.createQuery(UnresolvedReference.class);
            Root<UnresolvedReference> root = cq.from(UnresolvedReference.class);
            cq.where(cb.equal(root.get("status"), UnresolvedReference.STATUS_OPEN),
                    cb.equal(root.get("referenceType"), referenceType),
                    cb.equal(root.get("referenceValue"), referenceValue));
            List<UnresolvedReference> matches = entityManager.createQuery(cq).setMaxResults(1).getResultList();
            return matches.isEmpty() ? null : matches.get(0);
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException(
                    "Error looking up the open reference " + referenceType + " '" + referenceValue + "'", e);
        }
    }
}
