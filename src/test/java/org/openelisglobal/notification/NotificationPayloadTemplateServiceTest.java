package org.openelisglobal.notification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.notification.service.DefaultPayloadTemplates;
import org.openelisglobal.notification.service.NotificationPayloadTemplateService;
import org.openelisglobal.notification.valueholder.NotificationPayloadTemplate;
import org.openelisglobal.notification.valueholder.NotificationPayloadTemplate.NotificationPayloadType;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for {@link NotificationPayloadTemplateService} and the
 * {@link DefaultPayloadTemplates} constants. These guard:
 * <ul>
 * <li>persistence of subject + message via {@code
 *     updatePayloadTemplateMessagesAndSubject}
 * <li>byte-for-byte parity between the Java factory defaults and the strings
 * Liquibase 036 / 037 seeded — drift here would silently break "Reset to
 * Default" against fresh databases.
 * </ul>
 */
public class NotificationPayloadTemplateServiceTest extends BaseWebContextSensitiveTest {

    private static final String SYS_USER = "1";

    @Autowired
    private NotificationPayloadTemplateService payloadTemplateService;

    @Test
    public void defaults_matchLiquibaseSeed_forReferralOut() {
        NotificationPayloadTemplate seeded = payloadTemplateService
                .getSystemDefaultPayloadTemplateForType(NotificationPayloadType.REFERRAL_OUT);
        assertNotNull("Liquibase migration 036 must seed REFERRAL_OUT template", seeded);
        DefaultPayloadTemplates.Default factory = DefaultPayloadTemplates.forType(NotificationPayloadType.REFERRAL_OUT);
        assertNotNull("DefaultPayloadTemplates must expose REFERRAL_OUT", factory);
        assertEquals("REFERRAL_OUT factory subject must match Liquibase seed (drift breaks Reset to Default)",
                seeded.getSubjectTemplate(), factory.getSubject());
        assertEquals("REFERRAL_OUT factory message must match Liquibase seed (drift breaks Reset to Default)",
                seeded.getMessageTemplate(), factory.getMessage());
    }

    @Test
    public void defaults_matchLiquibaseSeed_forSubcontractDispatched() {
        NotificationPayloadTemplate seeded = payloadTemplateService
                .getSystemDefaultPayloadTemplateForType(NotificationPayloadType.SUBCONTRACT_DISPATCHED);
        assertNotNull("Liquibase migration 037 must seed SUBCONTRACT_DISPATCHED template", seeded);
        DefaultPayloadTemplates.Default factory = DefaultPayloadTemplates
                .forType(NotificationPayloadType.SUBCONTRACT_DISPATCHED);
        assertNotNull("DefaultPayloadTemplates must expose SUBCONTRACT_DISPATCHED", factory);
        assertEquals("SUBCONTRACT_DISPATCHED factory subject must match Liquibase seed (drift breaks Reset to Default)",
                seeded.getSubjectTemplate(), factory.getSubject());
        assertEquals("SUBCONTRACT_DISPATCHED factory message must match Liquibase seed (drift breaks Reset to Default)",
                seeded.getMessageTemplate(), factory.getMessage());
    }

    @Test
    public void updatePayloadTemplateMessagesAndSubject_persistsBothAndPreservesOtherFields() {
        NotificationPayloadTemplate seeded = payloadTemplateService
                .getSystemDefaultPayloadTemplateForType(NotificationPayloadType.REFERRAL_OUT);
        assertNotNull(seeded);
        Integer originalId = seeded.getId();
        String originalSubject = seeded.getSubjectTemplate();
        String originalMessage = seeded.getMessageTemplate();
        String originalExternalId = seeded.getTemplateExternalId();

        try {
            NotificationPayloadTemplate update = new NotificationPayloadTemplate();
            update.setId(originalId);
            update.setType(NotificationPayloadType.REFERRAL_OUT);
            update.setSubjectTemplate("CUSTOM SUBJECT [sampleAccessionNumber]");
            update.setMessageTemplate("CUSTOM BODY for [labName] referral [referralId]");

            payloadTemplateService.updatePayloadTemplateMessagesAndSubject(update, SYS_USER);

            NotificationPayloadTemplate reloaded = payloadTemplateService
                    .getSystemDefaultPayloadTemplateForType(NotificationPayloadType.REFERRAL_OUT);
            assertEquals("subject must persist", "CUSTOM SUBJECT [sampleAccessionNumber]",
                    reloaded.getSubjectTemplate());
            assertEquals("message must persist", "CUSTOM BODY for [labName] referral [referralId]",
                    reloaded.getMessageTemplate());
            assertEquals("id must be unchanged (update, not insert)", originalId, reloaded.getId());
            assertEquals("templateExternalId must be untouched by message/subject save", originalExternalId,
                    reloaded.getTemplateExternalId());
        } finally {
            // BaseWebContextSensitiveTest uses Propagation.NOT_SUPPORTED — writes commit
            // and persist across tests. Restore the seeded values so the drift-guard
            // tests aren't poisoned by this method's mutations on subsequent runs.
            NotificationPayloadTemplate restore = new NotificationPayloadTemplate();
            restore.setId(originalId);
            restore.setType(NotificationPayloadType.REFERRAL_OUT);
            restore.setSubjectTemplate(originalSubject);
            restore.setMessageTemplate(originalMessage);
            payloadTemplateService.updatePayloadTemplateMessagesAndSubject(restore, SYS_USER);
        }
    }
}
