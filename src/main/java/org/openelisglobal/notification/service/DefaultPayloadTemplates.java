package org.openelisglobal.notification.service;

import java.util.Locale;
import java.util.Map;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.notification.valueholder.NotificationPayloadTemplate.NotificationPayloadType;

/**
 * Factory-default subject and message per {@link NotificationPayloadType}: what
 * {@code Reset to Default} restores in the admin template editor and what a
 * fresh database is seeded with. None of them names the patient (FR-D1). The
 * English text lives here; every shipped language carries its own under
 * {@code notification.template.<TYPE>.subject|message} in the message bundles,
 * which {@link #forType(NotificationPayloadType, Locale)} reads (FR-A15).
 */
public final class DefaultPayloadTemplates {

    public static final class Default {
        private final String subject;
        private final String message;

        public Default(String subject, String message) {
            this.subject = subject;
            this.message = message;
        }

        public String getSubject() {
            return subject;
        }

        public String getMessage() {
            return message;
        }
    }

    private static final Map<NotificationPayloadType, Default> DEFAULTS = Map.of(NotificationPayloadType.REFERRAL_OUT,
            new Default("Sample referred out: [sampleAccessionNumber]",
                    "Sample [sampleAccessionNumber] has been referred to [labName] for test [testName]."
                            + " Referral id: [referralId]."),
            NotificationPayloadType.SUBCONTRACT_DISPATCHED,
            new Default("Sample dispatched: [agreementReference]",
                    "Sample [sampleAccessionNumber] (referral [referralId]) has been dispatched to [labName]."
                            + " Handoff at [handoffDatetime]. Expected return: [expectedReturnDate]."
                            + " CoC contact: [cocContactName]."),
            NotificationPayloadType.REFERRAL_REJECTED_NEEDS_RECOLLECTION,
            new Default("Re-collection needed: [sampleAccessionNumber]",
                    "Result for sample [sampleAccessionNumber] (test [testName]) returned by [labName] was rejected:"
                            + " [rejectReason]. Please re-collect and re-refer. Order: [deepLink]"),
            NotificationPayloadType.REFERRAL_NUDGE,
            new Default("Result awaited: [sampleAccessionNumber]",
                    "Sample [sampleAccessionNumber] (referral [referralId], test [testName]) has been outstanding"
                            + " [daysOutstanding] day(s) at [labName]. Please advise on status. [freeFormMessage]"),
            NotificationPayloadType.SAMPLE_RESAMPLED,
            new Default("Resampling required: Lab #[labNumber]",
                    "Sample [labNumber] requires re-collection. Reason: [reason]. A replacement order has been"
                            + " created (Lab #[newLabNumber]); please collect and submit a fresh sample."),
            NotificationPayloadType.TEST_RESULT, new Default("[testName] results ready",
                    "[testName] results are ready. Please contact your clinician or the laboratory."));

    private DefaultPayloadTemplates() {
    }

    public static Default forType(NotificationPayloadType type) {
        return DEFAULTS.get(type);
    }

    /**
     * The default in the given language when the message bundle carries one,
     * otherwise the English default.
     */
    public static Default forType(NotificationPayloadType type, Locale locale) {
        Default english = DEFAULTS.get(type);
        if (locale == null || english == null) {
            return english;
        }
        String subjectKey = "notification.template." + type.name() + ".subject";
        String messageKey = "notification.template." + type.name() + ".message";
        String subject = MessageUtil.getMessage(subjectKey, locale);
        String message = MessageUtil.getMessage(messageKey, locale);
        boolean hasSubject = subject != null && !subject.equals(subjectKey);
        boolean hasMessage = message != null && !message.equals(messageKey);
        return new Default(hasSubject ? subject : english.getSubject(), hasMessage ? message : english.getMessage());
    }

    public static boolean hasDefault(NotificationPayloadType type) {
        return DEFAULTS.containsKey(type);
    }
}
