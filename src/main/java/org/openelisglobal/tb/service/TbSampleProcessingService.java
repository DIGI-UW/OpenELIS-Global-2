package org.openelisglobal.tb.service;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.tb.valueholder.TbEnums.DecontaminationMethod;
import org.openelisglobal.tb.valueholder.TbEnums.ProcessingStatus;
import org.openelisglobal.tb.valueholder.TbSampleProcessing;

/**
 * Service interface for TB sample processing operations.
 */
public interface TbSampleProcessingService extends BaseObjectService<TbSampleProcessing, Integer> {

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
     * Find samples pending processing.
     */
    List<String> findSampleItemIdsPendingProcessing();

    /**
     * Mark a sample as processed.
     */
    TbSampleProcessing markAsProcessed(Integer id);

    /**
     * Mark a sample as ready for inoculation.
     */
    TbSampleProcessing markReadyForInoculation(Integer id);

    /**
     * Process multiple samples with the same method (batch processing).
     */
    List<TbSampleProcessing> batchProcess(List<String> sampleItemIds, DecontaminationMethod method,
            String processedById);
}
