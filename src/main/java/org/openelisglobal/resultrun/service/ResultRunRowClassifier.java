package org.openelisglobal.resultrun.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.RuleResultScope;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.resultlimit.service.ResultLimitService;
import org.openelisglobal.resultlimits.valueholder.ResultLimit;
import org.openelisglobal.resultrun.form.ResultRunExceptionKind;
import org.openelisglobal.resultrun.form.ResultRunRowCategory;
import org.openelisglobal.resultrun.form.RunOrderState;
import org.openelisglobal.resultrun.valueholder.ResultRun;
import org.openelisglobal.resultvalidation.util.ValidationSignals;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Sorts a run's rows for review (OGC-1200 FR-E1a): a value is judged against
 * the result limit the Results worklist selects for the row's analysis (its
 * specimen, the component, the patient's sex and age), with the rule the
 * worklist and Validation use, so a run and the worklist flag a value alike. A
 * value its run cannot accept until someone resolves it, a lab number that
 * names no order, a test not on its order or already resulted there, or a code
 * with no mapping, is an exception of the kind the run's Exceptions block lists
 * (FR-E4).
 */
@Component
public class ResultRunRowClassifier {

    /**
     * Codes and answers the import holds for a mapping fix, which Map now resolves:
     * the same ones the analyzer import page links to the mapping.
     */
    static final Set<String> UNMAPPED_ISSUES = Set.of(AnalyzerResults.IMPORT_ISSUE_UNKNOWN_TEST,
            AnalyzerResults.IMPORT_ISSUE_TEST_MAPPING_NOT_READY, AnalyzerResults.IMPORT_ISSUE_UNKNOWN_RESULT_VALUE,
            AnalyzerResults.IMPORT_ISSUE_RESULT_MAPPING_NOT_READY, AnalyzerResults.IMPORT_ISSUE_INVALID_RESULT_MAPPING,
            AnalyzerResults.IMPORT_ISSUE_CONFLICTING_ANSWER_MAPPING, AnalyzerResults.IMPORT_ISSUE_READING_CHANGED,
            AnalyzerResults.IMPORT_ISSUE_TYPE_READING_CHANGED, AnalyzerResults.IMPORT_ISSUE_OTHER_REVISION);

    /** One row with its flag, review category and, for an exception, its kind. */
    public record ClassifiedRow(AnalyzerResults row, String flag, ResultRunRowCategory category,
            ResultRunExceptionKind exceptionKind) {
    }

    @Lazy
    @Autowired
    private SampleService sampleService;
    @Lazy
    @Autowired
    private SampleHumanService sampleHumanService;
    @Lazy
    @Autowired
    private ResultLimitService resultLimitService;
    @Lazy
    @Autowired
    private AnalysisService analysisService;
    @Lazy
    @Autowired
    private RunQcService runQcService;
    @Lazy
    @Autowired
    private IStatusService statusService;
    @Lazy
    @Autowired
    private TestService testService;
    @Lazy
    @Autowired
    private ResultService resultService;
    @Lazy
    @Autowired
    private RuleResultScope ruleResultScope;

    public List<ClassifiedRow> classify(ResultRun run, List<AnalyzerResults> rows) {
        Map<String, Optional<Sample>> samples = new HashMap<>();
        Map<String, Optional<Patient>> patients = new HashMap<>();
        Map<String, ResultLimit> limits = new HashMap<>();
        Map<String, RunOrderState> ordered = new HashMap<>();
        Set<String> heldTests = run.getId() == null ? Set.of() : runQcService.heldTestIds(run.getId());
        return rows.stream().map(row -> {
            ResultRunExceptionKind kind = exceptionKindOf(row, samples, ordered);
            String flag = flagFor(row, samples, patients, limits);
            return new ClassifiedRow(row, flag, categoryOf(row, flag, heldTests.contains(row.getTestId()), kind), kind);
        }).toList();
    }

    static ResultRunRowCategory categoryOf(AnalyzerResults row, String flag, boolean testHeld,
            ResultRunExceptionKind exceptionKind) {
        if (row.getIsControl()) {
            return ResultRunRowCategory.CONTROL;
        }
        if (exceptionKind != null || StringUtils.isNotBlank(row.getImportIssueReason())
                || row.getDuplicateAnalyzerResultId() != null || "INVALID".equals(flag)) {
            return ResultRunRowCategory.EXCEPTION;
        }
        if (testHeld) {
            return ResultRunRowCategory.HELD;
        }
        if ("CRITICAL".equals(flag)) {
            return ResultRunRowCategory.CRITICAL;
        }
        if ("ABNORMAL".equals(flag)) {
            return ResultRunRowCategory.ABNORMAL;
        }
        return ResultRunRowCategory.NORMAL;
    }

