package org.openelisglobal.referral;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.externalconnections.service.ExternalConnectionService;
import org.openelisglobal.externalconnections.valueholder.BasicAuthenticationData;
import org.openelisglobal.externalconnections.valueholder.ExternalConnection;
import org.openelisglobal.externalconnections.valueholder.ExternalConnection.AuthType;
import org.openelisglobal.externalconnections.valueholder.ExternalConnection.ProgrammedConnection;
import org.openelisglobal.notification.service.NotificationLogService;
import org.openelisglobal.notification.service.NotificationTriggerConfigService;
import org.openelisglobal.notification.service.sender.WhatsAppNotificationSender;
import org.openelisglobal.notification.valueholder.NotificationChannel;
import org.openelisglobal.notification.valueholder.NotificationLog;
import org.openelisglobal.notification.valueholder.NotificationLog.OverallStatus;
import org.openelisglobal.notification.valueholder.NotificationLogChannel;
import org.openelisglobal.notification.valueholder.NotificationLogChannel.ChannelStatus;
import org.openelisglobal.notification.valueholder.NotificationRecipientType;
import org.openelisglobal.notification.valueholder.NotificationTriggerConfig;
import org.openelisglobal.notification.valueholder.WhatsAppNotification;
import org.openelisglobal.referral.service.ReferralService;
import org.openelisglobal.referral.valueholder.Referral;
import org.openelisglobal.referral.valueholder.ReferralStatusHistory;
import org.openelisglobal.referral.valueholder.ReferralSubcontract;
import org.openelisglobal.referral.valueholder.SubcontractStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

public class ReferralSubcontractDispatchIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ReferralService referralService;

    @Autowired
    private WhatsAppNotificationSender whatsAppNotificationSender;

    @Autowired
    private NotificationTriggerConfigService notificationTriggerConfigService;

    @Autowired
    private ExternalConnectionService externalConnectionService;

    @Autowired
    private NotificationLogService notificationLogService;

    private Object originalSenderHttpClient;
    private org.apache.http.impl.client.CloseableHttpClient mockHttpClient;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/referral.xml");
        // Connection + credentials live on the WHATSAPP_SERVER external_connection +
        // BasicAuthenticationData. Tests that exercise dispatch or directly invoke the
        // sender opt in via seedActiveWhatsappServerExternalConnection(); the default
        // no-fire baseline tests rely on the trigger being disabled by default.
        ReflectionTestUtils.setField(whatsAppNotificationSender, "whatsappFromNumber", "+14155238886");

        originalSenderHttpClient = ReflectionTestUtils.getField(whatsAppNotificationSender, "httpClient");
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
        ReflectionTestUtils.setField(whatsAppNotificationSender, "httpClient", mockHttpClient);
    }

    @After
    public void tearDown() {
        ReflectionTestUtils.setField(whatsAppNotificationSender, "httpClient", originalSenderHttpClient);
        // Reset the WHATSAPP_SERVER connection back to inactive so tests that
        // expect a no-fire baseline (e.g. dispatch_transitionsDraftToDispatched...)
        // aren't poisoned by a prior test having flipped it active.
        externalConnectionService.getMatch("programmedConnection", ProgrammedConnection.WHATSAPP_SERVER.name())
                .ifPresent(conn -> {
                    if (Boolean.TRUE.equals(conn.getActive())) {
                        conn.setActive(false);
                        conn.setSysUserId(ACTOR);
                        externalConnectionService.save(conn);
                    }
                });
    }

    private static final Timestamp HANDOFF = Timestamp.valueOf("2026-05-18 10:30:00");
    private static final String ACTOR = "42";

    @Test
    public void dispatch_transitionsDraftToDispatchedAndPersistsHandoffTimestamp() throws Exception {
        Referral before = referralService.getReferralById("1");
        assertEquals(SubcontractStatus.DRAFT, before.getSubcontract().getSubcontractStatus());

        referralService.dispatchSubcontract("1", HANDOFF, ACTOR, "courier ABC");
        Referral after = referralService.getReferralById("1");

        ReferralSubcontract subcontract = after.getSubcontract();
        assertNotNull(subcontract);
        assertEquals(SubcontractStatus.DISPATCHED, subcontract.getSubcontractStatus());
        assertEquals(HANDOFF, subcontract.getHandoffDatetime());

        Referral reloaded = referralService.getReferralById("1");
        assertEquals(SubcontractStatus.DISPATCHED, reloaded.getSubcontract().getSubcontractStatus());
        assertEquals(HANDOFF, reloaded.getSubcontract().getHandoffDatetime());

        // Dispatch must write the FR-02 audit row delegated through ReferralService.
        List<ReferralStatusHistory> history = referralService.getSubcontractStatusHistory("1");
        ReferralStatusHistory latest = history.get(history.size() - 1);
        assertEquals(SubcontractStatus.DRAFT, latest.getFromStatus());
        assertEquals(SubcontractStatus.DISPATCHED, latest.getToStatus());
        assertEquals(ACTOR, latest.getChangedByUserId());
        assertEquals("courier ABC", latest.getNotes());

        // OGC-527: dispatch no longer triggers WhatsApp delivery — notifications are
        // wired to
        // the REFERRAL_OUT event on referral save, not on the DISPATCHED transition.
        // Give any
        // legacy async path a moment to run if it were going to, then assert the
        // sender's HTTP
        // client was never invoked.
        Thread.sleep(300);
        verify(mockHttpClient, never()).execute(any(org.apache.http.client.methods.HttpUriRequest.class));
    }

    @Test
    public void dispatch_referralWithoutSubcontract_isNoopAndDoesNotCallSender() throws Exception {
        // Pre-S-14 historical row (no subcontract attached) — dispatchSubcontract logs
        // and
        // returns rather than throwing, so the dispatch service follows suit.
        long beforeCount = referralService.getSubcontractStatusHistory("2").size();

        referralService.dispatchSubcontract("2", HANDOFF, ACTOR, null);
        Referral after = referralService.getReferralById("2");
        assertNotNull(after);
        org.junit.Assert.assertNull(after.getSubcontract());

        Thread.sleep(300);
        verify(mockHttpClient, never()).execute(any(org.apache.http.client.methods.HttpUriRequest.class));
        assertEquals(beforeCount, referralService.getSubcontractStatusHistory("2").size());
    }

    @Test
    public void dispatch_alreadyDispatchedSubcontract_throwsAndDoesNotCallSender() throws Exception {
        referralService.dispatchSubcontract("1", HANDOFF, ACTOR, null);

        try {
            referralService.dispatchSubcontract("1", HANDOFF, ACTOR, null);
            fail("Expected IllegalStateException when subcontract is not in DRAFT state");
        } catch (IllegalStateException expected) {
            // expected
        }
        verify(mockHttpClient, never()).execute(any(org.apache.http.client.methods.HttpUriRequest.class));
    }

    @Test
    public void dispatch_nullHandoffDatetime_rejected() throws Exception {
        try {
            referralService.dispatchSubcontract("1", null, ACTOR, null);
            fail("Expected IllegalArgumentException when handoffDatetime is null");
        } catch (IllegalArgumentException expected) {
            // expected
        }
        // State must not advance and no notification must fire.
        Referral after = referralService.getReferralById("1");
        assertEquals(SubcontractStatus.DRAFT, after.getSubcontract().getSubcontractStatus());
        Thread.sleep(300);
        verify(mockHttpClient, never()).execute(any(org.apache.http.client.methods.HttpUriRequest.class));
    }

    @Test
    public void dispatch_whenSubcontractDispatchedTriggerEnabled_firesWhatsAppToCocContact() throws Exception {
        enableSubcontractDispatchedTrigger();
        seedActiveWhatsappServerExternalConnection();
        long logCountBefore = notificationLogService.countMatching(Optional.of("SUBCONTRACT_DISPATCHED"),
                Optional.empty());

        referralService.dispatchSubcontract("1", HANDOFF, ACTOR, "courier ABC");

        ArgumentCaptor<org.apache.http.client.methods.HttpUriRequest> captor = ArgumentCaptor
                .forClass(org.apache.http.client.methods.HttpUriRequest.class);
        verify(mockHttpClient, timeout(2000)).execute(captor.capture());

        org.apache.http.HttpEntityEnclosingRequest entityRequest = (org.apache.http.HttpEntityEnclosingRequest) captor
                .getValue();
        String body = new String(entityRequest.getEntity().getContent().readAllBytes(),
                java.nio.charset.StandardCharsets.UTF_8);
        // CoC contact phone from referral.xml fixture (subcontract id=10):
        // "+1-555-0142"
        // URL-encoded as the "To=whatsapp:..." param: "+" -> "%2B", ":" -> "%3A".
        org.junit.Assert.assertTrue("body should carry the CoC contact phone as the WhatsApp recipient: " + body,
                body.contains("whatsapp%3A%2B1-555-0142"));

        // The @EventListener must persist a NotificationLog row on the async
        // dispatcher thread. Previously this was silently failing with
        // "no transaction is in progress" because the @Transactional advice
        // wasn't being applied through the JDK dynamic proxy on the async path;
        // the listener now drives the write through TransactionTemplate.
        // Poll briefly because the listener fires after the HTTP call.
        long deadline = System.currentTimeMillis() + 2000;
        long logCountAfter = logCountBefore;
        while (System.currentTimeMillis() < deadline && logCountAfter == logCountBefore) {
            logCountAfter = notificationLogService.countMatching(Optional.of("SUBCONTRACT_DISPATCHED"),
                    Optional.empty());
            if (logCountAfter == logCountBefore) {
                Thread.sleep(50);
            }
        }
        assertEquals("dispatcher fire must persist one NotificationLog row via @EventListener", logCountBefore + 1,
                logCountAfter);
        List<NotificationLog> rows = notificationLogService.findPage(Optional.of("SUBCONTRACT_DISPATCHED"),
                Optional.empty(), 1, 1);
        NotificationLog persisted = rows.get(0);
        assertEquals(NotificationRecipientType.COC_CONTACT, persisted.getRecipientType());
        assertEquals(OverallStatus.SENT, persisted.getOverallStatus());
        assertEquals(1, persisted.getChannels().size());
        NotificationLogChannel ch = persisted.getChannels().iterator().next();
        assertEquals(NotificationChannel.WHATSAPP, ch.getChannel());
        assertEquals(ChannelStatus.SUCCESS, ch.getStatus());
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

    private void seedActiveWhatsappServerExternalConnection() {
        Optional<ExternalConnection> existing = externalConnectionService.getMatch("programmedConnection",
                ProgrammedConnection.WHATSAPP_SERVER.name());
        if (existing.isPresent()) {
            externalConnectionService.updateExternalConnectionFields(existing.get().getId(), ACTOR, true, null,
                    AuthType.BASIC, null, null, null, "ACtest", "secret");
            return;
        }
        BasicAuthenticationData basicAuth = new BasicAuthenticationData();
        basicAuth.setSysUserId(ACTOR);
        basicAuth.setUsername("ACtest");
        basicAuth.setPassword("secret");
        ExternalConnection connection = new ExternalConnection();
        connection.setSysUserId(ACTOR);
        connection.setActive(true);
        connection.setProgrammedConnection(ProgrammedConnection.WHATSAPP_SERVER);
        connection.setActiveAuthenticationType(AuthType.BASIC);
        externalConnectionService.createNewExternalConnection(Map.of(AuthType.BASIC, basicAuth), List.of(), connection);
    }

    @Test
    public void senderSendDirectly_withoutTemplate_usesFreeTextBody() throws Exception {
        seedActiveWhatsappServerExternalConnection();
        WhatsAppNotification notification = new WhatsAppNotification();
        notification.setReceiverPhoneNumber("+15550199");
        org.openelisglobal.notification.valueholder.NotificationPayload payload = new org.openelisglobal.notification.valueholder.NotificationPayload() {
            @Override
            public String getMessage() {
                return "Hello WhatsApp";
            }

            @Override
            public String getSubject() {
                return "Subj";
            }
        };
        notification.setPayload(payload);

        whatsAppNotificationSender.send(notification);

        ArgumentCaptor<org.apache.http.client.methods.HttpUriRequest> captor = ArgumentCaptor
                .forClass(org.apache.http.client.methods.HttpUriRequest.class);
        verify(mockHttpClient, timeout(2000)).execute(captor.capture());
        org.apache.http.HttpEntityEnclosingRequest entityRequest = (org.apache.http.HttpEntityEnclosingRequest) captor
                .getValue();
        String body = new String(entityRequest.getEntity().getContent().readAllBytes(),
                java.nio.charset.StandardCharsets.UTF_8);
        org.junit.Assert.assertTrue("body should carry free-text Body when no template: " + body,
                body.contains("Body=Hello+WhatsApp"));
    }
}
