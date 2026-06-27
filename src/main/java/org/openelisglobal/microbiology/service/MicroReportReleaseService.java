package org.openelisglobal.microbiology.service;

import org.openelisglobal.microbiology.valueholder.MicroCase;

public interface MicroReportReleaseService {

    MicroCase releasePreliminary(String caseId, String performedBy);

    MicroCase releaseFinal(String caseId, String performedBy);
}
