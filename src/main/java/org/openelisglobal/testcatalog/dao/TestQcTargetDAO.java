package org.openelisglobal.testcatalog.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.testcatalog.valueholder.TestQcTarget;

public interface TestQcTargetDAO extends BaseDAO<TestQcTarget, String> {

    /**
     * Every target row of a test, active or not, level defaults before lot
     * overrides.
     */
    List<TestQcTarget> getByTestId(String testId);
}
