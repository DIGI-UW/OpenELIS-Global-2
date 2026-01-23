package org.openelisglobal.biorepository.dao;

import java.util.List;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalItem;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalItem.ItemStatus;
import org.openelisglobal.common.dao.BaseDAO;

/**
 * DAO interface for SampleRetrievalItem entity operations.
 */
public interface SampleRetrievalItemDAO extends BaseDAO<SampleRetrievalItem, Integer> {

    /**
     * Find all retrieval items for a specific request.
     *
     * @param retrievalRequestId the parent request ID
     * @return list of items for this request
     */
    List<SampleRetrievalItem> getByRetrievalRequestId(Integer retrievalRequestId);

    /**
     * Find all retrieval items for a specific BioSample.
     *
     * @param bioSampleId the BioSample ID
     * @return list of items for this BioSample ordered by retrieved timestamp
     */
    List<SampleRetrievalItem> getByBioSampleId(Integer bioSampleId);

    /**
     * Find retrieval items with a specific status.
     *
     * @param status the item status
     * @return list of items with this status
     */
    List<SampleRetrievalItem> getByStatus(ItemStatus status);

    /**
     * Find items that are currently checked out (RETRIEVED or IN_ANALYSIS).
     *
     * @return list of checked out items
     */
    List<SampleRetrievalItem> getCheckedOutItems();

    /**
     * Find items that are overdue for return.
     *
     * @return list of overdue items
     */
    List<SampleRetrievalItem> getOverdueItems();

    /**
     * Count items by status.
     *
     * @param status the item status
     * @return count of items with this status
     */
    long countByStatus(ItemStatus status);

    /**
     * Check if a BioSample has any pending or active retrieval item.
     *
     * @param bioSampleId the BioSample ID
     * @return true if an active item exists
     */
    boolean hasActiveItemForBioSample(Integer bioSampleId);
}
