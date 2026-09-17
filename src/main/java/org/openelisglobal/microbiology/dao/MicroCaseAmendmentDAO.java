package org.openelisglobal.microbiology.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroCaseAmendment;

public interface MicroCaseAmendmentDAO extends BaseDAO<MicroCaseAmendment, String> {

    MicroCaseAmendment getOpenByCaseId(String caseId);

    List<MicroCaseAmendment> getByCaseId(String caseId);

    int getNextSequence(String caseId);
}
