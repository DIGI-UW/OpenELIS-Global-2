package org.openelisglobal.biorepository.service;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.biorepository.dao.SampleTransferRequestDAO;
import org.openelisglobal.biorepository.valueholder.BioSample;
import org.openelisglobal.biorepository.valueholder.SampleTransferItem;
import org.openelisglobal.biorepository.valueholder.SampleTransferItem.ItemStatus;
import org.openelisglobal.biorepository.valueholder.SampleTransferRequest;
import org.openelisglobal.biorepository.valueholder.SampleTransferRequest.TransferStatus;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for SampleTransferRequest operations.
 */
@Service
public class SampleTransferServiceImpl extends AuditableBaseObjectServiceImpl<SampleTransferRequest, Integer>
        implements SampleTransferService {

    @Autowired
    protected SampleTransferRequestDAO baseObjectDAO;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private BioSampleService bioSampleService;

    @Autowired
    private SystemUserService systemUserService;

    SampleTransferServiceImpl() {
        super(SampleTransferRequest.class);
    }

    @Override
    protected SampleTransferRequestDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional
    public SampleTransferRequest createTransferRequest(String sourceLab, List<Integer> sampleItemIds,
            String requestNotes, String sysUserId) {

        if (sourceLab == null || sourceLab.trim().isEmpty()) {
            throw new IllegalArgumentException("Source lab is required");
        }
        if (sampleItemIds == null || sampleItemIds.isEmpty()) {
            throw new IllegalArgumentException("At least one sample item ID is required");
        }

        SystemUser requestingUser = systemUserService.get(sysUserId);
        if (requestingUser == null) {
            throw new IllegalArgumentException("User not found: " + sysUserId);
        }

        SampleTransferRequest request = new SampleTransferRequest();
        request.setSourceLab(sourceLab.trim());
        request.setDestinationLab("BIOREPOSITORY");
        request.setStatus(TransferStatus.PENDING);
        request.setRequestedBy(requestingUser);
        request.setRequestedTimestamp(new Timestamp(System.currentTimeMillis()));
        request.setRequestNotes(requestNotes);
        request.setSysUserId(sysUserId);

        for (Integer sampleItemId : sampleItemIds) {
            SampleItem sampleItem;
            try {
                sampleItem = sampleItemService.get(String.valueOf(sampleItemId));
            } catch (RuntimeException e) {
                throw new IllegalArgumentException("Sample item not found: " + sampleItemId, e);
            }
            if (sampleItem == null) {
                throw new IllegalArgumentException("Sample item not found: " + sampleItemId);
            }

            if (baseObjectDAO.hasPendingTransferForSampleItem(sampleItemId)) {
                throw new IllegalStateException("Sample item already has a pending transfer: " + sampleItemId);
            }

            if (bioSampleService.existsBySampleItemId(sampleItemId)) {
                throw new IllegalStateException("Sample item already exists in biorepository: " + sampleItemId);
            }

            SampleTransferItem item = new SampleTransferItem();
            item.setSampleItem(sampleItem);
            item.setStatus(ItemStatus.PENDING);
            item.setSysUserId(sysUserId);
            request.addItem(item);
        }

        return save(request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleTransferRequest> getPendingRequests(int limit) {
        return baseObjectDAO.getPendingRequests(limit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleTransferRequest> getByStatus(TransferStatus status) {
        return baseObjectDAO.getByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleTransferRequest> getBySourceLab(String sourceLab) {
        return baseObjectDAO.getBySourceLab(sourceLab);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleTransferRequest> getBySampleItemId(Integer sampleItemId) {
        return baseObjectDAO.getBySampleItemId(sampleItemId);
    }

    @Override
    @Transactional
    public SampleTransferItem acceptItem(Integer transferItemId, BioSample bioSample, String sysUserId) {
        SampleTransferRequest request = findRequestByItemId(transferItemId);
        if (request == null) {
            throw new IllegalArgumentException("Transfer item not found: " + transferItemId);
        }

        SampleTransferItem item = request.getItems().stream().filter(i -> i.getId().equals(transferItemId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Transfer item not found: " + transferItemId));

        if (!item.isPending()) {
            throw new IllegalStateException("Transfer item is not pending: " + transferItemId);
        }

        bioSample.setSysUserId(sysUserId);
        BioSample createdBioSample = bioSampleService.createForSampleItem(item.getSampleItem(), bioSample);

        item.setStatus(ItemStatus.ACCEPTED);
        item.setBioSample(createdBioSample);

        updateRequestStatus(request, sysUserId);
        update(request);

        return item;
    }

    @Override
    @Transactional
    public SampleTransferItem rejectItem(Integer transferItemId, String rejectionReason, String sysUserId) {
        SampleTransferRequest request = findRequestByItemId(transferItemId);
        if (request == null) {
            throw new IllegalArgumentException("Transfer item not found: " + transferItemId);
        }

        SampleTransferItem item = request.getItems().stream().filter(i -> i.getId().equals(transferItemId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Transfer item not found: " + transferItemId));

        if (!item.isPending()) {
            throw new IllegalStateException("Transfer item is not pending: " + transferItemId);
        }

        item.setStatus(ItemStatus.REJECTED);
        item.setRejectionReason(rejectionReason);

        updateRequestStatus(request, sysUserId);
        update(request);

        return item;
    }

    @Override
    @Transactional
    public SampleTransferRequest acceptAll(Integer transferRequestId, BioSample bioSampleTemplate, String sysUserId) {
        SampleTransferRequest request = get(transferRequestId);
        if (request == null) {
            throw new IllegalArgumentException("Transfer request not found: " + transferRequestId);
        }

        List<SampleTransferItem> pendingItems = request.getItems().stream().filter(SampleTransferItem::isPending)
                .toList();

        if (pendingItems.isEmpty()) {
            throw new IllegalStateException("No pending items to accept");
        }

        for (SampleTransferItem item : pendingItems) {
            BioSample bioSample = createBioSampleFromTemplate(bioSampleTemplate);
            bioSample.setSysUserId(sysUserId);
            BioSample createdBioSample = bioSampleService.createForSampleItem(item.getSampleItem(), bioSample);

            item.setStatus(ItemStatus.ACCEPTED);
            item.setBioSample(createdBioSample);
        }

        updateRequestStatus(request, sysUserId);
        return update(request);
    }

    @Override
    @Transactional
    public SampleTransferRequest rejectAll(Integer transferRequestId, String rejectionReason, String sysUserId) {
        SampleTransferRequest request = get(transferRequestId);
        if (request == null) {
            throw new IllegalArgumentException("Transfer request not found: " + transferRequestId);
        }

        List<SampleTransferItem> pendingItems = request.getItems().stream().filter(SampleTransferItem::isPending)
                .toList();

        if (pendingItems.isEmpty()) {
            throw new IllegalStateException("No pending items to reject");
        }

        for (SampleTransferItem item : pendingItems) {
            item.setStatus(ItemStatus.REJECTED);
            item.setRejectionReason(rejectionReason);
        }

        updateRequestStatus(request, sysUserId);
        request.setRejectionReason(rejectionReason);
        return update(request);
    }

    @Override
    @Transactional
    public SampleTransferRequest cancelRequest(Integer transferRequestId, String sysUserId) {
        SampleTransferRequest request = get(transferRequestId);
        if (request == null) {
            throw new IllegalArgumentException("Transfer request not found: " + transferRequestId);
        }

        if (request.isProcessed()) {
            throw new IllegalStateException("Cannot cancel a processed transfer request");
        }

        request.setStatus(TransferStatus.CANCELLED);
        request.setProcessedTimestamp(new Timestamp(System.currentTimeMillis()));

        for (SampleTransferItem item : request.getItems()) {
            if (item.isPending()) {
                item.setStatus(ItemStatus.REJECTED);
                item.setRejectionReason("Transfer request cancelled");
            }
        }

        return update(request);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasPendingTransfer(Integer sampleItemId) {
        return baseObjectDAO.hasPendingTransferForSampleItem(sampleItemId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByStatus(TransferStatus status) {
        return baseObjectDAO.countByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getDistinctSourceLabs() {
        return baseObjectDAO.getDistinctSourceLabs();
    }

    @Override
    @Transactional(readOnly = true)
    public SampleTransferItem getTransferItem(Integer transferItemId) {
        List<SampleTransferRequest> requests = getAll();
        for (SampleTransferRequest request : requests) {
            for (SampleTransferItem item : request.getItems()) {
                if (item.getId().equals(transferItemId)) {
                    return item;
                }
            }
        }
        return null;
    }

    private SampleTransferRequest findRequestByItemId(Integer transferItemId) {
        List<SampleTransferRequest> requests = getAll();
        for (SampleTransferRequest request : requests) {
            for (SampleTransferItem item : request.getItems()) {
                if (item.getId().equals(transferItemId)) {
                    return request;
                }
            }
        }
        return null;
    }

    private void updateRequestStatus(SampleTransferRequest request, String sysUserId) {
        long pendingCount = request.getItems().stream().filter(SampleTransferItem::isPending).count();
        long acceptedCount = request.getAcceptedItemCount();
        long rejectedCount = request.getRejectedItemCount();
        int totalCount = request.getTotalItemCount();

        if (pendingCount == 0) {
            SystemUser processedByUser = systemUserService.get(sysUserId);
            request.setProcessedBy(processedByUser);
            request.setProcessedTimestamp(new Timestamp(System.currentTimeMillis()));

            if (acceptedCount == totalCount) {
                request.setStatus(TransferStatus.ACCEPTED);
            } else if (rejectedCount == totalCount) {
                request.setStatus(TransferStatus.REJECTED);
            } else {
                request.setStatus(TransferStatus.PARTIALLY_ACCEPTED);
            }
        }
    }

    private BioSample createBioSampleFromTemplate(BioSample template) {
        BioSample bioSample = new BioSample();
        if (template != null) {
            bioSample.setBiosafetyLevel(template.getBiosafetyLevel());
            bioSample.setEthicsApprovalRef(template.getEthicsApprovalRef());
            bioSample.setMtaReference(template.getMtaReference());
            bioSample.setSpecialHandling(template.getSpecialHandling());
            bioSample.setRequiredTempMin(template.getRequiredTempMin());
            bioSample.setRequiredTempMax(template.getRequiredTempMax());
            bioSample.setPrincipalInvestigator(template.getPrincipalInvestigator());
        }
        return bioSample;
    }
}
