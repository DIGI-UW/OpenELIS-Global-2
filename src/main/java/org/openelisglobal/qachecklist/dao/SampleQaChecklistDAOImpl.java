package org.openelisglobal.qachecklist.dao;

import jakarta.persistence.TypedQuery;
import java.util.List;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.qachecklist.valueholder.SampleQaChecklist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SampleQaChecklistDAOImpl extends BaseDAOImpl<SampleQaChecklist, Integer> implements SampleQaChecklistDAO {

    private static final Logger logger = LoggerFactory.getLogger(SampleQaChecklistDAOImpl.class);

    public SampleQaChecklistDAOImpl() {
        super(SampleQaChecklist.class);
    }

    @Override
    @Transactional(readOnly = true)
    public SampleQaChecklist findBySampleId(Integer sampleId) {
        if (sampleId == null) {
            return null;
        }

        try {
            String jpql = "FROM SampleQaChecklist sqc WHERE sqc.sampleId = :sampleId";
            TypedQuery<SampleQaChecklist> query = entityManager.createQuery(jpql, SampleQaChecklist.class);
            query.setParameter("sampleId", sampleId);
            query.setMaxResults(1);

            List<SampleQaChecklist> results = query.getResultList();
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            logger.error("Error finding SampleQaChecklist by sample ID: {}", sampleId, e);
            throw new LIMSRuntimeException("Error finding SampleQaChecklist by sample ID: " + sampleId, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public SampleQaChecklist findBySampleId(String sampleId) {
        if (sampleId == null || sampleId.trim().isEmpty()) {
            return null;
        }

        try {
            Integer sampleIdInt = Integer.parseInt(sampleId.trim());
            return findBySampleId(sampleIdInt);
        } catch (NumberFormatException e) {
            logger.warn("Invalid sample ID format (must be numeric): {}", sampleId);
            return null;
        }
    }
}
