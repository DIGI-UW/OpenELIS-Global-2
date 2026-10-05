package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.microbiology.valueholder.MicroCaseAmendment;
import org.springframework.security.access.prepost.PreAuthorize;

public interface MicroCaseAmendmentService {

    @PreAuthorize("hasAuthority('PRIV_MICRO_SUPERVISE')")
    MicroCaseAmendment openAmendment(String caseId, String reason, String performedBy);

    @PreAuthorize("hasAuthority('PRIV_MICRO_SUPERVISE')")
    MicroCaseAmendment completeAmendment(String caseId, MicroReportProjectionResult projection, String performedBy);

    @PreAuthorize("hasAuthority('PRIV_MICRO_SUPERVISE')")
    MicroCaseAmendment cancelAmendment(String caseId, String reason, String performedBy);

    @PreAuthorize("hasAuthority('PRIV_MICRO_VIEW')")
    MicroCaseAmendment getOpenAmendment(String caseId);

    @PreAuthorize("hasAuthority('PRIV_MICRO_VIEW')")
    List<MicroCaseAmendment> getHistory(String caseId);
}
