package org.openelisglobal.microbiology.service;

import org.openelisglobal.microbiology.form.MicroWhonetExportQueryForm;
import org.openelisglobal.microbiology.form.MicroWhonetFilterOptionsForm;
import org.springframework.security.access.prepost.PreAuthorize;

public interface MicroWhonetDatasetService {

    @PreAuthorize("hasAuthority('PRIV_MICRO_VIEW')")
    MicroWhonetDataset compile(MicroWhonetExportQueryForm query);

    @PreAuthorize("hasAuthority('PRIV_MICRO_VIEW')")
    MicroWhonetFilterOptionsForm getFilterOptions(MicroWhonetExportQueryForm query);
}
