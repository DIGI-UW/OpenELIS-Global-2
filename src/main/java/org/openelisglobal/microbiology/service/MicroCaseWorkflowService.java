package org.openelisglobal.microbiology.service;

import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroWorkflowType;
import org.springframework.security.access.prepost.PreAuthorize;

public interface MicroCaseWorkflowService {

    @PreAuthorize("hasAuthority('PRIV_MICRO_BENCH')")
    MicroCase changeWorkflow(String caseId, MicroWorkflowType workflowType, String cultureMethodId, String reason,
            boolean preserveExistingWorkConfirmed, String performedBy);

    @PreAuthorize("hasAuthority('PRIV_MICRO_VIEW')")
    boolean requiresPreservationConfirmation(String caseId);
}
