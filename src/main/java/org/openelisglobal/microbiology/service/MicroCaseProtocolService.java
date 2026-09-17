package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.microbiology.form.MicroCaseProtocolOptionForm;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.springframework.security.access.prepost.PreAuthorize;

public interface MicroCaseProtocolService {

    @PreAuthorize("hasAuthority('PRIV_MICRO_VIEW')")
    List<MicroCaseProtocolOptionForm> getProtocolOptions(String caseId);

    @PreAuthorize("hasAuthority('PRIV_MICRO_BENCH')")
    MicroCase changeProtocol(String caseId, String cultureMethodId, String reason, String performedBy);
}
