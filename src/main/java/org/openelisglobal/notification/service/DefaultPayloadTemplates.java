package org.openelisglobal.notification.service;

import java.util.Map;
import org.openelisglobal.notification.valueholder.NotificationPayloadTemplate.NotificationPayloadType;

/**
 * Factory-default subject + message strings per
 * {@link NotificationPayloadType}. These are the canonical strings that
 * {@code Reset to Default} restores in the admin template editor and that the
 * Liquibase migrations 036 / 037 seed on a fresh DB. Keep them in lockstep with
 * the migrations — {@code
 * NotificationPayloadTemplateServiceTest.defaults_matchLiquibaseSeed_forBothEventCodes}
 * guards against drift.
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
                    "Sample [sampleAccessionNumber] for patient [patientFirstName] [patientLastName]"
                            + " has been referred to [labName] for test [testName]. Referral id: [referralId]."),
            NotificationPayloadType.SUBCONTRACT_DISPATCHED,
            new Default("Sample dispatched: [agreementReference]",
                    "Sample [sampleAccessionNumber] (referral [referralId]) has been dispatched to [labName]."
                            + " Handoff at [handoffDatetime]. Expected return: [expectedReturnDate]."
                            + " CoC contact: [cocContactName]."));

    private DefaultPayloadTemplates() {
    }

    public static Default forType(NotificationPayloadType type) {
        return DEFAULTS.get(type);
    }

    public static boolean hasDefault(NotificationPayloadType type) {
        return DEFAULTS.containsKey(type);
    }
}
