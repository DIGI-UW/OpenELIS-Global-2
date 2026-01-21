package org.openelisglobal.biorepository.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalItem;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalRequest;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalRequest.DestinationType;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalRequest.PriorityLevel;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalRequest.RequestStatus;
import org.openelisglobal.common.service.BaseObjectService;

/**
 * Service interface for SampleRetrievalRequest operations.
 *
 * Handles outbound sample retrievals from the biorepository to researchers,
 * labs, and other authorized requesters.
 *
 * Workflow: 1. Requester creates retrieval request with sample items (DRAFT) 2.
 * Requester submits for approval (PENDING_APPROVAL) 3. Supervisor reviews and
 * approves/rejects 4. On approval: Work order generated, samples can be
 * retrieved (APPROVED -> IN_PROGRESS) 5. Samples retrieved and released to
 * requester 6. Samples returned or consumed (COMPLETED)
 */
public interface SampleRetrievalService extends BaseObjectService<SampleRetrievalRequest, Integer> {

    /**
     * Create a new retrieval request.
     *
     * @param requestPurpose     purpose for the retrieval
     * @param bioSampleIds       list of BioSample IDs to retrieve
     * @param projectId          optional project ID
     * @param ethicsApprovalRef  optional ethics approval reference
     * @param destinationType    type of destination (INTERNAL_LAB, EXTERNAL_LAB,
     *                           ANALYSIS_RETURN)
     * @param destinationDetails optional details about destination
     * @param priorityLevel      request priority (NORMAL, URGENT, CRITICAL)
     * @param requiredByDate     optional date by which samples are needed
     * @param sysUserId          the user creating the request
     * @return the created retrieval request in DRAFT status
     * @throws IllegalArgumentException if any BioSample ID is invalid
     * @throws IllegalStateException    if any sample is not available for retrieval
     */
    SampleRetrievalRequest createRequest(String requestPurpose, List<Integer> bioSampleIds, Integer projectId,
            String ethicsApprovalRef, DestinationType destinationType, String destinationDetails,
            PriorityLevel priorityLevel, LocalDate requiredByDate, String sysUserId);

    /**
     * Submit a draft request for approval.
     *
     * @param requestId the request ID
     * @param sysUserId the user submitting the request
     * @return the updated request in PENDING_APPROVAL status
     * @throws IllegalArgumentException if request not found
     * @throws IllegalStateException    if request is not in DRAFT status
     */
    SampleRetrievalRequest submitForApproval(Integer requestId, String sysUserId);

    /**
     * Approve a pending retrieval request.
     *
     * @param requestId     the request ID
     * @param approvalNotes optional approval notes
     * @param sysUserId     the user approving the request
     * @return the approved request with work order generated
     * @throws IllegalArgumentException if request not found
     * @throws IllegalStateException    if request is not PENDING_APPROVAL or if
     *                                  approver is the requester
     */
    SampleRetrievalRequest approveRequest(Integer requestId, String approvalNotes, String sysUserId);

    /**
     * Reject a pending retrieval request.
     *
     * @param requestId       the request ID
     * @param rejectionReason reason for rejection
     * @param sysUserId       the user rejecting the request
     * @return the rejected request
     * @throws IllegalArgumentException if request not found
     * @throws IllegalStateException    if request is not PENDING_APPROVAL
     */
    SampleRetrievalRequest rejectRequest(Integer requestId, String rejectionReason, String sysUserId);

    /**
     * Cancel a retrieval request (by the requester).
     *
     * @param requestId the request ID
     * @param sysUserId the user cancelling the request
     * @return the cancelled request
     * @throws IllegalArgumentException if request not found
     * @throws IllegalStateException    if request cannot be cancelled (already
     *                                  processed)
     */
    SampleRetrievalRequest cancelRequest(Integer requestId, String sysUserId);

    /**
     * Record physical retrieval of a sample from storage.
     *
     * @param retrievalItemId        the retrieval item ID
     * @param conditionAtRelease     condition of sample at retrieval (e.g., "Good",
     *                               "Thawed")
     * @param conditionNotes         optional notes about condition
     * @param temperatureAtRetrieval optional temperature reading
     * @param sysUserId              the user performing the retrieval
     * @return the updated retrieval item in RETRIEVED status
     * @throws IllegalArgumentException if item not found
     * @throws IllegalStateException    if item is not PENDING or request not
     *                                  APPROVED
     */
    SampleRetrievalItem retrieveItem(Integer retrievalItemId, String conditionAtRelease, String conditionNotes,
            BigDecimal temperatureAtRetrieval, String sysUserId);

