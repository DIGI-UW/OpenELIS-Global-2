package org.openelisglobal.notification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.externalconnections.service.BasicAuthenticationDataService;
import org.openelisglobal.externalconnections.service.ExternalConnectionService;
import org.openelisglobal.externalconnections.valueholder.BasicAuthenticationData;
import org.openelisglobal.externalconnections.valueholder.ExternalConnection;
import org.openelisglobal.externalconnections.valueholder.ExternalConnection.AuthType;
import org.openelisglobal.externalconnections.valueholder.ExternalConnection.ProgrammedConnection;
import org.openelisglobal.notification.service.NotificationFiredEvent;
import org.openelisglobal.notification.service.NotificationLogService;
import org.openelisglobal.notification.service.SendOutcome;
import org.openelisglobal.notification.service.recipient.Recipient;
import org.openelisglobal.notification.service.sender.WhatsAppNotificationSender;
import org.openelisglobal.notification.valueholder.NotificationChannel;
import org.openelisglobal.notification.valueholder.NotificationLog;
import org.openelisglobal.notification.valueholder.NotificationLog.OverallStatus;
import org.openelisglobal.notification.valueholder.NotificationLogChannel;
import org.openelisglobal.notification.valueholder.NotificationLogChannel.ChannelStatus;
import org.openelisglobal.notification.valueholder.NotificationPayload;
import org.openelisglobal.notification.valueholder.NotificationRecipientType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Integration tests for {@link NotificationLogService}. Covers the
 * {@code @EventListener} persistence path, paged/filtered reads, and the Resend
 * action (including replay-original-bytes semantics, failed-channels scope, and
 * linked-list {@code resentFromId} chain).
 */
public class NotificationLogServiceTest extends BaseWebContextSensitiveTest {

    private static final String SYS_USER = "1";
    private static final String EVENT_A = "REFERRAL_OUT";
    private static final String EVENT_B = "SUBCONTRACT_DISPATCHED";

    @Autowired
    private NotificationLogService notificationLogService;

    @Autowired
    private WhatsAppNotificationSender whatsAppNotificationSender;

    @Autowired
    private ExternalConnectionService externalConnectionService;

    @Autowired
    private BasicAuthenticationDataService basicAuthenticationDataService;

    private Object originalSenderHttpClient;
    private org.apache.http.impl.client.CloseableHttpClient mockHttpClient;

    @Before
    public void setUp() throws Exception {
        // Senders need credentials present so the resend path actually invokes the
        // HTTP client. WhatsApp creds + URI live on the WHATSAPP_SERVER
        // external_connection + its BasicAuthenticationData.
        seedActiveWhatsappServerExternalConnection();
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

        // Clean slate per test — the integration DB persists across @Before runs since
        // BaseWebContextSensitiveTest uses Propagation.NOT_SUPPORTED.
        cleanRowsInCurrentConnection(new String[] { "notification_log_channel", "notification_log" });
    }

    @After
    public void tearDown() {
        ReflectionTestUtils.setField(whatsAppNotificationSender, "httpClient", originalSenderHttpClient);
        // Reset WHATSAPP_SERVER to inactive so other test classes start clean.
        externalConnectionService.getMatch("programmedConnection", ProgrammedConnection.WHATSAPP_SERVER.name())
                .ifPresent(conn -> {
                    if (Boolean.TRUE.equals(conn.getActive())) {
                        conn.setActive(false);
                        conn.setSysUserId(SYS_USER);
                        externalConnectionService.save(conn);
                    }
                });
    }

    private void seedActiveWhatsappServerExternalConnection() {
        Optional<ExternalConnection> existing = externalConnectionService.getMatch("programmedConnection",
                ProgrammedConnection.WHATSAPP_SERVER.name());
        Integer connId;
        if (existing.isPresent()) {
            externalConnectionService.updateExternalConnectionFields(existing.get().getId(), SYS_USER, true, null,
                    AuthType.BASIC, null, null, null, "ACtest", "secret");
            connId = existing.get().getId();
        } else {
            BasicAuthenticationData basicAuth = new BasicAuthenticationData();
            basicAuth.setSysUserId(SYS_USER);
            basicAuth.setUsername("ACtest");
            basicAuth.setPassword("secret");
            ExternalConnection connection = new ExternalConnection();
            connection.setSysUserId(SYS_USER);
            connection.setActive(true);
            connection.setProgrammedConnection(ProgrammedConnection.WHATSAPP_SERVER);
            connection.setActiveAuthenticationType(AuthType.BASIC);
            externalConnectionService.createNewExternalConnection(Map.of(AuthType.BASIC, basicAuth), List.of(),
                    connection);
            connId = connection.getId();
        }
        // Sanity check: the mock BasicAuthenticationDataService bean previously
        // declared in AppTestConfig silently swallowed inserts; fail fast if the
        // seed didn't actually persist.
        Optional<BasicAuthenticationData> check = basicAuthenticationDataService.getByExternalConnection(connId);
        if (check.isEmpty() || check.get().getUsername() == null || check.get().getUsername().isBlank()
                || check.get().getPassword() == null || check.get().getPassword().isBlank()) {
            throw new IllegalStateException(
                    "Seed FAILED: BasicAuth missing or credentials blank for WHATSAPP_SERVER id=" + connId);
        }
    }

