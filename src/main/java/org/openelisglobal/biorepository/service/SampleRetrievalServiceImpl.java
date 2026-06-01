package org.openelisglobal.biorepository.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.openelisglobal.biorepository.dao.SampleRetrievalItemDAO;
import org.openelisglobal.biorepository.dao.SampleRetrievalRequestDAO;
import org.openelisglobal.biorepository.valueholder.BioSample;
import org.openelisglobal.biorepository.valueholder.BioSample.WorkflowStatus;
import org.openelisglobal.biorepository.valueholder.ChainOfCustodyLog.CustodyAction;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalItem;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalItem.ItemStatus;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalRequest;
import org.openelisglobal.biorepository.util.Brf02SamplePathFormatter;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalRequest.DestinationType;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalRequest.PriorityLevel;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalRequest.RequestStatus;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.notebook.service.NotebookSampleEntryService;
import org.openelisglobal.project.service.ProjectService;
import org.openelisglobal.project.valueholder.Project;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.storage.service.SampleStorageService;
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

    @Autowired
    private NotebookSampleEntryService notebookSampleEntryService;

    @Autowired
    private SampleStorageService sampleStorageService;

    @Autowired
    private SampleTransferService sampleTransferService;

    @Autowired
    private SampleItemService sampleItemService;

    SampleRetrievalServiceImpl() {
        super(SampleRetrievalRequest.class);
    }

    @Override
    protected SampleRetrievalRequestDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional
    public SampleRetrievalRequest createRequest(String requestPurpose, List<RetrievalItemCreate> items,
            Integer projectId, String ethicsApprovalRef, DestinationType destinationType, String destinationDetails,
            PriorityLevel priorityLevel, LocalDate requiredByDate, String sysUserId) {

        if (requestPurpose == null || requestPurpose.trim().isEmpty()) {
            throw new IllegalArgumentException("Request purpose is required");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("At least one retrieval item is required");
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

        for (RetrievalItemCreate itemCreate : items) {
            if (itemCreate == null) {
                throw new IllegalArgumentException("Retrieval item cannot be null");
            }

            if (itemCreate.isReferenceOnly()) {
                request.addItem(buildReferenceRetrievalItem(itemCreate, destinationType, sysUserId));
                continue;
            }

            request.addItem(buildDirectRetrievalItem(itemCreate, destinationType, sysUserId));
        }

        return save(request);
    }

    private SampleRetrievalItem buildReferenceRetrievalItem(RetrievalItemCreate itemCreate,
            DestinationType destinationType, String sysUserId) {
        String sampleType = trimToNull(itemCreate.getRequestedSampleType());
        if (sampleType == null) {
            throw new IllegalArgumentException("Requested sample type is required for reference-only items");
        }

        BigDecimal quantityRequested = itemCreate.getQuantityRequested();
        if (quantityRequested == null || quantityRequested.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity requested must be greater than zero");
        }

        SampleRetrievalItem item = new SampleRetrievalItem();
        item.setStatus(ItemStatus.AWAITING_FULFILLMENT);
        item.setRequestedAccessionNumber(trimToNull(itemCreate.getRequestedAccessionNumber()));
        item.setRequestedBarcode(trimToNull(itemCreate.getRequestedBarcode()));
        item.setRequestedSampleType(sampleType);
        item.setRequestedOriginLab(trimToNull(itemCreate.getRequestedOriginLab()));
        item.setRequestedProjectId(trimToNull(itemCreate.getRequestedProjectId()));
        item.setRequestedCollectionDateFrom(itemCreate.getRequestedCollectionDateFrom());
        item.setRequestedCollectionDateTo(itemCreate.getRequestedCollectionDateTo());
        item.setQuantityRequested(quantityRequested);
        item.setUnitOfMeasure(trimToNull(itemCreate.getUnitOfMeasure()));
        item.setRemark(trimToNull(itemCreate.getRemark()));
        item.setReturnExpected(destinationType == DestinationType.ANALYSIS_RETURN);
        item.setSysUserId(sysUserId);
        return item;
    }

    private SampleRetrievalItem buildDirectRetrievalItem(RetrievalItemCreate itemCreate,
            DestinationType destinationType, String sysUserId) {
        Integer bioSampleId = itemCreate.getBioSampleId();
        BioSample bioSample = bioSampleService.get(bioSampleId);
        if (bioSample == null) {
            throw new IllegalArgumentException("BioSample not found: " + bioSampleId);
        }

        if (!isAvailableForFulfillmentAttach(bioSample)) {
            throw new IllegalStateException("BioSample is not available for retrieval (status: "
                    + bioSample.getWorkflowStatus() + "): " + bioSampleId);
        }

        if (baseObjectDAO.hasActiveRetrievalForBioSample(bioSampleId)) {
            throw new IllegalStateException("BioSample already has a pending retrieval: " + bioSampleId);
        }

        SampleItem sampleItem = bioSample.getSampleItem();
        if (sampleItem == null) {
            throw new IllegalArgumentException("BioSample has no linked sample item: " + bioSampleId);
        }

        BigDecimal quantityRequested = itemCreate.getQuantityRequested();
        BigDecimal available = resolveFulfillmentAvailableQuantity(sampleItem, quantityRequested);
        if (available.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("BioSample has no available quantity: " + bioSampleId);
        }
        if (quantityRequested == null) {
            quantityRequested = available;
        }
        if (quantityRequested.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity requested must be greater than zero for BioSample: "
                    + bioSampleId);
        }
        if (quantityRequested.compareTo(available) > 0) {
            throw new IllegalArgumentException("Quantity requested (" + quantityRequested
                    + ") exceeds available quantity (" + available + ") for BioSample: " + bioSampleId);
        }

        String unitOfMeasure = itemCreate.getUnitOfMeasure();
        if (unitOfMeasure == null || unitOfMeasure.trim().isEmpty()) {
            unitOfMeasure = sampleItem.getUnitOfMeasureName();
        }

        SampleRetrievalItem item = new SampleRetrievalItem();
        item.setBioSample(bioSample);
        item.setStatus(ItemStatus.PENDING);
        item.setQuantityRequested(quantityRequested);
        item.setUnitOfMeasure(unitOfMeasure != null ? unitOfMeasure.trim() : null);
        item.setRemark(itemCreate.getRemark() != null ? itemCreate.getRemark().trim() : null);
        item.setReturnExpected(destinationType == DestinationType.ANALYSIS_RETURN);
        item.setSysUserId(sysUserId);
        applySourceStorageSnapshot(item, sampleItem);
        return item;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Override
    @Transactional
    public SampleRetrievalItem attachSampleToReferenceItem(Integer referenceItemId, Integer bioSampleId,
            BigDecimal quantityRequested, String sysUserId) {
        SampleRetrievalItem referenceItem = getRetrievalItemInternal(referenceItemId);
        if (!referenceItem.isReferenceLine() || !referenceItem.isAwaitingFulfillment()) {
            throw new IllegalStateException("Item is not awaiting fulfillment: " + referenceItemId);
        }

        SampleRetrievalRequest request = referenceItem.getRetrievalRequest();
        if (request == null) {
            throw new IllegalStateException("Reference item has no parent request");
        }
        if (!request.isApproved()) {
            throw new IllegalStateException("Request must be approved before attaching samples: " + request.getStatus());
        }

        if (bioSampleId == null) {
            throw new IllegalArgumentException("BioSample ID is required");
        }

        BioSample bioSample = bioSampleService.get(bioSampleId);
        if (bioSample == null) {
            throw new IllegalArgumentException("BioSample not found: " + bioSampleId);
        }
        if (!isAvailableForFulfillmentAttach(bioSample)) {
            throw new IllegalStateException("BioSample is not available for retrieval (status: "
                    + bioSample.getWorkflowStatus() + "): " + bioSampleId);
        }
        if (baseObjectDAO.hasActiveRetrievalForBioSample(bioSampleId)) {
            throw new IllegalStateException("BioSample already has a pending retrieval: " + bioSampleId);
        }

        SampleItem sampleItem = bioSample.getSampleItem();
        if (sampleItem == null) {
            throw new IllegalArgumentException("BioSample has no linked sample item: " + bioSampleId);
        }

        BigDecimal attachQty = quantityRequested != null ? quantityRequested : referenceItem.getQuantityRequested();
        BigDecimal available = resolveFulfillmentAvailableQuantity(sampleItem, attachQty);
        if (available.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("BioSample has no available quantity: " + bioSampleId);
        }
        if (attachQty == null) {
            attachQty = available;
        }
        if (attachQty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity requested must be greater than zero");
        }
        if (attachQty.compareTo(available) > 0) {
            throw new IllegalArgumentException("Quantity requested (" + attachQty
                    + ") exceeds available quantity (" + available + ")");
        }

        String unitOfMeasure = referenceItem.getUnitOfMeasure();
        if (unitOfMeasure == null || unitOfMeasure.trim().isEmpty()) {
            unitOfMeasure = sampleItem.getUnitOfMeasureName();
        }

        SampleRetrievalItem fulfillmentItem = new SampleRetrievalItem();
        fulfillmentItem.setFulfillsItem(referenceItem);
        fulfillmentItem.setBioSample(bioSample);
        fulfillmentItem.setStatus(ItemStatus.PENDING);
        fulfillmentItem.setQuantityRequested(attachQty);
        fulfillmentItem.setUnitOfMeasure(unitOfMeasure != null ? unitOfMeasure.trim() : null);
        fulfillmentItem.setRemark(referenceItem.getRemark());
        fulfillmentItem.setReturnExpected(referenceItem.getReturnExpected());
        fulfillmentItem.setExpectedReturnDate(referenceItem.getExpectedReturnDate());
        fulfillmentItem.setSysUserId(sysUserId);
        applySourceStorageSnapshot(fulfillmentItem, sampleItem);

        request.addItem(fulfillmentItem);
        update(request);

        return fulfillmentItem;
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
            BioSample bioSample = item.getBioSample();
            if (bioSample == null || bioSample.getSampleItem() == null) {
                continue;
            }
            chainOfCustodyService.logCustodyAction(bioSample.getSampleItem(),
                    CustodyAction.CHECKOUT_REQUESTED, null, request, null, null, null, null, null,
                    "Request submitted for approval", sysUserId, "SampleRetrievalItem", item.getId(),
                    bioSample.getWorkflowStatus() != null ? bioSample.getWorkflowStatus().name() : null,
                    bioSample.getWorkflowStatus() != null ? bioSample.getWorkflowStatus().name() : null);
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
            BioSample bioSample = item.getBioSample();
            if (bioSample == null || bioSample.getSampleItem() == null) {
                continue;
            }
            chainOfCustodyService.logCustodyAction(bioSample.getSampleItem(), CustodyAction.CHECKOUT_APPROVED,
                    null, request, null, approver, null, null, null, "Request approved: " + approvalNotes, sysUserId,
                    "SampleRetrievalItem", item.getId(),
                    bioSample.getWorkflowStatus() != null ? bioSample.getWorkflowStatus().name() : null,
                    bioSample.getWorkflowStatus() != null ? bioSample.getWorkflowStatus().name() : null);
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
            BigDecimal temperatureAtRetrieval, BigDecimal quantityReleased, String sysUserId) {

        SampleRetrievalItem item = getRetrievalItemInternal(retrievalItemId);
        SampleRetrievalRequest request = item.getRetrievalRequest();

        if (!request.isApproved()) {
            throw new IllegalStateException("Request is not approved: " + request.getStatus());
        }

        if (!item.isPending()) {
            throw new IllegalStateException("Item is not pending: " + item.getStatus());
        }

        BioSample bioSample = item.getBioSample();
        if (bioSample == null) {
            throw new IllegalStateException("Cannot retrieve item without an attached BioSample");
        }

        SystemUser retriever = systemUserService.get(sysUserId);
        if (retriever == null) {
            throw new IllegalArgumentException("User not found: " + sysUserId);
        }

        SampleItem sampleItem = bioSample.getSampleItem();
        if (sampleItem == null) {
            throw new IllegalArgumentException("BioSample has no linked sample item");
        }

        BigDecimal releaseQty = quantityReleased;
        if (releaseQty == null) {
            releaseQty = item.getQuantityRequested();
        }
        BigDecimal available = resolveFulfillmentAvailableQuantity(sampleItem, releaseQty);
        if (releaseQty == null) {
            releaseQty = available;
        }
        if (releaseQty == null || releaseQty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity to release must be greater than zero");
        }
        if (available.compareTo(BigDecimal.ZERO) > 0 && releaseQty.compareTo(available) > 0) {
            throw new IllegalArgumentException(
                    "Quantity to release (" + releaseQty + ") exceeds available quantity (" + available + ")");
        }

        applySourceStorageSnapshot(item, sampleItem);

        item.setStatus(ItemStatus.RETRIEVED);
        item.setRetrievedBy(retriever);
        item.setRetrievedTimestamp(new Timestamp(System.currentTimeMillis()));
        item.setConditionAtRelease(conditionAtRelease);
        item.setConditionNotes(conditionNotes);
        item.setQuantityReleased(releaseQty);
        item.setSysUserId(sysUserId);

        String workflowStatusBefore = bioSample.getWorkflowStatus() != null ? bioSample.getWorkflowStatus().name() : null;
        bioSample.setWorkflowStatus(WorkflowStatus.IN_USE);
        bioSample.setSysUserId(sysUserId);
        bioSampleService.update(bioSample);

        String storageCoords = getStorageCoordinates(bioSample);
        chainOfCustodyService.logCustodyAction(bioSample.getSampleItem(), CustodyAction.CHECKOUT_RETRIEVED,
                findOriginalTransferRequest(bioSample), request, storageCoords, retriever, storageCoords, null,
                temperatureAtRetrieval, conditionNotes, sysUserId, "SampleRetrievalItem", item.getId(),
                workflowStatusBefore, WorkflowStatus.IN_USE.name());

        String checkoutNote = "Checked out for retrieval request "
                + (request.getRequestNumber() != null ? request.getRequestNumber() : request.getId());
        sampleStorageService.clearStorageAssignmentForCheckout(sampleItem.getId(), checkoutNote, sysUserId);

        if (request.getStatus() == RequestStatus.APPROVED) {
            request.setStatus(RequestStatus.IN_PROGRESS);
            request.setSysUserId(sysUserId);
        }

        updateRequestStatusAfterItemChange(request, sysUserId);

        return item;
    }

    @Override
    @Transactional
    public SampleRetrievalItem releaseItem(Integer retrievalItemId, String receivedByName, String sysUserId) {
        SampleRetrievalItem item = getRetrievalItemInternal(retrievalItemId);

        if (item.getStatus() != ItemStatus.RETRIEVED) {
            throw new IllegalStateException("Item is not in RETRIEVED status: " + item.getStatus());
        }

        SystemUser releaser = systemUserService.get(sysUserId);
        if (releaser == null) {
            throw new IllegalArgumentException("User not found: " + sysUserId);
        }

        item.setStatus(ItemStatus.IN_ANALYSIS);
        item.setReleasedTimestamp(new Timestamp(System.currentTimeMillis()));
        item.setReceivedByName(receivedByName != null && !receivedByName.trim().isEmpty() ? receivedByName.trim() : null);
        item.setSysUserId(sysUserId);

        BioSample bioSample = item.getBioSample();
        SampleRetrievalRequest request = item.getRetrievalRequest();

        BigDecimal quantityReleased = item.getQuantityReleased();
        if (quantityReleased == null) {
            quantityReleased = item.getQuantityRequested();
        }
        if (quantityReleased != null && quantityReleased.compareTo(BigDecimal.ZERO) > 0) {
            SampleItem sampleItem = bioSample.getSampleItem();
            if (sampleItem != null) {
                sampleItem.decrementRemainingQuantity(quantityReleased);
                sampleItem.setSysUserId(sysUserId);
                sampleItemService.update(sampleItem);
            }
        }

        chainOfCustodyService.logCustodyAction(bioSample.getSampleItem(), CustodyAction.CHECKOUT_RELEASED,
                findOriginalTransferRequest(bioSample), request, null, releaser, null, request.getDestinationDetails(),
                null, "Released to requester", sysUserId, "SampleRetrievalItem", item.getId(),
                bioSample.getWorkflowStatus() != null ? bioSample.getWorkflowStatus().name() : null,
                bioSample.getWorkflowStatus() != null ? bioSample.getWorkflowStatus().name() : null);

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
        String workflowStatusBefore = bioSample.getWorkflowStatus() != null ? bioSample.getWorkflowStatus().name() : null;
        String storageCoords = getStorageCoordinates(bioSample);

        if (fullyConsumed) {
            bioSample.setWorkflowStatus(WorkflowStatus.DISPOSED);
        } else {
            bioSample.setWorkflowStatus(WorkflowStatus.PENDING_STORAGE);
        }
        bioSample.setSysUserId(sysUserId);
        bioSampleService.update(bioSample);

        CustodyAction action = fullyConsumed ? CustodyAction.DISPOSED : CustodyAction.RETURN_RECEIVED;
        chainOfCustodyService.logCustodyAction(bioSample.getSampleItem(), action,
                findOriginalTransferRequest(bioSample), request, storageCoords, returner, null, storageCoords, null,
                returnNotes, sysUserId, "SampleRetrievalItem", item.getId(), workflowStatusBefore,
                bioSample.getWorkflowStatus() != null ? bioSample.getWorkflowStatus().name() : null);

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
        long awaitingFulfillmentCount = request.getAwaitingFulfillmentItemCount();

        if (totalCount == 0 && awaitingFulfillmentCount > 0) {
            request.setSysUserId(sysUserId);
            update(request);
            return;
        }

        long completedCount = returnedCount + consumedCount;

        if (totalCount > 0 && completedCount == totalCount) {
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
        if (bioSample == null || bioSample.getSampleItem() == null || bioSample.getSampleItem().getId() == null) {
            return null;
        }

        var location = sampleStorageService.getSampleItemLocation(bioSample.getSampleItem().getId());
        if (location == null || location.isEmpty()) {
            return null;
        }

        Object hierarchicalPath = location.get("hierarchicalPath");
        if (hierarchicalPath instanceof String path && !path.trim().isEmpty()) {
            return path;
        }

        Object rawLocation = location.get("location");
        if (rawLocation instanceof String path && !path.trim().isEmpty()) {
            return path;
        }

        return null;
    }

    private void applySourceStorageSnapshot(SampleRetrievalItem item, SampleItem sampleItem) {
        if (item == null || sampleItem == null || sampleItem.getId() == null) {
            return;
        }
        Map<String, Object> location = sampleStorageService.getSampleItemLocation(sampleItem.getId());
        if (location == null || location.isEmpty()) {
            return;
        }

        item.setSourceStorageAssignmentId(asInteger(location.get("assignmentId")));
        item.setSourceStorageLocationId(asInteger(location.get("locationId")));
        item.setSourceStorageLocationType(asString(location.get("locationType")));
        item.setSourceStoragePositionCoordinate(asString(location.get("positionCoordinate")));
        String brf02Path = Brf02SamplePathFormatter.format(location);
        if (!hasText(brf02Path)) {
            String path = asString(location.get("hierarchicalPath"));
            if (!hasText(path)) {
                path = asString(location.get("location"));
            }
            brf02Path = path;
        }
        item.setSourceStoragePath(brf02Path);
    }

    private Integer asInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            String text = String.valueOf(value).trim();
            return text.isEmpty() ? null : Integer.valueOf(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String asString(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private org.openelisglobal.biorepository.valueholder.SampleTransferRequest findOriginalTransferRequest(
            BioSample bioSample) {
        if (bioSample == null || bioSample.getSampleItem() == null || bioSample.getSampleItem().getId() == null) {
            return null;
        }

        try {
            List<org.openelisglobal.biorepository.valueholder.SampleTransferRequest> requests = sampleTransferService
                    .getBySampleItemId(Integer.valueOf(bioSample.getSampleItem().getId()));
            if (requests == null || requests.isEmpty()) {
                return null;
            }

            return requests.stream().filter(request -> request.getStatus() != null)
                    .sorted((left, right) -> {
                        Timestamp leftTimestamp = left.getProcessedTimestamp() != null ? left.getProcessedTimestamp()
                                : left.getRequestedTimestamp();
                        Timestamp rightTimestamp = right.getProcessedTimestamp() != null ? right.getProcessedTimestamp()
                                : right.getRequestedTimestamp();
                        if (leftTimestamp == null && rightTimestamp == null) {
                            return Integer.compare(
                                    right.getId() != null ? right.getId() : Integer.MIN_VALUE,
                                    left.getId() != null ? left.getId() : Integer.MIN_VALUE);
                        }
                        if (leftTimestamp == null) {
                            return 1;
                        }
                        if (rightTimestamp == null) {
                            return -1;
                        }
                        return rightTimestamp.compareTo(leftTimestamp);
                    }).findFirst().orElse(null);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleRetrievalRequest> getByNotebookEntryId(Integer notebookEntryId) {
        return baseObjectDAO.findByNotebookEntryId(notebookEntryId);
    }

    @Override
    @Transactional
    public int linkRetrievedSamplesToNotebook(Integer requestId, Integer notebookId, String sysUserId) {
        SampleRetrievalRequest request = get(requestId);
        if (request == null) {
            throw new IllegalArgumentException("Retrieval request not found: " + requestId);
        }
        if (!request.isApproved()) {
            throw new IllegalStateException(
                    "Request must be approved before linking samples. Current status: " + request.getStatus());
        }

        // Link items that have been retrieved from storage or released to requester
        List<Integer> sampleItemIds = request.getItems().stream()
                .filter(item -> item.getStatus() == SampleRetrievalItem.ItemStatus.RETRIEVED
                        || item.getStatus() == SampleRetrievalItem.ItemStatus.IN_ANALYSIS)
                .map(item -> item.getBioSample().getSampleItem().getId()).filter(id -> id != null).map(Integer::valueOf)
                .collect(java.util.stream.Collectors.toList());

        if (sampleItemIds.isEmpty()) {
            return 0;
        }

        return notebookSampleEntryService.linkSamplesToNotebook(notebookId, sampleItemIds);
    }

    /**
     * Resolves quantity available for fulfillment attach/retrieve. Stored biorepository
     * specimens may not have remainingQuantity populated; use requested qty or 1 unit when
     * the sample has an active storage assignment.
     */
    private BigDecimal resolveFulfillmentAvailableQuantity(SampleItem sampleItem, BigDecimal quantityHint) {
        if (sampleItem == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal available = sampleItem.getEffectiveRemainingQuantity();
        if (available != null && available.compareTo(BigDecimal.ZERO) > 0) {
            return available;
        }
        if (quantityHint != null && quantityHint.compareTo(BigDecimal.ZERO) > 0) {
            return quantityHint;
        }
        if (sampleItem.getQuantity() != null && sampleItem.getQuantity() > 0) {
            return BigDecimal.valueOf(sampleItem.getQuantity());
        }
        Map<String, Object> location = sampleStorageService.getSampleItemLocation(sampleItem.getId());
        boolean hasStorage =
                location != null
                        && !location.isEmpty()
                        && (location.get("location") != null || location.get("hierarchicalPath") != null);
        if (hasStorage) {
            return BigDecimal.ONE;
        }
        return BigDecimal.ZERO;
    }

    private boolean isAvailableForFulfillmentAttach(BioSample bioSample) {
        if (bioSample == null) {
            return false;
        }
        if (WorkflowStatus.STORED.equals(bioSample.getWorkflowStatus())) {
            return true;
        }
        if (bioSample.getSampleItem() == null || bioSample.getSampleItem().getId() == null) {
            return false;
        }
        Map<String, Object> location = sampleStorageService.getSampleItemLocation(bioSample.getSampleItem().getId());
        return location != null
                && !location.isEmpty()
                && (location.get("location") != null || location.get("hierarchicalPath") != null);
    }
}
