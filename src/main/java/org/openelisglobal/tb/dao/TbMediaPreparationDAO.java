package org.openelisglobal.tb.dao;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.tb.valueholder.TbEnums.MediaQcStatus;
import org.openelisglobal.tb.valueholder.TbEnums.MediaType;
import org.openelisglobal.tb.valueholder.TbMediaPreparation;

/**
 * Data access interface for TB media preparation batches.
 */
public interface TbMediaPreparationDAO extends BaseDAO<TbMediaPreparation, Integer> {

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
}
