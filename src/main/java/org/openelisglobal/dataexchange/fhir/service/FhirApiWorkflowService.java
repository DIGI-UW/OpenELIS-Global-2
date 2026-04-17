package org.openelisglobal.dataexchange.fhir.service;

import org.hl7.fhir.r4.model.ResourceType;
import org.openelisglobal.common.service.CrossDomainService;

@CrossDomainService(callers = "FHIR data exchange pipeline — internal infrastructure")
public interface FhirApiWorkflowService {

    void processWorkflow(ResourceType resourceType);

    void pollForRemoteTasks();
}
