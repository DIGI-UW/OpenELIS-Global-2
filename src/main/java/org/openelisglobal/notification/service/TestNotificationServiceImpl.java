package org.openelisglobal.notification.service;

import jakarta.annotation.PostConstruct;
import java.util.Optional;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.UserContextHolder;
import org.openelisglobal.delivery.service.DeliveryOutboxService;
import org.openelisglobal.delivery.service.DeliveryRequest;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.notification.valueholder.AnalysisNotificationConfig;
import org.openelisglobal.notification.valueholder.NotificationChannel;
import org.openelisglobal.notification.valueholder.NotificationConfig;
import org.openelisglobal.notification.valueholder.NotificationConfigOption;
import org.openelisglobal.notification.valueholder.NotificationConfigOption.NotificationMethod;
import org.openelisglobal.notification.valueholder.NotificationConfigOption.NotificationNature;
import org.openelisglobal.notification.valueholder.NotificationConfigOption.NotificationPersonType;
import org.openelisglobal.notification.valueholder.NotificationLog.Product;
import org.openelisglobal.notification.valueholder.NotificationPayload;
import org.openelisglobal.notification.valueholder.NotificationPayloadTemplate;
import org.openelisglobal.notification.valueholder.NotificationPayloadTemplate.NotificationPayloadType;
import org.openelisglobal.notification.valueholder.NotificationRecipientType;
import org.openelisglobal.notification.valueholder.PatientResultsViewNotificationPayload;
import org.openelisglobal.notification.valueholder.TestNotificationConfig;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.security.DaemonContextExecutor;
import org.openelisglobal.testresultsview.service.ClientResultsViewInfoService;
import org.openelisglobal.testresultsview.valueholder.ClientResultsViewBean;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TestNotificationServiceImpl implements TestNotificationService {

    @Autowired
    private ClientResultsViewInfoService clientResultsViewInfoService;
    @Autowired
    private NotificationPayloadTemplateService notificationPayloadTemplateService;
    @Autowired
    private SampleHumanService sampleHumanService;
    @Autowired
    private DictionaryService dictionaryService;
    @Autowired
    private TestNotificationConfigService testNotificationConfigService;
    @Autowired
    private AnalysisNotificationConfigService analysisNotificationConfigService;
    @Autowired
    private UserContextHolder userContextHolder;
    @Autowired
    private DaemonContextExecutor daemonContextExecutor;
    @Autowired
    private DeliveryOutboxService deliveryOutboxService;

    @Value("${org.openelisglobal.ozeki.active:false}")
    private Boolean ozekiActive;

    @PostConstruct
    public void init() {
        daemonContextExecutor
                .executeAsDaemon(() -> ensureNotificationsPayloadTemplatesExist(NotificationPayloadType.TEST_RESULT));
    }

    private void ensureNotificationsPayloadTemplatesExist(NotificationPayloadType testResult) {
        if (notificationPayloadTemplateService.getSystemDefaultPayloadTemplateForType(testResult) == null) {
            createSystemDefaultNotificationPayloadTemplate(NotificationPayloadType.TEST_RESULT);
        }
    }

    private NotificationPayloadTemplate createSystemDefaultNotificationPayloadTemplate(NotificationPayloadType type) {
        NotificationPayloadTemplate template = new NotificationPayloadTemplate();
        DefaultPayloadTemplates.Default factory = DefaultPayloadTemplates.forType(type);
        template.setMessageTemplate(factory.getMessage());
        template.setSubjectTemplate(factory.getSubject());
        template.setSysUserId(userContextHolder.getDaemonSysUserId());
        template.setType(type);
        notificationPayloadTemplateService.save(template);
        return template;
    }

    @Override
    @Async
    @Transactional(readOnly = true)
    public void createAndSendNotificationsToConfiguredSources(NotificationNature nature, Result result) {
        Optional<? extends NotificationConfig<?>> notificationConfig = analysisNotificationConfigService
                .getAnalysisNotificationConfigForAnalysisId(result.getAnalysis().getId());
        if (notificationConfig.isEmpty()) {
            // analysis hasn't been configured to return results
            notificationConfig = testNotificationConfigService
                    .getTestNotificationConfigForTestId(result.getAnalysis().getTest().getId());
            if (notificationConfig.isEmpty()) {
                // test hasn't been configured to send notifications
                return;
            }
        }

        switch (nature) {
        case RESULT_VALIDATION:
            createAndSendResultsNotificationsToConfiguredSources(nature, result, notificationConfig);
        default:
        }
    }

    private void createAndSendResultsNotificationsToConfiguredSources(NotificationNature nature, Result result,
            Optional<? extends NotificationConfig<?>> notificationConfig) {
        ClientResultsViewBean resultsViewInfo = new ClientResultsViewBean(result);
        resultsViewInfo = clientResultsViewInfoService.save(resultsViewInfo);

        String resultForDisplay = "";

        if (TypeOfTestResultServiceImpl.ResultType.isMultiSelectVariant(result.getResultType())) {
            // TODO
        } else if (TypeOfTestResultServiceImpl.ResultType.isDictionaryVariant(result.getResultType())) {
            Dictionary dictionary = dictionaryService.getDataForId(result.getValue());
            resultForDisplay = dictionary.getLocalizedName();

            if ("unknown".equals(resultForDisplay)) {
                resultForDisplay = GenericValidator.isBlankOrNull(dictionary.getLocalAbbreviation())
                        ? dictionary.getDictEntry()
                        : dictionary.getLocalAbbreviation();
            }
            // resultForDisplay =
            // dictionaryService.getDataForId(result.getValue()).getDictEntry();
        } else if (TypeOfTestResultServiceImpl.ResultType.isNumeric(result.getResultType())) {
            resultForDisplay = result.getValue();
        } else if (TypeOfTestResultServiceImpl.ResultType.isTextOnlyVariant(result.getResultType())) {
            resultForDisplay = result.getValue();
        }
        for (NotificationMethod methodType : NotificationMethod.values()) {
            if (systemEnabledForMethod(methodType)) {
                createAndSendNotificationsConfiguredForTest(nature, methodType, notificationConfig.get(),
                        resultForDisplay, resultsViewInfo);
            }
        }
    }

    private void createAndSendNotificationsConfiguredForTest(NotificationNature nature, NotificationMethod methodType,
            NotificationConfig<?> notificationConfig, String resultForDisplay, ClientResultsViewBean resultsViewInfo) {
        for (NotificationPersonType personType : NotificationPersonType.values()) {
            NotificationConfigOption option = notificationConfig.getOptionFor(nature, methodType, personType);
            if (option.getActive()) {
                createAndSendNotificationToPerson(nature, methodType, personType, option, resultForDisplay,
                        resultsViewInfo);
            }
        }
    }

    private void createAndSendNotificationToPerson(NotificationNature nature, NotificationMethod methodType,
            NotificationPersonType personType, NotificationConfigOption option, String resultForDisplay,
            ClientResultsViewBean resultsViewInfo) {
        Person testPerson = sampleHumanService
                .getPatientForSample(resultsViewInfo.getResult().getAnalysis().getSampleItem().getSample()).getPerson();
        Person receiverPerson = null;
        if (NotificationPersonType.PATIENT.equals(personType)) {
            receiverPerson = testPerson;
        } else if (NotificationPersonType.PROVIDER.equals(personType)) {
            receiverPerson = sampleHumanService
                    .getProviderForSample(resultsViewInfo.getResult().getAnalysis().getSampleItem().getSample())
                    .getPerson();
        }
        if (NotificationMethod.EMAIL.equals(methodType) && canSendEmail(receiverPerson)) {
            createAndSendResultsNotificationEmail(testPerson, receiverPerson, option, resultForDisplay,
                    resultsViewInfo);
        } else if (NotificationMethod.SMS.equals(methodType) && canSendSMS(receiverPerson)) {
            createAndSendResultsNotificationSMS(testPerson, receiverPerson, option, resultForDisplay, resultsViewInfo);
        }
    }

    private void createAndSendResultsNotificationSMS(Person testPerson, Person receiverPerson,
            NotificationConfigOption option, String resultForDisplay, ClientResultsViewBean resultsViewInfo) {
        try {
            String rawPhone = !GenericValidator.isBlankOrNull(receiverPerson.getPrimaryPhone())
                    ? receiverPerson.getPrimaryPhone()
                    : receiverPerson.getWorkPhone();
            String phoneNumber = "";
            for (char ch : rawPhone.toCharArray()) {
                if (Character.isDigit(ch) || ch == '+') {
                    phoneNumber = phoneNumber + ch;
                }
            }
            NotificationPayload payload = payloadFor(option, resultForDisplay, resultsViewInfo, testPerson);
            queue(NotificationChannel.SMS, receiverPerson, null, phoneNumber, payload, resultsViewInfo);
        } catch (RuntimeException e) {
            LogEvent.logError(this.getClass().getSimpleName(), "createAndSendResultsNotificationSMS",
                    "could not queue sms notification");
            LogEvent.logError(e);
        }
    }

    private void createAndSendResultsNotificationEmail(Person testPerson, Person receiverPerson,
            NotificationConfigOption option, String resultForDisplay, ClientResultsViewBean resultsViewInfo) {
        try {
            NotificationPayload payload = payloadFor(option, resultForDisplay, resultsViewInfo, testPerson);
            queue(NotificationChannel.EMAIL, receiverPerson, receiverPerson.getEmail(), null, payload, resultsViewInfo);
            if (option.getAdditionalContacts() != null) {
                for (String contact : option.getAdditionalContacts()) {
                    if (!GenericValidator.isBlankOrNull(contact)) {
                        queue(NotificationChannel.EMAIL, null, contact, null, payload, resultsViewInfo);
                    }
                }
            }
        } catch (RuntimeException e) {
            LogEvent.logError(this.getClass().getSimpleName(), "createAndSendResultsNotificationEmail",
                    "could not queue email notification");
            LogEvent.logError(e);
        }
    }

    private NotificationPayload payloadFor(NotificationConfigOption option, String resultForDisplay,
            ClientResultsViewBean resultsViewInfo, Person testPerson) {
        NotificationPayloadTemplate template = findTemplate(option);
        return new PatientResultsViewNotificationPayload(resultsViewInfo.getPassword(), "",
                resultsViewInfo.getResult().getAnalysis().getTest().getName(), resultForDisplay, "", "", template);
    }

    /**
     * One Sent Messages row per recipient and channel; the delivery worker sends
     * it, retries it and records every attempt.
     */
    private void queue(NotificationChannel channel, Person receiver, String email, String phone,
            NotificationPayload payload, ClientResultsViewBean resultsViewInfo) {
        Sample sample = resultsViewInfo.getResult().getAnalysis().getSampleItem().getSample();
        Person patient = sampleHumanService.getPatientForSample(sample).getPerson();
        boolean toPatient = receiver != null && patient != null && receiver.getId() != null
                && receiver.getId().equals(patient.getId());
        String displayName = receiver == null ? (email == null ? phone : email)
                : (receiver.getLastName() == null ? "" : receiver.getLastName() + ", ")
                        + (receiver.getFirstName() == null ? "" : receiver.getFirstName());
        DeliveryRequest request = new DeliveryRequest().setProduct(Product.NOTICE)
                .setEventCode(NotificationPayloadType.TEST_RESULT.name())
                .setRecipientType(receiver == null ? NotificationRecipientType.TYPED_ADDRESS
                        : toPatient ? NotificationRecipientType.PATIENT : NotificationRecipientType.ORDERING_PROVIDER)
                .setRecipientDisplayName(displayName).setRecipientEmail(email).setRecipientPhone(phone)
                .setSubject(payload.getSubject()).setMessage(payload.getMessage())
                .setReferenceAccession(sample.getAccessionNumber()).addChannel(channel);
        deliveryOutboxService.enqueue(request);
    }

    private NotificationPayloadTemplate findTemplate(NotificationConfigOption option) {
        NotificationPayloadTemplate template;
        TestNotificationConfig testNotificationConfig = testNotificationConfigService
                .getForConfigOption(option.getId());
        AnalysisNotificationConfig analyzerNotificationConfig = analysisNotificationConfigService
                .getForConfigOption(option.getId());
        // default to system default for type
        if (option.getPayloadTemplate() == null
                && (testNotificationConfig == null || testNotificationConfig.getDefaultPayloadTemplate() == null)
                && (analyzerNotificationConfig == null
                        || analyzerNotificationConfig.getDefaultPayloadTemplate() == null)) {
            template = notificationPayloadTemplateService
                    .getSystemDefaultPayloadTemplateForType(NotificationPayloadType.TEST_RESULT);
            // ... unless this option is for a test and it has a test default
        } else if (option.getPayloadTemplate() == null && (analyzerNotificationConfig == null
                || analyzerNotificationConfig.getDefaultPayloadTemplate() == null)) {
            template = testNotificationConfig.getDefaultPayloadTemplate();
            // ... unless this option is for an analysis and it has an analysis default
        } else if (option.getPayloadTemplate() == null) {
            template = analyzerNotificationConfig.getDefaultPayloadTemplate();
            // ...unless there is one configured for this option
        } else {
            template = option.getPayloadTemplate();
        }
        return template;
    }

    private boolean systemEnabledForMethod(NotificationMethod methodType) {
        switch (methodType) {
        case EMAIL:
            return emailEnabledForSystem();
        case SMS:
            return smsEnabledForSystem();
        default:
            return false;
        }
    }

    /**
     * SMS goes through the SMS channel once it is set up; until then the gateways
     * configured by property keep working as before.
     */
    private boolean smsEnabledForSystem() {
        return deliveryOutboxService.isChannelSetUp(NotificationChannel.SMS)
                || Boolean.TRUE.toString()
                        .equals(ConfigurationProperties.getInstance()
                                .getPropertyValue(Property.PATIENT_RESULTS_BMP_SMS_ENABLED))
                || Boolean.TRUE.toString()
                        .equals(ConfigurationProperties.getInstance()
                                .getPropertyValue(Property.PATIENT_RESULTS_SMPP_SMS_ENABLED))
                || Boolean.TRUE.equals(ozekiActive);
    }

    /**
     * Email is the Email channel on the Channels page, not the retired SMTP
     * connection.
     */
    private boolean emailEnabledForSystem() {
        return deliveryOutboxService.isChannelSetUp(NotificationChannel.EMAIL);
    }

    private boolean canSendSMS(Person person) {
        boolean canSend = person != null && (!GenericValidator.isBlankOrNull(person.getPrimaryPhone())
                || !GenericValidator.isBlankOrNull(person.getWorkPhone()));
        if (!canSend) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "canSendSMS",
                    "can't send SMS to person as they have no phone on file");
        }
        return canSend;
    }

    private boolean canSendEmail(Person person) {
        boolean canSend = person != null && !GenericValidator.isBlankOrNull(person.getEmail());
        if (!canSend) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "canSendEmail",
                    "can't send email to person as they have no email on file");
        }
        return canSend;
    }
}
