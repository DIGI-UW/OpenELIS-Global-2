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
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.qc.dao.QCResultDAO;
import org.openelisglobal.qc.form.BenchQCCaptureForm;
import org.openelisglobal.qc.service.QCControlLotService;
import org.openelisglobal.qc.service.QCResultService;
import org.openelisglobal.qc.valueholder.QCControlLot;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.RunControlVerdict;
import org.openelisglobal.qc.valueholder.RunVerdictSource;
import org.openelisglobal.resultrun.dao.ResultRunDAO;
import org.openelisglobal.resultrun.dao.ResultRunEventDAO;
import org.openelisglobal.resultrun.form.RunQcBlock;
import org.openelisglobal.resultrun.form.RunTestVerdict;
import org.openelisglobal.resultrun.valueholder.ResultRun;
import org.openelisglobal.resultrun.valueholder.ResultRunEvent;
import org.openelisglobal.resultrun.valueholder.ResultRunState;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.service.TestServiceImpl;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testcatalog.service.TestQcTargetService;
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
        Map<String, RunTestVerdict> verdicts = new LinkedHashMap<>();
        for (String testId : tests) {
            verdicts.put(testId, verdictOf(controls.stream().filter(c -> testId.equals(c.getTestId())).toList()));
        }
        return verdicts;
    }

    static RunTestVerdict verdictOf(List<QCResult> covering) {
        if (covering.isEmpty()) {
            return RunTestVerdict.NOT_REQUIRED;
        }
        return covering.stream().anyMatch(control -> control.getRunVerdict() == RunControlVerdict.FAIL)
                ? RunTestVerdict.FAIL
                : RunTestVerdict.PASS;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> heldTestIds(Long runId) {
        return qcResultDAO.findByRunIds(List.of(runId)).stream()
                .filter(control -> control.getRunVerdict() == RunControlVerdict.FAIL).map(QCResult::getTestId)
                .filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> holdingRuns(Collection<String> analysisIds) {
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
            if (heldByRun.computeIfAbsent(runId, this::heldTestIds).contains(testId)) {
                held.put(analysisId, runId);
            }
        }
        return held;
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
        Set<String> held = heldTestIds(runId);
        Set<String> wasHeld = new HashSet<>();
        for (ResultRunEvent event : resultRunEventDAO.getEvents(runId)) {
            if (ResultRunEvent.TEST_HELD.equals(event.getEvent())) {
                wasHeld.add(event.getTestId());
            } else if (ResultRunEvent.TEST_RELEASED.equals(event.getEvent())) {
                wasHeld.remove(event.getTestId());
            }
        }
        held.stream().filter(testId -> !wasHeld.contains(testId))
                .forEach(testId -> log(runId, ResultRunEvent.TEST_HELD, testId, "qcFailed", sysUserId));
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
        List<RunQcBlock.TestLine> lines = verdicts.entrySet().stream()
                .map(entry -> new RunQcBlock.TestLine(entry.getKey(),
                        TestServiceImpl.getUserLocalizedTestName(entry.getKey()), entry.getValue().name(),
                        controls.stream().filter(control -> entry.getKey().equals(control.getTestId()))
                                .map(control -> controlLine(control, lots)).toList()))
                .toList();
        return new RunQcBlock(overallOf(verdicts.values()), lines);
    }

    static String overallOf(Collection<RunTestVerdict> verdicts) {
        if (verdicts.contains(RunTestVerdict.FAIL)) {
            return "HELD";
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
        return user == null ? null : user.getDisplayName();
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
}
