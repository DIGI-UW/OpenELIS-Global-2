package org.openelisglobal.microbiology.service;

import org.openelisglobal.microbiology.valueholder.MicroCase;

public interface MicroReportReleaseService {

    MicroCase releasePreliminary(String caseId, String performedBy);

    MicroCase releaseFinal(String caseId, String performedBy, java.util.List<String> confirmedIncomingIds);

    MicroCase releaseFinal(String caseId, String performedBy);

    MicroCase releaseAmended(String caseId, String performedBy, java.util.List<String> confirmedIncomingIds);

    MicroCase releaseAmended(String caseId, String performedBy);
}
