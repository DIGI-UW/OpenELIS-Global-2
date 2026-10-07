package org.openelisglobal.eqa.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.validator.GenericValidator;
import org.hibernate.ObjectNotFoundException;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.eqa.dao.EQACycleDAO;
import org.openelisglobal.eqa.dao.EQADistributionDAO;
import org.openelisglobal.eqa.dao.EQAPanelDAO;
import org.openelisglobal.eqa.dao.EQAPanelSampleDAO;
import org.openelisglobal.eqa.dao.EQAProgramEnrollmentDAO;
import org.openelisglobal.eqa.dao.EQAResultDAO;
import org.openelisglobal.eqa.dao.EQARoundDAO;
import org.openelisglobal.eqa.valueholder.EQACycle;
import org.openelisglobal.eqa.valueholder.EQACycleStatus;
import org.openelisglobal.eqa.valueholder.EQADistribution;
import org.openelisglobal.eqa.valueholder.EQADistributionStatus;
import org.openelisglobal.eqa.valueholder.EQAPanel;
import org.openelisglobal.eqa.valueholder.EQAPanelSample;
import org.openelisglobal.eqa.valueholder.EQAPerformanceStatus;
import org.openelisglobal.eqa.valueholder.EQAProgramEnrollment;
import org.openelisglobal.eqa.valueholder.EQAProgramTest;
import org.openelisglobal.eqa.valueholder.EQAResult;
import org.openelisglobal.eqa.valueholder.EQARound;
import org.openelisglobal.eqa.valueholder.EQAStateMachine;
import org.openelisglobal.eqa.valueholder.EQASubmissionMethod;
import org.openelisglobal.eqa.valueholder.EQATriggerEvent;
import org.openelisglobal.eqa.valueholder.EQATriggerType;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** See {@link EQAProviderScoringService}. */
@Service
@Transactional
public class EQAProviderScoringServiceImpl implements EQAProviderScoringService {

    /** Unacceptable in 2 of the last 3 cycles is persistent failure. */
    private static final int PERSISTENT_FAILURE_WINDOW = 3;
    private static final int PERSISTENT_FAILURE_THRESHOLD = 2;

    /** The performance window: a laboratory's last four scored cycles. */
    private static final int ROLLING_WINDOW = 4;

    private static final String ACTIVE_ENROLLMENT = "Active";

    /**
     * The provider machine from submissions_open to scored, walked one edge at a
     * time.
     */
    private static final List<EQACycleStatus> SCORING_PATH = List.of(EQACycleStatus.SUBMISSIONS_OPEN,
            EQACycleStatus.SUBMISSIONS_CLOSED, EQACycleStatus.SCORING, EQACycleStatus.SCORED);

    @Autowired
    private EQACycleDAO eqaCycleDAO;

    @Autowired
    private EQACycleService eqaCycleService;

    @Autowired
    private EQADistributionDAO eqaDistributionDAO;

    @Autowired
    private EQARoundDAO eqaRoundDAO;

    @Autowired
    private EQAResultDAO eqaResultDAO;

    @Autowired
    private EQAStatisticsService eqaStatisticsService;

    @Autowired
    private EQAFhirSubmissionService eqaFhirSubmissionService;

    @Autowired
    private EQAParticipantFollowupService followupService;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private SystemUserService systemUserService;
    @Autowired
    private EQAResultService eqaResultService;
    @Autowired
    private EQAProgramService eqaProgramService;
    @Autowired
    private EQAPanelDAO eqaPanelDAO;
    @Autowired
    private EQAPanelSampleDAO eqaPanelSampleDAO;
    @Autowired
    private EQAPanelService eqaPanelService;
    @Autowired
    private EQAProgramEnrollmentDAO eqaProgramEnrollmentDAO;

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getScoreRows(Long cycleId) {
        EQADistribution distribution = distributionOf(cycle(cycleId));
        if (distribution == null) {
            return List.of();
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map.Entry<Long, List<EQAResult>> entry : byOrganization(distribution.getId()).entrySet()) {
            Organization participant = organizationService.getOrganizationById(String.valueOf(entry.getKey()));
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("organizationId", entry.getKey());
            row.put("organizationName", participant == null ? null : participant.getOrganizationName());
            row.put("resultCount", entry.getValue().size());
            row.put("acceptableCount", count(entry.getValue(), EQAPerformanceStatus.ACCEPTABLE));
            row.put("questionableCount", count(entry.getValue(), EQAPerformanceStatus.QUESTIONABLE));
            row.put("unacceptableCount", count(entry.getValue(), EQAPerformanceStatus.UNACCEPTABLE));
            row.put("worstZScore", worstZScore(entry.getValue()));
            rows.add(row);
        }
        return rows;
    }

