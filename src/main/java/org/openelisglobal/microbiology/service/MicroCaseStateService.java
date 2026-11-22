package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseStage;

public interface MicroCaseStateService {

    MicroCase advanceStage(String caseId, MicroCaseStage nextStage, String performedBy, String note);

    MicroCase advanceStage(String caseId, MicroCaseStage nextStage, String performedBy, String note,
            List<MicroLotSelection> lotSelections);
}
