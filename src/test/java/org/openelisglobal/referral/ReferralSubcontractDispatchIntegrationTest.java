package org.openelisglobal.referral;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.delivery.dao.DeliveryChannelDAO;
import org.openelisglobal.delivery.dao.DeliveryQueueDAO.Claim;
import org.openelisglobal.delivery.dto.DeliveryChannelDTO;
import org.openelisglobal.delivery.service.DeliveryChannelService;
import org.openelisglobal.delivery.service.DeliveryOutboxService;
import org.openelisglobal.delivery.service.sender.DeliveryMessage;
import org.openelisglobal.delivery.service.sender.WhatsAppDeliverySender;
import org.openelisglobal.delivery.valueholder.DeliveryChannel;
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
import org.openelisglobal.referral.valueholder.Referral;
import org.openelisglobal.referral.valueholder.ReferralStatus;
import org.openelisglobal.referral.valueholder.ReferralStatusHistory;
import org.openelisglobal.referral.valueholder.ReferralSubcontract;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

public class ReferralSubcontractDispatchIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ReferralService referralService;

    @Autowired
    private WhatsAppDeliverySender whatsAppDeliverySender;

    @Autowired
    private NotificationTriggerConfigService notificationTriggerConfigService;

    @Autowired
    private DeliveryChannelService deliveryChannelService;

    @Autowired
    private DeliveryChannelDAO deliveryChannelDAO;

    @Autowired
    private DeliveryOutboxService deliveryOutboxService;

    @Autowired
    private NotificationLogService notificationLogService;

    private Object originalSenderHttpClient;
    private org.apache.http.impl.client.CloseableHttpClient mockHttpClient;

    /**
     * The WhatsApp transport the delivery worker would use, steered by the test:
     * the Twilio HTTP client is a mock that answers 201 with a message sid.
     */
    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/referral.xml");
        jdbcTemplate.update("DELETE FROM clinlims.notification_log_attempt WHERE notification_log_id IN"
                + " (SELECT id FROM clinlims.notification_log WHERE event_code = 'SUBCONTRACT_DISPATCHED')");
        jdbcTemplate.update("DELETE FROM clinlims.notification_log_channel WHERE notification_log_id IN"
                + " (SELECT id FROM clinlims.notification_log WHERE event_code = 'SUBCONTRACT_DISPATCHED')");
        jdbcTemplate.update("DELETE FROM clinlims.notification_log WHERE event_code = 'SUBCONTRACT_DISPATCHED'");
        notificationTriggerConfigService.getByEventCode("SUBCONTRACT_DISPATCHED").ifPresent(config -> {
            if (config.isEnabled()) {
                config.setEnabled(false);
                notificationTriggerConfigService.saveConfig(config, ACTOR);
            }
        });
        originalSenderHttpClient = ReflectionTestUtils.getField(whatsAppDeliverySender, "httpClient");
        mockHttpClient = Mockito.mock(org.apache.http.impl.client.CloseableHttpClient.class);
        org.apache.http.client.methods.CloseableHttpResponse mockResponse = Mockito
                .mock(org.apache.http.client.methods.CloseableHttpResponse.class);
        org.apache.http.StatusLine statusLine = Mockito.mock(org.apache.http.StatusLine.class);
        Mockito.when(statusLine.getStatusCode()).thenReturn(201);
        Mockito.when(mockResponse.getStatusLine()).thenReturn(statusLine);
        Mockito.when(mockResponse.getEntity()).thenReturn(new org.apache.http.entity.StringEntity(
                "{\"sid\":\"SMtest\"}", java.nio.charset.StandardCharsets.UTF_8));
        Mockito.when(mockHttpClient.execute(any(org.apache.http.client.methods.HttpUriRequest.class)))
                .thenReturn(mockResponse);
        ReflectionTestUtils.setField(whatsAppDeliverySender, "httpClient", mockHttpClient);
    }

    @After
    public void tearDown() {
        ReflectionTestUtils.setField(whatsAppDeliverySender, "httpClient", originalSenderHttpClient);
        DeliveryChannelDTO whatsApp = deliveryChannelService.get(Channel.WHATSAPP).orElseThrow();
        whatsApp.setActive(false);
        deliveryChannelService.save(Channel.WHATSAPP, whatsApp, ACTOR);
    }

    private long subcontractDispatchedRows() {
        return notificationLogService.countMatching(Optional.of("SUBCONTRACT_DISPATCHED"), Optional.empty());
    }

    private static final Timestamp HANDOFF = Timestamp.valueOf("2026-05-18 10:30:00");
    private static final String ACTOR = "42";

    @Test
    public void dispatch_transitionsDraftToRequestedAndPersistsHandoffTimestamp() throws Exception {
        Referral before = referralService.getReferralById("1");
        assertEquals(ReferralStatus.DRAFT, before.getStatus());

        referralService.dispatchReferral("1", HANDOFF, ACTOR, "courier ABC");
        Referral after = referralService.getReferralById("1");

        assertEquals(ReferralStatus.REQUESTED, after.getStatus());
        ReferralSubcontract subcontract = after.getSubcontract();
        assertNotNull(subcontract);
        assertEquals(HANDOFF, subcontract.getHandoffDatetime());

        Referral reloaded = referralService.getReferralById("1");
        assertEquals(ReferralStatus.REQUESTED, reloaded.getStatus());
        assertEquals(HANDOFF, reloaded.getSubcontract().getHandoffDatetime());

        // Dispatch must write the audit row delegated through ReferralService.
        List<ReferralStatusHistory> history = referralService.getSubcontractStatusHistory("1");
        ReferralStatusHistory latest = history.get(history.size() - 1);
        assertEquals(ReferralStatus.DRAFT, latest.getFromStatus());
        assertEquals(ReferralStatus.REQUESTED, latest.getToStatus());
        assertEquals(ACTOR, latest.getChangedByUserId());
        assertEquals("courier ABC", latest.getNotes());

        // OGC-527: dispatch no longer triggers WhatsApp delivery — notifications are
        // wired to the REFERRAL_OUT event on referral save, not on the DISPATCHED
        // transition. Give any legacy async path a moment to run if it were going
        // to, then assert nothing was queued.
        Thread.sleep(300);
        assertEquals(0, subcontractDispatchedRows());
    }

    /**
     * The handoff has to reach the referral's own sent date, not only the
     * subcontract row. That column is what the Reference Lab Results page shows as
     * Sent Date and what it counts days outstanding from, so a referral dispatched
     * by a shipment box, which is the whole point of leaving one in DRAFT, would
     * otherwise never age and never appear as stuck.
     */
    @Test
    public void dispatch_stampsTheSentDateTheDashboardReads() {
        Referral before = referralService.getReferralById("1");
        assertNotEquals("precondition: the fixture's sent date is not the handoff", HANDOFF, before.getSentDate());

        referralService.dispatchReferral("1", HANDOFF, ACTOR, null);

        Referral after = referralService.getReferralById("1");
        assertEquals("the sent date is the handoff", HANDOFF, after.getSentDate());
        assertEquals("and it agrees with the subcontract", after.getSubcontract().getHandoffDatetime(),
                after.getSentDate());
    }

    @Test
    public void dispatch_referralWithoutSubcontract_isNoopAndDoesNotCallSender() throws Exception {
        // Pre-S-14 historical row (no subcontract attached) — dispatchReferral logs
        // and returns rather than throwing, so the dispatch service follows suit.
        long beforeCount = referralService.getSubcontractStatusHistory("2").size();

        referralService.dispatchReferral("2", HANDOFF, ACTOR, null);
        Referral after = referralService.getReferralById("2");
        assertNotNull(after);
        org.junit.Assert.assertNull(after.getSubcontract());

        Thread.sleep(300);
        assertEquals(0, subcontractDispatchedRows());
        assertEquals(beforeCount, referralService.getSubcontractStatusHistory("2").size());
    }

    @Test
    public void dispatch_alreadyDispatchedReferral_throwsAndDoesNotCallSender() throws Exception {
        referralService.dispatchReferral("1", HANDOFF, ACTOR, null);

        try {
            referralService.dispatchReferral("1", HANDOFF, ACTOR, null);
            fail("Expected IllegalStateException when referral is not in DRAFT state");
        } catch (IllegalStateException expected) {
            // expected
        }
        assertEquals(0, subcontractDispatchedRows());
    }

    @Test
    public void dispatch_nullHandoffDatetime_rejected() throws Exception {
        try {
            referralService.dispatchReferral("1", null, ACTOR, null);
            fail("Expected IllegalArgumentException when handoffDatetime is null");
        } catch (IllegalArgumentException expected) {
            // expected
        }
        // State must not advance and no notification must fire.
        Referral after = referralService.getReferralById("1");
        assertEquals(ReferralStatus.DRAFT, after.getStatus());
        Thread.sleep(300);
        assertEquals(0, subcontractDispatchedRows());
    }

    /**
     * With the trigger on, a dispatch queues one WhatsApp message to the CoC
     * contact in the delivery outbox; the delivery worker then sends it through the
     * WhatsApp transport and the row turns Sent.
     */
    @Test
    public void dispatch_whenSubcontractDispatchedTriggerEnabled_queuesWhatsAppToCocContactAndTheWorkerSendsIt()
            throws Exception {
        enableSubcontractDispatchedTrigger();
        setUpWhatsAppChannel();
        long logCountBefore = subcontractDispatchedRows();

        referralService.dispatchReferral("1", HANDOFF, ACTOR, "courier ABC");

        long deadline = System.currentTimeMillis() + 5000;
        long logCountAfter = logCountBefore;
        while (System.currentTimeMillis() < deadline && logCountAfter == logCountBefore) {
            logCountAfter = subcontractDispatchedRows();
            if (logCountAfter == logCountBefore) {
                Thread.sleep(50);
            }
        }
        assertEquals("the dispatcher must queue one row", logCountBefore + 1, logCountAfter);
        NotificationLog queued = notificationLogService
                .findPage(Optional.of("SUBCONTRACT_DISPATCHED"), Optional.empty(), 1, 1).get(0);
        assertEquals(NotificationRecipientType.COC_CONTACT, queued.getRecipientType());
        // CoC contact phone from referral.xml fixture (subcontract id=10):
        // "+1-555-0142"
        assertEquals("+1-555-0142", queued.getRecipientPhone());
        assertEquals(OverallStatus.QUEUED, queued.getOverallStatus());
        assertEquals(1, queued.getChannels().size());
        NotificationLogChannel ch = queued.getChannels().iterator().next();
        assertEquals(NotificationChannel.WHATSAPP, ch.getChannel());
        assertEquals(ChannelStatus.QUEUED, ch.getStatus());

        List<Claim> claims = deliveryOutboxService.claim("test-worker", 10);
        assertEquals(1, claims.size());
        deliveryOutboxService.attempt(claims.get(0), "test-worker");

        ArgumentCaptor<org.apache.http.client.methods.HttpUriRequest> captor = ArgumentCaptor
                .forClass(org.apache.http.client.methods.HttpUriRequest.class);
        verify(mockHttpClient).execute(captor.capture());
        org.apache.http.HttpEntityEnclosingRequest entityRequest = (org.apache.http.HttpEntityEnclosingRequest) captor
                .getValue();
        String body = new String(entityRequest.getEntity().getContent().readAllBytes(),
                java.nio.charset.StandardCharsets.UTF_8);
        // URL-encoded as the "To=whatsapp:..." param: "+" -> "%2B", ":" -> "%3A".
        org.junit.Assert.assertTrue("body should carry the CoC contact phone as the WhatsApp recipient: " + body,
                body.contains("whatsapp%3A%2B1-555-0142"));
        NotificationLog sent = notificationLogService.get(queued.getId());
        assertEquals(OverallStatus.SENT, sent.getOverallStatus());
        assertEquals(ChannelStatus.SUCCESS, sent.getChannels().iterator().next().getStatus());
        assertEquals("SMtest", sent.getChannels().iterator().next().getGatewayMessageId());
        org.junit.Assert.assertFalse("the patient's name is neither stored nor sent as a template variable",
                String.valueOf(sent.getTemplateVariables()).contains("patientFirstName")
                        || body.contains("patientFirstName") || body.contains("patientLastName"));
    }

    private void enableSubcontractDispatchedTrigger() {
        NotificationTriggerConfig config = notificationTriggerConfigService.getByEventCode("SUBCONTRACT_DISPATCHED")
                .orElseThrow(() -> new IllegalStateException(
                        "SUBCONTRACT_DISPATCHED trigger seed missing — migration 037 not applied"));
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

    @Test
    public void senderSendDirectly_withoutTemplate_usesFreeTextBody() throws Exception {
        setUpWhatsAppChannel();
        DeliveryChannel config = deliveryChannelDAO.findByChannel(Channel.WHATSAPP).orElseThrow();

        String sid = whatsAppDeliverySender.send(config,
                new DeliveryMessage(NotificationChannel.WHATSAPP, "+15550199", "Subj", "Hello WhatsApp"));

        assertEquals("SMtest", sid);
        ArgumentCaptor<org.apache.http.client.methods.HttpUriRequest> captor = ArgumentCaptor
                .forClass(org.apache.http.client.methods.HttpUriRequest.class);
        verify(mockHttpClient, timeout(2000)).execute(captor.capture());
        org.apache.http.HttpEntityEnclosingRequest entityRequest = (org.apache.http.HttpEntityEnclosingRequest) captor
                .getValue();
        String body = new String(entityRequest.getEntity().getContent().readAllBytes(),
                java.nio.charset.StandardCharsets.UTF_8);
        org.junit.Assert.assertTrue("body should carry free-text Body when no template: " + body,
                body.contains("Body=Hello+WhatsApp"));
        org.junit.Assert.assertTrue("the sending number comes from the channel row: " + body,
                body.contains("From=whatsapp%3A%2B14155238886"));
    }
}
