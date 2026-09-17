package org.openelisglobal.microbiology.service;

import org.openelisglobal.microbiology.form.MicroBreakpointImportPreviewForm;
import org.springframework.security.access.prepost.PreAuthorize;

public interface MicroBreakpointImportService {

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    MicroBreakpointImportPreviewForm preview(String csv);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    MicroBreakpointImportPreviewForm apply(String previewToken, String actorId);
}
