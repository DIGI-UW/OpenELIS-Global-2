package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.microbiology.valueholder.MicroCaseAmendment;

public interface MicroCaseAmendmentService {

    MicroCaseAmendment openAmendment(String caseId, String reason, String performedBy);

    MicroCaseAmendment completeAmendment(String caseId, MicroReportProjectionResult projection, String performedBy);

    MicroCaseAmendment cancelAmendment(String caseId, String reason, String performedBy);

    MicroCaseAmendment getOpenAmendment(String caseId);

    List<MicroCaseAmendment> getHistory(String caseId);
}
