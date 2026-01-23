package org.openelisglobal.biorepository.dao;

import java.util.List;
import org.openelisglobal.biorepository.valueholder.SampleTransferRequest;
import org.openelisglobal.biorepository.valueholder.SampleTransferRequest.TransferStatus;
import org.openelisglobal.common.dao.BaseDAO;

/**
 * DAO interface for SampleTransferRequest entity operations.
 */
public interface SampleTransferRequestDAO extends BaseDAO<SampleTransferRequest, Integer> {

    /**
     * Find all transfer requests with a given status.
     *
     * @param status the transfer status
     * @return list of requests ordered by requested timestamp descending
     */
    List<SampleTransferRequest> getByStatus(TransferStatus status);

    /**
     * Find pending transfer requests with pagination.
     *
     * @param limit maximum results
     * @return list of pending requests ordered by requested timestamp ascending
     *         (oldest first)
     */
    List<SampleTransferRequest> getPendingRequests(int limit);

    /**
     * Find transfer requests from a specific source lab.
     *
     * @param sourceLab the source lab identifier
     * @return list of requests ordered by requested timestamp descending
     */
    List<SampleTransferRequest> getBySourceLab(String sourceLab);

    /**
     * Find transfer requests made by a specific user.
     *
     * @param requestedByUserId the requesting user's ID
     * @return list of requests ordered by requested timestamp descending
     */
    List<SampleTransferRequest> getByRequestedByUserId(Integer requestedByUserId);

    /**
     * Find transfer requests containing a specific sample item.
     *
     * @param sampleItemId the sample item ID
     * @return list of requests containing this sample item
     */
    List<SampleTransferRequest> getBySampleItemId(Integer sampleItemId);

    /**
     * Check if a pending transfer exists for a sample item.
     *
     * @param sampleItemId the sample item ID
     * @return true if a pending transfer exists
     */
    boolean hasPendingTransferForSampleItem(Integer sampleItemId);

    /**
     * Count transfer requests by status.
     *
     * @param status the transfer status
     * @return count of matching requests
     */
    long countByStatus(TransferStatus status);

    /**
     * Count transfer requests from a specific source lab.
     *
     * @param sourceLab the source lab identifier
     * @return count of matching requests
     */
    long countBySourceLab(String sourceLab);

    /**
     * Get distinct source labs from all transfer requests.
     *
     * @return list of distinct source lab names
     */
    List<String> getDistinctSourceLabs();
}
