package org.openelisglobal.sampletyperequest.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.sampletyperequest.valueholder.SampleTypeRequest;
import org.springframework.security.access.prepost.PreAuthorize;

public interface SampleTypeRequestService extends BaseObjectService<SampleTypeRequest, Integer> {

    /**
     * Get all sample type requests for a given sample.
     */
    @PreAuthorize("hasAuthority('PRIV_ORDER_VIEW')")
    List<SampleTypeRequest> getRequestsBySampleId(String sampleId);

    /**
     * Get pending (not yet collected) requests for a sample.
     */
    @PreAuthorize("hasAuthority('PRIV_ORDER_VIEW')")
    List<SampleTypeRequest> getPendingRequestsBySampleId(String sampleId);

    /**
     * Get fulfilled (collected) requests for a sample.
     */
    @PreAuthorize("hasAuthority('PRIV_ORDER_VIEW')")
    List<SampleTypeRequest> getFulfilledRequestsBySampleId(String sampleId);

    /**
     * Mark a request as fulfilled by linking it to a collected sample_item.
     */
    @PreAuthorize("hasAuthority('PRIV_ORDER_CREATE')")
    void fulfillRequest(Integer requestId, String sampleItemId);

    /**
     * Cancel a pending request.
     */
    @PreAuthorize("hasAuthority('PRIV_ORDER_CREATE')")
    void cancelRequest(Integer requestId);
}
