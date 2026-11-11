package org.openelisglobal.referral;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.delivery.dto.DeliveryChannelDTO;
import org.openelisglobal.delivery.service.DeliveryChannelService;
import org.openelisglobal.delivery.valueholder.DeliveryChannel.Channel;
import org.openelisglobal.notification.service.NotificationLogService;
import org.openelisglobal.notification.service.NotificationTriggerConfigService;
import org.openelisglobal.notification.valueholder.NotificationChannel;
import org.openelisglobal.notification.valueholder.NotificationLog;
import org.openelisglobal.notification.valueholder.NotificationLog.OverallStatus;
import org.openelisglobal.notification.valueholder.NotificationLogChannel;
import org.openelisglobal.notification.valueholder.NotificationLogChannel.ChannelStatus;
import org.openelisglobal.notification.valueholder.NotificationRecipientType;
import org.openelisglobal.notification.valueholder.NotificationTriggerConfig;
import org.openelisglobal.referral.service.ReferralService;
import org.openelisglobal.referral.valueholder.ReferralStatusHistory;
import org.springframework.beans.factory.annotation.Autowired;

public class ReferralNudgeIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String ACTOR = "42";

    @Autowired
    private ReferralService referralService;

    @Autowired
    private NotificationTriggerConfigService notificationTriggerConfigService;

    @Autowired
    private NotificationLogService notificationLogService;

    @Autowired
    private DeliveryChannelService deliveryChannelService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/referral.xml");
        clearQueuedRows("REFERRAL_NUDGE");
    }

    private void clearQueuedRows(String eventCode) {
        jdbcTemplate.update("DELETE FROM clinlims.notification_log_attempt WHERE notification_log_id IN"
                + " (SELECT id FROM clinlims.notification_log WHERE event_code = ?)", eventCode);
        jdbcTemplate.update("DELETE FROM clinlims.notification_log_channel WHERE notification_log_id IN"
                + " (SELECT id FROM clinlims.notification_log WHERE event_code = ?)", eventCode);
        jdbcTemplate.update("DELETE FROM clinlims.notification_log WHERE event_code = ?", eventCode);
    }

    @After
    public void tearDown() {
        DeliveryChannelDTO whatsApp = deliveryChannelService.get(Channel.WHATSAPP).orElseThrow();
        whatsApp.setActive(false);
        deliveryChannelService.save(Channel.WHATSAPP, whatsApp, ACTOR);
    }

    @Test
    public void nudge_writesAuditNoteWithTargetsAndMessage() {
        // referral 1 has subcontract 10 with coc_contact_email/phone.
        referralService.nudgeReferenceLab("1", "Please prioritise", ACTOR);

        List<ReferralStatusHistory> history = referralService.getSubcontractStatusHistory("1");
        ReferralStatusHistory latest = history.get(history.size() - 1);
        assertEquals(ACTOR, latest.getChangedByUserId());
        // Same-status audit row (no lifecycle transition on a nudge).
        assertEquals(latest.getFromStatus(), latest.getToStatus());
        assertTrue("note carries REFERRAL_NUDGE_SENT verb: " + latest.getNotes(),
                latest.getNotes().startsWith("REFERRAL_NUDGE_SENT"));
        assertTrue("note carries the COC target: " + latest.getNotes(), latest.getNotes().contains("+1-555-0142"));
        assertTrue("note carries the free-form message: " + latest.getNotes(),
                latest.getNotes().contains("Please prioritise"));
    }

    /**
     * A nudge with the trigger on queues one WhatsApp message to the CoC contact in
     * the delivery outbox; the delivery worker sends it from there.
     */
    @Test
    public void nudge_whenTriggerEnabled_queuesWhatsAppToCocContact() throws Exception {
        enableReferralNudgeTrigger();
        setUpWhatsAppChannel();
        long logCountBefore = notificationLogService.countMatching(Optional.of("REFERRAL_NUDGE"), Optional.empty());

        referralService.nudgeReferenceLab("1", "Please prioritise", ACTOR);

        long deadline = System.currentTimeMillis() + 5000;
        long logCountAfter = logCountBefore;
        while (System.currentTimeMillis() < deadline && logCountAfter == logCountBefore) {
            logCountAfter = notificationLogService.countMatching(Optional.of("REFERRAL_NUDGE"), Optional.empty());
            if (logCountAfter == logCountBefore) {
                Thread.sleep(50);
            }
        }
        assertEquals("nudge must queue one REFERRAL_NUDGE row", logCountBefore + 1, logCountAfter);
        NotificationLog queued = notificationLogService.findPage(Optional.of("REFERRAL_NUDGE"), Optional.empty(), 1, 1)
                .get(0);
        assertEquals(NotificationRecipientType.COC_CONTACT, queued.getRecipientType());
        // CoC contact phone from referral.xml (subcontract 10): "+1-555-0142".
        assertEquals("+1-555-0142", queued.getRecipientPhone());
        assertEquals(OverallStatus.QUEUED, queued.getOverallStatus());
        NotificationLogChannel channel = queued.getChannels().iterator().next();
        assertEquals(NotificationChannel.WHATSAPP, channel.getChannel());
        assertEquals(ChannelStatus.QUEUED, channel.getStatus());
        assertTrue("the rendered message names the free-form note: " + queued.getRenderedMessage(),
                queued.getRenderedMessage().contains("Please prioritise"));
    }

    private void enableReferralNudgeTrigger() {
        NotificationTriggerConfig config = notificationTriggerConfigService.getByEventCode("REFERRAL_NUDGE")
                .orElseThrow(() -> new IllegalStateException("REFERRAL_NUDGE trigger seed missing: migration 053"));
        config.setEnabled(true);
        Set<NotificationChannel> channels = new HashSet<>();
        channels.add(NotificationChannel.WHATSAPP);
        config.setChannels(channels);
        Set<NotificationRecipientType> recipients = new HashSet<>();
        recipients.add(NotificationRecipientType.COC_CONTACT);
        config.setRecipientTypes(recipients);
        notificationTriggerConfigService.saveConfig(config, ACTOR);
    }

    private void setUpWhatsAppChannel() {
        DeliveryChannelDTO whatsApp = deliveryChannelService.get(Channel.WHATSAPP).orElseThrow();
        whatsApp.setActive(true);
        whatsApp.setUsername("ACtest");
        whatsApp.setSecret("secret");
        whatsApp.setSenderAddress("+14155238886");
        deliveryChannelService.save(Channel.WHATSAPP, whatsApp, ACTOR);
    }
}