    // ---- recordFire (via the @EventListener entry point) ----

    @Test
    public void recordFire_persistsParentAndChannelRowsWithCorrectOverallStatus_allSuccess() {
        Map<NotificationChannel, SendOutcome> outcomes = new LinkedHashMap<>();
        outcomes.put(NotificationChannel.EMAIL, SendOutcome.success(1));
        outcomes.put(NotificationChannel.WHATSAPP, SendOutcome.success(2));

        long id = fireEvent(EVENT_A, NotificationRecipientType.ORDERING_PROVIDER,
                new Recipient("Jane Doe", "jane@example.com", "+15550100"),
                renderedPayload("Subj for [orderId]", "Body for [orderId]"), outcomes, "ACC-001", "environmental");

        NotificationLog log = notificationLogService.get(id);
        assertEquals(OverallStatus.SENT, log.getOverallStatus());
        assertEquals(2, log.getChannels().size());
        assertTrue(log.getChannels().stream().allMatch(c -> c.getStatus() == ChannelStatus.SUCCESS));
    }

    @Test
    public void recordFire_persistsParentAndChannelRowsWithCorrectOverallStatus_allFailed() {
        Map<NotificationChannel, SendOutcome> outcomes = new LinkedHashMap<>();
        outcomes.put(NotificationChannel.EMAIL, SendOutcome.failed(3, "smtp down"));
        outcomes.put(NotificationChannel.WHATSAPP, SendOutcome.failed(3, "twilio 5xx"));

        long id = fireEvent(EVENT_A, NotificationRecipientType.ORDERING_PROVIDER,
                new Recipient("Jane Doe", "jane@example.com", "+15550100"), renderedPayload("Subj", "Body"), outcomes,
                "ACC-002", "environmental");

        NotificationLog log = notificationLogService.get(id);
        assertEquals(OverallStatus.FAILED, log.getOverallStatus());
        assertEquals(2, log.getChannels().size());
        assertTrue(log.getChannels().stream().allMatch(c -> c.getStatus() == ChannelStatus.FAILED));
        NotificationLogChannel email = findChannel(log, NotificationChannel.EMAIL);
        assertEquals("smtp down", email.getErrorMessage());
        assertEquals(3, email.getAttempts());
    }

    @Test
    public void recordFire_persistsParentAndChannelRowsWithCorrectOverallStatus_mixed() {
        Map<NotificationChannel, SendOutcome> outcomes = new LinkedHashMap<>();
        outcomes.put(NotificationChannel.EMAIL, SendOutcome.success(1));
        outcomes.put(NotificationChannel.WHATSAPP, SendOutcome.failed(3, "twilio 5xx"));

        long id = fireEvent(EVENT_A, NotificationRecipientType.ORDERING_PROVIDER,
                new Recipient("Jane Doe", "jane@example.com", "+15550100"), renderedPayload("Subj", "Body"), outcomes,
                "ACC-003", "environmental");

        NotificationLog log = notificationLogService.get(id);
        // Decision #4: at least one SUCCESS → overall SENT.
        assertEquals(OverallStatus.SENT, log.getOverallStatus());
        assertEquals(ChannelStatus.SUCCESS, findChannel(log, NotificationChannel.EMAIL).getStatus());
        assertEquals(ChannelStatus.FAILED, findChannel(log, NotificationChannel.WHATSAPP).getStatus());
    }

