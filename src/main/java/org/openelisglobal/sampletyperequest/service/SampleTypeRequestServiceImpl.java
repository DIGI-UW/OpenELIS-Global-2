package org.openelisglobal.sampletyperequest.service;

import java.util.List;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.sampletyperequest.dao.SampleTypeRequestDAO;
import org.openelisglobal.sampletyperequest.valueholder.SampleTypeRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SampleTypeRequestServiceImpl extends AuditableBaseObjectServiceImpl<SampleTypeRequest, Integer>
        implements SampleTypeRequestService {

    @Autowired
    private SampleTypeRequestDAO sampleTypeRequestDAO;

    @Autowired
    private SampleItemService sampleItemService;

    public SampleTypeRequestServiceImpl() {
        super(SampleTypeRequest.class);
    }

    @Override
    protected SampleTypeRequestDAO getBaseObjectDAO() {
        return sampleTypeRequestDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleTypeRequest> getRequestsBySampleId(String sampleId) {
        List<SampleTypeRequest> requests = sampleTypeRequestDAO.getRequestsBySampleId(sampleId);
        // Initialize lazy-loaded associations within transaction
        for (SampleTypeRequest request : requests) {
            initializeLazyAssociations(request);
        }
        return requests;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleTypeRequest> getPendingRequestsBySampleId(String sampleId) {
        List<SampleTypeRequest> requests = sampleTypeRequestDAO.getPendingRequestsBySampleId(sampleId);
        // Initialize lazy-loaded associations within transaction
        for (SampleTypeRequest request : requests) {
            initializeLazyAssociations(request);
        }
        return requests;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleTypeRequest> getFulfilledRequestsBySampleId(String sampleId) {
        List<SampleTypeRequest> requests = sampleTypeRequestDAO.getFulfilledRequestsBySampleId(sampleId);
        // Initialize lazy-loaded associations within transaction
        for (SampleTypeRequest request : requests) {
            initializeLazyAssociations(request);
        }
        return requests;
    }

    /**
     * Initialize lazy-loaded associations to prevent LazyInitializationException
     * when converting to DTO outside of transaction.
     */
    private void initializeLazyAssociations(SampleTypeRequest request) {
        if (request.getSample() != null) {
            request.getSample().getId(); // Force load
        }
        if (request.getTypeOfSample() != null) {
            request.getTypeOfSample().getLocalizedName(); // Force load
        }
        if (request.getUnitOfMeasure() != null) {
            request.getUnitOfMeasure().getUnitOfMeasureName(); // Force load
        }
        if (request.getSampleItem() != null) {
            request.getSampleItem().getId(); // Force load
        }
    }

    @Override
    @Transactional
    public void fulfillRequest(Integer requestId, String sampleItemId) {
        SampleTypeRequest request = get(requestId);
        if (request == null) {
            throw new IllegalArgumentException("SampleTypeRequest not found: " + requestId);
        }
        if (request.getStatus() != SampleTypeRequest.Status.REQUESTED) {
            throw new IllegalStateException("Cannot fulfill request in status: " + request.getStatus());
        }

        SampleItem sampleItem = sampleItemService.get(sampleItemId);
        if (sampleItem == null) {
            throw new IllegalArgumentException("SampleItem not found: " + sampleItemId);
        }

        request.setStatus(SampleTypeRequest.Status.COLLECTED);
        request.setSampleItem(sampleItem);
        update(request);
    }

    @Override
    @Transactional
    public void cancelRequest(Integer requestId) {
        SampleTypeRequest request = get(requestId);
        if (request == null) {
            throw new IllegalArgumentException("SampleTypeRequest not found: " + requestId);
        }
        if (request.getStatus() != SampleTypeRequest.Status.REQUESTED) {
            throw new IllegalStateException("Cannot cancel request in status: " + request.getStatus());
        }

        request.setStatus(SampleTypeRequest.Status.CANCELLED);
        update(request);
    }
}
