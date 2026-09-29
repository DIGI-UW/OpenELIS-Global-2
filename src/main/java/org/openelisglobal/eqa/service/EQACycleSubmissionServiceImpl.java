package org.openelisglobal.eqa.service;

import static org.openelisglobal.eqa.valueholder.EQACycleStatus.CLOSED;
import static org.openelisglobal.eqa.valueholder.EQACycleStatus.PANEL_RECEIVED;
import static org.openelisglobal.eqa.valueholder.EQACycleStatus.PLANNED;
import static org.openelisglobal.eqa.valueholder.EQACycleStatus.READY_TO_SUBMIT;
import static org.openelisglobal.eqa.valueholder.EQACycleStatus.SCORED;
import static org.openelisglobal.eqa.valueholder.EQACycleStatus.SUBMITTED;
import static org.openelisglobal.eqa.valueholder.EQACycleStatus.TESTING;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.alert.service.AlertService;
import org.openelisglobal.alert.valueholder.AlertSeverity;
import org.openelisglobal.alert.valueholder.AlertType;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyte.service.AnalyteService;
import org.openelisglobal.analyte.valueholder.Analyte;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.eqa.dao.EQACycleDAO;
import org.openelisglobal.eqa.dao.EQALabProgramEnrollmentDAO;
import org.openelisglobal.eqa.dao.EQAParticipantResultDAO;
import org.openelisglobal.eqa.dao.EQARoundDAO;
import org.openelisglobal.eqa.dao.SampleEQADAO;
import org.openelisglobal.eqa.valueholder.EQACycle;
import org.openelisglobal.eqa.valueholder.EQACycleStateTransition;
import org.openelisglobal.eqa.valueholder.EQACycleStatus;
import org.openelisglobal.eqa.valueholder.EQALabEnrollmentTestMap;
import org.openelisglobal.eqa.valueholder.EQAParticipantResult;
import org.openelisglobal.eqa.valueholder.EQAPerformanceStatus;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.EQARound;
import org.openelisglobal.eqa.valueholder.EQASchemeType;
import org.openelisglobal.eqa.valueholder.EQAStateMachine;
import org.openelisglobal.eqa.valueholder.EQASubmissionChannel;
import org.openelisglobal.eqa.valueholder.EQASubmissionStatus;
import org.openelisglobal.eqa.valueholder.EQATriggerEvent;
import org.openelisglobal.eqa.valueholder.EQATriggerType;
import org.openelisglobal.eqa.valueholder.SampleEQA;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** See {@link EQACycleSubmissionService}. */
@Service
@Transactional
public class EQACycleSubmissionServiceImpl implements EQACycleSubmissionService {

    private static final Logger logger = LoggerFactory.getLogger(EQACycleSubmissionServiceImpl.class);

    private static final int MAX_ATTEMPTS = 5;

    private static final String ENTITY_TYPE_EQA_CYCLE = "EQACycle";

    /** System actor for sweep-initiated writes, as in EQADeadlineAlertScheduler. */
    private static final String SCHEDULER_USER = "1";

    /** The participant machine in order, walked one edge at a time. */
    private static final List<EQACycleStatus> PARTICIPANT_PATH = List.of(PLANNED, PANEL_RECEIVED, TESTING,
            READY_TO_SUBMIT, SUBMITTED, SCORED, CLOSED);

    /** States in which more results can arrive or a submission is still owed. */
    private static final Set<EQACycleStatus> ADVANCEABLE = EnumSet.of(PLANNED, PANEL_RECEIVED, TESTING,
            READY_TO_SUBMIT);

    /** A result that counts as this lab's answer for its analyte. */
    private static final Set<EQASubmissionStatus> ANSWERED = EnumSet.of(EQASubmissionStatus.VALIDATED_PARTIAL,
            EQASubmissionStatus.SUBMITTED, EQASubmissionStatus.SCORED);

    /** Sent, or awaiting scoring after being sent. */
    private static final Set<EQASubmissionStatus> SUBMITTABLE = EnumSet.of(EQASubmissionStatus.VALIDATED_PARTIAL,
            EQASubmissionStatus.SUBMITTED);

    @Value("${org.openelisglobal.eqa.submission.windowHours:1}")
    private long windowHours;

    @Value("${org.openelisglobal.eqa.submission.retryBaseMinutes:15}")
    private long retryBaseMinutes;

    @Autowired
    private EQACycleDAO cycleDAO;

    @Autowired
    private EQACycleService cycleService;

    @Autowired
    private EQARoundDAO roundDAO;

    @Autowired
    private EQAParticipantResultDAO participantResultDAO;

    @Autowired
    private EQAParticipantResultService participantResultService;

