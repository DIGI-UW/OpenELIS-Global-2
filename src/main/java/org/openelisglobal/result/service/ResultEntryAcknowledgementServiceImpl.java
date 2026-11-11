package org.openelisglobal.result.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analysis.service.AnalysisAnchorService;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyzerresults.action.beanitems.AnalyzerResultItem;
import org.openelisglobal.analyzerresults.service.AnalyzerResultsService;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.openelisglobal.common.services.IResultSaveService;
import org.openelisglobal.common.services.RuleResultScope;
import org.openelisglobal.common.services.registration.interfaces.IResultUpdate;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.result.action.util.ResultEntryAlert;
import org.openelisglobal.result.action.util.ResultSet;
import org.openelisglobal.result.dao.ResultEntryAcknowledgementDAO;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.result.valueholder.ResultEntryAcknowledgement;
import org.openelisglobal.resultlimit.service.ResultLimitService;
import org.openelisglobal.resultlimits.valueholder.ResultLimit;
import org.openelisglobal.resultvalidation.bean.AnalysisItem;
import org.openelisglobal.resultvalidation.util.ValidationSignals;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.test.beanItems.TestResultItem;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresultcomponent.service.TestResultComponentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Plain (non-audited) service: an acknowledgement row is write-once and is
 * itself the record.
 *
 * <p>
 * Both settings are read from {@link ConfigurationProperties} on every call,
 * which Result Configuration reloads when it saves, so a change in Admin
 * applies to the next save without a restart.
 */