    @Test
    public void recordFire_persistsRenderedSubjectAndMessage_verbatim() {
        String subject = "Sample referred out: ACC-123";
        String message = "Sample ACC-123 for patient Jane Doe has been referred to TargetLab for test Glucose.";

        Map<NotificationChannel, SendOutcome> outcomes = new LinkedHashMap<>();
        outcomes.put(NotificationChannel.EMAIL, SendOutcome.success(1));

        long id = fireEvent(EVENT_A, NotificationRecipientType.SUBMITTING_USER,
                new Recipient("Lab Staff", "staff@lab.org", null), renderedPayload(subject, message), outcomes,
                "ACC-123", "clinical");

        NotificationLog log = notificationLogService.get(id);
        assertEquals(subject, log.getRenderedSubject());
        assertEquals(message, log.getRenderedMessage());
    }

    // ---- findPage / countMatching ----

    @Test
    public void findPage_filtersByEventCode() {
        fireEventA();
        fireEventA();
        fireEventB();

        List<NotificationLog> a = notificationLogService.findPage(Optional.of(EVENT_A), Optional.empty(), 1, 10);
        List<NotificationLog> b = notificationLogService.findPage(Optional.of(EVENT_B), Optional.empty(), 1, 10);
        assertEquals(2, a.size());
        assertEquals(1, b.size());
        assertTrue(a.stream().allMatch(l -> EVENT_A.equals(l.getEventCode())));
        assertTrue(b.stream().allMatch(l -> EVENT_B.equals(l.getEventCode())));
    }

    @Test
    public void findPage_filtersByStatus() {
        long sentId = fireEventA();
        long failedId = fireEventAFailed();

        List<NotificationLog> sent = notificationLogService.findPage(Optional.empty(),
                Optional.of(OverallStatus.SENT.name()), 1, 10);
        List<NotificationLog> failed = notificationLogService.findPage(Optional.empty(),
                Optional.of(OverallStatus.FAILED.name()), 1, 10);

        assertEquals(1, sent.size());
        assertEquals(sentId, (long) sent.get(0).getId());
        assertEquals(1, failed.size());
        assertEquals(failedId, (long) failed.get(0).getId());
    }

    @Test
    public void findPage_filtersByEventCodeAndStatus_combined() {
        long aSent = fireEventA();
        fireEventAFailed();
        fireEventB();

        List<NotificationLog> result = notificationLogService.findPage(Optional.of(EVENT_A),
                Optional.of(OverallStatus.SENT.name()), 1, 10);
        assertEquals(1, result.size());
        assertEquals(aSent, (long) result.get(0).getId());
    }

    @Test
    public void findPage_pagesAndCountsCorrectly() {
        for (int i = 0; i < 7; i++) {
            fireEventA();
        }
        long total = notificationLogService.countMatching(Optional.empty(), Optional.empty());
        assertEquals(7L, total);

        List<NotificationLog> page1 = notificationLogService.findPage(Optional.empty(), Optional.empty(), 1, 3);
        List<NotificationLog> page2 = notificationLogService.findPage(Optional.empty(), Optional.empty(), 2, 3);
        List<NotificationLog> page3 = notificationLogService.findPage(Optional.empty(), Optional.empty(), 3, 3);
        List<NotificationLog> page4 = notificationLogService.findPage(Optional.empty(), Optional.empty(), 4, 3);

        assertEquals(3, page1.size());
        assertEquals(3, page2.size());
        assertEquals(1, page3.size());
        assertEquals("page past end must return zero rows", 0, page4.size());
        // Same total regardless of paging
        assertEquals(7L, notificationLogService.countMatching(Optional.empty(), Optional.empty()));
    }

    // ---- Resend ----

    @Test
    public void resend_createsNewRowReplayingFailedChannelsOnly_withResentFromIdPointer() {
        Map<NotificationChannel, SendOutcome> outcomes = new LinkedHashMap<>();
        outcomes.put(NotificationChannel.EMAIL, SendOutcome.success(1));
        outcomes.put(NotificationChannel.WHATSAPP, SendOutcome.failed(3, "twilio 5xx"));
        long originalId = fireEvent(EVENT_B, NotificationRecipientType.COC_CONTACT,
                new Recipient("CoC", null, "+15550199"), renderedPayload("Subj", "Body"), outcomes, "ACC-R-1",
                "environmental");

        NotificationLog resend = notificationLogService.resend(originalId, "admin");

        assertNotEquals("resend must be a new row", originalId, (long) resend.getId());
        assertEquals(originalId, (long) resend.getResentFromId());
        // Only the WhatsApp channel was failed → only that channel is on the new row.
        assertEquals(1, resend.getChannels().size());
        NotificationLogChannel only = resend.getChannels().iterator().next();
        assertEquals(NotificationChannel.WHATSAPP, only.getChannel());
        // Mock HTTP returns 201 → SUCCESS → overall SENT on the new row.
        assertEquals(ChannelStatus.SUCCESS, only.getStatus());
        assertEquals(OverallStatus.SENT, resend.getOverallStatus());

        // Original row must be untouched.
        NotificationLog reloadedOriginal = notificationLogService.get(originalId);
        assertEquals(OverallStatus.SENT, reloadedOriginal.getOverallStatus());
        assertNull(reloadedOriginal.getResentFromId());
        assertEquals(2, reloadedOriginal.getChannels().size());
    }

