package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.microbiology.form.MicroCaseDetailForm;
import org.openelisglobal.microbiology.valueholder.MicroCase;

public interface MicroCaseService {

    /**
     * Test/UAT provisioning only: returns the first case on the sample item or
     * creates one. No production path opens cases; V2 routing arrives in a later
     * roadmap step.
     */
    MicroCase createOrGetCase(String sampleItemId, String cultureMethodId, String performedBy);

    MicroCase getCase(String caseId);

    List<MicroCase> getSiblingCases(String sampleItemId);

    MicroCaseDetailForm getCaseDetail(String caseId);
}
