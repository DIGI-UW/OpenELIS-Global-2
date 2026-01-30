package org.openelisglobal.tb.service;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.tb.valueholder.TbEnums.MediaQcStatus;
import org.openelisglobal.tb.valueholder.TbEnums.MediaType;
import org.openelisglobal.tb.valueholder.TbMediaPreparation;

/**
 * Service interface for TB media preparation operations.
 */
public interface TbMediaPreparationService extends BaseObjectService<TbMediaPreparation, Integer> {

    /**
     * Find a media batch by its batch ID.
     */
    Optional<TbMediaPreparation> findByBatchId(String batchId);

    /**
     * Find all media batches by media type.
     */
    List<TbMediaPreparation> findByMediaType(MediaType mediaType);

    /**
     * Find all media batches by QC status.
     */
    List<TbMediaPreparation> findByQcStatus(MediaQcStatus qcStatus);

    /**
     * Find all media batches available for inoculation (passed QC, not expired).
     */
    List<TbMediaPreparation> findAvailableForInoculation();

    /**
     * Find available batches by media type.
     */
    List<TbMediaPreparation> findAvailableByMediaType(MediaType mediaType);

    /**
     * Check if a batch ID already exists.
     */
    boolean existsByBatchId(String batchId);

    /**
     * Count batches by QC status.
     */
    Long countByQcStatus(MediaQcStatus qcStatus);

    /**
     * Generate a unique batch ID with prefix and date.
     */
    String generateBatchId(MediaType mediaType);

    /**
     * Update the QC status of a media batch.
     */
    TbMediaPreparation updateQcStatus(Integer id, MediaQcStatus status, String notes, String sysUserId);

    /**
     * Find all media batches by notebook entry ID.
     */
    List<TbMediaPreparation> findByNotebookEntryId(Integer notebookEntryId);
}
