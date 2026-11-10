package org.openelisglobal.resultrun.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.resultlimit.service.ResultLimitService;
import org.openelisglobal.resultlimits.valueholder.ResultLimit;
import org.openelisglobal.resultrun.form.ResultRunRowCategory;
import org.openelisglobal.resultrun.valueholder.ResultRun;
import org.openelisglobal.resultrun.valueholder.ResultRunState;
import org.openelisglobal.resultvalidation.util.ValidationSignals;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Sorts a run's rows for review (OGC-1200 FR-E1a): a value is judged against
 * the result limit the Results worklist selects for the row's analysis (its
 * specimen, the component, the patient's sex and age), with the rule the
 * worklist and Validation use, so a run and the worklist flag a value alike.
 */
@Component
public class ResultRunRowClassifier {

    /** One row with its flag and review category. */
    public record ClassifiedRow(AnalyzerResults row, String flag, ResultRunRowCategory category) {
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

    public List<ClassifiedRow> classify(ResultRun run, List<AnalyzerResults> rows) {
        Map<String, Optional<Sample>> samples = new HashMap<>();
        Map<String, Optional<Patient>> patients = new HashMap<>();
        Map<String, ResultLimit> limits = new HashMap<>();
        boolean held = run.getState() == ResultRunState.HELD;
        return rows.stream().map(row -> {
            String flag = flagFor(row, samples, patients, limits);
            return new ClassifiedRow(row, flag, categoryOf(row, flag, held));
        }).toList();
    }

    static ResultRunRowCategory categoryOf(AnalyzerResults row, String flag, boolean runHeld) {
        if (row.getIsControl()) {
            return ResultRunRowCategory.CONTROL;
        }
        if (StringUtils.isNotBlank(row.getImportIssueReason()) || row.getDuplicateAnalyzerResultId() != null
                || "INVALID".equals(flag)) {
            return ResultRunRowCategory.EXCEPTION;
        }
        if (runHeld) {
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
