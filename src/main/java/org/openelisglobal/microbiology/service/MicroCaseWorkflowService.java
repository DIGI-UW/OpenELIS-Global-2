package org.openelisglobal.microbiology.service;

import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroWorkflowType;

public interface MicroCaseWorkflowService {

    MicroCase changeWorkflow(String caseId, MicroWorkflowType workflowType, String cultureMethodId, String reason,
            boolean preserveExistingWorkConfirmed, String performedBy);

    boolean requiresPreservationConfirmation(String caseId);
}
