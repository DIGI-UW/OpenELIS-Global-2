package org.openelisglobal.microbiology.service;

import org.openelisglobal.microbiology.form.MicroWhonetExportQueryForm;
import org.openelisglobal.microbiology.form.MicroWhonetPreviewForm;

public interface MicroWhonetDatasetService {

    MicroWhonetPreviewForm preview(MicroWhonetExportQueryForm query);

    MicroWhonetDataset compile(MicroWhonetExportQueryForm query);
}
