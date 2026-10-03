package org.openelisglobal.microbiology.service;

import org.openelisglobal.microbiology.form.MicroCaseNonconformanceRequestForm;

public interface MicroCaseNonconformanceService {

    MicroCaseNonconformanceResult report(String caseId, MicroCaseNonconformanceRequestForm request,
            String authenticatedUserId);
}
