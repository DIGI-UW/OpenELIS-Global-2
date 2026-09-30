package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.microbiology.form.MicroCaseDetailForm;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroWorkflowType;

public interface MicroCaseService {

    MicroCase createOrGetCase(String sampleItemId, MicroWorkflowType workflowType, String cultureMethodId,
            String performedBy);

    MicroCase getCase(String caseId);

    MicroCase getCaseForSampleItemWorkflow(String sampleItemId, MicroWorkflowType workflowType);

    List<MicroCase> getSiblingCases(String sampleItemId);

    MicroCaseDetailForm getCaseDetail(String caseId);
}
