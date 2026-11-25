package org.openelisglobal.microbiology.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroCaseRequest;
import org.openelisglobal.sampletyperequest.valueholder.SampleTypeRequest;

public interface MicroCaseRequestDAO extends BaseDAO<MicroCaseRequest, String> {

    MicroCaseRequest getActiveByRequestAndTest(Integer requestId, String testId);

    List<MicroCaseRequest> getByCaseId(String caseId);

    List<MicroCaseRequest> getBySampleItemId(String sampleItemId);

    SampleTypeRequest lockSampleTypeRequest(Integer requestId);
}
