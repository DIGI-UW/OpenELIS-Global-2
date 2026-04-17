package org.openelisglobal.dataexchange.fhir.service;

import java.util.List;
import java.util.UUID;
import org.hl7.fhir.r4.model.ResourceType;
import org.openelisglobal.common.service.CrossDomainService;
import org.openelisglobal.dataexchange.fhir.service.FhirApiWorkFlowServiceImpl.ReferralResultsImportObjects;

@CrossDomainService(callers = "FHIR data exchange pipeline — internal infrastructure")
public interface FhirApiWorkflowService {

    void processWorkflow(ResourceType resourceType);

    void pollForRemoteTasks();

    /**
     * Live-read the returned results for a single referral from the remote FHIR
     * store(s): the completed Task, its child ServiceRequest, and the
     * Observations/DiagnosticReport. Used by the Accept action (OGC-803) to post
     * results on demand — the poll only advances status, it no longer posts.
     */
    List<ReferralResultsImportObjects> fetchReturnedResults(UUID referralTaskUuid);
}
