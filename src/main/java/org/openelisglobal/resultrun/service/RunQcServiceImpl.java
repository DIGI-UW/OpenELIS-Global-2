package org.openelisglobal.resultrun.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.exception.ConstraintViolationException;
import org.openelisglobal.analyzerresults.service.AnalyzerResultsService;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.qaevent.service.NCEventService;
import org.openelisglobal.qaevent.service.QcViolationNceService;
import org.openelisglobal.qaevent.valueholder.NcEvent;
import org.openelisglobal.qc.dao.QCResultDAO;
import org.openelisglobal.qc.form.BenchQCCaptureForm;
import org.openelisglobal.qc.service.QCControlLotService;
import org.openelisglobal.qc.service.QCResultService;
import org.openelisglobal.qc.valueholder.QCControlLot;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.RunControlVerdict;
import org.openelisglobal.qc.valueholder.RunVerdictSource;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.resultrun.dao.ResultRunDAO;
import org.openelisglobal.resultrun.dao.ResultRunEventDAO;
import org.openelisglobal.resultrun.dao.ResultRunRetestDAO;
import org.openelisglobal.resultrun.dao.ResultRunRetestDAO.RetestedSample;
import org.openelisglobal.resultrun.dao.ResultRunTestDispositionDAO;
import org.openelisglobal.resultrun.form.RunHoldCause;
import org.openelisglobal.resultrun.form.RunLink;
import org.openelisglobal.resultrun.form.RunQcBlock;
import org.openelisglobal.resultrun.form.RunQcFact;
import org.openelisglobal.resultrun.form.RunTestVerdict;
import org.openelisglobal.resultrun.valueholder.ResultRun;
import org.openelisglobal.resultrun.valueholder.ResultRunEvent;
import org.openelisglobal.resultrun.valueholder.ResultRunRetest;
import org.openelisglobal.resultrun.valueholder.ResultRunState;
import org.openelisglobal.resultrun.valueholder.ResultRunTestDisposition;
import org.openelisglobal.resultrun.valueholder.RunTestAction;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.service.TestServiceImpl;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testcatalog.service.TestQcPolicyService;
import org.openelisglobal.testcatalog.service.TestQcTargetService;
import org.openelisglobal.testcatalog.valueholder.TestQcPolicy;
import org.openelisglobal.testcatalog.valueholder.TestQcTarget;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RunQcServiceImpl implements RunQcService {

    static final String SYSTEM_USER_ID = "1";

    @Autowired
    private ResultRunDAO resultRunDAO;
    @Autowired
    private ResultRunEventDAO resultRunEventDAO;
    @Autowired
    private QCResultDAO qcResultDAO;
    @Lazy
    @Autowired
    private QCControlLotService qcControlLotService;
    @Lazy
    @Autowired
    private TestQcTargetService testQcTargetService;
    @Lazy
    @Autowired
    private SystemUserService systemUserService;
    @Lazy
    @Autowired
    private QCResultService qcResultService;
    @Lazy
    @Autowired
    private TestService testService;
    @Autowired
    private ResultRunTestDispositionDAO dispositionDAO;
    @Lazy
    @Autowired
    private AnalyzerResultsService analyzerResultsService;
    @Lazy
    @Autowired
    private QcViolationNceService qcViolationNceService;
    @Lazy
    @Autowired
    private NCEventService ncEventService;
    @Autowired
    private ResultRunRetestDAO retestDAO;
    @Lazy
    @Autowired
    private TestQcPolicyService testQcPolicyService;

    @Override
    @Transactional(readOnly = true)
    public Map<String, RunTestVerdict> testVerdicts(Long runId) {
        List<QCResult> controls = qcResultDAO.findByRunIds(List.of(runId));
        Set<String> tests = new LinkedHashSet<>();
        resultRunDAO.getStagedRows(List.of(runId)).stream()
                .filter(row -> !row.getIsControl() && StringUtils.isNotBlank(row.getTestId()))
                .map(AnalyzerResults::getTestId).forEach(tests::add);
        resultRunDAO.getTestIdsOfResults(runId).stream().filter(Objects::nonNull).forEach(tests::add);
        controls.stream().map(QCResult::getTestId).filter(Objects::nonNull).forEach(tests::add);
        dispositionDAO.getDispositions(runId).stream().map(ResultRunTestDisposition::getTestId).filter(Objects::nonNull)
                .forEach(tests::add);
        Map<String, TestQcPolicy> policies = testQcPolicyService.policiesOf(tests);
        Map<String, RunTestVerdict> verdicts = new LinkedHashMap<>();
        for (String testId : tests) {
            verdicts.put(testId, verdictOf(controls.stream().filter(c -> testId.equals(c.getTestId())).toList(),
                    policies.get(testId)));
        }
        return verdicts;
    }

    /**
     * FR-C3, FR-H1, FR-H2: Fail if a covering control failed, Pass if every one
     * passed (a control the analyzer sent counts as one recorded), and with none
     * recorded, No control when the test requires one per run, else Not required.
     */
    static RunTestVerdict verdictOf(List<QCResult> covering, TestQcPolicy policy) {
        if (covering.isEmpty()) {
            return policy != null && policy.isControlRequired() ? RunTestVerdict.NO_CONTROL
                    : RunTestVerdict.NOT_REQUIRED;
        }
        return covering.stream().anyMatch(control -> control.getRunVerdict() == RunControlVerdict.FAIL)
                ? RunTestVerdict.FAIL
                : RunTestVerdict.PASS;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> heldTestIds(Long runId) {
        return new LinkedHashSet<>(holdCauses(runId).keySet());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, RunHoldCause> holdCauses(Long runId) {
        Map<String, RunHoldCause> causes = holdCausesOf(runId);
        dispositionDAO.getDispositions(runId).forEach(decision -> causes.remove(decision.getTestId()));
        return causes;
    }

    /**
     * Every test the run holds or held, decided or not: a failed control holds its
     * test (FR-C4), and so does a required control that is missing under a Hold
     * policy, while the run is open or held (FR-A3, FR-C7). A run that is complete
     * is never held by a policy changed after it.
     */
    private Map<String, RunHoldCause> holdCausesOf(Long runId) {
        Map<String, RunHoldCause> causes = new LinkedHashMap<>();
        failedTestIds(runId).forEach(testId -> causes.put(testId, RunHoldCause.QC_FAILED));
        missingHeldTestIds(runId).forEach(testId -> causes.putIfAbsent(testId, RunHoldCause.QC_MISSING));
        return causes;
    }

    private Set<String> missingHeldTestIds(Long runId) {
        ResultRun run = resultRunDAO.get(runId).orElse(null);
        if (run == null || run.getState() == null || !run.getState().isActive()) {
            return Set.of();
        }
        Set<String> missing = testVerdicts(runId).entrySet().stream()
                .filter(entry -> entry.getValue() == RunTestVerdict.NO_CONTROL).map(Map.Entry::getKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (missing.isEmpty()) {
            return Set.of();
        }
        Map<String, TestQcPolicy> policies = testQcPolicyService.policiesOf(missing);
        return missing.stream()
                .filter(testId -> policies.get(testId) != null && policies.get(testId).holdsWhenControlMissing())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> failedTestIds(Long runId) {
        return qcResultDAO.findByRunIds(List.of(runId)).stream()
                .filter(control -> control.getRunVerdict() == RunControlVerdict.FAIL).map(QCResult::getTestId)
                .filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * The tests whose stored results the run keeps from release: every failed test
     * but one accepted despite the failure.
     */
    private Set<String> blockingTestIds(Long runId) {
        Set<String> blocking = new LinkedHashSet<>(holdCausesOf(runId).keySet());
        dispositionDAO.getDispositions(runId).stream()
                .filter(decision -> decision.getAction() == RunTestAction.ACCEPT_DESPITE)
                .forEach(decision -> blocking.remove(decision.getTestId()));
        return blocking;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> holdingRuns(Collection<String> analysisIds) {
        return runsHolding(analysisIds, this::blockingTestIds);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, RunQcFact> qcOfAnalyses(Collection<String> analysisIds) {
        List<String> ids = analysisIds == null ? List.of()
                : analysisIds.stream().filter(StringUtils::isNumeric).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<String, Object[]> latestRunOfAnalysis = new HashMap<>();
        for (Object[] row : resultRunDAO.getRunsOfAnalyses(ids)) {
            String analysisId = String.valueOf(row[0]);
            Object[] known = latestRunOfAnalysis.get(analysisId);
            if (known == null || ((Number) row[1]).longValue() > ((Number) known[1]).longValue()) {
                latestRunOfAnalysis.put(analysisId, row);
            }
        }
        Map<Long, Map<String, RunQcFact>> factsByRun = new HashMap<>();
        Map<String, RunQcFact> facts = new HashMap<>();
        latestRunOfAnalysis.forEach((analysisId, row) -> {
            Long runId = ((Number) row[1]).longValue();
            RunQcFact fact = factsByRun.computeIfAbsent(runId, this::factsOfRun).get(String.valueOf(row[2]));
            if (fact != null) {
                facts.put(analysisId, fact);
            }
        });
        return facts;
    }

    private Map<String, RunQcFact> factsOfRun(Long runId) {
        Map<String, RunTestVerdict> verdicts = testVerdicts(runId);
        List<QCResult> controls = controlsOf(runId);
        Map<String, QCControlLot> lots = new HashMap<>();
        Map<String, ResultRunTestDisposition> decisions = new HashMap<>();
        dispositionDAO.getDispositions(runId).forEach(decision -> decisions.put(decision.getTestId(), decision));
        Map<String, RunQcFact> facts = new HashMap<>();
        verdicts.forEach((testId, verdict) -> {
            ResultRunTestDisposition decision = decisions.get(testId);
            facts.put(testId,
                    new RunQcFact(runId, ResultRun.labelOf(runId), verdict.name(),
                            decision == null ? null : decision.getAction().name(),
                            decision == null ? null : decision.getReason(),
                            decision == null || decision.getNceId() == null ? null : nceNumberOf(decision.getNceId()),
                            controls.stream().filter(control -> testId.equals(control.getTestId()))
                                    .map(control -> controlLine(control, lots)).toList()));
        });
        return facts;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> entryHoldingRuns(Collection<String> analysisIds) {
        return runsHolding(analysisIds, this::heldTestIds);
    }

    private Map<String, Long> runsHolding(Collection<String> analysisIds, Function<Long, Set<String>> testsHeldBy) {
        List<String> ids = analysisIds == null ? List.of()
                : analysisIds.stream().filter(StringUtils::isNumeric).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, Set<String>> heldByRun = new HashMap<>();
        Map<String, Long> held = new HashMap<>();
        for (Object[] row : resultRunDAO.getRunsOfAnalyses(ids)) {
            String analysisId = String.valueOf(row[0]);
            Long runId = ((Number) row[1]).longValue();
            String testId = String.valueOf(row[2]);
            if (heldByRun.computeIfAbsent(runId, testsHeldBy).contains(testId)) {
                held.put(analysisId, runId);
            }
        }
        return held;
    }

    @Override
    @Transactional(readOnly = true)
    public void detachFromDecidedRuns(Collection<Result> results) {
        if (results == null) {
            return;
        }
        Map<Long, Set<String>> setAsideByRun = new HashMap<>();
        for (Result result : results) {
            if (result == null || result.getRunId() == null || result.getAnalysis() == null
                    || result.getAnalysis().getTest() == null) {
                continue;
            }
            Set<String> setAside = setAsideByRun.computeIfAbsent(result.getRunId(),
                    runId -> dispositionDAO.getDispositions(runId).stream()
                            .filter(decision -> decision.getAction() != RunTestAction.ACCEPT_DESPITE)
                            .map(ResultRunTestDisposition::getTestId).collect(Collectors.toSet()));
            if (setAside.contains(result.getAnalysis().getTest().getId())) {
                result.setRunId(null);
                result.setAnalyzerReportedValue(null);
            }
        }
    }

    @Override
    @Transactional
    public void attachAnalyzerControl(QCResult control, Long runId, String componentId) {
        if (control == null || runId == null) {
            return;
        }
        control.setRunId(runId);
        if (control.getQualitativeOutcome() != null) {
            control.setRunVerdict(
                    control.getQualitativeOutcome().isFailing() ? RunControlVerdict.FAIL : RunControlVerdict.PASS);
            control.setVerdictSource(RunVerdictSource.COMPUTED);
        } else if (control.getResultValue() != null) {
            judgeAgainstTarget(control, componentId);
        }
        qcResultDAO.update(control);
        logControl(control, SYSTEM_USER_ID);
        refreshState(runId, SYSTEM_USER_ID);
    }

    private void judgeAgainstTarget(QCResult control, String componentId) {
        QCControlLot lot = control.getControlLotId() == null ? null
                : qcControlLotService.get(control.getControlLotId());
        TestQcTarget target = lot == null ? null
                : testQcTargetService.resolveEffectiveTarget(control.getTestId(), componentId, lot.getControlLevel(),
                        lot.getId());
        if (target == null || target.getExpectedValue() == null || target.getUncertainty() == null) {
            return;
        }
        BigDecimal expected = target.getExpectedValue();
        BigDecimal uncertainty = target.getUncertainty();
        control.setExpectedValue(expected);
        control.setUncertainty(uncertainty);
        boolean within = control.getResultValue().subtract(expected).abs().compareTo(uncertainty) <= 0;
        control.setRunVerdict(within ? RunControlVerdict.PASS : RunControlVerdict.FAIL);
        control.setVerdictSource(RunVerdictSource.COMPUTED);
    }

    @Override
    @Transactional
    public void attachRecordedControl(QCResult control, Long runId, String sysUserId) {
        control.setRunId(runId);
        control.setRunVerdict(control.getQualitativeOutcome() != null && control.getQualitativeOutcome().isFailing()
                ? RunControlVerdict.FAIL
                : RunControlVerdict.PASS);
        control.setVerdictSource(RunVerdictSource.DECLARED);
        qcResultDAO.update(control);
        logControl(control, sysUserId);
        refreshState(runId, sysUserId);
    }

    /**
     * A measured control whose stated Pass or Fail disagrees with its own value
     * against the expected value and uncertainty typed with it.
     */
    static boolean contradictsItsNumbers(BenchQCCaptureForm capture) {
        if (capture.resultValue() == null || capture.expectedValue() == null || capture.uncertainty() == null
                || capture.qualitativeOutcome() == null) {
            return false;
        }
        boolean within = capture.resultValue().subtract(capture.expectedValue()).abs()
                .compareTo(capture.uncertainty()) <= 0;
        return within == capture.qualitativeOutcome().isFailing();
    }

    @Override
    @Transactional(readOnly = true)
    public List<QCResult> controlsOf(Long runId) {
        return qcResultDAO.findByRunIds(List.of(runId));
    }

    @Override
    @Transactional
    public void refreshState(Long runId, String sysUserId) {
        ResultRun run = resultRunDAO.get(runId).orElse(null);
        if (run == null) {
            return;
        }
        Map<String, RunHoldCause> causes = holdCauses(runId);
        Set<String> held = causes.keySet();
        Set<String> wasHeld = new HashSet<>();
        for (ResultRunEvent event : resultRunEventDAO.getEvents(runId)) {
            if (ResultRunEvent.TEST_HELD.equals(event.getEvent())) {
                wasHeld.add(event.getTestId());
            } else if (ResultRunEvent.TEST_RELEASED.equals(event.getEvent())) {
                wasHeld.remove(event.getTestId());
            }
        }
        held.stream().filter(testId -> !wasHeld.contains(testId)).forEach(testId -> log(runId, ResultRunEvent.TEST_HELD,
                testId, causes.get(testId) == RunHoldCause.QC_MISSING ? "qcMissing" : "qcFailed", sysUserId));
        wasHeld.stream().filter(testId -> !held.contains(testId))
                .forEach(testId -> log(runId, ResultRunEvent.TEST_RELEASED, testId, null, sysUserId));
        if (run.getState() == null || !run.getState().isActive()) {
            return;
        }
        ResultRunState state = held.isEmpty() ? ResultRunState.OPEN : ResultRunState.HELD;
        if (run.getState() != state) {
            run.setState(state);
            run.setSysUserId(sysUserId);
            resultRunDAO.update(run);
        }
    }

    @Override
    @Transactional
    public void refreshActiveRunsOfTest(String testId, String sysUserId) {
        if (StringUtils.isBlank(testId)) {
            return;
        }
        for (ResultRun run : resultRunDAO.getActiveRuns()) {
            if (testVerdicts(run.getId()).containsKey(testId)) {
                refreshState(run.getId(), sysUserId);
            }
        }
    }

    private void logControl(QCResult control, String sysUserId) {
        String detail = (control.getRunVerdict() == null ? "NOT_EVALUATED" : control.getRunVerdict().name())
                + (control.getVerdictSource() == null ? "" : " " + control.getVerdictSource().name());
        log(control.getRunId(), ResultRunEvent.CONTROL_RECORDED, control.getTestId(), detail, sysUserId);
    }

    private void log(Long runId, String eventName, String testId, String detail, String sysUserId) {
        ResultRunEvent event = new ResultRunEvent();
        event.setRunId(runId);
        event.setEvent(eventName);
        event.setTestId(testId);
        event.setDetail(detail);
        event.setActorId(sysUserId);
        event.setOccurredAt(new Timestamp(System.currentTimeMillis()));
        event.setSysUserId(sysUserId);
        resultRunEventDAO.insert(event);
    }

    @Override
    @Transactional(readOnly = true)
    public RunQcBlock qcBlock(Long runId) {
        Map<String, RunTestVerdict> verdicts = testVerdicts(runId);
        List<QCResult> controls = controlsOf(runId);
        Map<String, QCControlLot> lots = new HashMap<>();
        Map<String, ResultRunTestDisposition> decisions = new HashMap<>();
        dispositionDAO.getDispositions(runId).forEach(decision -> decisions.put(decision.getTestId(), decision));
        Map<String, RunHoldCause> everHeld = holdCausesOf(runId);
        decisions.keySet().forEach(testId -> everHeld.putIfAbsent(testId,
                verdicts.get(testId) == RunTestVerdict.FAIL ? RunHoldCause.QC_FAILED : RunHoldCause.QC_MISSING));
        Map<String, RunHoldCause> held = new LinkedHashMap<>(everHeld);
        decisions.keySet().forEach(held::remove);
        Map<String, TestQcPolicy> policies = testQcPolicyService.policiesOf(verdicts.keySet());
        Map<String, List<RunLink>> retestOf = retestOf(runId);
        Map<String, List<RunLink>> supersededBy = supersededBy(runId);
        List<RunQcBlock.TestLine> lines = verdicts.entrySet().stream().map(entry -> {
            ResultRunTestDisposition decision = decisions.get(entry.getKey());
            return new RunQcBlock.TestLine(entry.getKey(), TestServiceImpl.getUserLocalizedTestName(entry.getKey()),
                    entry.getValue().name(),
                    controls.stream().filter(control -> entry.getKey().equals(control.getTestId()))
                            .map(control -> controlLine(control, lots)).toList(),
                    decision == null ? null : decision.getAction().name(),
                    decision == null || decision.getNceId() == null ? null : nceNumberOf(decision.getNceId()),
                    retestOf.getOrDefault(entry.getKey(), List.of()),
                    offersRetestLinks(entry.getKey(), held.keySet(), decision)
                            ? retestSuggestions(runId, entry.getKey())
                            : List.of(),
                    supersededBy.getOrDefault(entry.getKey(), List.of()), held.get(entry.getKey()),
                    whenMissingOf(policies.get(entry.getKey())));
        }).toList();
        return new RunQcBlock(overallOf(verdicts.values(), held.keySet(), everHeld), lines);
    }

    private static String whenMissingOf(TestQcPolicy policy) {
        return policy == null || !policy.isControlRequired() ? null : policy.getControlWhenMissing().name();
    }

    private String nceNumberOf(Integer nceId) {
        NcEvent nce = ncEventService.get(nceId);
        return nce == null ? null : nce.getNceNumber();
    }

    /**
     * Held while a held test is undecided; Decided once every held test has a
     * decision, so a failure, or a missing control, stays visible after it is dealt
     * with; No control when a test lacks a required control and its policy only
     * warns (FR-E2).
     */
    static String overallOf(Collection<RunTestVerdict> verdicts, Set<String> heldUndecided,
            Map<String, RunHoldCause> everHeld) {
        if (!heldUndecided.isEmpty()) {
            return "HELD";
        }
        if (!everHeld.isEmpty()) {
            return everHeld.values().stream().allMatch(cause -> cause == RunHoldCause.QC_MISSING) ? "DECIDED_MISSING"
                    : "DECIDED";
        }
        if (verdicts.contains(RunTestVerdict.NO_CONTROL)) {
            return RunTestVerdict.NO_CONTROL.name();
        }
        if (verdicts.contains(RunTestVerdict.PASS)) {
            return RunTestVerdict.PASS.name();
        }
        return RunTestVerdict.NOT_REQUIRED.name();
    }

    private RunQcBlock.ControlLine controlLine(QCResult control, Map<String, QCControlLot> lots) {
        QCControlLot lot = control.getControlLotId() == null ? null
                : lots.computeIfAbsent(control.getControlLotId(), qcControlLotService::get);
        String observed = control.getResultValue() != null
                ? control.getResultValue().stripTrailingZeros().toPlainString()
                : control.getQualitativeOutcome() == null ? null : control.getQualitativeOutcome().name();
        String expected = control.getExpectedValue() == null ? null
                : control.getExpectedValue().stripTrailingZeros().toPlainString()
                        + (control.getUncertainty() == null ? ""
                                : " ± " + control.getUncertainty().stripTrailingZeros().toPlainString());
        return new RunQcBlock.ControlLine(control.getId(),
                lot == null ? control.getControlLabel() : lot.getControlLevel(),
                lot == null ? null : lot.getLotNumber(), observed, expected,
                control.getRunVerdict() == null ? null : control.getRunVerdict().name(),
                control.getVerdictSource() == null ? null : control.getVerdictSource().name(), operatorOf(control),
                control.getRunDateTime() == null ? null : control.getRunDateTime().toInstant().toString());
    }

    private String operatorOf(QCResult control) {
        Integer userId = control.getTechnicianId() != null ? control.getTechnicianId() : control.getSystemUserId();
        if (userId == null) {
            return null;
        }
        SystemUser user = systemUserService.getUserById(String.valueOf(userId));
        return user == null ? null : StringUtils.trim(user.getNameForDisplay());
    }

    @Override
    @Transactional
    public QCResult recordControl(Long runId, BenchQCCaptureForm capture, int sysUserId) {
        if (capture.testId() == null || !testVerdicts(runId).containsKey(capture.testId())) {
            throw new IllegalArgumentException(MessageUtil.getMessage("run.refusal.testNotInRun"));
        }
        if (contradictsItsNumbers(capture)) {
            throw new IllegalArgumentException(MessageUtil.getMessage("run.refusal.outcomeContradicts"));
        }
        String decidedTest = capture.testId();
        if (dispositionDAO.getDispositions(runId).stream()
                .anyMatch(decision -> decidedTest.equals(decision.getTestId()))) {
            throw new IllegalArgumentException(MessageUtil.getMessage("run.refusal.decided"));
        }
        if (StringUtils.isBlank(capture.testSectionId()) && capture.testId() != null) {
            Test test = testService.get(capture.testId());
            capture = new BenchQCCaptureForm(capture.source(), capture.testId(),
                    test == null || test.getTestSection() == null ? null : test.getTestSection().getId(),
                    capture.controlLotId(), capture.controlLabel(), capture.resultValue(), capture.unitOfMeasure(),
                    capture.qualitativeOutcome(), capture.expectedValue(), capture.uncertainty(), capture.runDateTime(),
                    capture.notes());
        }
        QCResult control = qcResultService.createBenchQCResult(capture, sysUserId);
        attachRecordedControl(control, runId, String.valueOf(sysUserId));
        return control;
    }

    @Override
    @Transactional
    public ResultRunTestDisposition decide(Long runId, String testId, RunTestAction action, String reason,
            boolean fileNce, String sysUserId) {
        if (action == null || testId == null || !heldTestIds(runId).contains(testId)) {
            throw new IllegalArgumentException(MessageUtil.getMessage("run.refusal.notHeld"));
        }
        if (action == RunTestAction.ACCEPT_DESPITE && StringUtils.isBlank(reason)) {
            throw new IllegalArgumentException(MessageUtil.getMessage("run.refusal.reasonRequired"));
        }
        ResultRun run = resultRunDAO.get(runId).orElseThrow();
        List<String> analysisIds = resultRunDAO.getAnalysisIdsOfRunTest(runId, testId);
        ResultRunTestDisposition decision = new ResultRunTestDisposition();
        decision.setRunId(runId);
        decision.setTestId(testId);
        decision.setAction(action);
        decision.setReason(StringUtils.trimToNull(reason));
        decision.setDecidedBy(sysUserId);
        decision.setDecidedAt(new Timestamp(System.currentTimeMillis()));
        decision.setSysUserId(sysUserId);
        try {
            dispositionDAO.insert(decision);
        } catch (RuntimeException e) {
            if (isDuplicateDecision(e)) {
                throw new IllegalArgumentException(MessageUtil.getMessage("run.refusal.notHeld"));
            }
            throw e;
        }
        if (action == RunTestAction.ACCEPT_DESPITE || (action == RunTestAction.REJECT && fileNce)) {
            decision.setNceId(fileNce(run, testId, action, decision, analysisIds, sysUserId).getId());
            dispositionDAO.update(decision);
        }
        if (action == RunTestAction.RETEST) {
            rememberSetAside(decision, analysisIds, sysUserId);
        }
        if (action != RunTestAction.ACCEPT_DESPITE) {
            resultRunDAO.getStagedRows(List.of(runId)).stream()
                    .filter(row -> !row.getIsControl() && testId.equals(row.getTestId())).forEach(row -> {
                        releaseLaterReadingsOf(row, sysUserId);
                        analyzerResultsService.delete(row);
                    });
        }
        log(runId, eventOf(action), testId, decision.getNceId() == null ? null : "nce " + decision.getNceId(),
                sysUserId);
        refreshState(runId, sysUserId);
        return decision;
    }

    /**
     * A reading of the same sample and test that arrived after this one was flagged
     * as its duplicate; with this one set aside it is a reading of its own again,
     * open to review and acceptance.
     */
    private void releaseLaterReadingsOf(AnalyzerResults row, String sysUserId) {
        for (AnalyzerResults later : analyzerResultsService.getAllMatching("duplicateAnalyzerResultId", row.getId())) {
            if (later.getId().equals(row.getId())) {
                continue;
            }
            later.setDuplicateAnalyzerResultId(null);
            later.setReadOnly(false);
            later.setSysUserId(sysUserId);
            analyzerResultsService.update(later);
        }
    }

    private void rememberSetAside(ResultRunTestDisposition decision, List<String> analysisIds, String sysUserId) {
        for (String analysisId : analysisIds) {
            ResultRunRetest retest = new ResultRunRetest();
            retest.setDispositionId(decision.getId());
            retest.setAnalysisId(analysisId);
            retest.setSysUserId(sysUserId);
            retestDAO.insert(retest);
        }
    }

    /**
     * A run may be the retest of an earlier run's test unless its own control holds
     * that test undecided or it set the test aside itself.
     */
    private static boolean offersRetestLinks(String testId, Set<String> held, ResultRunTestDisposition decision) {
        return !held.contains(testId) && (decision == null || decision.getAction() == RunTestAction.ACCEPT_DESPITE);
    }

    private List<RunLink> retestSuggestions(Long runId, String testId) {
        Map<Long, Long> unlinkedByRun = retestDAO
                .findUnlinked(testId, resultRunDAO.getAnalysisIdsOfRunTest(runId, testId), runId).stream()
                .collect(Collectors.groupingBy(RetestedSample::runId, java.util.TreeMap::new, Collectors.counting()));
        return unlinkedByRun.entrySet().stream().map(entry -> new RunLink(entry.getKey(),
                ResultRun.labelOf(entry.getKey()), entry.getValue().intValue(), setAsideCount(entry.getKey(), testId)))
                .toList();
    }

    private int setAsideCount(Long runId, String testId) {
        return (int) retestDAO.findSetAsideBy(runId).stream().filter(sample -> testId.equals(sample.testId())).count();
    }

    private Map<String, List<RunLink>> retestOf(Long runId) {
        Map<String, Map<Long, Long>> byTest = retestDAO.findRetestedIn(runId).stream()
                .collect(Collectors.groupingBy(RetestedSample::testId,
                        Collectors.groupingBy(RetestedSample::runId, java.util.TreeMap::new, Collectors.counting())));
        Map<String, List<RunLink>> links = new HashMap<>();
        byTest.forEach(
                (testId, counts) -> links
                        .put(testId,
                                counts.entrySet().stream()
                                        .map(entry -> new RunLink(entry.getKey(), ResultRun.labelOf(entry.getKey()),
                                                entry.getValue().intValue(), setAsideCount(entry.getKey(), testId)))
                                        .toList()));
        return links;
    }

    private Map<String, List<RunLink>> supersededBy(Long runId) {
        Map<String, List<RetestedSample>> byTest = retestDAO.findSetAsideBy(runId).stream()
                .collect(Collectors.groupingBy(RetestedSample::testId));
        Map<String, List<RunLink>> links = new HashMap<>();
        byTest.forEach((testId, samples) -> {
            Map<Long, Long> byRetestRun = samples.stream().filter(sample -> sample.retestedInRunId() != null)
                    .collect(Collectors.groupingBy(RetestedSample::retestedInRunId, java.util.TreeMap::new,
                            Collectors.counting()));
            if (!byRetestRun.isEmpty()) {
                links.put(testId,
                        byRetestRun
                                .entrySet().stream().map(entry -> new RunLink(entry.getKey(),
                                        ResultRun.labelOf(entry.getKey()), entry.getValue().intValue(), samples.size()))
                                .toList());
            }
        });
        return links;
    }

    @Override
    @Transactional
    public void confirmRetest(Long runId, String testId, Long retestOfRunId, String sysUserId) {
        ResultRunTestDisposition decision = testId == null ? null
                : dispositionDAO.getDispositions(runId).stream().filter(d -> testId.equals(d.getTestId())).findFirst()
                        .orElse(null);
        List<RetestedSample> unlinked = testId == null || retestOfRunId == null
                || !offersRetestLinks(testId, heldTestIds(runId), decision) ? List.of()
                        : retestDAO.findUnlinked(testId, resultRunDAO.getAnalysisIdsOfRunTest(runId, testId), runId)
                                .stream().filter(sample -> retestOfRunId.equals(sample.runId())).toList();
        int linked = unlinked.isEmpty() ? 0
                : retestDAO.link(unlinked.get(0).dispositionId(),
                        unlinked.stream().map(RetestedSample::analysisId).toList(), runId, sysUserId,
                        new Timestamp(System.currentTimeMillis()));
        if (linked == 0) {
            throw new IllegalArgumentException(MessageUtil.getMessage("run.refusal.noRetestToLink"));
        }
        log(retestOfRunId, ResultRunEvent.SUPERSEDED, testId,
                "by " + ResultRun.labelOf(runId) + ": " + linked + " of " + setAsideCount(retestOfRunId, testId),
                sysUserId);
        supersedeIfRetested(retestOfRunId, sysUserId);
    }

    @Override
    @Transactional
    public void supersedeIfRetested(Long runId, String sysUserId) {
        ResultRun run = runId == null ? null : resultRunDAO.get(runId).orElse(null);
        if (run == null || run.getState() == ResultRunState.SUPERSEDED
                || (run.getState() == ResultRunState.OPEN && resultRunDAO.countStagedRows(runId) > 0)) {
            return;
        }
        Map<String, List<RetestedSample>> setAside = retestDAO.findSetAsideBy(runId).stream()
                .collect(Collectors.groupingBy(RetestedSample::testId));
        if (setAside.isEmpty()) {
            return;
        }
        Set<String> covered = testVerdicts(runId).keySet();
        boolean replaced = !covered.isEmpty() && covered.stream().allMatch(testId -> setAside.containsKey(testId)
                && setAside.get(testId).stream().allMatch(sample -> sample.retestedInRunId() != null));
        if (!replaced) {
            return;
        }
        run.setState(ResultRunState.SUPERSEDED);
        run.setSysUserId(sysUserId);
        resultRunDAO.update(run);
        log(runId, ResultRunEvent.SUPERSEDED, null,
                setAside.values().stream().flatMap(List::stream).map(RetestedSample::retestedInRunId).distinct()
                        .sorted().map(ResultRun::labelOf).collect(Collectors.joining(", ", "by ", "")),
                sysUserId);
    }

    /**
     * A second decision on the same test that passed the held check together with
     * the first, and lost on the run-and-test unique key when the first committed.
     */
    static boolean isDuplicateDecision(Throwable e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation && violation.getConstraintName() != null
                    && violation.getConstraintName().toLowerCase().endsWith("uq_run_test_disposition")) {
                return true;
            }
        }
        return false;
    }

    private static String eventOf(RunTestAction action) {
        return switch (action) {
        case RETEST -> ResultRunEvent.RETEST_REQUESTED;
        case REJECT -> ResultRunEvent.REJECTED_QC;
        case ACCEPT_DESPITE -> ResultRunEvent.ACCEPTED_DESPITE_QC;
        };
    }

    private NcEvent fileNce(ResultRun run, String testId, RunTestAction action, ResultRunTestDisposition decision,
            List<String> analysisIds, String sysUserId) {
        String testName = TestServiceImpl.getUserLocalizedTestName(testId);
        String failing = controlsOf(run.getId()).stream().filter(
                control -> testId.equals(control.getTestId()) && control.getRunVerdict() == RunControlVerdict.FAIL)
                .map(control -> {
                    RunQcBlock.ControlLine line = controlLine(control, new HashMap<>());
                    return (line.level() == null ? "" : line.level() + " ")
                            + (line.observed() == null ? "" : line.observed())
                            + (line.expected() == null ? "" : " against " + line.expected());
                }).collect(Collectors.joining("; "));
        boolean missing = holdCausesOf(run.getId()).get(testId) == RunHoldCause.QC_MISSING;
        String verb = missing
                ? action == RunTestAction.ACCEPT_DESPITE ? "accepted without its required control"
                        : "rejected for a missing control"
                : action == RunTestAction.ACCEPT_DESPITE ? "accepted despite the QC failure" : "rejected on QC";
        String title = (missing ? "Missing control on " : "QC failure on ") + testName + " in " + run.getLabel() + ": "
                + verb;
        String cause = missing ? "The test requires a control on every run, and none was recorded on this one."
                : "Failing control: " + (failing.isBlank() ? "not recorded" : failing) + ".";
        String description = testName + " in " + run.getLabel()
                + (run.getAnalyzerId() == null ? "" : " (analyzer " + run.getAnalyzerId() + ")") + " was " + verb + ". "
                + cause + " " + analysisIds.size() + " affected result(s)."
                + (decision.getReason() == null ? "" : " Reason: " + decision.getReason());
        return qcViolationNceService.createNceForRunDecision(String.valueOf(decision.getId()), title, description,
                analysisIds, sysUserId);
    }
}
