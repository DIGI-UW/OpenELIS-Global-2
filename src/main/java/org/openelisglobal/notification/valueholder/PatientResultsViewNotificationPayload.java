package org.openelisglobal.notification.valueholder;

public class PatientResultsViewNotificationPayload implements NotificationPayload {

    private String accessPassword;

    private String accessAddress;

    private String testName;

    private String testResult;

    private String patientFirstName;

    private String patientLastNameInitial;

    private NotificationPayloadTemplate payloadTemplate;

    public PatientResultsViewNotificationPayload(String accessPassword, String accessAddress, String testName,
            String testResult, String patientFirstName, String patientLastNameInitial,
            NotificationPayloadTemplate payloadTemplate) {
        this.accessPassword = accessPassword;
        this.accessAddress = accessAddress;
        this.testName = testName;
        this.testResult = testResult;
        this.patientFirstName = patientFirstName;
        this.patientLastNameInitial = patientLastNameInitial;
        this.payloadTemplate = payloadTemplate;
    }

    @Override
    public String getMessage() {
        String message = payloadTemplate.getMessageTemplate();
        message = message.replaceAll("\\[testName\\]", testName);
        message = message.replaceAll("\\[accessPassword\\]", accessPassword);
        message = message.replaceAll("\\[accessAddress\\]", accessAddress);
        message = message.replaceAll("\\[testResult\\]", testResult);
        message = message.replaceAll("\\[patientFirstName\\]", patientFirstName);
        message = message.replaceAll("\\[patientLastNameInitial\\]", patientLastNameInitial);
        return message;
    }

    @Override
    public String getSubject() {
        String subject = payloadTemplate.getSubjectTemplate();
        subject = subject.replaceAll("\\[testName\\]", testName);
        subject = subject.replaceAll("\\[accessPassword\\]", accessPassword);
        subject = subject.replaceAll("\\[accessAddress\\]", accessAddress);
        subject = subject.replaceAll("\\[testResult\\]", testResult);
        subject = subject.replaceAll("\\[patientFirstName\\]", patientFirstName);
        subject = subject.replaceAll("\\[patientLastNameInitial\\]", patientLastNameInitial);
        return subject;
    }
}
