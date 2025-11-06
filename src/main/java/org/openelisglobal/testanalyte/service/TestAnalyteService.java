package org.openelisglobal.testanalyte.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testanalyte.valueholder.TestAnalyte;

public interface TestAnalyteService extends BaseObjectService<TestAnalyte, String> {
    TestAnalyte getData(TestAnalyte testAnalyte);

    List<TestAnalyte> getAllTestAnalytes();

    List<TestAnalyte> getPageOfTestAnalytes(int startingRecNo);

    TestAnalyte getTestAnalyteById(TestAnalyte testAnalyte);

    List<TestAnalyte> getAllTestAnalytesPerTest(Test test);

    List<TestAnalyte> getTestAnalytes(String filter);
}
