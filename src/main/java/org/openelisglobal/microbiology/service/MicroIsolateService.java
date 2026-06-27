package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.microbiology.valueholder.MicroIsolate;
import org.openelisglobal.microbiology.valueholder.MicroIsolateIdentificationStatus;
import org.openelisglobal.microbiology.valueholder.MicroIsolateSignificance;

public interface MicroIsolateService {

    MicroIsolate createIsolate(String caseId, String isolateLabel, String organismId, String preliminaryOrganismText,
            MicroIsolateSignificance significance, String performedBy);

    MicroIsolate updateIdentification(String isolateId, String organismId, String preliminaryOrganismText,
            MicroIsolateSignificance significance, MicroIsolateIdentificationStatus identificationStatus,
            String performedBy);

    List<MicroIsolate> getIsolatesForCase(String caseId);
}
