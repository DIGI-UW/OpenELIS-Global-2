package org.openelisglobal.resultlimit.service;

import java.util.List;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.resultlimit.valueholder.ComplianceEvaluation;
import org.openelisglobal.resultlimits.valueholder.ResultLimit;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ResultLimitService extends BaseObjectService<ResultLimit, String> {

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    List<ResultLimit> getAllResultLimits() throws LIMSRuntimeException;

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    List<ResultLimit> getPageOfResultLimits(int startingRecNo) throws LIMSRuntimeException;

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    void getData(ResultLimit resultLimit) throws LIMSRuntimeException;

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<ResultLimit> getAllResultLimitsForTest(String testId) throws LIMSRuntimeException;

    /** OGC-949 M7: reference ranges scoped to a result component. */
    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<ResultLimit> getResultLimitsByComponentId(String componentId);

    /**
     * OGC-949 M7: atomically replace a test's reference ranges with {@code desired}
     * (diff-save). Rows with an id that still exist are updated in place; rows
     * without an id are inserted (numeric result-type FK resolved here); existing
     * rows absent from {@code desired} are deleted. Runs in one transaction so a
     * partial failure rolls the whole set back.
     */
    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    void saveRangesForTest(String testId, List<ResultLimit> desired, String sysUserId);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    ResultLimit getResultLimitById(String resultLimitId) throws LIMSRuntimeException;

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    String getDisplayAgeRange(ResultLimit resultLimit, String separator);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    String getDisplayValidRange(ResultLimit resultLimit, String significantDigits, String separator);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    String getDisplayReportingRange(ResultLimit resultLimit, String significantDigits, String separator);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    String getDisplayCriticalRange(ResultLimit resultLimit, String significantDigits, String separator);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    String getDisplayReferenceRange(ResultLimit resultLimit, String significantDigits, String separator);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    String getDisplayNormalRange(double low, double high, String significantDigits, String separator);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    ResultLimit getResultLimitForTestAndPatient(String testId, Patient patient);

    /**
     * OGC-1145 Phase 2 — specimen-aware selection: limits scoped to
     * {@code sampleTypeId} win over shared (null-scope) rows; null sample type
     * evaluates against the shared set.
     */
    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    ResultLimit getResultLimitForTestAndPatient(String testId, Patient patient, String sampleTypeId);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    ResultLimit getResultLimitForTestAndPatient(Test test, Patient patient);

    /**
     * OGC-1127/OGC-949 — the reference range for a specific result component,
     * chosen for the patient's age/gender exactly as the test-level selection does
     * but scoped to the component's own limits. Returns null when the component has
     * no matching range.
     */
    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    ResultLimit getResultLimitForComponentAndPatient(String componentId, Patient patient);

    /** Specimen-aware variant of the component selection (OGC-1145 Phase 2). */
    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    ResultLimit getResultLimitForComponentAndPatient(String componentId, Patient patient, String sampleTypeId);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<IdValuePair> getPredefinedAgeRanges();

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<ResultLimit> getResultLimits(String testId);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<ResultLimit> getResultLimits(Test test);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    ResultLimit getResultLimitForAnalysis(Analysis analysis);

    List<ComplianceEvaluation> getComplianceResultsForAnalysis(Analysis analysis);

    List<ComplianceEvaluation> getComplianceResultsForAnalysis(Analysis analysis, String resultValue);

    /**
     * The reference range for one displayed result row: a multi-component test uses
     * the range of the component the result belongs to, any other test uses the
     * test-level range, and both are chosen for the patient's age/gender and scoped
     * to the analysis's specimen.
     *
     * <p>
     * This is the single selection behind both Results Entry and Validation, so the
     * two screens cannot show different ranges for the same result.
     */
    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    ResultLimit getResultLimitForResult(Analysis analysis, Result result, Patient patient);

    /**
     * As above, but for a caller that already knows which component it is rendering
     * — a results screen lays out one row per component, including components with
     * no result recorded yet, and each row must show its own component's range.
     * {@code componentId} wins when set; otherwise the component is derived from
     * the result.
     */
    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    ResultLimit getResultLimitForResult(Analysis analysis, Result result, Patient patient, String componentId);
}
