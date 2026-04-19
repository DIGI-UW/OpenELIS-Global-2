package org.openelisglobal.sampletyperequest.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.sampletyperequest.valueholder.SampleTypeRequest;

public interface SampleTypeRequestDAO extends BaseDAO<SampleTypeRequest, Integer> {

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
}
