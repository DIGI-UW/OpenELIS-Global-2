package org.openelisglobal.microbiology.service;

import org.openelisglobal.microbiology.form.MicroWhonetExportQueryForm;
import org.openelisglobal.microbiology.form.MicroWhonetFilterOptionsForm;

public interface MicroWhonetDatasetService {

    MicroWhonetDataset compile(MicroWhonetExportQueryForm query);

    MicroWhonetFilterOptionsForm getFilterOptions(MicroWhonetExportQueryForm query);
}
