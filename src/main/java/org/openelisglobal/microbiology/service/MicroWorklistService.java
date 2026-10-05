package org.openelisglobal.microbiology.service;

import org.openelisglobal.microbiology.form.MicroWorklistPageForm;
import org.openelisglobal.microbiology.form.MicroWorklistQueryForm;
import org.springframework.security.access.prepost.PreAuthorize;

public interface MicroWorklistService {

    @PreAuthorize("hasAuthority('PRIV_MICRO_VIEW')")
    MicroWorklistPageForm getWorklistPage(MicroWorklistQueryForm query);
}