@Service
public class ResultEntryAcknowledgementServiceImpl extends BaseObjectServiceImpl<ResultEntryAcknowledgement, String>
        implements ResultEntryAcknowledgementService {

    /**
     * The value the shipped site_information row carries until an admin sets one.
     */
    static final String CRITICAL_MESSAGE_PLACEHOLDER = "Set new critical result message";

    @Autowired
    private ResultEntryAcknowledgementDAO acknowledgementDAO;
    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private ResultService resultService;
    @Autowired
    private ResultLimitService resultLimitService;
    @Autowired
    private AnalysisAnchorService analysisAnchorService;
    @Autowired
    private SampleHumanService sampleHumanService;
    @Autowired
    private TestResultComponentService testResultComponentService;
    @Autowired
    private AnalyzerResultsService analyzerResultsService;
    @Autowired
    private SampleService sampleService;
    @Autowired
    private TestResultService testResultService;
    @Autowired
    private DictionaryService dictionaryService;
    @Autowired
    private RuleResultScope ruleResultScope;

    public ResultEntryAcknowledgementServiceImpl() {
        super(ResultEntryAcknowledgement.class);
    }

    @Override
    protected ResultEntryAcknowledgementDAO getBaseObjectDAO() {
        return acknowledgementDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResultEntryAcknowledgement> getByAnalysisId(String analysisId) {
        return acknowledgementDAO.getByAnalysisId(analysisId);
    }

    @Override
    public List<ResultEntryAlert> alertsFor(ResultLimit limit, String resultType, String value, String previousValue) {
        List<ResultEntryAlert> alerts = new ArrayList<>();
        if (GenericValidator.isBlankOrNull(value)) {
            return alerts;
        }
        String numeric = value.trim().replaceAll("[<>]", "").trim();
        String flag = ValidationSignals.resultFlag(limit, resultType, numeric);
        if (flag == null || sameNumber(numeric, previousValue)) {
            return alerts;
        }
        if (ValidationSignals.FLAG_INVALID.equals(flag) && isInvalidAlertEnabled()) {
            alerts.add(newAlert(ResultEntryAlert.KIND_INVALID, value, limit));
        }
        if (ValidationSignals.FLAG_CRITICAL.equals(flag)
                || (ValidationSignals.FLAG_INVALID.equals(flag) && criticalNumber(limit, numeric))) {
            alerts.add(newAlert(ResultEntryAlert.KIND_CRITICAL, value, limit));
        }
        return alerts;
    }

    private static ResultEntryAlert newAlert(String kind, String value, ResultLimit limit) {
        ResultEntryAlert alert = new ResultEntryAlert(kind, value.trim());
        alert.setValidRange(ValidationSignals.authoredBound(limit.getLowValid()),
                ValidationSignals.authoredBound(limit.getHighValid()));
        return alert;
    }

    /**
     * A value beyond a critical bound is critical even when it is also outside the
     * valid range.
     */
    private static boolean criticalNumber(ResultLimit limit, String numeric) {
        try {
            return ValidationSignals.isCritical(limit,
                    Double.parseDouble(StringUtil.normalizeScientificNotation(numeric)));
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public boolean isInvalidAlertEnabled() {
        return ConfigurationProperties.getInstance().isPropertyValueEqual(Property.ALERT_FOR_INVALID_RESULTS, "true");
    }

    @Override
    public String getCustomCriticalMessage() {
        String message = ConfigurationProperties.getInstance().getPropertyValue(Property.customCriticalMessage);
        if (GenericValidator.isBlankOrNull(message) || CRITICAL_MESSAGE_PLACEHOLDER.equals(message.trim())) {
            return null;
        }
        return message.trim();
    }

    @Override
    public Map<String, Object> refusalBody(List<ResultEntryAlert> unacknowledged) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", MessageUtil.getMessage("result.entry.acknowledgementRequired"));
        body.put("code", "ACKNOWLEDGEMENT_REQUIRED");
        body.put("acknowledgementRequired", unacknowledged);
        body.put("customCriticalMessage", StringUtil.blankIfNull(getCustomCriticalMessage()));
        return body;
    }

    @Override
    @Transactional
    public void recordAcknowledgements(List<ResultEntryAlert> alerts, String source, String sysUserId) {
        Timestamp now = Timestamp.from(Instant.now());
        for (ResultEntryAlert alert : alerts) {
            if (!alert.isAcknowledged() || GenericValidator.isBlankOrNull(alert.getAnalysisId())) {
                continue;
            }
            ResultEntryAcknowledgement acknowledgement = new ResultEntryAcknowledgement();
            acknowledgement.setAnalysisId(alert.getAnalysisId());
            if (alert.getResult() != null) {
                acknowledgement.setResultId(alert.getResult().getId());
            }
            boolean critical = ResultEntryAlert.KIND_CRITICAL.equals(alert.getKind());
            acknowledgement.setKind(
                    critical ? ResultEntryAcknowledgement.KIND_CRITICAL : ResultEntryAcknowledgement.KIND_INVALID);
            acknowledgement.setResultValue(alert.getValue());
            acknowledgement.setMessage(critical ? criticalMessageShown() : invalidMessageShown(alert));
            acknowledgement.setSource(source);
            acknowledgement.setAcknowledgedBy(sysUserId);
            acknowledgement.setAcknowledgedAt(now);
            acknowledgement.setSysUserId(sysUserId);
            insert(acknowledgement);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResultEntryAlert> alertsForItems(List<TestResultItem> items) {
        List<ResultEntryAlert> alerts = new ArrayList<>();
        for (TestResultItem item : items) {
            if (item.isRejected() || item.isShadowRejected()) {
                continue;
            }
            for (ResultEntryAlert alert : alertsForEntry(item.getAnalysisId(), item.getResultId(),
                    item.getTestResultComponentId(), item.getResultType(), item.getResultValue())) {
                alert.setTestName(item.getTestName());
                alert.setAccessionNumber(item.getAccessionNumber());
                alert.setAcknowledged(
                        ResultEntryAlert.KIND_CRITICAL.equals(alert.getKind()) ? item.isCriticalAcknowledged()
                                : item.isInvalidResultConfirmed());
                alerts.add(alert);
            }
        }
        return alerts;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResultEntryAlert> alertsForValidationItems(List<AnalysisItem> items) {
        List<ResultEntryAlert> alerts = new ArrayList<>();
        for (AnalysisItem item : items) {
            for (ResultEntryAlert alert : alertsForEntry(item.getAnalysisId(), item.getResultId(),
                    item.getTestResultComponentId(), item.getResultType(), item.getResult())) {
                alert.setTestName(item.getTestName());
                alert.setAccessionNumber(item.getAccessionNumber());
                alert.setAcknowledged(
                        ResultEntryAlert.KIND_CRITICAL.equals(alert.getKind()) ? item.isCriticalAcknowledged()
                                : item.isInvalidResultConfirmed());
                alerts.add(alert);
            }
        }
        return alerts;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResultEntryAlert> alertsForAnalyzerItems(List<AnalyzerResultItem> items) {
        List<ResultEntryAlert> alerts = new ArrayList<>();
        for (AnalyzerResultItem item : items) {
            if (!item.getIsAccepted() || item.getIsDeleted() || GenericValidator.isBlankOrNull(item.getId())
                    || GenericValidator.isBlankOrNull(item.getResult())) {
                continue;
            }
            AnalyzerResults staged = analyzerResultsService.get(item.getId());
            if (staged == null || staged.isReadOnly() || GenericValidator.isBlankOrNull(staged.getTestId())) {
                continue;
            }
            String componentId = multiComponent(staged.getTestId()) ? staged.getComponentId() : null;
            List<ResultEntryAlert> owed;
            if ("D".equals(staged.getResultType())) {
                owed = answerAlerts(staged.getTestId(), staged.getComponentId(), item.getResult(), staged.getResult());
            } else if ("N".equals(staged.getResultType())) {
                Sample sample = sampleService.getSampleByAccessionNumber(staged.getAccessionNumber());
                Patient patient = sample == null ? null : sampleHumanService.getPatientForSample(sample);
                ResultLimit limit = GenericValidator.isBlankOrNull(componentId)
                        ? resultLimitService.getResultLimitForTestAndPatient(staged.getTestId(), patient,
                                item.getTypeOfSampleId())
                        : resultLimitService.getResultLimitForComponentAndPatient(componentId, patient,
                                item.getTypeOfSampleId());
                owed = alertsFor(limit, "N", item.getResult(), staged.getResult());
            } else {
                continue;
            }
            for (ResultEntryAlert alert : owed) {
                alert.setRowId(staged.getId());
                alert.setTestId(staged.getTestId());
                alert.setComponentId(componentId);
                alert.setTestName(staged.getTestName());
                alert.setAccessionNumber(staged.getAccessionNumber());
                alert.setAcknowledged(
                        ResultEntryAlert.KIND_CRITICAL.equals(alert.getKind()) ? item.isCriticalAcknowledged()
                                : item.isInvalidResultConfirmed());
                alerts.add(alert);
            }
        }
        return alerts;
    }

    private boolean multiComponent(String testId) {
        return testResultComponentService.getActiveComponentsByTestId(testId).size() >= 2;
    }

    private List<ResultEntryAlert> alertsForEntry(String analysisId, String resultId, String componentId,
            String resultType, String value) {
        if (!("N".equals(resultType) || "D".equals(resultType)) || GenericValidator.isBlankOrNull(value)
                || GenericValidator.isBlankOrNull(analysisId)) {
            return new ArrayList<>();
        }
        Analysis analysis = analysisService.get(analysisId);
        if (analysis == null || analysis.getTest() == null) {
            return new ArrayList<>();
        }
        Result stored = GenericValidator.isBlankOrNull(resultId) ? null : resultService.get(resultId);
        String previousValue = stored == null ? null : stored.getValue();
        String scope = componentScope(analysis, componentId);
        List<ResultEntryAlert> alerts;
        if ("D".equals(resultType)) {
            alerts = answerAlerts(analysis.getTest().getId(), componentId, value, previousValue);
        } else {
            ResultLimit limit = resultLimitService
                    .selectResultLimitForResult(analysis, stored, patientOf(analysis), scope).getResultLimit();
            alerts = alertsFor(limit, resultType, value, previousValue);
        }
        for (ResultEntryAlert alert : alerts) {
            alert.setAnalysisId(analysis.getId());
            alert.setComponentId(scope);
        }
        return alerts;
    }

    private List<ResultEntryAlert> answerAlerts(String testId, String componentId, String value, String previousValue) {
        List<ResultEntryAlert> alerts = new ArrayList<>();
        String answer = value.trim();
        // A blank component is the main record, as the save binds it.
        String component = GenericValidator.isBlankOrNull(componentId) ? ruleResultScope.primaryComponentId(testId)
                : componentId;
        if (answer.equals(StringUtil.blankIfNull(previousValue).trim())
                || !testResultService.isCriticalAnswer(testId, component, "D", answer)) {
            return alerts;
        }
        Dictionary label = dictionaryService.getDictionaryById(answer);
        alerts.add(new ResultEntryAlert(ResultEntryAlert.KIND_CRITICAL,
                label == null ? answer : label.getLocalizedName()));
        return alerts;
    }

    @Override
    public IResultUpdate acknowledgementRecorder(List<ResultEntryAlert> alerts, String source, String sysUserId) {
        return new IResultUpdate() {
            @Override
            public void transactionalUpdate(IResultSaveService saveService) throws LIMSRuntimeException {
                List<ResultSet> saved = new ArrayList<>(saveService.getNewResults());
                saved.addAll(saveService.getModifiedResults());
                for (ResultEntryAlert alert : alerts.stream().filter(pending -> pending.getResult() == null)
                        .collect(Collectors.toList())) {
                    saved.stream().map(rs -> rs.result).filter(result -> savedFor(result, alert)).findFirst()
                            .ifPresent(alert::setResult);
                }
                recordAcknowledgements(alerts, source, sysUserId);
            }

            @Override
            public void postTransactionalCommitUpdate(IResultSaveService saveService) {
            }
        };
    }

    private static boolean savedFor(Result result, ResultEntryAlert alert) {
        if (result == null || result.getAnalysis() == null
                || !alert.getAnalysisId().equals(result.getAnalysis().getId())) {
            return false;
        }
        if (alert.getComponentId() == null) {
            return true;
        }
        return result.getTestResult() != null && alert.getComponentId().equals(result.getTestResult().getComponentId());
    }

    /**
     * A component scopes the limit only on a multi-component test, as on the
     * Results Entry row; a single-component test is judged on its test-level
     * limits.
     */
    private String componentScope(Analysis analysis, String componentId) {
        if (GenericValidator.isBlankOrNull(componentId) || !multiComponent(analysis.getTest().getId())) {
            return null;
        }
        return componentId;
    }

    private Patient patientOf(Analysis analysis) {
        Sample sample = analysisAnchorService.resolveSample(analysis);
        return sample == null ? null : sampleHumanService.getPatientForSample(sample);
    }

    private String criticalMessageShown() {
        String custom = getCustomCriticalMessage();
        return custom != null ? custom : MessageUtil.getMessage("result.critical.defaultMessage");
    }

    private String invalidMessageShown(ResultEntryAlert alert) {
        return MessageUtil.getMessage("result.outOfValidRange.range",
                new Object[] { boundText(alert.getLowValid()), boundText(alert.getHighValid()) });
    }

    private static String boundText(Double bound) {
        return bound == null ? "-" : BigDecimal.valueOf(bound).stripTrailingZeros().toPlainString();
    }

    private static boolean sameNumber(String value, String previousValue) {
        if (GenericValidator.isBlankOrNull(previousValue)) {
            return false;
        }
        try {
            return Double.compare(Double.parseDouble(StringUtil.normalizeScientificNotation(value.trim())),
                    Double.parseDouble(StringUtil.normalizeScientificNotation(previousValue.trim()))) == 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
