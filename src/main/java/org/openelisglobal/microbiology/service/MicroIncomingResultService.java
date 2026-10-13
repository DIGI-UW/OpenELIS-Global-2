package org.openelisglobal.microbiology.service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.openelisglobal.microbiology.form.*;

public interface MicroIncomingResultService {
    List<MicroIncomingForm> list(String caseId, String actor);

    MicroIncomingForm receive(String caseId, String sampleItemId, String testId, String source, String sourceKey,
            String raw, MicroCaseResultRequestForm result, boolean exactPlacement, String actor);

    MicroIncomingForm fail(String caseId, String id, String reason, String actor);

    MicroIncomingForm place(String caseId, String id, MicroIncomingPlacementForm request, String actor,
            HttpServletRequest http);
}
