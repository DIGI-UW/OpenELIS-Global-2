package org.openelisglobal.microbiology.service;

import java.time.LocalDate;
import java.util.List;
import org.openelisglobal.microbiology.form.MicroWhonetExportQueryForm;
import org.openelisglobal.microbiology.form.MicroWhonetFilterOptionsForm;

public interface MicroWhonetDatasetService {

    MicroWhonetDataset compile(MicroWhonetExportQueryForm query);

    MicroWhonetFilterOptionsForm getFilterOptions(MicroWhonetExportQueryForm query);

    /**
     * First isolates collected in the range, chosen by the export's default rules,
     * with their reportable S/I/R results; no WHONET code mapping is needed.
     */
    List<FirstIsolate> selectFirstIsolates(LocalDate from, LocalDate to);

    record FirstIsolate(String isolateId, String organismId, List<AstResult> results) {
    }

    record AstResult(String antibioticId, String interpretation) {
    }
}
