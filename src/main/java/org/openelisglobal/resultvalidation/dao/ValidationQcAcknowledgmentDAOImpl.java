package org.openelisglobal.resultvalidation.dao;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.util.List;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.resultvalidation.valueholder.ValidationQcAcknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class ValidationQcAcknowledgmentDAOImpl extends BaseDAOImpl<ValidationQcAcknowledgment, Integer>
        implements ValidationQcAcknowledgmentDAO {

    public ValidationQcAcknowledgmentDAOImpl() {
        super(ValidationQcAcknowledgment.class);
    }

    @Override
    public List<ValidationQcAcknowledgment> findByAnalysisId(Integer analysisId) throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<ValidationQcAcknowledgment> cq = cb.createQuery(ValidationQcAcknowledgment.class);
            Root<ValidationQcAcknowledgment> root = cq.from(ValidationQcAcknowledgment.class);
            cq.where(cb.equal(root.get("analysisId"), analysisId));
            cq.orderBy(cb.desc(root.get("acknowledgedAt")));
            return entityManager.createQuery(cq).getResultList();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving QC acknowledgments by analysis ID", e);
        }
    }
}