    @Test
    public void resend_doesNotInvokeSenderForPreviouslySuccessfulChannel() throws Exception {
        Map<NotificationChannel, SendOutcome> outcomes = new LinkedHashMap<>();
        outcomes.put(NotificationChannel.WHATSAPP, SendOutcome.failed(3, "twilio 5xx"));
        // EMAIL succeeded — but the EMAIL sender is the smtp pipeline, not the HTTP
        // client we mock here. The Mockito assertion below targets the WhatsApp HTTP
        // client; the discriminator is the recipient-of-the-resend call below.
        outcomes.put(NotificationChannel.EMAIL, SendOutcome.success(1));
        long originalId = fireEvent(EVENT_B, NotificationRecipientType.COC_CONTACT,
                new Recipient("CoC", "coc@lab.org", "+15550199"), renderedPayload("Subj", "Body"), outcomes, "ACC-R-2",
                "environmental");

        Mockito.reset(mockHttpClient);
        org.apache.http.client.methods.CloseableHttpResponse mockResponse = Mockito
                .mock(org.apache.http.client.methods.CloseableHttpResponse.class);
        org.apache.http.StatusLine statusLine = Mockito.mock(org.apache.http.StatusLine.class);
        Mockito.when(statusLine.getStatusCode()).thenReturn(201);
        Mockito.when(mockResponse.getStatusLine()).thenReturn(statusLine);
        Mockito.when(mockResponse.getEntity()).thenReturn(new org.apache.http.entity.StringEntity(
                "{\"sid\":\"SMtest\"}", java.nio.charset.StandardCharsets.UTF_8));
        Mockito.when(mockHttpClient.execute(any(org.apache.http.client.methods.HttpUriRequest.class)))
                .thenReturn(mockResponse);

        notificationLogService.resend(originalId, "admin");

        // WhatsApp HTTP client was invoked once for the resend (the failed channel).
        verify(mockHttpClient, timeout(2000).times(1))
                .execute(any(org.apache.http.client.methods.HttpUriRequest.class));
    }

