package org.openelisglobal.dataexchange.fhir.service;

import org.hl7.fhir.r4.model.ResourceType;

public interface FhirApiWorkflowService {

    void processWorkflow(ResourceType resourceType);

    void pollForRemoteTasks();
}