    /**
     * Mark an item as released to the requester.
     *
     * @param retrievalItemId the retrieval item ID
     * @param sysUserId       the user releasing the item
     * @return the updated retrieval item in IN_ANALYSIS status
     * @throws IllegalArgumentException if item not found
     * @throws IllegalStateException    if item is not RETRIEVED
     */
    SampleRetrievalItem releaseItem(Integer retrievalItemId, String sysUserId);

    /**
     * Process return of a sample.
     *
     * @param retrievalItemId   the retrieval item ID
     * @param returnedCondition condition of sample on return
     * @param returnNotes       optional notes about the return
     * @param fullyConsumed     if true, mark as CONSUMED; if false, mark as
     *                          RETURNED
     * @param sysUserId         the user processing the return
     * @return the updated retrieval item in RETURNED or CONSUMED status
     * @throws IllegalArgumentException if item not found
     * @throws IllegalStateException    if item is not checked out (RETRIEVED or
     *                                  IN_ANALYSIS)
     */
    SampleRetrievalItem returnItem(Integer retrievalItemId, String returnedCondition, String returnNotes,
            boolean fullyConsumed, String sysUserId);

    /**
     * Mark an item as unavailable (damaged, missing, etc.).
     *
     * @param retrievalItemId the retrieval item ID
     * @param reason          reason for unavailability
     * @param sysUserId       the user marking the item
     * @return the updated retrieval item in UNAVAILABLE status
     * @throws IllegalArgumentException if item not found
     * @throws IllegalStateException    if item is not PENDING
     */
    SampleRetrievalItem markItemUnavailable(Integer retrievalItemId, String reason, String sysUserId);

    /**
     * Get retrieval requests pending approval.
     *
     * @param limit maximum results
     * @return list of pending requests ordered by requested timestamp (oldest
     *         first)
     */
    List<SampleRetrievalRequest> getPendingApproval(int limit);

    /**
     * Get retrieval requests by status.
     *
     * @param status the request status
     * @return list of matching requests
     */
    List<SampleRetrievalRequest> getByStatus(RequestStatus status);

    /**
     * Get retrieval requests by requester.
     *
     * @param requestedByUserId the requester user ID
     * @return list of matching requests
     */
    List<SampleRetrievalRequest> getByRequestedByUserId(Integer requestedByUserId);

    /**
     * Get retrieval requests by project.
     *
     * @param projectId the project ID
     * @return list of matching requests
     */
    List<SampleRetrievalRequest> getByProjectId(Integer projectId);

    /**
     * Get retrieval requests containing a specific BioSample.
     *
     * @param bioSampleId the BioSample ID
     * @return list of matching requests
     */
    List<SampleRetrievalRequest> getByBioSampleId(Integer bioSampleId);

    /**
     * Get requests with currently checked out items.
     *
     * @return list of requests with checked out items
     */
    List<SampleRetrievalRequest> getWithCheckedOutItems();

    /**
     * Get requests with overdue returns.
     *
     * @return list of requests with items past expected return date
     */
    List<SampleRetrievalRequest> getWithOverdueReturns();

    /**
     * Check if a BioSample has an active (pending or in progress) retrieval.
     *
     * @param bioSampleId the BioSample ID
     * @return true if an active retrieval exists
     */
    boolean hasActiveRetrieval(Integer bioSampleId);

    /**
     * Get a retrieval request by its request number.
     *
     * @param requestNumber the request number
     * @return the request or null if not found
     */
    SampleRetrievalRequest getByRequestNumber(String requestNumber);

    /**
     * Get a retrieval item by ID.
     *
     * @param retrievalItemId the retrieval item ID
     * @return the item or null if not found
     */
    SampleRetrievalItem getRetrievalItem(Integer retrievalItemId);

    /**
     * Count retrieval requests by status.
     *
     * @param status the request status
     * @return count of matching requests
     */
    long countByStatus(RequestStatus status);

    /**
     * Get currently checked out items.
     *
     * @return list of items that are RETRIEVED or IN_ANALYSIS
     */
    List<SampleRetrievalItem> getCheckedOutItems();

    /**
     * Get items that are overdue for return.
     *
     * @return list of overdue items
     */
    List<SampleRetrievalItem> getOverdueItems();
}
