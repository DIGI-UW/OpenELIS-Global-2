package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.microbiology.form.MicroCaseInoculationForm;
import org.openelisglobal.microbiology.valueholder.MicroCaseInoculation;

public interface MicroCaseInoculationService {

    MicroCaseInoculation record(String caseId, String sourceInoculationId, String containerIdentifier, String media,
            String incubation, String atmosphere, List<MicroLotSelection> lotSelections, String performedBy);

    List<MicroCaseInoculationForm> getByCaseId(String caseId);
}
