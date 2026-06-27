package org.openelisglobal.microbiology.service;

import org.openelisglobal.microbiology.form.MicroWhonetReadinessForm;

public interface MicroWhonetReadinessService {

    MicroWhonetReadinessForm getReadiness(String caseId);
}
