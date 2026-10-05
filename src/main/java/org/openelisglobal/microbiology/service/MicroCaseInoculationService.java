package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.microbiology.form.MicroCaseInoculationForm;
import org.openelisglobal.microbiology.valueholder.MicroCaseInoculation;
import org.springframework.security.access.prepost.PreAuthorize;

public interface MicroCaseInoculationService {

    @PreAuthorize("hasAuthority('PRIV_MICRO_BENCH')")
    MicroCaseInoculation record(String caseId, String sourceInoculationId, String containerIdentifier, String media,
            String incubation, String atmosphere, List<MicroLotSelection> lotSelections, String performedBy);

    @PreAuthorize("hasAuthority('PRIV_MICRO_VIEW')")
    List<MicroCaseInoculationForm> getByCaseId(String caseId);
}
