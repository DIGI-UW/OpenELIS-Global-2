package org.openelisglobal.sampletyperequest.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.sampletyperequest.valueholder.SampleTypeRequest;

public interface SampleTypeRequestService extends BaseObjectService<SampleTypeRequest, Integer> {

    /**
     * Get all sample type requests for a given sample.
     */
    List<SampleTypeRequest> getRequestsBySampleId(String sampleId);

    /**
     * Get pending (not yet collected) requests for a sample.
     */
    List<SampleTypeRequest> getPendingRequestsBySampleId(String sampleId);

    /**
     * Get fulfilled (collected) requests for a sample.
     */
    List<SampleTypeRequest> getFulfilledRequestsBySampleId(String sampleId);

    /**
     * Mark a request as fulfilled by linking it to a collected sample_item.
     */
    void fulfillRequest(Integer requestId, String sampleItemId);

    /**
     * Cancel a pending request.
     */
    void cancelRequest(Integer requestId);
}
