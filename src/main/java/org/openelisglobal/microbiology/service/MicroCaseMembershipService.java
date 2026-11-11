package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.microbiology.valueholder.MicroCaseRequest;
import org.openelisglobal.microbiology.valueholder.MicroCaseRole;
import org.openelisglobal.microbiology.valueholder.MicroCaseSample;

public interface MicroCaseMembershipService {
    MicroCaseRequest ownRequest(String caseId, Integer requestId, String testId, MicroCaseRole role,
            boolean collectedInSets, String actor);

    MicroCaseRequest fulfillRequest(String ownershipId, String sampleItemId, String analysisId, String actor);

    void cancelRequest(String ownershipId, String reason, String actor);

    MicroCaseSample addSample(String caseId, String sampleItemId, String actor);

    List<MicroCaseSample> getCaseSamples(String caseId);

    List<MicroCaseRequest> getCaseRequests(String caseId);

    List<String> getRelatedCaseIds(String caseId);
}
