package org.openelisglobal.testsamplehandling.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.testsamplehandling.valueholder.TestSampleHandlingHistory;

public interface TestSampleHandlingHistoryService extends BaseObjectService<TestSampleHandlingHistory, String> {

    List<TestSampleHandlingHistory> getByHandlingId(String handlingId);
}
