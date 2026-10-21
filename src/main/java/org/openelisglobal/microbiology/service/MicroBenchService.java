package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.microbiology.form.*;

public interface MicroBenchService {
    MicroBenchPageForm search(MicroBenchQueryForm query, String actor);

    List<MicroBenchPageForm.Row> selectedCases(String labUnitId, List<String> caseIds, String actor);

    List<MicroBenchResultForm> apply(MicroBenchRequestForm request, String actor);
}
