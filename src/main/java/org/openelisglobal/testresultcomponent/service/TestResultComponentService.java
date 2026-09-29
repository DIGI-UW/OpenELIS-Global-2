package org.openelisglobal.testresultcomponent.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.common.security.CrudPrivileges;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.testresultcomponent.valueholder.TestResultComponent;
import org.openelisglobal.testresultinterpretation.valueholder.TestResultInterpretation;
import org.springframework.security.access.prepost.PreAuthorize;

// A test's result COMPONENTS are catalogue configuration: the fields a test
// reports, with their codes, labels, order, result type, unit and precision.
// Result entry, validation, reports and the FHIR transform all read them to know
// what to render; gating the reads on PRIV_TEST_CONFIGURE denied the Results role
// its own workbench. The reads below accept PRIV_CATALOGUE_VIEW; every write
// keeps PRIV_TEST_CONFIGURE, pinned individually and via @CrudPrivileges so the
// inherited insert/update/delete do not fall back to the widened read gate.
@PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
@CrudPrivileges(write = "PRIV_TEST_CONFIGURE")
public interface TestResultComponentService extends BaseObjectService<TestResultComponent, String> {

    @PreAuthorize("hasAnyAuthority('PRIV_TEST_CONFIGURE','PRIV_CATALOGUE_VIEW')")
    List<TestResultComponent> getComponentsByTestId(String testId);

    @PreAuthorize("hasAnyAuthority('PRIV_TEST_CONFIGURE','PRIV_CATALOGUE_VIEW')")
    List<TestResultComponent> getActiveComponentsByTestId(String testId);

    @PreAuthorize("hasAnyAuthority('PRIV_TEST_CONFIGURE','PRIV_CATALOGUE_VIEW')")
    TestResultComponent getByTestIdAndCode(String testId, String code);

    /**
     * Reconciles a test's active components to the desired set: a desired component
     * whose id matches an existing active row is updated in place; one without a
     * matching id is inserted; an existing active component absent from the desired
     * set is soft-deleted (is_active='N'). Returns the resulting active list.
     */
    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    List<TestResultComponent> saveComponentsForTest(String testId, List<TestResultComponent> desired, String sysUserId);

    /**
     * Atomically persists the Sample &amp; Results config: reconciles the test's
     * components (insert/update/soft-delete), then per component (keyed by code)
     * reconciles its interpretations, all in one transaction. Returns the active
     * components.
     */
    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    List<TestResultComponent> saveSampleResults(String testId, List<TestResultComponent> components,
            Map<String, List<TestResultInterpretation>> interpretationsByComponentCode,
            Map<String, List<TestResult>> optionsByComponentCode, String sysUserId);

    /**
     * Copies the active result components of {@code sourceTestId} (with their
     * options + interpretations) onto {@code targetTestId}, skipping any component
     * whose code already exists on the target. New rows get fresh ids.
     */
    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
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
    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    void syncPrimaryComponentFromLegacy(String testId, String sysUserId);
}
