package org.openelisglobal.tb.dao;

import java.util.List;
import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.tb.valueholder.TbEnums.DecontaminationMethod;
import org.openelisglobal.tb.valueholder.TbEnums.ProcessingStatus;
import org.openelisglobal.tb.valueholder.TbSampleProcessing;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Data access implementation for TB sample processing records.
 */
@Component
@Transactional
public class TbSampleProcessingDAOImpl extends BaseDAOImpl<TbSampleProcessing, Integer>
        implements TbSampleProcessingDAO {

    public TbSampleProcessingDAOImpl() {
        super(TbSampleProcessing.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TbSampleProcessing> findBySampleItemId(String sampleItemId) {
        try {
            String hql = "FROM TbSampleProcessing sp " + "LEFT JOIN FETCH sp.sampleItem "
                    + "LEFT JOIN FETCH sp.processedBy " + "WHERE sp.sampleItem.id = :sampleItemId";
            Query<TbSampleProcessing> query = entityManager.unwrap(Session.class).createQuery(hql,
                    TbSampleProcessing.class);
            query.setParameter("sampleItemId", sampleItemId);
            return query.uniqueResultOptional();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding sample processing by sample ID: " + sampleItemId, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TbSampleProcessing> findByProcessingStatus(ProcessingStatus status) {
        try {
            String hql = "FROM TbSampleProcessing sp " + "LEFT JOIN FETCH sp.sampleItem "
                    + "LEFT JOIN FETCH sp.processedBy " + "WHERE sp.processingStatus = :status "
                    + "ORDER BY sp.processingDate DESC";
            Query<TbSampleProcessing> query = entityManager.unwrap(Session.class).createQuery(hql,
                    TbSampleProcessing.class);
            query.setParameter("status", status);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding sample processing by status: " + status, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TbSampleProcessing> findReadyForInoculation() {
        return findByProcessingStatus(ProcessingStatus.READY_FOR_INOCULATION);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TbSampleProcessing> findByDecontaminationMethod(DecontaminationMethod method) {
        try {
            String hql = "FROM TbSampleProcessing sp " + "LEFT JOIN FETCH sp.sampleItem "
                    + "LEFT JOIN FETCH sp.processedBy " + "WHERE sp.decontaminationMethod = :method "
                    + "ORDER BY sp.processingDate DESC";
            Query<TbSampleProcessing> query = entityManager.unwrap(Session.class).createQuery(hql,
                    TbSampleProcessing.class);
            query.setParameter("method", method);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding sample processing by method: " + method, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsBySampleItemId(String sampleItemId) {
        try {
            String hql = "SELECT COUNT(sp) FROM TbSampleProcessing sp WHERE sp.sampleItem.id = :sampleItemId";
            Query<Long> query = entityManager.unwrap(Session.class).createQuery(hql, Long.class);
            query.setParameter("sampleItemId", sampleItemId);
            return query.uniqueResult() > 0;
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error checking sample processing existence: " + sampleItemId, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Long countByProcessingStatus(ProcessingStatus status) {
        try {
            String hql = "SELECT COUNT(sp) FROM TbSampleProcessing sp WHERE sp.processingStatus = :status";
            Query<Long> query = entityManager.unwrap(Session.class).createQuery(hql, Long.class);
            query.setParameter("status", status);
            return query.uniqueResult();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error counting samples by processing status: " + status, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findSampleItemIdsPendingProcessing() {
        try {
            String sql = "SELECT DISTINCT si.id FROM clinlims.sample_item si "
                    + "JOIN clinlims.tb_sample_registration tsr ON tsr.sample_item_id = si.id " + "WHERE NOT EXISTS ("
                    + "  SELECT 1 FROM clinlims.tb_sample_processing sp " + "  WHERE sp.sample_item_id = si.id" + ")";
            @SuppressWarnings("unchecked")
            Query<String> query = (Query<String>) entityManager.unwrap(Session.class).createNativeQuery(sql);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding samples pending processing", e);
        }
    }
}
