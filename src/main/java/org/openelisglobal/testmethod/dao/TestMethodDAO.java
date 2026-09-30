package org.openelisglobal.testmethod.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.testmethod.valueholder.TestMethod;

public interface TestMethodDAO extends BaseDAO<TestMethod, String> {

    List<TestMethod> getActiveTestMethodsByTestId(String testId);

    boolean testMethodLinkExists(String testId, String methodId);

    void clearDefaultsForTest(String testId, String sysUserId);

    void updateIsDefaultAndEffectiveDate(String id, boolean isDefault, java.sql.Date effectiveDate);

    void deactivateLink(String id);
}
