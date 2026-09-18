package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.microbiology.form.MicroCaseDetailForm;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroWorkflowType;
import org.springframework.security.access.prepost.PreAuthorize;

public interface MicroCaseService {

    @PreAuthorize("hasAuthority('PRIV_MICRO_BENCH')")
    MicroCase createOrGetCase(String sampleItemId, MicroWorkflowType workflowType, String cultureMethodId,
            String performedBy);

    @PreAuthorize("hasAuthority('PRIV_MICRO_VIEW')")
    MicroCase getCase(String caseId);

    @PreAuthorize("hasAuthority('PRIV_MICRO_VIEW')")
    MicroCase getCaseForSampleItemWorkflow(String sampleItemId, MicroWorkflowType workflowType);

    @PreAuthorize("hasAuthority('PRIV_MICRO_VIEW')")
    List<MicroCase> getSiblingCases(String sampleItemId);

    @PreAuthorize("hasAuthority('PRIV_MICRO_VIEW')")
    MicroCaseDetailForm getCaseDetail(String caseId);
}
