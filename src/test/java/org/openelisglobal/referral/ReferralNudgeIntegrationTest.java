package org.openelisglobal.referral;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

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
import org.openelisglobal.notification.valueholder.NotificationRecipientType;
import org.openelisglobal.notification.valueholder.NotificationTriggerConfig;
import org.openelisglobal.referral.service.ReferralService;
import org.openelisglobal.referral.valueholder.ReferralStatusHistory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

public class ReferralNudgeIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String ACTOR = "42";

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
        externalConnectionService.getMatch("programmedConnection", ProgrammedConnection.WHATSAPP_SERVER.name())
                .ifPresent(conn -> {
                    if (Boolean.TRUE.equals(conn.getActive())) {
                        conn.setActive(false);
                        conn.setSysUserId(ACTOR);
                        externalConnectionService.save(conn);
                    }
                });
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

    @Test
    public void nudge_whenTriggerEnabled_firesWhatsAppToCocContact() throws Exception {
        enableReferralNudgeTrigger();
        seedActiveWhatsappServerExternalConnection();
        long logCountBefore = notificationLogService.countMatching(Optional.of("REFERRAL_NUDGE"), Optional.empty());

        referralService.nudgeReferenceLab("1", "Please prioritise", ACTOR);

        ArgumentCaptor<org.apache.http.client.methods.HttpUriRequest> captor = ArgumentCaptor
                .forClass(org.apache.http.client.methods.HttpUriRequest.class);
        verify(mockHttpClient, timeout(2000)).execute(captor.capture());
        org.apache.http.HttpEntityEnclosingRequest entityRequest = (org.apache.http.HttpEntityEnclosingRequest) captor
                .getValue();
        String body = new String(entityRequest.getEntity().getContent().readAllBytes(),
                java.nio.charset.StandardCharsets.UTF_8);
        // CoC contact phone from referral.xml (subcontract 10): "+1-555-0142".
        assertTrue("WhatsApp recipient must be the CoC contact: " + body, body.contains("whatsapp%3A%2B1-555-0142"));

        long deadline = System.currentTimeMillis() + 2000;
        long logCountAfter = logCountBefore;
        while (System.currentTimeMillis() < deadline && logCountAfter == logCountBefore) {
            logCountAfter = notificationLogService.countMatching(Optional.of("REFERRAL_NUDGE"), Optional.empty());
            if (logCountAfter == logCountBefore) {
                Thread.sleep(50);
            }
        }
        assertEquals("nudge must persist one REFERRAL_NUDGE NotificationLog row", logCountBefore + 1, logCountAfter);
    }

    private void enableReferralNudgeTrigger() {
        NotificationTriggerConfig config = notificationTriggerConfigService.getByEventCode("REFERRAL_NUDGE")
                .orElseThrow(() -> new IllegalStateException("REFERRAL_NUDGE trigger seed missing — migration 053"));
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
}
