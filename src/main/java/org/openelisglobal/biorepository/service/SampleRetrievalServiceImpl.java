package org.openelisglobal.biorepository.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import org.openelisglobal.biorepository.dao.SampleRetrievalItemDAO;
import org.openelisglobal.biorepository.dao.SampleRetrievalRequestDAO;
import org.openelisglobal.biorepository.valueholder.BioSample;
import org.openelisglobal.biorepository.valueholder.BioSample.WorkflowStatus;
import org.openelisglobal.biorepository.valueholder.ChainOfCustodyLog.CustodyAction;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalItem;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalItem.ItemStatus;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalRequest;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalRequest.DestinationType;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalRequest.PriorityLevel;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalRequest.RequestStatus;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.project.service.ProjectService;
import org.openelisglobal.project.valueholder.Project;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for SampleRetrievalRequest operations.
 */
@Service
public class SampleRetrievalServiceImpl extends AuditableBaseObjectServiceImpl<SampleRetrievalRequest, Integer>
        implements SampleRetrievalService {

    @Autowired
    protected SampleRetrievalRequestDAO baseObjectDAO;

    @Autowired
    private SampleRetrievalItemDAO retrievalItemDAO;

    @Autowired
    private BioSampleService bioSampleService;

    @Autowired
    private ChainOfCustodyService chainOfCustodyService;

    @Autowired
    private SystemUserService systemUserService;

    @Autowired
    private ProjectService projectService;

    SampleRetrievalServiceImpl() {
        super(SampleRetrievalRequest.class);
    }

    @Override
    protected SampleRetrievalRequestDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional
    public SampleRetrievalRequest createRequest(String requestPurpose, List<Integer> bioSampleIds, Integer projectId,
            String ethicsApprovalRef, DestinationType destinationType, String destinationDetails,
            PriorityLevel priorityLevel, LocalDate requiredByDate, String sysUserId) {

        if (requestPurpose == null || requestPurpose.trim().isEmpty()) {
            throw new IllegalArgumentException("Request purpose is required");
        }
        if (bioSampleIds == null || bioSampleIds.isEmpty()) {
            throw new IllegalArgumentException("At least one BioSample ID is required");
        }
        if (destinationType == null) {
            throw new IllegalArgumentException("Destination type is required");
        }

        SystemUser requestingUser = systemUserService.get(sysUserId);
        if (requestingUser == null) {
            throw new IllegalArgumentException("User not found: " + sysUserId);
        }

        SampleRetrievalRequest request = new SampleRetrievalRequest();
        int sequenceNumber = baseObjectDAO.getNextRequestNumberSequence();
        request.setRequestNumber(SampleRetrievalRequest.generateRequestNumber(sequenceNumber));
        request.setRequestPurpose(requestPurpose.trim());
        request.setStatus(RequestStatus.DRAFT);
        request.setDestinationType(destinationType);
        request.setDestinationDetails(destinationDetails);
        request.setPriorityLevel(priorityLevel != null ? priorityLevel : PriorityLevel.NORMAL);
        request.setRequiredByDate(requiredByDate);
        request.setRequestedBy(requestingUser);
        request.setRequestedTimestamp(new Timestamp(System.currentTimeMillis()));
        request.setEthicsApprovalRef(ethicsApprovalRef);
        request.setSysUserId(sysUserId);

        if (projectId != null) {
            Project project = projectService.get(String.valueOf(projectId));
            if (project == null) {
                throw new IllegalArgumentException("Project not found: " + projectId);
            }
            request.setProject(project);
        }

        for (Integer bioSampleId : bioSampleIds) {
            BioSample bioSample = bioSampleService.get(bioSampleId);
            if (bioSample == null) {
                throw new IllegalArgumentException("BioSample not found: " + bioSampleId);
            }

            if (!WorkflowStatus.STORED.equals(bioSample.getWorkflowStatus())) {
                throw new IllegalStateException("BioSample is not available for retrieval (status: "
                        + bioSample.getWorkflowStatus() + "): " + bioSampleId);
            }

            if (baseObjectDAO.hasActiveRetrievalForBioSample(bioSampleId)) {
                throw new IllegalStateException("BioSample already has a pending retrieval: " + bioSampleId);
            }

            SampleRetrievalItem item = new SampleRetrievalItem();
            item.setBioSample(bioSample);
            item.setStatus(ItemStatus.PENDING);
            item.setReturnExpected(destinationType == DestinationType.ANALYSIS_RETURN);
            item.setSysUserId(sysUserId);
            request.addItem(item);
        }

        return save(request);
    }

    @Override
    @Transactional
    public SampleRetrievalRequest submitForApproval(Integer requestId, String sysUserId) {
        SampleRetrievalRequest request = get(requestId);
        if (request == null) {
            throw new IllegalArgumentException("Retrieval request not found: " + requestId);
        }

        if (!request.isDraft()) {
            throw new IllegalStateException("Request is not in DRAFT status: " + request.getStatus());
        }

        if (request.getItems().isEmpty()) {
            throw new IllegalStateException("Request has no items");
        }

        request.setStatus(RequestStatus.PENDING_APPROVAL);
        request.setSysUserId(sysUserId);

        for (SampleRetrievalItem item : request.getItems()) {
            chainOfCustodyService.logCustodyAction(item.getBioSample().getSampleItem(),
                    CustodyAction.CHECKOUT_REQUESTED, null, request, null, null, null, null, null,
                    "Request submitted for approval", sysUserId);
        }

        return update(request);
    }

    @Override
    @Transactional
    public SampleRetrievalRequest approveRequest(Integer requestId, String approvalNotes, String sysUserId) {
        SampleRetrievalRequest request = get(requestId);
        if (request == null) {
            throw new IllegalArgumentException("Retrieval request not found: " + requestId);
        }

        if (!request.isPendingApproval()) {
            throw new IllegalStateException("Request is not pending approval: " + request.getStatus());
        }

        // TODO: Re-enable self-approval prevention for production
        // if (request.getRequestedBy().getId().equals(sysUserId)) {
        // throw new IllegalStateException("Cannot approve your own request");
        // }

        SystemUser approver = systemUserService.get(sysUserId);
        if (approver == null) {
            throw new IllegalArgumentException("Approver user not found: " + sysUserId);
        }

        request.setStatus(RequestStatus.APPROVED);
        request.setApprovedBy(approver);
        request.setApprovedTimestamp(new Timestamp(System.currentTimeMillis()));
        request.setApprovalNotes(approvalNotes);
        request.setWorkOrderNumber(generateWorkOrderNumber(request));
        request.setSysUserId(sysUserId);

        for (SampleRetrievalItem item : request.getItems()) {
            chainOfCustodyService.logCustodyAction(item.getBioSample().getSampleItem(), CustodyAction.CHECKOUT_APPROVED,
                    null, request, null, approver, null, null, null, "Request approved: " + approvalNotes, sysUserId);
        }

        return update(request);
    }

    @Override
    @Transactional
    public SampleRetrievalRequest rejectRequest(Integer requestId, String rejectionReason, String sysUserId) {
        SampleRetrievalRequest request = get(requestId);
        if (request == null) {
            throw new IllegalArgumentException("Retrieval request not found: " + requestId);
        }

        if (!request.isPendingApproval()) {
            throw new IllegalStateException("Request is not pending approval: " + request.getStatus());
        }

        if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
            throw new IllegalArgumentException("Rejection reason is required");
        }

        SystemUser rejecter = systemUserService.get(sysUserId);
        if (rejecter == null) {
            throw new IllegalArgumentException("Rejector user not found: " + sysUserId);
        }

        request.setStatus(RequestStatus.REJECTED);
        request.setApprovedBy(rejecter);
        request.setApprovedTimestamp(new Timestamp(System.currentTimeMillis()));
        request.setRejectionReason(rejectionReason);
        request.setSysUserId(sysUserId);

        return update(request);
    }

    @Override
    @Transactional
    public SampleRetrievalRequest cancelRequest(Integer requestId, String sysUserId) {
        SampleRetrievalRequest request = get(requestId);
        if (request == null) {
            throw new IllegalArgumentException("Retrieval request not found: " + requestId);
        }

        if (!request.canBeCancelled()) {
            throw new IllegalStateException("Request cannot be cancelled: " + request.getStatus());
        }

        request.setStatus(RequestStatus.CANCELLED);
        request.setSysUserId(sysUserId);

        return update(request);
    }

    @Override
    @Transactional
    public SampleRetrievalItem retrieveItem(Integer retrievalItemId, String conditionAtRelease, String conditionNotes,
            BigDecimal temperatureAtRetrieval, String sysUserId) {

        SampleRetrievalItem item = getRetrievalItemInternal(retrievalItemId);
        SampleRetrievalRequest request = item.getRetrievalRequest();

        if (!request.isApproved()) {
            throw new IllegalStateException("Request is not approved: " + request.getStatus());
        }

        if (!item.isPending()) {
            throw new IllegalStateException("Item is not pending: " + item.getStatus());
        }

        SystemUser retriever = systemUserService.get(sysUserId);
        if (retriever == null) {
            throw new IllegalArgumentException("User not found: " + sysUserId);
        }

        item.setStatus(ItemStatus.RETRIEVED);
        item.setRetrievedBy(retriever);
        item.setRetrievedTimestamp(new Timestamp(System.currentTimeMillis()));
        item.setConditionAtRelease(conditionAtRelease);
        item.setConditionNotes(conditionNotes);
        item.setSysUserId(sysUserId);

        BioSample bioSample = item.getBioSample();
        bioSample.setWorkflowStatus(WorkflowStatus.IN_USE);
        bioSample.setSysUserId(sysUserId);
        bioSampleService.update(bioSample);

        String storageCoords = getStorageCoordinates(bioSample);
        chainOfCustodyService.logCustodyAction(bioSample.getSampleItem(), CustodyAction.CHECKOUT_RETRIEVED,
                findOriginalTransferRequest(bioSample), request, storageCoords, retriever, storageCoords, null,
                temperatureAtRetrieval, conditionNotes, sysUserId);

        if (request.getStatus() == RequestStatus.APPROVED) {
            request.setStatus(RequestStatus.IN_PROGRESS);
            request.setSysUserId(sysUserId);
        }

        updateRequestStatusAfterItemChange(request, sysUserId);

        return item;
    }

    @Override
    @Transactional
    public SampleRetrievalItem releaseItem(Integer retrievalItemId, String sysUserId) {
        SampleRetrievalItem item = getRetrievalItemInternal(retrievalItemId);

        if (item.getStatus() != ItemStatus.RETRIEVED) {
            throw new IllegalStateException("Item is not in RETRIEVED status: " + item.getStatus());
        }

        SystemUser releaser = systemUserService.get(sysUserId);
        if (releaser == null) {
            throw new IllegalArgumentException("User not found: " + sysUserId);
        }

        item.setStatus(ItemStatus.IN_ANALYSIS);
        item.setSysUserId(sysUserId);

        BioSample bioSample = item.getBioSample();
        SampleRetrievalRequest request = item.getRetrievalRequest();

        chainOfCustodyService.logCustodyAction(bioSample.getSampleItem(), CustodyAction.CHECKOUT_RELEASED,
                findOriginalTransferRequest(bioSample), request, null, releaser, null, request.getDestinationDetails(),
                null, "Released to requester", sysUserId);

        update(request);
        return item;
    }

    @Override
    @Transactional
    public SampleRetrievalItem returnItem(Integer retrievalItemId, String returnedCondition, String returnNotes,
            boolean fullyConsumed, String sysUserId) {

        SampleRetrievalItem item = getRetrievalItemInternal(retrievalItemId);

        if (!item.isCheckedOut()) {
            throw new IllegalStateException("Item is not checked out: " + item.getStatus());
        }

        SystemUser returner = systemUserService.get(sysUserId);
        if (returner == null) {
            throw new IllegalArgumentException("User not found: " + sysUserId);
        }

        if (fullyConsumed) {
            item.setStatus(ItemStatus.CONSUMED);
        } else {
            item.setStatus(ItemStatus.RETURNED);
        }

        item.setReturnedBy(returner);
        item.setReturnedTimestamp(new Timestamp(System.currentTimeMillis()));
        item.setReturnedCondition(returnedCondition);
        item.setReturnNotes(returnNotes);
        item.setSysUserId(sysUserId);

        BioSample bioSample = item.getBioSample();
        SampleRetrievalRequest request = item.getRetrievalRequest();

        if (fullyConsumed) {
            bioSample.setWorkflowStatus(WorkflowStatus.DISPOSED);
        } else {
            bioSample.setWorkflowStatus(WorkflowStatus.STORED);
        }
        bioSample.setSysUserId(sysUserId);
        bioSampleService.update(bioSample);

        CustodyAction action = fullyConsumed ? CustodyAction.RETURN_RECEIVED : CustodyAction.RETURN_STORED;
        String storageCoords = getStorageCoordinates(bioSample);
        chainOfCustodyService.logCustodyAction(bioSample.getSampleItem(), action,
                findOriginalTransferRequest(bioSample), request, storageCoords, returner, null, storageCoords, null,
                returnNotes, sysUserId);

        updateRequestStatusAfterItemChange(request, sysUserId);

        return item;
    }

    @Override
    @Transactional
    public SampleRetrievalItem markItemUnavailable(Integer retrievalItemId, String reason, String sysUserId) {
        SampleRetrievalItem item = getRetrievalItemInternal(retrievalItemId);

        if (!item.isPending()) {
            throw new IllegalStateException("Item is not pending: " + item.getStatus());
        }

        item.setStatus(ItemStatus.UNAVAILABLE);
        item.setConditionNotes(reason);
        item.setSysUserId(sysUserId);

        SampleRetrievalRequest request = item.getRetrievalRequest();
        updateRequestStatusAfterItemChange(request, sysUserId);

        return item;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleRetrievalRequest> getPendingApproval(int limit) {
        return baseObjectDAO.getPendingApproval(limit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleRetrievalRequest> getByStatus(RequestStatus status) {
        return baseObjectDAO.getByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleRetrievalRequest> getByRequestedByUserId(Integer requestedByUserId) {
        return baseObjectDAO.getByRequestedByUserId(requestedByUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleRetrievalRequest> getByProjectId(Integer projectId) {
        return baseObjectDAO.getByProjectId(projectId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleRetrievalRequest> getByBioSampleId(Integer bioSampleId) {
        return baseObjectDAO.getByBioSampleId(bioSampleId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleRetrievalRequest> getWithCheckedOutItems() {
        return baseObjectDAO.getWithCheckedOutItems();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleRetrievalRequest> getWithOverdueReturns() {
        return baseObjectDAO.getWithOverdueReturns();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveRetrieval(Integer bioSampleId) {
        return baseObjectDAO.hasActiveRetrievalForBioSample(bioSampleId);
    }

    @Override
    @Transactional(readOnly = true)
    public SampleRetrievalRequest getByRequestNumber(String requestNumber) {
        return baseObjectDAO.getByRequestNumber(requestNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public SampleRetrievalItem getRetrievalItem(Integer retrievalItemId) {
        List<SampleRetrievalRequest> requests = getAll();
        for (SampleRetrievalRequest request : requests) {
            for (SampleRetrievalItem item : request.getItems()) {
                if (item.getId().equals(retrievalItemId)) {
                    return item;
                }
            }
        }
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public long countByStatus(RequestStatus status) {
        return baseObjectDAO.countByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleRetrievalItem> getCheckedOutItems() {
        return retrievalItemDAO.getCheckedOutItems();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleRetrievalItem> getOverdueItems() {
        return retrievalItemDAO.getOverdueItems();
    }

    private SampleRetrievalItem getRetrievalItemInternal(Integer retrievalItemId) {
        SampleRetrievalItem item = getRetrievalItem(retrievalItemId);
        if (item == null) {
            throw new IllegalArgumentException("Retrieval item not found: " + retrievalItemId);
        }
        return item;
    }

    private void updateRequestStatusAfterItemChange(SampleRetrievalRequest request, String sysUserId) {
        long pendingCount = request.getPendingItemCount();
        long retrievedCount = request.getRetrievedItemCount();
        long returnedCount = request.getReturnedItemCount();
        long consumedCount = request.getConsumedItemCount();
        int totalCount = request.getTotalItemCount();

        long completedCount = returnedCount + consumedCount;

        if (completedCount == totalCount) {
            request.setStatus(RequestStatus.COMPLETED);
        } else if (completedCount > 0 && (pendingCount > 0 || retrievedCount > 0)) {
            request.setStatus(RequestStatus.PARTIALLY_COMPLETED);
        } else if (retrievedCount > 0) {
            request.setStatus(RequestStatus.IN_PROGRESS);
        }

        request.setSysUserId(sysUserId);
        update(request);
    }

    private String generateWorkOrderNumber(SampleRetrievalRequest request) {
        return "WO-" + request.getRequestNumber().substring(3);
    }

    private String getStorageCoordinates(BioSample bioSample) {
        return null;
    }

    private org.openelisglobal.biorepository.valueholder.SampleTransferRequest findOriginalTransferRequest(
            BioSample bioSample) {
        return null;
    }
}
