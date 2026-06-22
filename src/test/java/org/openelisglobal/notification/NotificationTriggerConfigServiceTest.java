package org.openelisglobal.notification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.notification.service.NotificationTriggerConfigService;
import org.openelisglobal.notification.valueholder.NotificationChannel;
import org.openelisglobal.notification.valueholder.NotificationRecipientType;
import org.openelisglobal.notification.valueholder.NotificationTriggerConfig;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration test verifying the {@code REFERRAL_OUT} trigger seed migration
 * ran and that the service correctly persists channel + recipient selections.
 * Asserts full state (no shallow not-null / count > 0 checks).
 */
public class NotificationTriggerConfigServiceTest extends BaseWebContextSensitiveTest {

    private static final String EVENT_REFERRAL_OUT = "REFERRAL_OUT";
    private static final String SYS_USER = "1";

    @Autowired
    private NotificationTriggerConfigService configService;

    @Test
    public void seedMigration_createsReferralOutRowWithDisabledDefault() {
        Optional<NotificationTriggerConfig> seeded = configService.getByEventCode(EVENT_REFERRAL_OUT);
        assertTrue("REFERRAL_OUT trigger seed row must exist after liquibase migration 043", seeded.isPresent());

        NotificationTriggerConfig config = seeded.get();
        assertEquals(EVENT_REFERRAL_OUT, config.getEventCode());
        assertFalse("REFERRAL_OUT must default to disabled per spec", config.isEnabled());
        assertTrue("seed must have no channels selected", config.getChannels().isEmpty());
        assertTrue("seed must have no recipients selected", config.getRecipientTypes().isEmpty());
        assertNotNull("seed must be wired to a REFERRAL_OUT payload template", config.getPayloadTemplate());
        assertEquals("Sample referred out: [sampleAccessionNumber]", config.getPayloadTemplate().getSubjectTemplate());
    }

    @Test
    public void saveConfig_persistsEnabledChannelsAndRecipients() {
        NotificationTriggerConfig incoming = configService.getByEventCode(EVENT_REFERRAL_OUT).orElseThrow();
        incoming.setEnabled(true);
        Set<NotificationChannel> channels = new HashSet<>();
        channels.add(NotificationChannel.EMAIL);
        channels.add(NotificationChannel.WHATSAPP);
        incoming.setChannels(channels);
        Set<NotificationRecipientType> recipients = new HashSet<>();
        recipients.add(NotificationRecipientType.ORDERING_PROVIDER);
        recipients.add(NotificationRecipientType.SUBMITTING_USER);
        incoming.setRecipientTypes(recipients);

        configService.saveConfig(incoming, SYS_USER);

        NotificationTriggerConfig reloaded = configService.getByEventCode(EVENT_REFERRAL_OUT).orElseThrow();
        assertTrue("enabled flag must persist", reloaded.isEnabled());
        assertEquals("channel selection must persist exactly",
                Set.of(NotificationChannel.EMAIL, NotificationChannel.WHATSAPP), reloaded.getChannels());
        assertEquals("recipient selection must persist exactly",
                Set.of(NotificationRecipientType.ORDERING_PROVIDER, NotificationRecipientType.SUBMITTING_USER),
                reloaded.getRecipientTypes());
    }

    @Test
    public void saveConfig_replacesPriorSelections_doesNotAccumulate() {
        NotificationTriggerConfig first = configService.getByEventCode(EVENT_REFERRAL_OUT).orElseThrow();
        first.setEnabled(true);
        Set<NotificationChannel> bothChannels = new HashSet<>();
        bothChannels.add(NotificationChannel.EMAIL);
        bothChannels.add(NotificationChannel.WHATSAPP);
        first.setChannels(bothChannels);
        first.setRecipientTypes(
                Set.of(NotificationRecipientType.ORDERING_PROVIDER, NotificationRecipientType.SUBMITTING_USER));
        configService.saveConfig(first, SYS_USER);

        // Now narrow the selection — Email-only, Provider-only.
        NotificationTriggerConfig second = configService.getByEventCode(EVENT_REFERRAL_OUT).orElseThrow();
        second.setChannels(Set.of(NotificationChannel.EMAIL));
        second.setRecipientTypes(Set.of(NotificationRecipientType.ORDERING_PROVIDER));
        configService.saveConfig(second, SYS_USER);

        NotificationTriggerConfig reloaded = configService.getByEventCode(EVENT_REFERRAL_OUT).orElseThrow();
        assertEquals("removed channels must be gone", Set.of(NotificationChannel.EMAIL), reloaded.getChannels());
        assertEquals("removed recipients must be gone", Set.of(NotificationRecipientType.ORDERING_PROVIDER),
                reloaded.getRecipientTypes());
    }

    @Test
    public void getAllConfigs_returnsAtLeastTheSeedRow() {
        List<NotificationTriggerConfig> all = configService.getAllConfigs();
        boolean found = all.stream().anyMatch(c -> EVENT_REFERRAL_OUT.equals(c.getEventCode()));
        assertTrue("REFERRAL_OUT seed must be returned by getAllConfigs(); got: "
                + all.stream().map(NotificationTriggerConfig::getEventCode).toList(), found);
    }
}
