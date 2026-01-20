package org.openelisglobal.dataexchange.fhir.service;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.ResourceType;
import org.openelisglobal.dataexchange.fhir.form.TaskOrderProcessingSummaryForm;

public interface FhirApiWorkflowService {

	void processWorkflow(ResourceType resourceType);

	void pollForRemoteTasks();

	TaskOrderProcessingSummaryForm processIncomingOrderBundle(Bundle bundle);

}
