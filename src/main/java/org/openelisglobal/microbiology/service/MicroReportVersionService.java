package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseAmendment;
import org.openelisglobal.microbiology.valueholder.MicroReportVersion;
import org.openelisglobal.microbiology.valueholder.MicroReportVersionSource;

public interface MicroReportVersionService {

    MicroReportVersion ensureFinalBaseline(MicroCase microCase);

    MicroReportVersion recordInitialFinal(String caseId, MicroReportProjectionResult projection, String performedBy);

    MicroReportVersion recordAmendedFinal(MicroCaseAmendment amendment, MicroReportProjectionResult projection,
            String performedBy);

    List<MicroReportVersion> getVersions(String caseId);

    List<MicroReportVersionSource> getSourcesForCase(String caseId);
}
