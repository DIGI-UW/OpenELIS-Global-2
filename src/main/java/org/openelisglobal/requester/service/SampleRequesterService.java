package org.openelisglobal.requester.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.requester.valueholder.SampleRequester;

public interface SampleRequesterService extends BaseObjectService<SampleRequester, String> {

    List<SampleRequester> getRequestersForSampleId(String sampleId);

    List<SampleRequester> getRequestersForRequesterId(String requesterId, String requesterTypeId);
}