    @Autowired
    private SampleEQADAO sampleEQADAO;

    @Autowired
    private EQALabProgramEnrollmentDAO enrollmentDAO;

    @Autowired
    private EQAPanelService eqaPanelService;

    @Autowired
    private AnalyteService analyteService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private ResultService resultService;

    @Autowired
    private IStatusService statusService;

    @Autowired
    private EQAFhirSubmissionService fhirSubmissionService;

    @Autowired
    private AlertService alertService;

    @Autowired
    private FhirConfig fhirConfig;

    /** Package-private setter so tests can pin the submission window. */
    private Clock clock = Clock.systemDefaultZone();

    void setClock(Clock clock) {
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findAdvanceCandidates() {
        Set<Long> ids = new LinkedHashSet<>();
        for (EQACycleStatus status : ADVANCEABLE) {
            for (EQACycle cycle : cycleDAO.getAllMatching("status", status)) {
                ids.add(cycle.getId());
            }
        }
        return new ArrayList<>(ids);
    }

    @Override
    public boolean advanceCycle(Long cycleId) {
        EQACycle cycle = cycleDAO.get(cycleId).orElse(null);
        if (cycle == null || !ADVANCEABLE.contains(cycle.getStatus())) {
            return false;
        }
        EQAProgram scheme = cycle.getScheme();
        // An in-house cycle is driven by the blinding service, which creates its
        // results, submits and scores them itself. Sweeping it here
        // would race that path and submit blinded answers to nobody.
        if (scheme == null || scheme.getSchemeType() == EQASchemeType.IN_HOUSE) {
            return false;
        }

        Tally tally = bridgeResults(cycle, SCHEDULER_USER);
        if (tally.answered == 0) {
            return tally.written > 0;
        }

        boolean changed = tally.written > 0;
        EQACycleStatus target = tally.answered >= tally.expected ? READY_TO_SUBMIT : TESTING;
        changed |= advanceTo(cycle, target, EQATriggerType.AUTO, EQATriggerEvent.LAST_VALIDATED_RESULT, null, null,
                SCHEDULER_USER);

        if (target == READY_TO_SUBMIT) {
            changed |= submitIfDue(cycleDAO.get(cycleId).orElseThrow(), scheme);
        }
        return changed;
    }

    // ---- bridge: standard pipeline -> eqa_participant_result ----

    /**
     * One participant result per analyte the lab actually reported, promoted to
     * VALIDATED_PARTIAL once its analysis is finalized. The denominator is
     * analyses, not analytes: a not-yet-finalized analysis has no result rows to
     * count, so counting analytes would call a half-finished cycle complete.
     */
    @Override
    public boolean assignAnalyst(Analysis analysis, Long analystId, String sysUserId) {
        if (analysis == null || analystId == null || analysis.getSampleItem() == null
                || analysis.getSampleItem().getSample() == null) {
            return false;
        }
        Long sampleId = Long.valueOf(analysis.getSampleItem().getSample().getId());
        SampleEQA sample = sampleEQADAO.getAllMatching("sampleId", sampleId).stream().findFirst().orElse(null);
        if (sample == null || !Boolean.TRUE.equals(sample.getIsEqaSample()) || sample.getCycleId() == null) {
            logger.debug("EQA analyst skipped for analysis {}: sample {} eqa={} cycle={}", analysis.getId(), sampleId,
                    sample == null ? null : sample.getIsEqaSample(), sample == null ? null : sample.getCycleId());
            return false;
        }
        EQACycle cycle = cycleDAO.get(sample.getCycleId()).orElse(null);
        EQAProgram scheme = cycle == null ? null : cycle.getScheme();
        if (scheme == null || !Boolean.TRUE.equals(scheme.getPerAnalyst())) {
            logger.debug("EQA analyst skipped for analysis {}: cycle {} scheme {} perAnalyst={}", analysis.getId(),
                    sample.getCycleId(), scheme == null ? null : scheme.getId(),
                    scheme == null ? null : scheme.getPerAnalyst());
            return false;
        }

        Long enrollmentId = sample.getEqaEnrollmentId();
        Long roundId = resolveRound(sample, roundDAO.getAllMatching("cycle.id", cycle.getId()));
        if (enrollmentId == null || roundId == null) {
            logger.warn("EQA analyst not recorded for analysis {}: enrollment={} round={}", analysis.getId(),
                    enrollmentId, roundId);
            return false;
        }

        Map<Long, Long> schemeAnalytes = analyteByTest(enrollmentId);
        boolean recorded = false;
        for (Result pipelineResult : resultService.getResultsByAnalysis(analysis)) {
            Long analyteId = analyteIdOf(pipelineResult, analysis, schemeAnalytes);
            if (analyteId == null) {
                // Same unmapped-analyte case the bridge logs, and worth saying twice:
                // here a user has explicitly named an analyst and would otherwise get
                // no record and no reason.
                logger.warn("EQA analyst not recorded for analysis {}: it names no test, so it resolves no analyte",
                        analysis.getId());
                continue;
            }
            String value = EqaReportedValue.of(resultService, pipelineResult);
            recorded |= recordAnalyst(cycle, roundId, enrollmentId, analysis, analyteId, value, analystId, sysUserId);
        }
        return recorded;
    }

    /**
     * Finds the draft row this analyte already has, or opens one. A scored row is
     * left alone: re-attributing a result the provider has already judged would
     * move an ISO 15189 record after the fact.
     */
    private boolean recordAnalyst(EQACycle cycle, Long roundId, Long enrollmentId, Analysis analysis, Long analyteId,
            String value, Long analystId, String sysUserId) {
        List<EQAParticipantResult> existing = participantResultDAO
                .getAllMatching(Map.of("round.id", roundId, "labEnrollmentId", enrollmentId, "analyteId", analyteId));
        EQAParticipantResult row = existing.isEmpty() ? null : existing.get(0);

        if (row == null) {
            row = new EQAParticipantResult();
            row.setCycle(cycle);
            EQARound roundRef = new EQARound();
            roundRef.setId(roundId);
            row.setRound(roundRef);
            row.setLabEnrollmentId(enrollmentId);
            row.setAnalyteId(analyteId);
            row.setAnalysisId(Long.valueOf(analysis.getId()));
            row.setResultValue(GenericValidator.isBlankOrNull(value) ? null : value.trim());
        } else if (row.getSubmissionStatus() != EQASubmissionStatus.DRAFT) {
            // Refusing is the point, but say so: whoever picked the analyst gets no
            // record and would otherwise have no way to know the result was already
            // out of their hands.
            logger.info("EQA analyst not recorded for analysis {}: participant result {} is already {}",
                    analysis.getId(), row.getId(), row.getSubmissionStatus());
            return false;
        } else if (analystId.equals(row.getAssignedAnalystId())) {
            return false;
        }

        row.setAssignedAnalystId(analystId);
        row.setSysUserId(sysUserId);
        participantResultService.saveDraft(row);
        return true;
    }

    private Tally bridgeResults(EQACycle cycle, String sysUserId) {
        Tally tally = new Tally();
        String finalizedId = statusService.getStatusID(AnalysisStatus.Finalized);
        String canceledId = statusService.getStatusID(AnalysisStatus.Canceled);
        List<EQARound> rounds = roundDAO.getAllMatching("cycle.id", cycle.getId());

        for (SampleEQA sample : sampleEQADAO.getAllMatching("cycleId", cycle.getId())) {
            if (!Boolean.TRUE.equals(sample.getIsEqaSample()) || sample.getSampleId() == null) {
                continue;
            }
            Long enrollmentId = sample.getEqaEnrollmentId();
            Long roundId = resolveRound(sample, rounds);
            if (enrollmentId == null || roundId == null) {
                // Refused rather than guessed: eqa_participant_result.round_id and
                // lab_enrollment_id are both NOT NULL, and filing a result under the
                // wrong round misattributes an ISO 15189 record. A multi-round cycle
                // whose orders do not name their round needs the order to carry it.
                logger.warn("EQA sample {} skipped: enrollment={} round={} (cycle {})", sample.getSampleId(),
                        enrollmentId, roundId, cycle.getId());
                continue;
            }

            Map<Long, Long> schemeAnalytes = analyteByTest(enrollmentId);
            for (Analysis analysis : analysisService.getAnalysesBySampleId(String.valueOf(sample.getSampleId()))) {
                if (canceledId.equals(analysis.getStatusId())) {
                    continue;
                }
                tally.expected++;
                if (!finalizedId.equals(analysis.getStatusId())) {
                    continue;
                }
                if (bridgeAnalysis(cycle, roundId, enrollmentId, analysis, schemeAnalytes, sysUserId, tally)) {
                    tally.answered++;
                }
            }
        }
        return tally;
    }

    /**
     * @return true when this analysis now has at least one answered participant
     *         result
     */
    private boolean bridgeAnalysis(EQACycle cycle, Long roundId, Long enrollmentId, Analysis analysis,
            Map<Long, Long> schemeAnalytes, String sysUserId, Tally tally) {
        boolean answered = false;
        for (Result pipelineResult : resultService.getResultsByAnalysis(analysis)) {
            String value = EqaReportedValue.of(resultService, pipelineResult);
            if (GenericValidator.isBlankOrNull(value)) {
                continue;
            }
            Long analyteId = analyteIdOf(pipelineResult, analysis, schemeAnalytes);
            if (analyteId == null) {
                // Logged rather than skipped quietly: the cycle then stays short of
                // its denominator and never submits, and this line is the only thing
                // that says why.
                logger.warn("EQA analysis {} names no test, so it resolves no analyte and cannot be submitted",
                        analysis.getId());
                continue;
            }
            if (upsert(cycle, roundId, enrollmentId, analysis, analyteId, value.trim(), sysUserId, tally)) {
                answered = true;
            }
        }
        return answered;
    }

    /**
     * eqa_participant_result.analyte_id is a NOT NULL FK, so a result with no
     * resolvable analyte cannot be bridged. Three sources, in order of how
     * specifically each one describes THIS value:
     *
     * <ol>
     * <li>the result's own analyte — the only source with the right grain when one
     * test reports several analytes. ResultSaveService fills it from test_analyte,
     * and only when such a row exists.
     * <li>the enrollment's scheme mapping (qa/030) — what the lab declared this
     * test reports for this scheme. The normal external-PT case: most test
     * configurations carry no test_analyte row at all, so source 1 is null.
     * <li>the test's own analyte, created from the test name when the catalog has
     * none. This is the same resolution the provider makes when it seals a panel
     * sample, and that is the point: a result crosses to the provider under its
     * <em>analyte name</em>, and scores come back matched the same way, so both
     * instances have to derive the same name. Deriving it from the shared test name
     * on both sides is what makes them agree without a new field on the wire.
     * </ol>
     */
    private Long analyteIdOf(Result pipelineResult, Analysis analysis, Map<Long, Long> schemeAnalytes) {
        Analyte reported = pipelineResult.getAnalyte();
        if (reported != null && reported.getId() != null) {
            return Long.valueOf(reported.getId());
        }
        Long testId = analysis.getTest() == null ? null : Long.valueOf(analysis.getTest().getId());
        if (testId != null && schemeAnalytes.get(testId) != null) {
            return schemeAnalytes.get(testId);
        }
        if (analysis.getTest() == null) {
            return null;
        }
        return eqaPanelService.analyteIdForTest(analysis.getTest().getId());
    }

    /** This enrollment's declared test-to-analyte mapping, empty when unmapped. */
    private Map<Long, Long> analyteByTest(Long enrollmentId) {
        return enrollmentDAO.get(enrollmentId).map(enrollment -> {
            Map<Long, Long> byTest = new java.util.HashMap<>();
            for (EQALabEnrollmentTestMap testMap : enrollment.getTestMaps()) {
                if (testMap.getTestId() != null && testMap.getAnalyteId() != null) {
                    byTest.put(testMap.getTestId(), testMap.getAnalyteId());
                }
            }
            return byTest;
        }).orElseGet(Map::of);
    }

    private boolean upsert(EQACycle cycle, Long roundId, Long enrollmentId, Analysis analysis, Long analyteId,
            String value, String sysUserId, Tally tally) {
        List<EQAParticipantResult> existing = participantResultDAO
                .getAllMatching(Map.of("round.id", roundId, "labEnrollmentId", enrollmentId, "analyteId", analyteId));
        EQAParticipantResult row = existing.isEmpty() ? null : existing.get(0);

        if (row == null) {
            row = new EQAParticipantResult();
            row.setCycle(cycle);
            EQARound roundRef = new EQARound();
            roundRef.setId(roundId);
            row.setRound(roundRef);
            row.setLabEnrollmentId(enrollmentId);
            row.setAnalyteId(analyteId);
            row.setAnalysisId(Long.valueOf(analysis.getId()));
            row.setResultValue(value);
            row.setSysUserId(sysUserId);
            row = participantResultService.saveDraft(row);
            tally.written++;
        } else if (row.getSubmissionStatus() == EQASubmissionStatus.DRAFT && !value.equals(row.getResultValue())) {
            // External PT is one result per analyte per round, so a second sample
            // reporting the same analyte updates this row rather than adding one —
            // the partial unique index would refuse the insert anyway.
            row.setResultValue(value);
            row.setSysUserId(sysUserId);
            row = participantResultService.saveDraft(row);
            tally.written++;
        }

        if (row.getSubmissionStatus() == EQASubmissionStatus.DRAFT) {
            row = participantResultService.transitionStatus(row.getId(), EQASubmissionStatus.VALIDATED_PARTIAL,
                    sysUserId);
            tally.written++;
        }
        return ANSWERED.contains(row.getSubmissionStatus());
    }

    /**
     * The order's own round when it carries one, otherwise the cycle's single
     * round. A multi-round cycle is never guessed at.
     */
    private Long resolveRound(SampleEQA sample, List<EQARound> rounds) {
        if (sample.getRoundId() != null) {
            return sample.getRoundId();
        }
        return rounds.size() == 1 ? rounds.get(0).getId() : null;
    }

    // ---- submission ----

    /**
     * Submit automatically only when the scheme delegates it, an endpoint exists,
     * the review window has elapsed and the retry budget allows another try.
     */
    private boolean submitIfDue(EQACycle cycle, EQAProgram scheme) {
        if (cycle.getStatus() != READY_TO_SUBMIT || Boolean.TRUE.equals(scheme.getRequiresCycleReview())) {
            return false;
        }
        // No FHIR store configured is the manual-fallback deployment,
        // not a failure: attempting a post would burn the retry budget on a
        // submission channel this lab never had.
        if (StringUtils.isBlank(fhirConfig.getLocalFhirStorePath())) {
            return false;
        }

        Timestamp readyAt = readyToSubmitAt(cycle.getId());
        Instant now = clock.instant();
        if (readyAt == null || now.isBefore(readyAt.toInstant().plus(windowHours, ChronoUnit.HOURS))) {
            return false;
        }

        int attempts = cycle.getSubmissionAttempts() == null ? 0 : cycle.getSubmissionAttempts();
        if (attempts >= MAX_ATTEMPTS) {
            return false;
        }
        if (cycle.getLastSubmissionAttemptAt() != null && now.isBefore(
                cycle.getLastSubmissionAttemptAt().toInstant().plus(backoffMinutes(attempts), ChronoUnit.MINUTES))) {
            return false;
        }

        List<Long> enrollments = submittingEnrollments(cycle.getId());
        if (enrollments.isEmpty()) {
            return false;
        }

        if (postToProvider(cycle.getId(), enrollments)) {
            stampSubmitted(cycle, enrollments, EQATriggerType.AUTO, EQATriggerEvent.FHIR_SUBMIT_SUCCESS, null, null,
                    SCHEDULER_USER);
            return true;
        }
        recordFailedAttempt(cycle, attempts + 1);
        return true;
    }

    /**
     * Posts to every enrolled laboratory before reporting, so one unreachable
     * enrollment does not hide the state of the rest. A partial post counts as a
     * failure: the caller stamps nothing and the whole cycle is sent again.
     */
    private boolean postToProvider(Long cycleId, List<Long> enrollments) {
        boolean allSent = true;
        for (Long enrollmentId : enrollments) {
            if (!fhirSubmissionService.submitCycleViaFhir(cycleId, enrollmentId)) {
                allSent = false;
            }
        }
        return allSent;
    }

    /**
     * What a successful send leaves behind: the channel and timestamp on each
     * result row, a cleared retry budget, and the cycle at SUBMITTED. The state
     * transition alone is not a record of a submission, because nothing reading
     * {@code eqa_participant_result} would see the results as sent.
     */
    private void stampSubmitted(EQACycle cycle, List<Long> enrollments, EQATriggerType triggerType,
            EQATriggerEvent triggerEvent, Long triggeredBy, String reason, String sysUserId) {
        for (Long enrollmentId : enrollments) {
            markSent(cycle.getId(), enrollmentId, EQASubmissionChannel.FHIR, null, sysUserId);
        }
        clearRetryBudget(cycle, sysUserId);
        advanceTo(cycle, SUBMITTED, triggerType, triggerEvent, triggeredBy, reason, sysUserId);
    }

    @Override
    public EQACycle submitAfterReview(Long cycleId, String sysUserId) {
        EQACycle cycle = cycleDAO.get(cycleId)
                .orElseThrow(() -> new IllegalArgumentException("Cycle not found: " + cycleId));
        if (cycle.getStatus() != READY_TO_SUBMIT) {
            throw new IllegalStateException("Only a cycle that is ready to submit can be reviewed and submitted");
        }
        // The scheme's review flag is not consulted: the reviewer clicking is what
        // the flag asks for. Neither the review window nor the retry budget is
        // consulted either — both exist to pace an unattended sweep, and a person
        // waiting for an answer is told the outcome instead.
        if (StringUtils.isBlank(fhirConfig.getLocalFhirStorePath())) {
            throw new IllegalStateException(
                    "This laboratory has no automatic submission channel configured; submit by hand instead");
        }
        List<Long> enrollments = submittingEnrollments(cycleId);
        if (enrollments.isEmpty()) {
            throw new IllegalStateException("No validated result to submit for cycle " + cycleId);
        }
        if (!postToProvider(cycleId, enrollments)) {
            logger.warn("EQA cycle {} review submission could not reach the provider", cycleId);
            throw new IllegalStateException("The provider could not be reached, so the cycle was not submitted");
        }
        stampSubmitted(cycle, enrollments, EQATriggerType.MANUAL, EQATriggerEvent.MANUAL_OVERRIDE,
                actingUser(sysUserId), "Reviewed and submitted from My Cycles", sysUserId);
        return cycleDAO.get(cycleId).orElseThrow();
    }

    /** 15, 30, 60, 120 minutes by default — doubling from the first failure. */
    private long backoffMinutes(int attempts) {
        return attempts <= 0 ? 0 : retryBaseMinutes << (attempts - 1);
    }

    private Timestamp readyToSubmitAt(Long cycleId) {
        Timestamp readyAt = null;
        for (EQACycleStateTransition transition : cycleService.getTransitions(cycleId)) {
            if (READY_TO_SUBMIT.name().equals(transition.getNewState())) {
                readyAt = transition.getOccurredAt();
            }
        }
        return readyAt;
    }

    /**
     * A submission that got through spends none of the budget. The counter had only
     * ever been incremented, so a cycle that failed its way to the ceiling once
     * could never be submitted automatically again for the rest of its life — and
     * the same would have been true of the next cycle to reach the ceiling after a
     * transient outage.
     */
    private void clearRetryBudget(EQACycle cycle, String sysUserId) {
        if (cycle.getSubmissionAttempts() == null || cycle.getSubmissionAttempts() == 0) {
            return;
        }
        cycle.setSubmissionAttempts(0);
        cycle.setLastSubmissionAttemptAt(null);
        cycle.setSysUserId(sysUserId);
        cycleDAO.update(cycle);
    }

    private void recordFailedAttempt(EQACycle cycle, int attempts) {
        cycle.setSubmissionAttempts(attempts);
        cycle.setLastSubmissionAttemptAt(Timestamp.from(clock.instant()));
        cycle.setSysUserId(SCHEDULER_USER);
        cycleDAO.update(cycle);

        // One alert, when the budget is gone. Alerting on every attempt would put
        // five rows on the lab's dashboard for one unreachable endpoint.
        if (attempts >= MAX_ATTEMPTS) {
            alertService.createAlert(AlertType.EQA_SUBMISSION_FAILED, ENTITY_TYPE_EQA_CYCLE, cycle.getId(),
                    AlertSeverity.CRITICAL,
                    "Automatic EQA submission failed " + attempts + " times; submit manually before the deadline",
                    String.format("{\"cycleId\":%d,\"attempts\":%d}", cycle.getId(), attempts));
        }
        logger.warn("EQA cycle {} submission attempt {} failed", cycle.getId(), attempts);
    }

    private List<Long> submittingEnrollments(Long cycleId) {
        Set<Long> enrollments = new LinkedHashSet<>();
        for (EQAParticipantResult result : participantResultDAO.getAllMatching("cycle.id", cycleId)) {
            if (SUBMITTABLE.contains(result.getSubmissionStatus())) {
                enrollments.add(result.getLabEnrollmentId());
            }
        }
        return new ArrayList<>(enrollments);
    }

    /** Stamps the channel the transition itself does not carry. */
    private int markSent(Long cycleId, Long labEnrollmentId, EQASubmissionChannel channel, String reference,
            String sysUserId) {
        int sent = 0;
        for (EQAParticipantResult result : results(cycleId, labEnrollmentId)) {
            if (result.getSubmissionStatus() != EQASubmissionStatus.VALIDATED_PARTIAL) {
                continue;
            }
            EQAParticipantResult submitted = participantResultService.transitionStatus(result.getId(),
                    EQASubmissionStatus.SUBMITTED, sysUserId);
            submitted.setSubmissionChannel(channel);
            if (reference != null) {
                submitted.setManualSubmissionReference(reference);
            }
            submitted.setSysUserId(sysUserId);
            participantResultDAO.update(submitted);
            sent++;
        }
        return sent;
    }

    // ---- fallbacks ----

    @Override
    public EQACycle submitManually(Long cycleId, Long labEnrollmentId, String reference, String sysUserId) {
        if (GenericValidator.isBlankOrNull(reference)) {
            throw new IllegalArgumentException("A manual submission requires the provider's reference");
        }
        EQACycle cycle = cycleDAO.get(cycleId)
                .orElseThrow(() -> new IllegalArgumentException("Cycle not found: " + cycleId));

        int sent = markSent(cycleId, labEnrollmentId, EQASubmissionChannel.MANUAL, reference.trim(), sysUserId);
        if (sent == 0) {
            throw new IllegalArgumentException("No validated result to submit for cycle " + cycleId);
        }
        clearRetryBudget(cycle, sysUserId);
        advanceTo(cycle, SUBMITTED, EQATriggerType.MANUAL, EQATriggerEvent.MANUAL_OVERRIDE, actingUser(sysUserId),
                "Manual submission, provider reference " + reference.trim(), sysUserId);
        return cycleDAO.get(cycleId).orElseThrow();
    }

    @Override
    @Transactional(readOnly = true)
    public String exportBundleCsv(Long cycleId, Long labEnrollmentId) {
        // analyte_name travels with the id: the provider that imports this bundle is
        // another instance whose analyte ids differ, so the name is what it matches on.
        StringBuilder csv = new StringBuilder("cycle_id,cycle_name,round_number,analyte_id,analyte_name,"
                + "result_value,result_unit,submission_status,entered_at\n");
        for (EQAParticipantResult result : results(cycleId, labEnrollmentId)) {
            if (!SUBMITTABLE.contains(result.getSubmissionStatus())) {
                continue;
            }
            EQACycle cycle = result.getCycle();
            EQARound round = result.getRound();
            csv.append(cycleId).append(',').append(StringUtil.csvEscape(cycle == null ? null : cycle.getCycleName()))
                    .append(',').append(round == null || round.getRoundNumber() == null ? "" : round.getRoundNumber())
                    .append(',').append(result.getAnalyteId()).append(',')
                    .append(StringUtil.csvEscape(eqaPanelService.analyteName(result.getAnalyteId()))).append(',')
                    .append(bundleValue(result.getResultValue())).append(',')
                    .append(StringUtil.csvEscape(result.getResultUnit())).append(',')
                    .append(result.getSubmissionStatus().name()).append(',')
                    .append(result.getEnteredAt() == null ? "" : result.getEnteredAt()).append('\n');
        }
        return csv.toString();
    }

    /**
     * A reported value as a bundle cell. Numbers go out raw: the formula guard in
     * {@link StringUtil#csvEscape} would prefix a negative result with an
     * apostrophe, and the provider that imports this file would then read it as
     * text. Everything else, including semi-quantitative answers such as "3+", is
     * escaped.
     */
    private static String bundleValue(String value) {
        return value != null && value.matches("-?\\d+(\\.\\d+)?") ? value : StringUtil.csvEscape(value);
    }

    @Override
    public int intakeScores(Long cycleId, Long labEnrollmentId, List<Map<String, Object>> scores, String sysUserId) {
        if (scores == null || scores.isEmpty()) {
            throw new IllegalArgumentException("A score intake needs at least one score");
        }
        EQACycle cycle = cycleDAO.get(cycleId)
                .orElseThrow(() -> new IllegalArgumentException("Cycle not found: " + cycleId));

        int scored = 0;
        for (Map<String, Object> entry : scores) {
            EQAParticipantResult row = resolveScoredRow(cycleId, labEnrollmentId, entry);
            participantResultService.recordScore(row.getId(), verdictOf(entry), decimalOf(entry, "zScore"),
                    longOf(entry, "eqaResultId"), sysUserId);
            scored++;
        }

        // The cycle is scored when nothing is still waiting for a
        // verdict. A missed-deadline result is resolved, not pending.
        boolean pending = participantResultDAO.getAllMatching("cycle.id", cycleId).stream()
                .anyMatch(r -> r.getSubmissionStatus() != EQASubmissionStatus.SCORED
                        && r.getSubmissionStatus() != EQASubmissionStatus.MISSED_DEADLINE);
        if (!pending) {
            advanceTo(cycle, SCORED, EQATriggerType.AUTO, EQATriggerEvent.SCORE_INTAKE, null, null, sysUserId);
        }
        return scored;
    }

    @Override
    public Map<String, Object> intakeScoresCsv(Long cycleId, Long labEnrollmentId, String csv, String sysUserId) {
        if (csv == null || csv.isBlank()) {
            throw new IllegalArgumentException("The CSV is empty");
        }
        String[] lines = EqaCsv.lines(csv);
        List<String> header = EqaCsv.split(lines[0]);
        int nameColumn = EqaCsv.indexOf(header, "analyte_name");
        int verdictColumn = EqaCsv.indexOf(header, "performance_status");
        int zColumn = EqaCsv.indexOf(header, "z_score");
        if (nameColumn < 0 || verdictColumn < 0) {
            throw new IllegalArgumentException(
                    "The CSV needs analyte_name and performance_status columns (the provider's scores CSV)");
        }
        List<Map<String, Object>> scores = new ArrayList<>();
        List<String> unmapped = new ArrayList<>();
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].isBlank()) {
                continue;
            }
            List<String> cells = EqaCsv.split(lines[i]);
            String name = EqaCsv.cell(cells, nameColumn);
            String verdict = EqaCsv.cell(cells, verdictColumn);
            if (name.isEmpty() || verdict.isEmpty()) {
                continue;
            }
            Analyte probe = new Analyte();
            probe.setAnalyteName(name);
            Analyte analyte = analyteService.getAnalyteByName(probe, true);
            if (analyte == null) {
                unmapped.add(name);
                continue;
            }
            Map<String, Object> score = new LinkedHashMap<>();
            score.put("analyteId", Long.valueOf(analyte.getId()));
            score.put("performance", verdict);
            String z = EqaCsv.cell(cells, zColumn);
            if (!z.isEmpty()) {
                score.put("zScore", z);
            }
            scores.add(score);
        }
        if (scores.isEmpty()) {
            throw new IllegalArgumentException(
                    "No row names an analyte this laboratory knows" + (unmapped.isEmpty() ? "" : ": " + unmapped));
        }
        int scored = intakeScores(cycleId, labEnrollmentId, scores, sysUserId);
        Map<String, Object> outcome = new LinkedHashMap<>();
        outcome.put("cycleId", cycleId);
        outcome.put("scored", scored);
        outcome.put("unmapped", unmapped);
        return outcome;
    }

    private EQAParticipantResult resolveScoredRow(Long cycleId, Long labEnrollmentId, Map<String, Object> entry) {
        Long resultId = longOf(entry, "resultId");
        if (resultId == null) {
            Long analyteId = longOf(entry, "analyteId");
            if (analyteId == null) {
                throw new IllegalArgumentException("Each score needs a resultId or an analyteId");
            }
            return results(cycleId, labEnrollmentId).stream().filter(
                    r -> analyteId.equals(r.getAnalyteId()) && r.getSubmissionStatus() == EQASubmissionStatus.SUBMITTED)
                    .findFirst().orElseThrow(() -> new IllegalArgumentException(
                            "No submitted result for analyte " + analyteId + " in cycle " + cycleId));
        }
        EQAParticipantResult row = participantResultService.get(resultId);
        if (row.getCycle() == null || !cycleId.equals(row.getCycle().getId())) {
            throw new IllegalArgumentException("Result " + resultId + " does not belong to cycle " + cycleId);
        }
        return row;
    }

    // ---- shared helpers ----

    private List<EQAParticipantResult> results(Long cycleId, Long labEnrollmentId) {
        return labEnrollmentId == null ? participantResultDAO.getAllMatching("cycle.id", cycleId)
                : participantResultDAO.getAllMatching(Map.of("cycle.id", cycleId, "labEnrollmentId", labEnrollmentId));
    }

    /**
     * Walks the participant machine edge by edge so every step keeps its own audit
     * row. A cycle whose panel receipt was never recorded is walked through
     * PANEL_RECEIVED too: a validated result is proof the panel arrived, and the
     * audit row names the real trigger rather than claiming a receipt.
     */
    private boolean advanceTo(EQACycle cycle, EQACycleStatus target, EQATriggerType triggerType,
            EQATriggerEvent triggerEvent, Long triggeredBy, String reason, String sysUserId) {
        int from = PARTICIPANT_PATH.indexOf(cycle.getStatus());
        int to = PARTICIPANT_PATH.indexOf(target);
        if (from < 0 || to <= from) {
            return false;
        }
        for (int step = from + 1; step <= to; step++) {
            cycleService.transition(cycle.getId(), PARTICIPANT_PATH.get(step), EQAStateMachine.PARTICIPANT, triggerType,
                    triggerEvent, triggeredBy, reason, sysUserId);
        }
        return true;
    }

    private Long actingUser(String sysUserId) {
        try {
            return Long.valueOf(sysUserId);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private EQAPerformanceStatus verdictOf(Map<String, Object> entry) {
        Object raw = entry.get("performance");
        if (raw == null) {
            throw new IllegalArgumentException("Each score needs a performance verdict");
        }
        try {
            return EQAPerformanceStatus.valueOf(String.valueOf(raw).toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown performance status: " + raw);
        }
    }

    private Long longOf(Map<String, Object> entry, String key) {
        Object raw = entry.get(key);
        if (raw == null || String.valueOf(raw).isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(raw));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(key + " must be a number");
        }
    }

    private BigDecimal decimalOf(Map<String, Object> entry, String key) {
        Object raw = entry.get(key);
        if (raw == null || String.valueOf(raw).isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(String.valueOf(raw));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(key + " must be a number");
        }
    }

    /** Analyses seen, analyses answered, and rows this sweep actually wrote. */
    private static final class Tally {
        private int expected;
        private int answered;
        private int written;
    }
}
