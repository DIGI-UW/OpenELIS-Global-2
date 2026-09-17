package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.microbiology.form.MicroCaseActivityForm;
import org.springframework.security.access.prepost.PreAuthorize;

public interface MicroCaseTimelineService {

    @PreAuthorize("hasAuthority('PRIV_MICRO_VIEW')")
    List<MicroCaseActivityForm> getTimeline(String caseId);

    @PreAuthorize("hasAuthority('PRIV_MICRO_BENCH')")
    MicroCaseActivityForm addNote(String caseId, String text, String performedBy);
}
