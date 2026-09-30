package org.openelisglobal.microbiology.service;

import org.openelisglobal.microbiology.form.MicroBreakpointImportPreviewForm;

public interface MicroBreakpointImportService {

    MicroBreakpointImportPreviewForm preview(String csv);

    MicroBreakpointImportPreviewForm apply(String previewToken, String actorId);
}
