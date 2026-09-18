package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseStage;
import org.springframework.security.access.prepost.PreAuthorize;

public interface MicroCaseStateService {

    @PreAuthorize("hasAuthority('PRIV_MICRO_BENCH')")
    MicroCase advanceStage(String caseId, MicroCaseStage nextStage, String performedBy, String note);

    @PreAuthorize("hasAuthority('PRIV_MICRO_BENCH')")
    MicroCase advanceStage(String caseId, MicroCaseStage nextStage, String performedBy, String note,
            List<MicroLotSelection> lotSelections);
}
