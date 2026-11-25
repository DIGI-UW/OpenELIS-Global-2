package org.openelisglobal.testcatalog.dao;

import java.util.Collection;
import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.testcatalog.valueholder.TestQcPolicy;

public interface TestQcPolicyDAO extends BaseDAO<TestQcPolicy, Long> {

    /** The stored policies of the given tests; a test without one is absent. */
    List<TestQcPolicy> getByTestIds(Collection<Long> testIds);
}
