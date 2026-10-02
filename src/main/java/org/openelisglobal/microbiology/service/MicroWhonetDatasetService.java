package org.openelisglobal.microbiology.service;

import org.openelisglobal.microbiology.form.MicroWhonetExportQueryForm;
import org.openelisglobal.microbiology.form.MicroWhonetFilterOptionsForm;
import org.springframework.security.access.prepost.PreAuthorize;

public interface MicroWhonetDatasetService {

    @PreAuthorize("hasAuthority('PRIV_MICRO_VIEW')")
    MicroWhonetDataset compile(MicroWhonetExportQueryForm query);

    // Also PRIV_REPORT_RUN: the WHONET export page is routed to Reports, which
    // does not hold micro:view; this is the one dataset read its filter step
    // needs.
    @PreAuthorize("hasAnyAuthority('PRIV_MICRO_VIEW','PRIV_REPORT_RUN')")
    MicroWhonetFilterOptionsForm getFilterOptions(MicroWhonetExportQueryForm query);
}
