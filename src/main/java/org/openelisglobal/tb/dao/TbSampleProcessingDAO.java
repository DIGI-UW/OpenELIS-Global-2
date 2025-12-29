package org.openelisglobal.tb.dao;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.tb.valueholder.TbEnums.DecontaminationMethod;
import org.openelisglobal.tb.valueholder.TbEnums.ProcessingStatus;
import org.openelisglobal.tb.valueholder.TbSampleProcessing;

/**
 * Data access interface for TB sample processing records.
 */
public interface TbSampleProcessingDAO extends BaseDAO<TbSampleProcessing, Integer> {

    /**
     * Find processing record by sample item ID.
     */
    Optional<TbSampleProcessing> findBySampleItemId(String sampleItemId);

    /**
     * Find all samples by processing status.
     */
    List<TbSampleProcessing> findByProcessingStatus(ProcessingStatus status);

    /**
     * Find all samples ready for inoculation.
     */
    List<TbSampleProcessing> findReadyForInoculation();

    /**
     * Find samples by decontamination method.
     */
    List<TbSampleProcessing> findByDecontaminationMethod(DecontaminationMethod method);

    /**
     * Check if a sample has been processed.
     */
    boolean existsBySampleItemId(String sampleItemId);

    /**
     * Count samples by processing status.
     */
    Long countByProcessingStatus(ProcessingStatus status);

    /**
     * Find samples pending processing (registered but not yet processed).
     */
    List<String> findSampleItemIdsPendingProcessing();
}
