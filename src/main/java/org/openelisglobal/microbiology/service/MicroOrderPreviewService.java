package org.openelisglobal.microbiology.service;

import org.openelisglobal.microbiology.form.MicroOrderPreviewForm;
import org.openelisglobal.microbiology.form.MicroOrderPreviewRequestForm;

public interface MicroOrderPreviewService {
    MicroOrderPreviewForm preview(MicroOrderPreviewRequestForm request, String userId);
}
