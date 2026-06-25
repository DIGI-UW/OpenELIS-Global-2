package org.openelisglobal.testalertrule.service;

import java.util.List;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.notification.service.sender.ClientNotificationSender;
import org.openelisglobal.notification.valueholder.EmailNotification;
import org.openelisglobal.notification.valueholder.RemoteNotification;
import org.openelisglobal.notification.valueholder.SMSNotification;
import org.openelisglobal.notifications.service.HeaderNotificationService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
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

    @Autowired
    private TestAlertRuleService alertRuleService;

    @Autowired
    private ResultService resultService;

    @Autowired
    private HeaderNotificationService headerNotificationService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private SampleHumanService sampleHumanService;

    // The SMS/Email senders used by the testNotificationConfig page. Optional so
    // the processor still runs (header-only) in environments with none wired.
    @Autowired(required = false)
    private List<ClientNotificationSender> notificationSenders;

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
        List<TestAlertRule> rules = alertRuleService.getByTestId(test.getId());
        if (rules == null || rules.isEmpty()) {
            return;
        }
        String value = result.getValue();
        for (TestAlertRule rule : rules) {
            if (!Boolean.TRUE.equals(rule.getEnabled()) || !matches(rule, result, value)) {
                continue;
            }
            String testName = test.getLocalizedName() != null ? test.getLocalizedName() : test.getName();
            String subject = "Test alert: " + testName;
            String message = "[ALERT: " + rule.getName() + "] " + testName + (value != null ? " result " + value : "");
            dispatchHeader(rule, message, sysUserId);
            dispatchExternal(rule, subject, message, result);
        }
    }

    private boolean matches(TestAlertRule rule, Result result, String value) {
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
        default:
            // CRITICAL needs critical-range evaluation; COMPLIANCE_BREACH needs the
            // S-01 compliance module (OGC-528). Neither fires until those land.
            return false;
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

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void dispatch(RemoteNotification notification) {
        if (notificationSenders == null) {
            return;
        }
        for (ClientNotificationSender sender : notificationSenders) {
            try {
                if (sender.forClass().isInstance(notification)) {
                    sender.send(notification);
                }
            } catch (RuntimeException e) {
                LogEvent.logError(e);
            }
        }
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
