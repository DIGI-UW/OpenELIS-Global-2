package org.openelisglobal.sampleacceptance.dao;

import jakarta.persistence.TypedQuery;
import java.util.List;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.sampleacceptance.valueholder.SampleAcceptanceRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SampleAcceptanceRecordDAOImpl extends BaseDAOImpl<SampleAcceptanceRecord, Integer>
        implements SampleAcceptanceRecordDAO {

    private static final Logger logger = LoggerFactory.getLogger(SampleAcceptanceRecordDAOImpl.class);

    public SampleAcceptanceRecordDAOImpl() {
        super(SampleAcceptanceRecord.class);
    }

    @Override
    @Transactional(readOnly = true)
    public SampleAcceptanceRecord findLatestBySampleItemId(Integer sampleItemId) {
        if (sampleItemId == null) {
            return null;
        }
        try {
            String jpql = "FROM SampleAcceptanceRecord r WHERE r.sampleItemId = :sampleItemId ORDER BY r.id DESC";
            TypedQuery<SampleAcceptanceRecord> query = entityManager.createQuery(jpql, SampleAcceptanceRecord.class);
            query.setParameter("sampleItemId", sampleItemId);
            query.setMaxResults(1);
            List<SampleAcceptanceRecord> results = query.getResultList();
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            logger.error("Error finding latest SampleAcceptanceRecord by sample item ID: {}", sampleItemId, e);
            throw new LIMSRuntimeException(
                    "Error finding latest SampleAcceptanceRecord by sample item ID: " + sampleItemId, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleAcceptanceRecord> findHistoryBySampleItemId(Integer sampleItemId) {
        if (sampleItemId == null) {
            return List.of();
        }
        try {
            String jpql = "FROM SampleAcceptanceRecord r WHERE r.sampleItemId = :sampleItemId ORDER BY r.id DESC";
            TypedQuery<SampleAcceptanceRecord> query = entityManager.createQuery(jpql, SampleAcceptanceRecord.class);
            query.setParameter("sampleItemId", sampleItemId);
            return query.getResultList();
        } catch (Exception e) {
            logger.error("Error finding SampleAcceptanceRecord history by sample item ID: {}", sampleItemId, e);
            throw new LIMSRuntimeException(
                    "Error finding SampleAcceptanceRecord history by sample item ID: " + sampleItemId, e);
        }
    }
}