    @Override
    public Map<String, Object> scoreCycle(Long cycleId, String sysUserId) {
        EQACycle cycle = cycle(cycleId);
        if (cycle.getStatus() != EQACycleStatus.SUBMISSIONS_OPEN
                && cycle.getStatus() != EQACycleStatus.SUBMISSIONS_CLOSED) {
            throw new IllegalStateException("A cycle in " + cycle.getStatus() + " is not open for scoring");
        }

        EQADistribution distribution = findOrOpenDistribution(cycle, sysUserId);
        long reported = eqaResultDAO.findByDistributionId(distribution.getId()).stream()
                .filter(result -> result.getResultValue() != null || result.getResultText() != null).count();
        if (reported == 0) {
            throw new IllegalStateException("This cycle has no reported results to score");
        }
        // The floor protects the peer statistic, which is all an untargeted cycle
        // has: below it the mean and SD describe nothing. A cycle whose panel sealed
        // a target needs no crowd, so it scores at whatever roster size it ran.
        if (reported < EQAStatisticsService.MIN_PARTICIPANTS_FOR_STATS && !hasSealedTarget(cycle)) {
            throw new IllegalStateException("Scoring against peers needs at least "
                    + EQAStatisticsService.MIN_PARTICIPANTS_FOR_STATS + " reported results; this cycle has " + reported
                    + " and its panel carries no target to judge them against");
        }

        // The peer statistics run first and stay on the row as the reported z. They
        // cannot carry the verdict on their own: with the sample SD taken over every
        // reported value, the largest z anyone can reach is (n-1)/sqrt(n) — 1.79 at
        // the five participants MIN_PARTICIPANTS_FOR_STATS requires, below the
        // questionable threshold — so a laboratory reporting double the target scored
        // acceptable. Where the panel sealed a target, that target is the verdict.
        eqaStatisticsService.calculateAndUpdateStatistics(distribution.getId());
        judgeAgainstPanelTargets(cycle, distribution.getId());
        List<EQAResult> unjudged = unjudgedResults(distribution.getId());
        advanceToScored(cycle, sysUserId);

        int followups = 0;
        Map<Long, String> sampleCodes = sampleCodes(cycle);
        for (Map.Entry<Long, List<EQAResult>> entry : byOrganization(distribution.getId()).entrySet()) {
            if (count(entry.getValue(), EQAPerformanceStatus.UNACCEPTABLE) == 0) {
                continue;
            }
            followupService.enqueueForOrganization(cycle.getScheme(), cycle, entry.getKey(),
                    snapshotRows(entry.getValue(), sampleCodes), isPersistentFailure(cycle, entry.getKey()), sysUserId);
            followups++;
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("cycleId", cycleId);
        summary.put("distributionId", distribution.getId());
        summary.put("scoredCount", (int) reported);
        summary.put("followupCount", followups);
        summary.put("unjudgedCount", unjudged.size());
        summary.put("unjudgedTests", unjudged.stream().map(result -> testName(result.getTestId()))
                .filter(name -> name != null).distinct().sorted().collect(Collectors.toList()));
        summary.put("cycleStatus",
                eqaCycleDAO.get(cycleId).map(EQACycle::getStatus).map(EQACycleStatus::name).orElse(null));
        return summary;
    }

    @Override
    @Transactional(readOnly = true)
    public String buildScoreCsv(Long cycleId, Long organizationId) {
        // analyte_name is what a participant on another instance matches on when it
        // imports these scores: test ids and names are the provider's own.
        // sample_code names the panel sample each row answers, so a participant that
        // reported two samples of one test can tell the two scores apart. It is the
        // last column: readers find columns by header name, and one that predates it
        // simply ignores it.
        StringBuilder csv = new StringBuilder(
                "test,analyte_name,result_value,target_value,z_score,performance_status,scored_on,sample_code\n");
        // The column has always been named for scoring and filled from the
        // submission date, which is a different fact, and participating
        // laboratories import this file. Scoring runs over the whole cycle at once,
        // so the date is the cycle's: stamped the first time it reached SCORED, and
        // blank on a cycle scored before anything recorded that.
        EQACycle cycle = cycle(cycleId);
        String scoredOn = scoredOn(cycle);
        Map<Long, String> sampleCodes = sampleCodes(cycle);
        for (EQAResult result : resultsFor(cycleId, organizationId)) {
            String sampleCode = sampleCodes.get(result.getPanelSampleId());
            // Only the free-text cells are escaped. Running a decimal through csvEscape
            // would quote a negative Z as a formula and print it as '-0.28 (found
            // driving the download, 2026-08-24).
            String analyte = eqaPanelService.analyteName(analyteIdOrNull(result.getTestId()));
            csv.append(StringUtil.csvEscape(testName(result.getTestId()))).append(',')
                    .append(analyte == null ? "" : StringUtil.csvEscape(analyte)).append(',')
                    .append(result.getResultText() != null ? StringUtil.csvEscape(result.getResultText())
                            : number(result.getResultValue()))
                    .append(',').append(number(result.getTargetValue())).append(',').append(number(result.getZScore()))
                    .append(',')
                    .append(result.getPerformanceStatus() == null ? "" : result.getPerformanceStatus().name())
                    .append(',').append(scoredOn).append(',')
                    .append(sampleCode == null ? "" : StringUtil.csvEscape(sampleCode)).append('\n');
        }
        return csv.toString();
    }

    @Override
    public Map<String, Object> distributeScores(Long cycleId, Long organizationId) {
        EQADistribution distribution = distributionOf(cycle(cycleId));
        if (distribution == null) {
            throw new IllegalArgumentException("This cycle has no distribution, so there are no scores to return");
        }
        return eqaFhirSubmissionService.submitResultsViaFhir(distribution.getId(), organizationId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> intakeGrid(Long cycleId, Long organizationId) {
        EQACycle cycle = cycle(cycleId);
        EQADistribution distribution = distributionOf(cycle);
        Map<String, EQAResult> onFile = new HashMap<>();
        if (distribution != null) {
            for (EQAResult result : eqaResultDAO.findByDistributionId(distribution.getId())) {
                if (organizationId.equals(result.getParticipantOrganizationId())) {
                    onFile.put(rowKey(result.getTestId(), result.getPanelSampleId()), result);
                }
            }
        }
        List<Map<String, Object>> tests = new ArrayList<>();
        for (IntakeRow intake : intakeRows(cycle)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("testId", intake.testId());
            row.put("testName", testName(intake.testId()));
            row.put("analyteId", intake.analyteId());
            row.put("analyteName", intake.analyteName());
            row.put("panelSampleId", intake.panelSampleId());
            row.put("sampleCode", intake.sampleCode());
            EQAResult result = onFile.get(rowKey(intake.testId(), intake.panelSampleId()));
            row.put("reported", result == null ? null : reportedOf(result));
            row.put("performanceStatus", result == null || result.getPerformanceStatus() == null ? null
                    : result.getPerformanceStatus().name());
            tests.add(row);
        }
        Map<String, Object> grid = new LinkedHashMap<>();
        grid.put("cycleId", cycleId);
        grid.put("organizationId", organizationId);
        grid.put("distributionId", distribution == null ? null : distribution.getId());
        grid.put("tests", tests);
        return grid;
    }

    private static final Set<EQACycleStatus> INTAKE_STATES = EnumSet.of(EQACycleStatus.SHIPPED,
            EQACycleStatus.DELIVERED, EQACycleStatus.SUBMISSIONS_OPEN, EQACycleStatus.SUBMISSIONS_CLOSED,
            EQACycleStatus.SCORING);

    @Override
    public Map<String, Object> takeIn(Long cycleId, Long organizationId, List<EQAIntakeValue> reported,
            EQASubmissionMethod method, String sysUserId) {
        EQACycle cycle = cycle(cycleId);
        if (!INTAKE_STATES.contains(cycle.getStatus())) {
            throw new IllegalStateException(
                    "Results are taken in between shipping and scoring; this cycle is " + cycle.getStatus());
        }
        // Every value is resolved before anything is written, so one bad row refuses
        // the whole submission instead of leaving it half taken in.
        List<IntakeRow> rows = intakeRows(cycle);
        List<EQAIntakeValue> resolved = new ArrayList<>();
        for (EQAIntakeValue value : reported) {
            resolved.add(
                    new EQAIntakeValue(value.testId(), resolve(rows, cycle, value).panelSampleId(), value.value()));
        }
        EQADistribution distribution = findOrOpenDistribution(cycle, sysUserId);
        for (EQAIntakeValue value : resolved) {
            if (value.value() == null || value.value().isBlank()) {
                continue;
            }
            eqaResultService.submitReportedValue(distribution.getId(), organizationId, value.testId(),
                    value.panelSampleId(), value.value(), method, sysUserId);
        }
        return intakeGrid(cycleId, organizationId);
    }

    /**
     * The intake row a value answers. A value that names no panel sample is
     * accepted only where its test has a single row: with two samples of one test
     * on the panel, choosing either would judge the value against a target it may
     * never have been measured against.
     */
    private IntakeRow resolve(List<IntakeRow> rows, EQACycle cycle, EQAIntakeValue value) {
        List<IntakeRow> forTest = rows.stream().filter(row -> row.testId().equals(value.testId())).toList();
        if (forTest.isEmpty()) {
            // Named by id: the test may not exist at all, and resolving its name would
            // turn a clean refusal into a not-found error.
            throw new IllegalArgumentException(
                    "Test " + value.testId() + " is not part of scheme " + cycle.getScheme().getName());
        }
        if (value.panelSampleId() != null) {
            return forTest.stream().filter(row -> value.panelSampleId().equals(row.panelSampleId())).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Panel sample " + value.panelSampleId()
                            + " is not a sample of test " + value.testId() + " in this cycle"));
        }
        if (forTest.size() > 1) {
            throw new IllegalArgumentException("Test " + value.testId() + " has " + forTest.size()
                    + " samples on this cycle's panel; say which sample the value answers");
        }
        return forTest.get(0);
    }

    @Override
    public Map<String, Object> importReportedCsv(Long cycleId, Long organizationId, String csv, String sysUserId) {
        if (csv == null || csv.isBlank()) {
            throw new IllegalArgumentException("The CSV is empty");
        }
        String[] lines = EqaCsv.lines(csv);
        List<String> header = EqaCsv.split(lines[0]);
        int nameColumn = EqaCsv.indexOf(header, "analyte_name");
        int valueColumn = EqaCsv.indexOf(header, "result_value");
        int sampleColumn = EqaCsv.indexOf(header, "sample_code");
        if (nameColumn < 0 || valueColumn < 0) {
            throw new IllegalArgumentException(
                    "The CSV needs analyte_name and result_value columns (the participant's export bundle)");
        }
        List<LabelledValue> values = new ArrayList<>();
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].isBlank()) {
                continue;
            }
            List<String> cells = EqaCsv.split(lines[i]);
            String value = EqaCsv.cell(cells, valueColumn);
            if (value.isEmpty()) {
                continue;
            }
            String sampleCode = sampleColumn < 0 ? "" : EqaCsv.cell(cells, sampleColumn);
            values.add(new LabelledValue(i + 1, sampleCode.isEmpty() ? null : sampleCode,
                    EqaCsv.cell(cells, nameColumn), value));
        }
        Map<String, Object> grid = takeInLabelled(cycle(cycleId), organizationId, values,
                EQASubmissionMethod.FILE_UPLOAD, sysUserId);
        List<?> errors = (List<?>) grid.remove("rejected");
        grid.put("imported", values.size() - errors.size());
        grid.put("errors", errors);
        return grid;
    }

    @Override
    public Map<String, Object> takeInByAnalyteName(Long cycleId, Long organizationId,
            Map<String, String> reportedByAnalyteName, EQASubmissionMethod method, String sysUserId) {
        List<LabelledValue> values = new ArrayList<>();
        reportedByAnalyteName.forEach((name, value) -> values.add(new LabelledValue(0, null, name, value)));
        Map<String, Object> grid = takeInLabelled(cycle(cycleId), organizationId, values, method, sysUserId);
        grid.remove("rejected");
        return grid;
    }

    /**
     * A value as another instance names it: by the panel sample code where it sends
     * one, otherwise by analyte name. {@code line} is its CSV row, or 0.
     */
    private record LabelledValue(int line, String sampleCode, String analyteName, String value) {
        String label() {
            return sampleCode != null ? sampleCode.trim() : analyteName == null ? "" : analyteName.trim();
        }
    }

    /**
     * Resolve named values to intake rows and take them in. The grid comes back
     * with {@code unmapped} (the labels that matched no single row) and
     * {@code rejected} (why, one line per value, prefixed with its CSV row).
     */
    private Map<String, Object> takeInLabelled(EQACycle cycle, Long organizationId, List<LabelledValue> values,
            EQASubmissionMethod method, String sysUserId) {
        List<IntakeRow> rows = intakeRows(cycle);
        List<EQAIntakeValue> reported = new ArrayList<>();
        List<String> unmapped = new ArrayList<>();
        List<String> rejected = new ArrayList<>();
        for (LabelledValue value : values) {
            String label = value.label();
            List<IntakeRow> matches;
            String problem;
            if (value.sampleCode() != null) {
                matches = rows.stream()
                        .filter(row -> row.sampleCode() != null && row.sampleCode().trim().equalsIgnoreCase(label))
                        .toList();
                problem = matches.isEmpty() ? "no sample '" + label + "' was sent in this cycle"
                        : "sample '" + label + "' is on more than one panel in this cycle";
            } else {
                // A value keyed by name alone may still name a sample: an instance that
                // sends sample codes over FHIR puts the code where the name would be.
                matches = rows.stream()
                        .filter(row -> row.sampleCode() != null && row.sampleCode().trim().equalsIgnoreCase(label))
                        .toList();
                if (matches.isEmpty()) {
                    matches = rows.stream().filter(
                            row -> row.analyteName() != null && row.analyteName().trim().equalsIgnoreCase(label))
                            .toList();
                }
                problem = matches.isEmpty() ? "no test in this scheme reports '" + label + "'"
                        : "'" + label + "' is reported by " + matches.size()
                                + " samples on this cycle's panel; add a sample_code column naming which one";
            }
            if (matches.size() != 1) {
                unmapped.add(label);
                rejected.add(value.line() > 0 ? "Row " + value.line() + ": " + problem : problem);
                continue;
            }
            IntakeRow row = matches.get(0);
            reported.add(new EQAIntakeValue(row.testId(), row.panelSampleId(), value.value()));
        }
        Map<String, Object> grid = takeIn(cycle.getId(), organizationId, reported, method, sysUserId);
        grid.put("unmapped", unmapped);
        grid.put("rejected", rejected);
        return grid;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, String> sealedTargetsByResult(Long cycleId) {
        EQACycle cycle = cycle(cycleId);
        EQADistribution distribution = distributionOf(cycle);
        Map<Long, String> byResult = new HashMap<>();
        if (distribution == null) {
            return byResult;
        }
        List<EQAPanelSample> samples = panelSamples(cycle);
        for (EQAResult result : eqaResultDAO.findByDistributionId(distribution.getId())) {
            EQAPanelSample sample = targetFor(result, samples);
            if (sample != null) {
                byResult.put(result.getId(), sample.getTargetValue());
            }
        }
        return byResult;
    }

    /**
     * The sealed panel sample a result is judged against: the sample it answers. A
     * result that names no sample (a V1 row, or one taken in before results named
     * their sample) is matched by analyte only where the panel seals a single
     * sample of it; with two, either choice could be the wrong target.
     */
    private EQAPanelSample targetFor(EQAResult result, List<EQAPanelSample> samples) {
        if (result.getPanelSampleId() != null) {
            return samples.stream().filter(
                    sample -> result.getPanelSampleId().equals(sample.getId()) && sample.getTargetValue() != null)
                    .findFirst().orElse(null);
        }
        Long analyteId = analyteIdOrNull(result.getTestId());
        if (analyteId == null) {
            return null;
        }
        List<EQAPanelSample> sealed = samples.stream()
                .filter(sample -> analyteId.equals(sample.getAnalyteId()) && sample.getTargetValue() != null).toList();
        return sealed.size() == 1 ? sealed.get(0) : null;
    }

    private boolean hasSealedTarget(EQACycle cycle) {
        return panelSamples(cycle).stream()
                .anyMatch(sample -> sample.getAnalyteId() != null && sample.getTargetValue() != null);
    }

    /**
     * Judge every reported value against the target its own panel sample sealed,
     * numeric and qualitative alike, through the same comparison the in-house lane
     * uses. A qualitative result has no peer z and never had one; a numeric result
     * keeps the z the statistics pass computed, as the reported statistic beside
     * the verdict rather than as the verdict.
     *
     * <p>
     * A result with no sealed target keeps whatever the peer statistics decided: a
     * scheme that reports without a target has nothing else to be judged against.
     */
    private void judgeAgainstPanelTargets(EQACycle cycle, Long distributionId) {
        List<EQAPanelSample> samples = panelSamples(cycle);
        for (EQAResult result : eqaResultDAO.findByDistributionId(distributionId)) {
            EQAPanelSample target = targetFor(result, samples);
            if (target == null) {
                continue;
            }
            String reported = result.getResultText() != null ? result.getResultText()
                    : result.getResultValue() == null ? null : result.getResultValue().toPlainString();
            if (reported == null) {
                continue;
            }
            if (result.getResultText() != null) {
                // Qualitative: no peer mean to place it against.
                result.setZScore(null);
            } else {
                // The report and the scores CSV show what a number was judged against;
                // a target that is a word has no column here and needs none.
                result.setTargetValue(EqaPanelVerdict.numericTargetOf(target));
                if (!EqaPanelVerdict.hasRange(target)) {
                    // A bare target compares for equality, and a laboratory reporting
                    // 39.5 against a target of 40 has not failed a proficiency test.
                    // Without a sealed tolerance the peer verdict is the only honest
                    // one; the target is still recorded above, for the report.
                    eqaResultDAO.update(result);
                    continue;
                }
            }
            result.setPerformanceStatus(EqaPanelVerdict.of(target, reported));
            eqaResultDAO.update(result);
        }
    }

    /**
     * The reported results that reached SCORED with no verdict on them. Neither
     * pass covers them: the peer statistic skips a test with fewer than
     * {@code MIN_PARTICIPANTS_FOR_STATS} numeric results and never places a
     * qualitative one at any roster size, and the target pass only touches analytes
     * the panel sealed a target for. Before this the row simply kept a null
     * performance_status and the only trace was an info line in the statistics log.
     */
    private List<EQAResult> unjudgedResults(Long distributionId) {
        return eqaResultDAO.findByDistributionId(distributionId).stream()
                .filter(result -> result.getResultValue() != null || result.getResultText() != null)
                .filter(result -> result.getPerformanceStatus() == null).collect(Collectors.toList());
    }

    private static Object reportedOf(EQAResult result) {
        return result.getResultText() != null ? result.getResultText() : result.getResultValue();
    }

    private Long analyteIdOrNull(Long testId) {
        return testId == null ? null : eqaPanelService.findAnalyteIdForTest(String.valueOf(testId));
    }

    /**
     * One row of a cycle's intake: a panel sample of an assigned test, or the test
     * itself when the panel carries no sample of it.
     */
    private record IntakeRow(Long testId, Long analyteId, String analyteName, EQAPanelSample sample) {
        Long panelSampleId() {
            return sample == null ? null : sample.getId();
        }

        String sampleCode() {
            return sample == null ? null : sample.getSampleCode();
        }
    }

    /**
     * The rows a laboratory reports into, in the scheme's test order. A multi-level
     * panel carries several samples of one test, each with its own target, so each
     * sample is its own row (OGC-1243). The first assigned test that reports a
     * sample's analyte takes it, so two tests sharing an analyte do not both claim
     * one sample.
     */
    private List<IntakeRow> intakeRows(EQACycle cycle) {
        Map<Long, List<EQAPanelSample>> samplesByAnalyte = new LinkedHashMap<>();
        for (EQAPanelSample sample : panelSamples(cycle)) {
            if (sample.getAnalyteId() != null) {
                samplesByAnalyte.computeIfAbsent(sample.getAnalyteId(), key -> new ArrayList<>()).add(sample);
            }
        }
        List<IntakeRow> rows = new ArrayList<>();
        for (EQAProgramTest assignment : eqaProgramService.getTestAssignments(cycle.getScheme().getId())) {
            if (!Boolean.TRUE.equals(assignment.getIsActive())) {
                continue;
            }
            Long analyteId = analyteIdOrNull(assignment.getTestId());
            String analyteName = eqaPanelService.analyteName(analyteId);
            List<EQAPanelSample> samples = analyteId == null ? null : samplesByAnalyte.remove(analyteId);
            if (samples == null) {
                rows.add(new IntakeRow(assignment.getTestId(), analyteId, analyteName, null));
                continue;
            }
            for (EQAPanelSample sample : samples) {
                rows.add(new IntakeRow(assignment.getTestId(), analyteId, analyteName, sample));
            }
        }
        return rows;
    }

    /**
     * Every panel sample the cycle carries, panel by panel and then by sample code.
     */
    private List<EQAPanelSample> panelSamples(EQACycle cycle) {
        List<EQAPanel> panels = new ArrayList<>(eqaPanelDAO.getAllMatching("cycle.id", cycle.getId()));
        panels.sort(Comparator.comparing(EQAPanel::getId));
        List<EQAPanelSample> samples = new ArrayList<>();
        for (EQAPanel panel : panels) {
            List<EQAPanelSample> onPanel = new ArrayList<>(eqaPanelSampleDAO.getAllMatching("panel.id", panel.getId()));
            onPanel.sort(Comparator.comparing(EQAPanelSample::getSampleCode,
                    Comparator.nullsLast(Comparator.naturalOrder())));
            samples.addAll(onPanel);
        }
        return samples;
    }

    private Map<Long, String> sampleCodes(EQACycle cycle) {
        Map<Long, String> codes = new HashMap<>();
        for (EQAPanelSample sample : panelSamples(cycle)) {
            codes.put(sample.getId(), sample.getSampleCode());
        }
        return codes;
    }

    /**
     * A result's place in the intake: its panel sample, or its test when it answers
     * none.
     */
    private static String rowKey(Long testId, Long panelSampleId) {
        return panelSampleId != null ? "sample:" + panelSampleId : "test:" + testId;
    }

    // ---- helpers ----

    private EQACycle cycle(Long cycleId) {
        return eqaCycleDAO.get(cycleId)
                .orElseThrow(() -> new ObjectNotFoundException(cycleId, EQACycle.class.getName()));
    }

    /**
     * When this cycle was scored: {@code actual_end_date}, stamped the first time
     * the cycle reached SCORED and never overwritten, so a re-score cannot move it.
     *
     * <p>
     * Empty rather than a substitute when the column is null. A cycle scored before
     * anything wrote that date has no recorded scoring date, and a blank cell says
     * so; the submission date this used to print said something else entirely.
     */
    static String scoredOn(EQACycle cycle) {
        return cycle == null || cycle.getActualEndDate() == null ? "" : cycle.getActualEndDate().toString();
    }

    /** The cycle's distribution, or null when no results have been taken in yet. */
    private EQADistribution distributionOf(EQACycle cycle) {
        List<EQADistribution> existing = eqaDistributionDAO.getAllMatching("cycleId", cycle.getId());
        return existing.isEmpty() ? null : existing.get(0);
    }

    /**
     * The distribution is the cycle's per-participant score container: results,
     * z-scores and the FHIR return all hang off it, and its {@code cycle_id} is
     * what ties them to the V2 cycle. Opened on demand so a cycle scored straight
     * after an import does not need a separate setup step.
     */
    private EQADistribution findOrOpenDistribution(EQACycle cycle, String sysUserId) {
        EQADistribution existing = distributionOf(cycle);
        if (existing != null) {
            return existing;
        }
        List<EQARound> rounds = eqaRoundDAO.getAllMatchingOrdered("cycle.id", cycle.getId(), "roundNumber", false);
        EQARound round = rounds.isEmpty() ? null : rounds.get(0);
        Timestamp distributionDate = firstNonNull(round == null ? null : round.getDistributionDate(),
                timestamp(cycle.getPlannedStartDate()), new Timestamp(System.currentTimeMillis()));

        EQADistribution distribution = new EQADistribution();
        distribution.setFhirUuid(UUID.randomUUID());
        distribution.setEqaProgram(cycle.getScheme());
        distribution.setDistributionName(
                GenericValidator.isBlankOrNull(cycle.getCycleName()) ? "Cycle " + cycle.getCycleNumber()
                        : cycle.getCycleName());
        distribution.setDistributionDate(distributionDate);
        distribution.setDeadline(firstNonNull(round == null ? null : round.getSubmissionDeadline(),
                timestamp(cycle.getPlannedEndDate()), distributionDate));
        distribution.setStatus(EQADistributionStatus.SHIPPED);
        distribution.setCreatedBy(systemUserService.get(sysUserId));
        distribution.setCycleId(cycle.getId());
        distribution.setRoundId(round == null ? null : round.getId());
        distribution.setSysUserId(sysUserId);
        distribution.setId(eqaDistributionDAO.insert(distribution));
        return distribution;
    }

    /** Each edge keeps its own audit row, as the state machine requires. */
    private void advanceToScored(EQACycle cycle, String sysUserId) {
        int from = SCORING_PATH.indexOf(cycle.getStatus());
        for (int step = from + 1; step < SCORING_PATH.size(); step++) {
            eqaCycleService.transition(cycle.getId(), SCORING_PATH.get(step), EQAStateMachine.PROVIDER,
                    EQATriggerType.AUTO, EQATriggerEvent.SCORE_INTAKE, null, "Provider scoring run", sysUserId);
        }
    }

    /**
     * Unacceptable in at least 2 of the participant's last 3 cycles in this scheme,
     * the current one included. Cycles the participant did not take part in are not
     * counted against it — only cycles that produced results.
     */
    private boolean isPersistentFailure(EQACycle cycle, Long organizationId) {
        List<EQACycle> cycles = eqaCycleDAO.getAllMatchingOrdered("scheme.id", cycle.getScheme().getId(), "cycleNumber",
                true);
        int considered = 0;
        int failures = 0;
        for (EQACycle candidate : cycles) {
            if (candidate.getCycleNumber() > cycle.getCycleNumber()) {
                continue;
            }
            List<EQAResult> results = resultsFor(candidate.getId(), organizationId);
            if (results.isEmpty()) {
                continue;
            }
            considered++;
            if (count(results, EQAPerformanceStatus.UNACCEPTABLE) > 0) {
                failures++;
            }
            if (considered == PERSISTENT_FAILURE_WINDOW) {
                break;
            }
        }
        return failures >= PERSISTENT_FAILURE_THRESHOLD;
    }

    private Map<Long, List<EQAResult>> byOrganization(Long distributionId) {
        Map<Long, List<EQAResult>> byOrganization = new LinkedHashMap<>();
        for (EQAResult result : eqaResultDAO.findByDistributionId(distributionId)) {
            byOrganization.computeIfAbsent(result.getParticipantOrganizationId(), key -> new ArrayList<>()).add(result);
        }
        return byOrganization;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EQAResult> reportedResultsFor(Long cycleId, Long organizationId) {
        return resultsFor(cycleId, organizationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getParticipantPerformance(Long schemeId) {
        if (eqaProgramService.get(schemeId) == null) {
            throw new IllegalArgumentException("Unknown scheme " + schemeId);
        }

        // Newest first: the rolling window is the last four cycles a laboratory
        // actually reported into, not the last four the scheme ran, so a lab that
        // sat one out is judged on the four it took part in.
        List<EQACycle> scoredCycles = eqaCycleDAO.findBySchemeIds(List.of(schemeId)).stream().filter(
                cycle -> cycle.getStatus() == EQACycleStatus.SCORED || cycle.getStatus() == EQACycleStatus.CLOSED)
                .sorted(Comparator.comparing(EQACycle::getCycleNumber,
                        Comparator.nullsFirst(Comparator.reverseOrder())))
                .toList();
        Map<Long, Long> openFollowups = followupService.countOpenProviderFollowupsByOrganization();

        List<Map<String, Object>> rows = new ArrayList<>();
        for (EQAProgramEnrollment enrollment : currentEnrollments(schemeId)) {
            rows.add(participantRow(enrollment, scoredCycles, openFollowups));
        }
        rows.sort(Comparator.comparing(row -> String.valueOf(row.get("organizationName")),
                String.CASE_INSENSITIVE_ORDER));
        return rows;
    }

    /**
     * One enrollment per laboratory: the current one. Withdrawal is terminal, so a
     * laboratory re-admitted after leaving holds two rows, and the page must show
     * it once with the status it holds now — otherwise a re-admitted participant
     * appears twice with identical figures, since the rate is a property of the
     * organization rather than of the paperwork.
     */
    private List<EQAProgramEnrollment> currentEnrollments(Long schemeId) {
        Map<Long, EQAProgramEnrollment> byOrganization = new LinkedHashMap<>();
        for (EQAProgramEnrollment enrollment : eqaProgramEnrollmentDAO.findByProgramId(schemeId)) {
            byOrganization.merge(enrollment.getOrganizationId(), enrollment,
                    (kept, candidate) -> supersedes(candidate, kept) ? candidate : kept);
        }
        return new ArrayList<>(byOrganization.values());
    }

    /** An active enrollment wins; otherwise the one enrolled later. */
    private static boolean supersedes(EQAProgramEnrollment candidate, EQAProgramEnrollment kept) {
        boolean candidateActive = ACTIVE_ENROLLMENT.equalsIgnoreCase(candidate.getStatus());
        boolean keptActive = ACTIVE_ENROLLMENT.equalsIgnoreCase(kept.getStatus());
        if (candidateActive != keptActive) {
            return candidateActive;
        }
        if (candidate.getEnrollmentDate() == null || kept.getEnrollmentDate() == null) {
            return candidate.getEnrollmentDate() != null;
        }
        return candidate.getEnrollmentDate().after(kept.getEnrollmentDate());
    }

    private Map<String, Object> participantRow(EQAProgramEnrollment enrollment, List<EQACycle> scoredCycles,
            Map<Long, Long> openFollowups) {
        Long organizationId = enrollment.getOrganizationId();
        Organization organization = organizationService.get(String.valueOf(organizationId));

        List<Map<String, Object>> history = new ArrayList<>();
        int accepted = 0;
        int judged = 0;
        String mostRecent = null;
        for (EQACycle cycle : scoredCycles) {
            List<EQAResult> reported = resultsFor(cycle.getId(), organizationId);
            if (reported.isEmpty()) {
                continue; // this laboratory did not report into that cycle
            }
            Map<String, Object> cycleRow = new LinkedHashMap<>();
            cycleRow.put("cycleId", cycle.getId());
            cycleRow.put("cycleNumber", cycle.getCycleNumber());
            cycleRow.put("cycleName", cycle.getCycleName());
            List<Map<String, Object>> analytes = new ArrayList<>();
            Map<Long, String> sampleCodes = sampleCodes(cycle);
            for (EQAResult result : reported) {
                Map<String, Object> analyte = new LinkedHashMap<>();
                analyte.put("test", testName(result.getTestId()));
                analyte.put("sampleCode", sampleCodes.get(result.getPanelSampleId()));
                analyte.put("reported", reportedOf(result));
                analyte.put("zScore", result.getZScore());
                analyte.put("performance",
                        result.getPerformanceStatus() == null ? null : result.getPerformanceStatus().name());
                analytes.add(analyte);
                if (result.getPerformanceStatus() == null) {
                    continue;
                }
                if (history.size() < ROLLING_WINDOW) {
                    judged++;
                    if (result.getPerformanceStatus() == EQAPerformanceStatus.ACCEPTABLE) {
                        accepted++;
                    }
                }
                if (mostRecent == null) {
                    mostRecent = result.getPerformanceStatus().name();
                }
            }
            cycleRow.put("analytes", analytes);
            history.add(cycleRow);
        }

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("organizationId", organizationId);
        row.put("organizationName",
                organization == null ? String.valueOf(organizationId) : organization.getOrganizationName());
        // The nearest thing the schema has to a region: an organization carries a
        // city and a state, and no administrative area beyond them.
        row.put("region", organization == null ? null : region(organization));
        row.put("enrollmentStatus", enrollment.getStatus());
        row.put("cyclesCounted", Math.min(history.size(), ROLLING_WINDOW));
        row.put("accepted", accepted);
        row.put("judged", judged);
        // Null rather than 0% where nothing has been judged: a laboratory with no
        // scored cycle has no rate, and printing zero would read as total failure.
        row.put("passRate", judged == 0 ? null
                : BigDecimal.valueOf(accepted * 100L).divide(BigDecimal.valueOf(judged), 1, RoundingMode.HALF_UP));
        row.put("mostRecentPerformance", mostRecent);
        row.put("openFollowups", openFollowups.getOrDefault(organizationId, 0L));
        row.put("cycles", history.subList(0, Math.min(history.size(), ROLLING_WINDOW)));
        return row;
    }

    private static String region(Organization organization) {
        String city = organization.getCity();
        String state = organization.getState();
        if (GenericValidator.isBlankOrNull(city)) {
            return GenericValidator.isBlankOrNull(state) ? null : state;
        }
        return GenericValidator.isBlankOrNull(state) ? city : city + ", " + state;
    }

    private List<EQAResult> resultsFor(Long cycleId, Long organizationId) {
        EQADistribution distribution = distributionOf(cycle(cycleId));
        if (distribution == null) {
            return List.of();
        }
        return eqaResultDAO.findByDistributionId(distribution.getId()).stream()
                .filter(result -> organizationId.equals(result.getParticipantOrganizationId())).toList();
    }

    /**
     * The snapshot the register prints; it outlives later re-scoring of the row.
     */
    private List<Map<String, Object>> snapshotRows(List<EQAResult> results, Map<Long, String> sampleCodes) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (EQAResult result : results) {
            if (result.getPerformanceStatus() != EQAPerformanceStatus.UNACCEPTABLE) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("testId", result.getTestId());
            row.put("testName", testName(result.getTestId()));
            row.put("panelSampleId", result.getPanelSampleId());
            row.put("sampleCode", sampleCodes.get(result.getPanelSampleId()));
            row.put("reported", reportedOf(result));
            row.put("target", result.getTargetValue());
            row.put("zScore", result.getZScore());
            row.put("performanceStatus", result.getPerformanceStatus().name());
            rows.add(row);
        }
        return rows;
    }

    /**
     * TestService is fetched rather than injected: injecting it into a service
     * pulls in a bean cycle through the test catalog's own dependencies.
     */
    private String testName(Long testId) {
        if (testId == null) {
            return null;
        }
        Test test = SpringContext.getBean(TestService.class).get(String.valueOf(testId));
        return test == null ? String.valueOf(testId) : test.getName();
    }

    private static int count(List<EQAResult> results, EQAPerformanceStatus status) {
        return (int) results.stream().filter(result -> result.getPerformanceStatus() == status).count();
    }

    private static BigDecimal worstZScore(List<EQAResult> results) {
        BigDecimal worst = null;
        for (EQAResult result : results) {
            BigDecimal z = result.getZScore();
            if (z != null && (worst == null || z.abs().compareTo(worst.abs()) > 0)) {
                worst = z;
            }
        }
        return worst;
    }

    /**
     * A decimal CSV cell: no separator or quote can occur in one, so no escaping.
     */
    private static String number(BigDecimal value) {
        return value == null ? "" : value.toPlainString();
    }

    private static Timestamp timestamp(java.sql.Date date) {
        return date == null ? null : new Timestamp(date.getTime());
    }

    private static Timestamp firstNonNull(Timestamp... candidates) {
        for (Timestamp candidate : candidates) {
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }
}
