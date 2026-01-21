package org.openelisglobal.biorepository.dao;

import java.util.List;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalRequest;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalRequest.RequestStatus;
import org.openelisglobal.common.dao.BaseDAO;

/**
 * DAO interface for SampleRetrievalRequest entity operations.
 */
public interface SampleRetrievalRequestDAO extends BaseDAO<SampleRetrievalRequest, Integer> {

    /**
     * Find all retrieval requests with a given status.
     *
     * @param status the request status
     * @return list of requests ordered by requested timestamp descending
     */
    List<SampleRetrievalRequest> getByStatus(RequestStatus status);

    /**
     * Find retrieval requests pending approval with pagination.
     *
     * @param limit maximum results
     * @return list of pending approval requests ordered by requested timestamp
     *         ascending (oldest first)
     */
    List<SampleRetrievalRequest> getPendingApproval(int limit);

    /**
     * Find retrieval requests made by a specific user.
     *
     * @param requestedByUserId the requesting user's ID
     * @return list of requests ordered by requested timestamp descending
     */
    List<SampleRetrievalRequest> getByRequestedByUserId(Integer requestedByUserId);

    /**
     * Find retrieval requests for a specific project.
     *
     * @param projectId the project ID
     * @return list of requests ordered by requested timestamp descending
     */
    List<SampleRetrievalRequest> getByProjectId(Integer projectId);

    /**
     * Find retrieval requests containing a specific BioSample.
     *
     * @param bioSampleId the BioSample ID
     * @return list of requests containing this BioSample
     */
    List<SampleRetrievalRequest> getByBioSampleId(Integer bioSampleId);

    /**
     * Check if a pending/active retrieval exists for a BioSample.
     *
     * @param bioSampleId the BioSample ID
     * @return true if an active retrieval exists
     */
    boolean hasActiveRetrievalForBioSample(Integer bioSampleId);

    /**
     * Count retrieval requests by status.
     *
     * @param status the request status
     * @return count of matching requests
     */
    long countByStatus(RequestStatus status);

    /**
     * Find requests with items currently checked out.
     *
     * @return list of requests with checked out items
     */
    List<SampleRetrievalRequest> getWithCheckedOutItems();

    /**
     * Find requests with overdue returns.
     *
     * @return list of requests with items past expected return date
     */
    List<SampleRetrievalRequest> getWithOverdueReturns();

    /**
     * Find a request by its request number.
     *
     * @param requestNumber the unique request number
     * @return the request or null if not found
     */
    SampleRetrievalRequest getByRequestNumber(String requestNumber);

    /**
     * Get the next sequence value for generating request numbers.
     *
     * @return next sequence value
     */
    int getNextRequestNumberSequence();
}
