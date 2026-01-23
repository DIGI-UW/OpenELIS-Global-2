package org.openelisglobal.biorepository.service;

import java.util.List;
import org.openelisglobal.biorepository.valueholder.BioSample;
import org.openelisglobal.biorepository.valueholder.SampleTransferItem;
import org.openelisglobal.biorepository.valueholder.SampleTransferRequest;
import org.openelisglobal.biorepository.valueholder.SampleTransferRequest.TransferStatus;
import org.openelisglobal.common.service.BaseObjectService;

/**
 * Service interface for SampleTransferRequest operations. Handles internal lab
 * sample transfers to the biorepository.
 */
public interface SampleTransferService extends BaseObjectService<SampleTransferRequest, Integer> {

    /**
     * Create a new transfer request with sample items.
     *
     * @param sourceLab     the source lab identifier
     * @param sampleItemIds list of sample item IDs to transfer
     * @param requestNotes  optional notes for the request
     * @param sysUserId     the user creating the request
     * @return the created transfer request
     * @throws IllegalArgumentException if any sample item ID is invalid
     * @throws IllegalStateException    if any sample item has a pending transfer
     */
    SampleTransferRequest createTransferRequest(String sourceLab, List<Integer> sampleItemIds, String requestNotes,
            String sysUserId);

    /**
     * Get pending transfer requests for biorepository review.
     *
     * @param limit maximum results
     * @return list of pending requests ordered by requested timestamp (oldest
     *         first)
     */
    List<SampleTransferRequest> getPendingRequests(int limit);

    /**
     * Get transfer requests by status.
     *
     * @param status the transfer status
     * @return list of matching requests
     */
    List<SampleTransferRequest> getByStatus(TransferStatus status);

    /**
     * Get transfer requests from a specific source lab.
     *
     * @param sourceLab the source lab identifier
     * @return list of matching requests
     */
    List<SampleTransferRequest> getBySourceLab(String sourceLab);

    /**
     * Get transfer requests for a specific sample item.
     *
     * @param sampleItemId the sample item ID
     * @return list of transfer requests containing this sample
     */
    List<SampleTransferRequest> getBySampleItemId(Integer sampleItemId);

    /**
     * Accept a single transfer item and create a BioSample extension.
     *
     * @param transferItemId the transfer item ID
     * @param bioSample      the BioSample to create (with biorepository metadata)
     * @param sysUserId      the user accepting the item
     * @return the accepted transfer item with linked BioSample
     * @throws IllegalArgumentException if item not found
     * @throws IllegalStateException    if item is not pending
     */
    SampleTransferItem acceptItem(Integer transferItemId, BioSample bioSample, String sysUserId);

    /**
     * Reject a single transfer item.
     *
     * @param transferItemId  the transfer item ID
     * @param rejectionReason reason for rejection
     * @param sysUserId       the user rejecting the item
     * @return the rejected transfer item
     * @throws IllegalArgumentException if item not found
     * @throws IllegalStateException    if item is not pending
     */
    SampleTransferItem rejectItem(Integer transferItemId, String rejectionReason, String sysUserId);

    /**
     * Accept all pending items in a transfer request.
     *
     * @param transferRequestId the transfer request ID
     * @param bioSampleTemplate template BioSample with common metadata (biosafety,
     *                          ethics, etc.)
     * @param sysUserId         the user accepting the request
     * @return the updated transfer request
     * @throws IllegalArgumentException if request not found
     * @throws IllegalStateException    if request has no pending items
     */
    SampleTransferRequest acceptAll(Integer transferRequestId, BioSample bioSampleTemplate, String sysUserId);

    /**
     * Reject all pending items in a transfer request.
     *
     * @param transferRequestId the transfer request ID
     * @param rejectionReason   reason for rejection
     * @param sysUserId         the user rejecting the request
     * @return the updated transfer request
     * @throws IllegalArgumentException if request not found
     * @throws IllegalStateException    if request has no pending items
     */
    SampleTransferRequest rejectAll(Integer transferRequestId, String rejectionReason, String sysUserId);

    /**
     * Cancel a transfer request (by the origin lab).
     *
     * @param transferRequestId the transfer request ID
     * @param sysUserId         the user cancelling the request
     * @return the cancelled transfer request
     * @throws IllegalArgumentException if request not found
     * @throws IllegalStateException    if request is already processed
     */
    SampleTransferRequest cancelRequest(Integer transferRequestId, String sysUserId);

    /**
     * Check if a sample item has a pending transfer.
     *
     * @param sampleItemId the sample item ID
     * @return true if a pending transfer exists
     */
    boolean hasPendingTransfer(Integer sampleItemId);

    /**
     * Count transfer requests by status.
     *
     * @param status the transfer status
     * @return count of matching requests
     */
    long countByStatus(TransferStatus status);

    /**
     * Get distinct source labs from all transfer requests.
     *
     * @return list of distinct source lab names
     */
    List<String> getDistinctSourceLabs();

    /**
     * Get a transfer item by ID.
     *
     * @param transferItemId the transfer item ID
     * @return the transfer item or null if not found
     */
    SampleTransferItem getTransferItem(Integer transferItemId);
}
