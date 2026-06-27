package org.openelisglobal.microbiology.service;

import org.openelisglobal.microbiology.form.MicroCaseReadinessForm;

public interface MicroCaseReadinessService {

    MicroCaseReadinessForm getReadiness(String caseId);
}
