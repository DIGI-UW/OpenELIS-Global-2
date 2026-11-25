package org.openelisglobal.microbiology.service;

import org.openelisglobal.microbiology.form.MicroBenchPrintForm;

public interface MicroBenchPrintService {
    MicroBenchPrintForm.SavedSheet print(MicroBenchPrintForm request, String actor);

    byte[] open(String id, String actor);
}
