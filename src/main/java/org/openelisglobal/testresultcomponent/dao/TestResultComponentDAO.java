package org.openelisglobal.testresultcomponent.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.testresultcomponent.valueholder.TestResultComponent;

public interface TestResultComponentDAO extends BaseDAO<TestResultComponent, String> {

    /** All components for a test (active and inactive), ordered for display. */
    List<TestResultComponent> getComponentsByTestId(String testId);

    /** Active components only, ordered by display order then code. */
    List<TestResultComponent> getActiveComponentsByTestId(String testId);

    /**
     * The component with the given code on a test, or null. Enforces the (test_id,
     * code) uniqueness invariant at the service layer.
     */
    TestResultComponent getByTestIdAndCode(String testId, String code);
}