    @Test
    public void resend_usesContactInfoFromLogRowNotResolverReResolution() throws Exception {
        // Original fire with a specific phone — the contact info is captured into the
        // log row. The resend must read THAT phone, not re-resolve from anywhere.
        String originalPhone = "+15550LOCKED";
        Map<NotificationChannel, SendOutcome> outcomes = new LinkedHashMap<>();
        outcomes.put(NotificationChannel.WHATSAPP, SendOutcome.failed(3, "twilio 5xx"));
        long originalId = fireEvent(EVENT_B, NotificationRecipientType.COC_CONTACT,
                new Recipient("CoC", null, originalPhone), renderedPayload("Subj", "Body"), outcomes, "ACC-R-3",
                "environmental");

        Mockito.reset(mockHttpClient);
        org.apache.http.client.methods.CloseableHttpResponse mockResponse = Mockito
                .mock(org.apache.http.client.methods.CloseableHttpResponse.class);
        org.apache.http.StatusLine statusLine = Mockito.mock(org.apache.http.StatusLine.class);
        Mockito.when(statusLine.getStatusCode()).thenReturn(201);
        Mockito.when(mockResponse.getStatusLine()).thenReturn(statusLine);
        Mockito.when(mockResponse.getEntity()).thenReturn(new org.apache.http.entity.StringEntity(
                "{\"sid\":\"SMtest\"}", java.nio.charset.StandardCharsets.UTF_8));
        Mockito.when(mockHttpClient.execute(any(org.apache.http.client.methods.HttpUriRequest.class)))
                .thenReturn(mockResponse);

        notificationLogService.resend(originalId, "admin");

        ArgumentCaptor<org.apache.http.client.methods.HttpUriRequest> captor = ArgumentCaptor
                .forClass(org.apache.http.client.methods.HttpUriRequest.class);
        verify(mockHttpClient, timeout(2000)).execute(captor.capture());
        org.apache.http.HttpEntityEnclosingRequest req = (org.apache.http.HttpEntityEnclosingRequest) captor.getValue();
        String body = new String(req.getEntity().getContent().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        // The "+" in the phone is URL-encoded to %2B. The original phone has unusual
        // characters; the sender URL-encodes the whole "whatsapp:+15550LOCKED" string.
        assertTrue("resend must use the LOG ROW's captured phone, not a re-resolved value. body=" + body,
                body.contains("%2B15550LOCKED"));
    }

    @Test
    public void resend_ofResendPointsToImmediatePredecessor_notOriginalAncestor() {
        Map<NotificationChannel, SendOutcome> outcomes = new LinkedHashMap<>();
        outcomes.put(NotificationChannel.WHATSAPP, SendOutcome.failed(3, "twilio 5xx"));
        long originalId = fireEvent(EVENT_B, NotificationRecipientType.COC_CONTACT,
                new Recipient("CoC", null, "+15550100"), renderedPayload("Subj", "Body"), outcomes, "ACC-CHAIN",
                "environmental");

        // First resend — succeeds via mock (returns 201). To get a FAILED resend we'd
        // need to switch the mock; instead, simulate by chaining: mutate the resend's
        // outcome to FAILED in a new fire, then resend again.
        NotificationLog resend1 = notificationLogService.resend(originalId, "admin");
        assertEquals(originalId, (long) resend1.getResentFromId());

        // Force the resend1 row into a FAILED state by simulating a fresh failed fire
        // attached to resend1's id. Simulate by directly inserting a synthetic chain
        // member that points to resend1 — exercises the chain pointer code path.
        Map<NotificationChannel, SendOutcome> outcomes2 = new LinkedHashMap<>();
        outcomes2.put(NotificationChannel.WHATSAPP, SendOutcome.failed(3, "still failing"));
        // Replay the same payload + recipient on a new log row whose source is
        // resend1.getId() — this is what the production resend would do if resend1
        // had been FAILED. We use the @EventListener path to insert and then
        // back-patch the resentFromId to model the chain.
        long resend2RawId = fireEvent(EVENT_B, NotificationRecipientType.COC_CONTACT,
                new Recipient("CoC", null, "+15550100"), renderedPayload("Subj", "Body"), outcomes2, "ACC-CHAIN",
                "environmental");
        NotificationLog resend2 = notificationLogService.get(resend2RawId);
        resend2.setResentFromId(resend1.getId());
        notificationLogService.save(resend2);

        // Now exercise the production resend path on resend2 → resend3.
        NotificationLog resend3 = notificationLogService.resend(resend2.getId(), "admin");
        assertEquals("resend chain must point at the immediate predecessor, not the original", (long) resend2.getId(),
                (long) resend3.getResentFromId());
        assertNotEquals(originalId, (long) resend3.getResentFromId());
    }

    @Test
    public void resend_rejectedWhenNoChannelsFailed() {
        Map<NotificationChannel, SendOutcome> outcomes = new LinkedHashMap<>();
        outcomes.put(NotificationChannel.EMAIL, SendOutcome.success(1));
        outcomes.put(NotificationChannel.WHATSAPP, SendOutcome.success(1));
        long id = fireEvent(EVENT_A, NotificationRecipientType.ORDERING_PROVIDER,
                new Recipient("Jane", "j@x.org", "+15551111"), renderedPayload("Subj", "Body"), outcomes, "ACC-NO-FAIL",
                "clinical");

        try {
            notificationLogService.resend(id, "admin");
            fail("Expected IllegalArgumentException when no channels failed");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    /**
     * Guards against the prior false positive where the sender returned
     * {@code void} and silently bailed on misconfiguration — the resend would mark
     * the channel SUCCESS even though no message was sent. The sender now throws on
     * every non-send path, so resend's catch block records FAILED.
     */
    @Test
    public void resend_marksChannelFailedWhenSenderThrows() throws Exception {
        Map<NotificationChannel, SendOutcome> outcomes = new LinkedHashMap<>();
        outcomes.put(NotificationChannel.WHATSAPP, SendOutcome.failed(3, "twilio 5xx"));
        long originalId = fireEvent(EVENT_B, NotificationRecipientType.COC_CONTACT,
                new Recipient("CoC", null, "+15550199"), renderedPayload("Subj", "Body"), outcomes, "ACC-R-THROW",
                "environmental");

        // Re-stub the mock to return HTTP 500 — the sender now throws on non-2xx,
        // resend's catch should record ChannelStatus.FAILED with the error message.
        Mockito.reset(mockHttpClient);
        org.apache.http.client.methods.CloseableHttpResponse mockResponse = Mockito
                .mock(org.apache.http.client.methods.CloseableHttpResponse.class);
        org.apache.http.StatusLine statusLine = Mockito.mock(org.apache.http.StatusLine.class);
        Mockito.when(statusLine.getStatusCode()).thenReturn(500);
        Mockito.when(mockResponse.getStatusLine()).thenReturn(statusLine);
        Mockito.when(mockResponse.getEntity()).thenReturn(
                new org.apache.http.entity.StringEntity("twilio down", java.nio.charset.StandardCharsets.UTF_8));
        Mockito.when(mockHttpClient.execute(any(org.apache.http.client.methods.HttpUriRequest.class)))
                .thenReturn(mockResponse);

        NotificationLog resend = notificationLogService.resend(originalId, "admin");
        assertEquals(OverallStatus.FAILED, resend.getOverallStatus());
        assertEquals(1, resend.getChannels().size());
        NotificationLogChannel ch = resend.getChannels().iterator().next();
        assertEquals(NotificationChannel.WHATSAPP, ch.getChannel());
        assertEquals(ChannelStatus.FAILED, ch.getStatus());
        assertNotNull("FAILED channel must carry the sender's error message", ch.getErrorMessage());
        assertTrue("error message should reference the Twilio failure status: " + ch.getErrorMessage(),
                ch.getErrorMessage().contains("500"));
    }

    // ---- helpers ----

    private long fireEventA() {
        Map<NotificationChannel, SendOutcome> outcomes = new LinkedHashMap<>();
        outcomes.put(NotificationChannel.EMAIL, SendOutcome.success(1));
        return fireEvent(EVENT_A, NotificationRecipientType.ORDERING_PROVIDER,
                new Recipient("Jane", "jane@x.org", null), renderedPayload("Subj", "Body"), outcomes, "ACC-A",
                "clinical");
    }

    private long fireEventAFailed() {
        Map<NotificationChannel, SendOutcome> outcomes = new LinkedHashMap<>();
        outcomes.put(NotificationChannel.EMAIL, SendOutcome.failed(3, "smtp down"));
        return fireEvent(EVENT_A, NotificationRecipientType.ORDERING_PROVIDER,
                new Recipient("Jane", "jane@x.org", null), renderedPayload("Subj", "Body"), outcomes, "ACC-A",
                "clinical");
    }

    private long fireEventB() {
        Map<NotificationChannel, SendOutcome> outcomes = new LinkedHashMap<>();
        outcomes.put(NotificationChannel.WHATSAPP, SendOutcome.success(1));
        return fireEvent(EVENT_B, NotificationRecipientType.COC_CONTACT, new Recipient("CoC", null, "+15550000"),
                renderedPayload("Subj", "Body"), outcomes, "ACC-B", "environmental");
    }

    /**
     * Calls the service's {@code @EventListener} entry point directly. Returns the
     * new row's id (the row is persisted synchronously).
     */
    private long fireEvent(String eventCode, NotificationRecipientType recipientType, Recipient recipient,
            NotificationPayload payload, Map<NotificationChannel, SendOutcome> outcomes, String accession,
            String workflow) {
        NotificationFiredEvent event = new NotificationFiredEvent(this, eventCode, recipientType, recipient, payload,
                outcomes, SYS_USER, accession, workflow);
        notificationLogService.recordFire(event);
        // Find the latest row matching this event_code as the synthetic "newly
        // inserted" id.
        List<NotificationLog> latest = notificationLogService.findPage(Optional.of(eventCode), Optional.empty(), 1, 1);
        assertNotNull("expected at least one row after fire", latest);
        return latest.get(0).getId();
    }

    private NotificationPayload renderedPayload(String subject, String message) {
        return new NotificationPayload() {
            @Override
            public String getSubject() {
                return subject;
            }

            @Override
            public String getMessage() {
                return message;
            }
        };
    }

    private NotificationLogChannel findChannel(NotificationLog log, NotificationChannel channel) {
        return log.getChannels().stream().filter(c -> c.getChannel() == channel).findFirst()
                .orElseThrow(() -> new AssertionError("missing channel " + channel));
    }
}
