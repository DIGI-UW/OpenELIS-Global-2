package org.openelisglobal.testsamplehandling.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.testsamplehandling.valueholder.TestSampleHandlingHistory;

public interface TestSampleHandlingHistoryDAO extends BaseDAO<TestSampleHandlingHistory, String> {

    /** Audit rows for a handling record, newest first. */
    List<TestSampleHandlingHistory> getByHandlingId(String handlingId);
}
