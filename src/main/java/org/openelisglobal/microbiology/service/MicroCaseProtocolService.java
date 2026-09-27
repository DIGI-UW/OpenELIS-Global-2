package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.microbiology.form.MicroCaseProtocolOptionForm;
import org.openelisglobal.microbiology.valueholder.MicroCase;

public interface MicroCaseProtocolService {

    List<MicroCaseProtocolOptionForm> getProtocolOptions(String caseId);

    MicroCase changeProtocol(String caseId, String cultureMethodId, String reason, String performedBy);
}
