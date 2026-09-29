package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseAmendment;
import org.openelisglobal.microbiology.valueholder.MicroReportVersion;
import org.openelisglobal.microbiology.valueholder.MicroReportVersionSource;
import org.springframework.security.access.prepost.PreAuthorize;

public interface MicroReportVersionService {

    @PreAuthorize("hasAuthority('PRIV_MICRO_SUPERVISE')")
    MicroReportVersion ensureFinalBaseline(MicroCase microCase);

    @PreAuthorize("hasAuthority('PRIV_MICRO_SUPERVISE')")
    MicroReportVersion recordInitialFinal(String caseId, MicroReportProjectionResult projection, String performedBy);

    @PreAuthorize("hasAuthority('PRIV_MICRO_SUPERVISE')")
    MicroReportVersion recordAmendedFinal(MicroCaseAmendment amendment, MicroReportProjectionResult projection,
            String performedBy);

    @PreAuthorize("hasAuthority('PRIV_MICRO_VIEW')")
    List<MicroReportVersion> getVersions(String caseId);

    @PreAuthorize("hasAuthority('PRIV_MICRO_VIEW')")
    List<MicroReportVersionSource> getSourcesForCase(String caseId);
}
