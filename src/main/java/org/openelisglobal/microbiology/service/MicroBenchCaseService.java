package org.openelisglobal.microbiology.service;

import org.openelisglobal.microbiology.form.*;

public interface MicroBenchCaseService {
    MicroBenchResultForm apply(MicroBenchRequestForm request, MicroBenchRequestForm.Selection selection, String actor);

    MicroBenchResultForm undo(String caseId, String actionId, String actor);
}
