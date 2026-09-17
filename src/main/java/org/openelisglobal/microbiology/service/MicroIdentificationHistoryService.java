package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.microbiology.valueholder.MicroIsolate;
import org.openelisglobal.microbiology.valueholder.MicroIsolateIdentificationEvent;
import org.springframework.security.access.prepost.PreAuthorize;

public interface MicroIdentificationHistoryService {

    @PreAuthorize("hasAuthority('PRIV_MICRO_BENCH')")
    MicroIsolateIdentificationEvent recordChange(MicroIsolate previous, MicroIsolate updated, String reason,
            String performedBy);

    @PreAuthorize("hasAuthority('PRIV_MICRO_SUPERVISE')")
    void revertAmendment(String amendmentId, String reason, String performedBy);

    @PreAuthorize("hasAuthority('PRIV_MICRO_VIEW')")
    List<MicroIsolateIdentificationEvent> getHistory(String isolateId);
}
