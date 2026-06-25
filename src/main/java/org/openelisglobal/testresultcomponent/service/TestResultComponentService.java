package org.openelisglobal.testresultcomponent.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.testresultcomponent.valueholder.TestResultComponent;
import org.openelisglobal.testresultinterpretation.valueholder.TestResultInterpretation;

public interface TestResultComponentService extends BaseObjectService<TestResultComponent, String> {

    List<TestResultComponent> getComponentsByTestId(String testId);

    List<TestResultComponent> getActiveComponentsByTestId(String testId);

    TestResultComponent getByTestIdAndCode(String testId, String code);

    /**
     * Reconciles a test's active components to the desired set: a desired component
     * whose id matches an existing active row is updated in place; one without a
     * matching id is inserted; an existing active component absent from the desired
     * set is soft-deleted (is_active='N'). Returns the resulting active list.
     */
    List<TestResultComponent> saveComponentsForTest(String testId, List<TestResultComponent> desired, String sysUserId);

    /**
     * Atomically persists the Sample &amp; Results config: reconciles the test's
     * components (insert/update/soft-delete), then per component (keyed by code)
     * reconciles its interpretations — all in one transaction. Returns the active
     * components.
     */
    List<TestResultComponent> saveSampleResults(String testId, List<TestResultComponent> components,
            Map<String, List<TestResultInterpretation>> interpretationsByComponentCode,
            Map<String, List<TestResult>> optionsByComponentCode, String sysUserId);

    /**
     * Copies the active result components of {@code sourceTestId} (with their
     * options + interpretations) onto {@code targetTestId}, skipping any component
     * whose code already exists on the target. New rows get fresh ids.
     */
    void copyComponentsFromTest(String sourceTestId, String targetTestId, String sysUserId);

    /**
     * Reconcile the test's PRIMARY component from its legacy data so edits made on
     * the old Test Add/Modify page surface in the new editor. Creates the PRIMARY
     * component if the test has none (legacy-created tests), then sets its
     * unit-of-measure ({@code test.uom_id}), result type and significant digits
     * (from the test's {@code test_result} rows), and repoints the test's options
     * ({@code test_result}) and ranges ({@code result_limits}) that legacy wrote
     * with a NULL {@code component_id} onto the PRIMARY component. The inverse of
     * the new-editor save; mirrors the M1 backfill, scoped to one test.
     */
    void syncPrimaryComponentFromLegacy(String testId, String sysUserId);
}
