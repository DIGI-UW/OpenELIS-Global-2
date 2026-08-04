package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.microbiology.valueholder.MicroIsolate;
import org.openelisglobal.microbiology.valueholder.MicroIsolateIdentificationEvent;

public interface MicroIdentificationHistoryService {

    MicroIsolateIdentificationEvent recordChange(MicroIsolate previous, MicroIsolate updated, String reason,
            String performedBy);

    void revertAmendment(String amendmentId, String reason, String performedBy);

    List<MicroIsolateIdentificationEvent> getHistory(String isolateId);
}
