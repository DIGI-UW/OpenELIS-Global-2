package org.openelisglobal.microbiology.service;

import org.openelisglobal.microbiology.form.MicroWhonetExportQueryForm;

public interface MicroWhonetDatasetService {

    MicroWhonetDataset compile(MicroWhonetExportQueryForm query);
}
