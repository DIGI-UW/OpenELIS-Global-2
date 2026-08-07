package org.openelisglobal.testalertrule.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.alert.service.AlertService;
import org.openelisglobal.alert.valueholder.AlertSeverity;
import org.openelisglobal.alert.valueholder.AlertType;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.notification.service.sender.AsyncNotificationDispatcher;
import org.openelisglobal.notification.valueholder.EmailNotification;
import org.openelisglobal.notification.valueholder.RemoteNotification;
import org.openelisglobal.notification.valueholder.SMSNotification;
import org.openelisglobal.notifications.service.HeaderNotificationService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.resultlimit.service.ResultLimitService;
import org.openelisglobal.resultlimits.valueholder.ResultLimit;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.role.valueholder.Role;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testalertrule.valueholder.TestAlertRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TestAlertEvaluationServiceImpl implements TestAlertEvaluationService {

    private static final ObjectMapper CONTEXT_MAPPER = new ObjectMapper();

    @Autowired
    private TestAlertRuleService alertRuleService;

    @Autowired
    private AlertService alertService;

    @Autowired
    private ResultLimitService resultLimitService;

    @Autowired
    private ResultService resultService;

    @Autowired
    private HeaderNotificationService headerNotificationService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private SampleHumanService sampleHumanService;

    // Sends SMS/Email off the request thread so the result-entry response isn't
    // blocked on SMTP / SMS-gateway calls (same async behavior as the default
    // test-notification flow).
    @Autowired
    private AsyncNotificationDispatcher asyncNotificationDispatcher;

    @Override
    @Transactional
    public void evaluateAndDispatch(Result result, String sysUserId) {
        if (result == null || result.getAnalysis() == null) {
            return;
        }
        Test test = result.getAnalysis().getTest();
        if (test == null) {
            return;
        }
        String value = result.getValue();
        boolean critical = isCriticalValue(result, value);
        if (critical) {
            recordCriticalResultAlert(result, test, value);
        }
        List<TestAlertRule> rules = alertRuleService.getByTestId(test.getId());
        if (rules == null || rules.isEmpty()) {
            return;
        }
        for (TestAlertRule rule : rules) {
            if (!Boolean.TRUE.equals(rule.getEnabled()) || !matches(rule, result, value, critical)) {
                continue;
            }
            String testName = test.getLocalizedName() != null ? test.getLocalizedName() : test.getName();
            String subject = "Test alert: " + testName;
            String message = "[ALERT: " + rule.getName() + "] " + testName + (value != null ? " result " + value : "");
            dispatchHeader(rule, message, sysUserId);
            dispatchExternal(rule, subject, message, result);
        }
    }

    private boolean matches(TestAlertRule rule, Result result, String value, boolean critical) {
        String trigger = rule.getTriggerType();
        if (trigger == null) {
            return false;
        }
        switch (trigger) {
        case "ALL":
            return true;
        case "SPECIFIC_VALUE":
            return rule.getTriggerValue() != null && rule.getTriggerValue().equals(value);
        case "ABNORMAL":
            return resultService.isAbnormalDictionaryResult(result);
        case "CRITICAL":
            return critical;
        default:
            // COMPLIANCE_BREACH needs the S-01 compliance module (OGC-528) and
            // doesn't fire until that lands.
            return false;
        }
    }

    /**
     * OGC-1022 (R3) — a numeric value outside the authored critical bounds of the
     * patient-conditional result limit. POSITIVE_INFINITY is the "not authored"
     * sentinel for both bounds, so tests without critical bounds never fire.
     */
    private boolean isCriticalValue(Result result, String value) {
        if (value == null || value.isBlank() || !"N".equals(result.getResultType())) {
            return false;
        }
        try {
            double numeric = Double.parseDouble(value);
            Analysis analysis = result.getAnalysis();
            Patient patient = sampleHumanService.getPatientForSample(analysis.getSampleItem().getSample());
            ResultLimit limit = resultLimitService.getResultLimitForResult(analysis, result, patient);
            if (limit == null) {
                return false;
            }
            boolean criticalLow = limit.getLowCritical() != Double.POSITIVE_INFINITY
                    && numeric < limit.getLowCritical();
            boolean criticalHigh = limit.getHighCritical() != Double.POSITIVE_INFINITY
                    && numeric > limit.getHighCritical();
            return criticalLow || criticalHigh;
        } catch (NumberFormatException e) {
            return false;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            return false;
        }
    }

    /**
     * Posts the critical value to the Alerts dashboard (FR-A4/L: acknowledgment is
     * a follow-up there and never gates Save). AlertService dedupes an already-open
     * alert for the same analysis into a duplicate-count bump. Failure to record
     * the alert must never fail the save that triggered it.
     */
    private void recordCriticalResultAlert(Result result, Test test, String value) {
        try {
            Analysis analysis = result.getAnalysis();
            String accession = analysis.getSampleItem().getSample().getAccessionNumber();
            String testName = test.getLocalizedName() != null ? test.getLocalizedName() : test.getName();
            String message = testName + " result " + value + " is a critical value (order " + accession + ")";
            Map<String, String> context = new LinkedHashMap<>();
            context.put("accessionNumber", accession);
            context.put("testName", testName);
            context.put("value", value);
            context.put("analysisId", analysis.getId());
            alertService.createAlert(AlertType.CRITICAL_RESULT, "ANALYSIS", Long.valueOf(analysis.getId()),
                    AlertSeverity.CRITICAL, message, CONTEXT_MAPPER.writeValueAsString(context));
        } catch (JsonProcessingException | RuntimeException e) {
            LogEvent.logError(e);
        }
    }

    private void dispatchHeader(TestAlertRule rule, String message, String sysUserId) {
        if (notBlank(rule.getNotifyRoleId())) {
            try {
                Role role = roleService.get(rule.getNotifyRoleId());
                if (role != null && role.getName() != null) {
                    headerNotificationService.notifyRole(role.getName(), message);
                }
            } catch (RuntimeException e) {
                LogEvent.logError(e);
            }
        }
        // Always surface to the validating user so the alert is visible in-app.
        if (notBlank(sysUserId)) {
            headerNotificationService.notifyUser(sysUserId, message);
        }
    }

    private void dispatchExternal(TestAlertRule rule, String subject, String message, Result result) {
        if (Boolean.TRUE.equals(rule.getNotifySms())) {
            if (notBlank(rule.getNotifyCustomPhone())) {
                sendSms(rule.getNotifyCustomPhone(), subject, message);
            }
            if (Boolean.TRUE.equals(rule.getNotifyPatient())) {
                String phone = patientContact(result, true);
                if (notBlank(phone)) {
                    sendSms(phone, subject, message);
                }
            }
        }
        if (Boolean.TRUE.equals(rule.getNotifyEmail())) {
            if (notBlank(rule.getNotifyCustomEmail())) {
                sendEmail(rule.getNotifyCustomEmail(), subject, message);
            }
            if (Boolean.TRUE.equals(rule.getNotifyPatient())) {
                String email = patientContact(result, false);
                if (notBlank(email)) {
                    sendEmail(email, subject, message);
                }
            }
        }
        // Ordering-physician and referring-facility recipient resolution is a
        // follow-up; custom + patient channels are wired here.
    }

    private void sendSms(String phone, String subject, String message) {
        SMSNotification sms = new SMSNotification();
        sms.setReceiverPhoneNumber(phone);
        sms.setPayload(new AlertNotificationPayload(subject, message));
        dispatch(sms);
    }

    private void sendEmail(String email, String subject, String message) {
        EmailNotification mail = new EmailNotification();
        mail.setRecipientEmailAddress(email);
        mail.setPayload(new AlertNotificationPayload(subject, message));
        dispatch(mail);
    }

    private void dispatch(RemoteNotification notification) {
        // Fire-and-forget on a separate thread; the notification already carries
        // a fully-resolved string payload, so no Hibernate session is needed.
        asyncNotificationDispatcher.dispatch(notification);
    }

    private String patientContact(Result result, boolean phone) {
        try {
            Patient patient = sampleHumanService.getPatientForSample(result.getAnalysis().getSampleItem().getSample());
            Person person = patient != null ? patient.getPerson() : null;
            if (person == null) {
                return null;
            }
            if (phone) {
                return notBlank(person.getCellPhone()) ? person.getCellPhone() : person.getPrimaryPhone();
            }
            return person.getEmail();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            return null;
        }
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
