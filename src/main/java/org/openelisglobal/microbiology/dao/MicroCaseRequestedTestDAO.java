package org.openelisglobal.microbiology.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroCaseRequestedTest;

public interface MicroCaseRequestedTestDAO extends BaseDAO<MicroCaseRequestedTest, String> {
    MicroCaseRequestedTest getByRequestAndTest(Integer requestId, String testId);

    List<MicroCaseRequestedTest> getByCaseId(String caseId);
}