    /**
     * The exception a value is, if any. A value held for another import issue, or a
     * repeat, is resolved on the analyzer import page and is none of these.
     */
    private ResultRunExceptionKind exceptionKindOf(AnalyzerResults row, Map<String, Optional<Sample>> samples,
            Map<String, RunOrderState> ordered) {
        if (row.getIsControl()) {
            return null;
        }
        if (row.getImportIssueReason() != null && UNMAPPED_ISSUES.contains(row.getImportIssueReason())) {
            return ResultRunExceptionKind.UNMAPPED;
        }
        if (StringUtils.isNotBlank(row.getImportIssueReason()) || row.getDuplicateAnalyzerResultId() != null) {
            return null;
        }
        String accession = StringUtils.defaultString(row.getAccessionNumber());
        Optional<Sample> sample = samples.computeIfAbsent(accession,
                number -> StringUtils.isBlank(number) ? Optional.empty()
                        : Optional.ofNullable(sampleService.getSampleByAccessionNumber(number)));
        if (sample.isEmpty()) {
            return ResultRunExceptionKind.UNMATCHED;
        }
        if (StringUtils.isBlank(row.getTestId())) {
            return null;
        }
        RunOrderState state = ordered.computeIfAbsent(
                accession + "|" + row.getTestId() + "|" + StringUtils.defaultString(row.getComponentId()),
                key -> orderStateOf(sample.get(), row.getTestId(), row.getComponentId()));
        return switch (state) {
        case AWAITING -> null;
        case RESULTED -> ResultRunExceptionKind.RESULTED;
        case NOT_ORDERED -> ResultRunExceptionKind.UNORDERED;
        };
    }

    /**
     * A value is named by the test it is mapped to, in the user's language as the
     * worklist and the QC block name it; a value mapped to no test keeps what the
     * analyzer called it.
     */
    public String testNameOf(AnalyzerResults row, Map<String, String> testNames) {
        if (StringUtils.isBlank(row.getTestId())) {
            return row.getTestName();
        }
        String name = testNames.computeIfAbsent(row.getTestId(), this::catalogNameOf);
        return name.isEmpty() ? row.getTestName() : name;
    }

    private String catalogNameOf(String testId) {
        Test test = testService.get(testId);
        if (test == null) {
            return "";
        }
        Localization localized = test.getLocalizedTestName();
        String name = localized == null ? null : StringUtils.trimToNull(localized.getLocalizedValue());
        return name != null ? name : StringUtils.trimToEmpty(test.getName());
    }

    /**
     * Where the order stands for the test: a cancelled analysis does not count, and
     * an analysis waits for the value only while it has no result for the value's
     * part, the rule the run's Results Entry panel applies before it offers the
     * value (it never overwrites a result).
     */
    public RunOrderState orderStateOf(Sample sample, String testId, String componentId) {
        String cancelled = statusService.getStatusID(AnalysisStatus.Canceled);
        List<Analysis> active = analysisService.getAnalysisBySampleAndTestIds(sample.getId(), List.of(testId)).stream()
                .filter(analysis -> !cancelled.equals(analysis.getStatusId())).toList();
        if (active.isEmpty()) {
            return RunOrderState.NOT_ORDERED;
        }
        return active.stream().anyMatch(analysis -> awaitsResult(analysis, componentId)) ? RunOrderState.AWAITING
                : RunOrderState.RESULTED;
    }

    private boolean awaitsResult(Analysis analysis, String componentId) {
        return resultService.getResultsByAnalysis(analysis).stream()
                .noneMatch(result -> StringUtils.isNotBlank(result.getValue())
                        && ruleResultScope.matchesComponent(result, componentId));
    }

    private String flagFor(AnalyzerResults row, Map<String, Optional<Sample>> samples,
            Map<String, Optional<Patient>> patients, Map<String, ResultLimit> limits) {
        String value = StringUtils.isNotBlank(row.getRawResultValue()) ? row.getRawResultValue() : row.getResult();
        if (row.getIsControl() || StringUtils.isBlank(value) || StringUtils.isBlank(row.getTestId())
                || StringUtils.isBlank(row.getAccessionNumber())) {
            return null;
        }
        Optional<Sample> sample = samples.computeIfAbsent(row.getAccessionNumber(),
                accession -> Optional.ofNullable(sampleService.getSampleByAccessionNumber(accession)));
        Optional<Patient> patient = patients.computeIfAbsent(row.getAccessionNumber(),
                accession -> sample.map(sampleHumanService::getPatientForSample));
        if (sample.isEmpty() || patient.isEmpty()) {
            return null;
        }
        String scope = StringUtils.isNotBlank(row.getComponentId()) ? "c" + row.getComponentId()
                : "t" + row.getTestId();
        String key = row.getAccessionNumber() + "|" + scope;
        ResultLimit limit = limits.containsKey(key) ? limits.get(key) : limitFor(row, sample.get(), patient.get());
        limits.put(key, limit);
        return ValidationSignals.resultFlag(limit, row.getResultType(), value.trim());
    }

    /**
     * The limit the Results worklist shows for this row: chosen from the sample's
     * analysis of the test, so a range scoped to the specimen wins over the shared
     * one. A row whose analysis cannot be found falls back to the test or component
     * range for the patient.
     */
    private ResultLimit limitFor(AnalyzerResults row, Sample sample, Patient patient) {
        String componentId = StringUtils.trimToNull(row.getComponentId());
        Optional<Analysis> analysis = analysisService
                .getAnalysisBySampleAndTestIds(sample.getId(), List.of(row.getTestId())).stream().findFirst();
        if (analysis.isPresent()) {
            return resultLimitService.selectResultLimitForResult(analysis.get(), null, patient, componentId)
                    .getResultLimit();
        }
        if (componentId != null) {
            ResultLimit limit = resultLimitService.getResultLimitForComponentAndPatient(componentId, patient);
            if (limit != null) {
                return limit;
            }
        }
        return resultLimitService.getResultLimitForTestAndPatient(row.getTestId(), patient);
    }
}
