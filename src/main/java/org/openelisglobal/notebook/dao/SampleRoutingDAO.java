package org.openelisglobal.notebook.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.notebook.valueholder.SampleRouting;
import org.openelisglobal.notebook.valueholder.SampleRouting.DestinationType;

/**
 * DAO interface for SampleRouting entity operations.
 */
public interface SampleRoutingDAO extends BaseDAO<SampleRouting, Integer> {

    /**
     * Get all routing records for a notebook.
     *
     * @param notebookId notebook ID
     * @return list of routing records
     */
    List<SampleRouting> getByNotebookId(Integer notebookId);

    /**
     * Get all routing records for a sample item.
     *
     * @param sampleItemId sample item ID
     * @return list of routing records
     */
    List<SampleRouting> getBySampleItemId(Integer sampleItemId);

    /**
     * Get routing record for a sample in a specific notebook.
     *
     * @param notebookId   notebook ID
     * @param sampleItemId sample item ID
     * @return routing record or null
     */
    SampleRouting getByNotebookIdAndSampleItemId(Integer notebookId, Integer sampleItemId);

    /**
     * Get routing records by destination type.
     *
     * @param notebookId      notebook ID
     * @param destinationType destination type
     * @return list of routing records
     */
    List<SampleRouting> getByNotebookIdAndDestinationType(Integer notebookId, DestinationType destinationType);

    /**
     * Find routing by box and well coordinate. Used for analyzer result import
     * matching.
     *
     * @param notebookId     notebook ID
     * @param boxId          storage box ID
     * @param wellCoordinate well coordinate (e.g., "A1")
     * @return routing record or null
     */
    SampleRouting getByBoxAndWell(Integer notebookId, Integer boxId, String wellCoordinate);

    /**
     * Get count of unrouted samples in a notebook.
     *
     * @param notebookId notebook ID
     * @return count of samples without routing
     */
    long getUnroutedSampleCount(Integer notebookId);

    /**
     * Get count of routing records by destination type.
     *
     * @param notebookId      notebook ID
     * @param destinationType destination type
     * @return count of routing records
     */
    long getCountByDestinationType(Integer notebookId, DestinationType destinationType);

    /**
     * Get routing records for samples in a specific box.
     *
     * @param notebookId notebook ID
     * @param boxId      storage box ID
     * @return list of routing records for the box
     */
    List<SampleRouting> getByBoxId(Integer notebookId, Integer boxId);
}
