package org.openelisglobal.delivery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.delivery.dao.DeliveryQueueDAO.Claim;
import org.openelisglobal.delivery.dao.DeliveryQueueSearch;
import org.openelisglobal.delivery.dto.DeliveryChannelDTO;
import org.openelisglobal.delivery.dto.SentMessageDTO;
import org.openelisglobal.delivery.dto.SentMessagesPageDTO;
import org.openelisglobal.delivery.service.DeliveryChannelService;
import org.openelisglobal.delivery.service.DeliveryOutboxService;
import org.openelisglobal.delivery.service.DeliveryRequest;
import org.openelisglobal.delivery.service.DeliverySettings;
import org.openelisglobal.delivery.service.DeliverySettingsService;
import org.openelisglobal.delivery.service.sender.DeliveryException;
import org.openelisglobal.delivery.service.sender.DeliveryMessage;
import org.openelisglobal.delivery.service.sender.DeliverySender;
import org.openelisglobal.delivery.valueholder.DeliveryChannel;
import org.openelisglobal.delivery.valueholder.DeliveryChannel.Channel;
import org.openelisglobal.notification.valueholder.NotificationChannel;
import org.openelisglobal.notification.valueholder.NotificationLog;
import org.openelisglobal.notification.valueholder.NotificationLog.OverallStatus;
import org.openelisglobal.notification.valueholder.NotificationLog.Product;
import org.openelisglobal.notification.valueholder.NotificationLogChannel;
import org.openelisglobal.notification.valueholder.NotificationLogChannel.ChannelStatus;
import org.openelisglobal.notification.valueholder.NotificationRecipientType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * The persistent outbox: rows are written before any attempt, the worker's
 * claim and attempt cycle sends, retries after the configured delays and keeps
 * every attempt, Skipped and "not set up" are recorded without a send, and
 * Resend queues the same text again.
 */
public class DeliveryOutboxServiceTest extends BaseWebContextSensitiveTest {

    private static final String ADMIN = "1";

    @Autowired
    private DeliveryOutboxService outbox;
    @Autowired
    private DeliveryChannelService channelService;
    @Autowired
    private DeliverySettingsService settingsService;

    private Object originalSenders;
    private FakeEmailSender email;

    /**
     * An Email transport the test steers: succeeds, or fails with a chosen error.
     */
    static class FakeEmailSender implements DeliverySender {
        boolean setUp = true;
        DeliveryException failure;
        final List<DeliveryMessage> sent = new ArrayList<>();

        @Override
        public NotificationChannel channel() {
            return NotificationChannel.EMAIL;
        }

        @Override
        public boolean isSetUp(DeliveryChannel config) {
            return setUp;
        }

        @Override
        public String send(DeliveryChannel config, DeliveryMessage message) {
            if (failure != null) {
                throw failure;
            }
            sent.add(message);
            return "msg-" + sent.size();
        }
    }

    @Before
    public void replaceTheEmailTransport() {
        ensureSiteInformation("delivery.retryDelays", "1,5,15");
        ensureSiteInformation("delivery.defaultFormat", "MASKED");
        ensureSiteInformation("delivery.holdMinutes", "10");
        ensureSiteInformation("delivery.sendPartials", "true");
        ensureSiteInformation("delivery.labContact", null);
        ensureSiteInformation("delivery.location", "LOCAL");
        ensureSiteInformation("delivery.centralAddress", null);
        ensureSiteInformation("delivery.secureLinks", "false");
        ensureSiteInformation("delivery.linkDays", "14");
        ensureSiteInformation("delivery.countryCode", null);
        cleanRows();
        email = new FakeEmailSender();
        Object target = AopTestUtils.getTargetObject(outbox);
        originalSenders = ReflectionTestUtils.getField(target, "senderBeans");
        List<DeliverySender> senders = new ArrayList<>();
        senders.add(email);
        ReflectionTestUtils.setField(target, "senderBeans", senders);
        Object channelTarget = AopTestUtils.getTargetObject(channelService);
        ReflectionTestUtils.setField(channelTarget, "senderBeans", senders);
        DeliveryChannelDTO configured = new DeliveryChannelDTO();
        configured.setActive(true);
        configured.setHost("smtp.example.org");
        configured.setPort(587);
        configured.setSecurity("STARTTLS");
        configured.setSenderAddress("lab@example.org");
        channelService.save(Channel.EMAIL, configured, ADMIN);
        DeliverySettings settings = settingsService.get();
        settings.setRetryDelays(List.of(1, 5, 15));
        settingsService.save(settings, ADMIN);
    }

    @After
    public void restoreTheEmailTransport() {
        Object outboxTarget = AopTestUtils.getTargetObject(outbox);
        ReflectionTestUtils.setField(outboxTarget, "senderBeans", originalSenders);
        Object channelTarget = AopTestUtils.getTargetObject(channelService);
        ReflectionTestUtils.setField(channelTarget, "senderBeans", originalSenders);
        cleanRows();
    }

    private void cleanRows() {
        jdbcTemplate.update("DELETE FROM clinlims.notification_log_attempt");
        jdbcTemplate.update("DELETE FROM clinlims.notification_log_channel");
        jdbcTemplate.update("DELETE FROM clinlims.notification_log");
    }

    private DeliveryRequest emailTo(String address, String accession) {
        return new DeliveryRequest().setProduct(Product.TRIGGER).setEventCode("REFERRAL_OUT")
                .setRecipientType(NotificationRecipientType.ORDERING_PROVIDER).setRecipientDisplayName("Dr Kila")
                .setRecipientEmail(address).setSubject("Sample referred out: " + accession)
                .setMessage("Sample " + accession + " has been referred.").setReferenceAccession(accession)
                .addChannel(NotificationChannel.EMAIL);
    }

    private NotificationLogChannel channel(NotificationLog log, NotificationChannel channel) {
        return outboxRow(log.getId()).getChannels().stream().filter(c -> c.getChannel() == channel).findFirst()
                .orElseThrow();
    }

    private NotificationLog outboxRow(Long id) {
        return jdbcTemplate.query("SELECT id FROM clinlims.notification_log WHERE id = ?", (rs, i) -> rs.getLong(1), id)
                .isEmpty() ? null : reload(id);
    }

    private NotificationLog reload(Long id) {
        org.openelisglobal.notification.dao.NotificationLogDAO dao = webApplicationContext
                .getBean(org.openelisglobal.notification.dao.NotificationLogDAO.class);
        Optional<NotificationLog> found = dao.get(id);
        return found.orElse(null);
    }

    @Test
    public void aMessageIsWrittenAsQueuedBeforeAnythingIsAttempted() {
        NotificationLog log = outbox.enqueue(emailTo("kila@example.org", "ACC-001"));

        assertNotNull(log.getId());
        assertEquals(OverallStatus.QUEUED, log.getOverallStatus());
        NotificationLogChannel row = channel(log, NotificationChannel.EMAIL);
        assertEquals(ChannelStatus.QUEUED, row.getStatus());
        assertEquals(0, row.getAttempts());
        assertNotNull("due at once", row.getNextAttemptAt());
        assertTrue("nothing sent yet", email.sent.isEmpty());
    }

    @Test
    public void aRecipientWithNoAddressForTheChannelIsSkippedWithTheReason() {
        DeliveryRequest request = emailTo(null, "ACC-002").addChannel(NotificationChannel.SMS);
        NotificationLog log = outbox.enqueue(request);

        assertEquals(OverallStatus.SKIPPED, log.getOverallStatus());
        assertEquals(ChannelStatus.SKIPPED, channel(log, NotificationChannel.EMAIL).getStatus());
        assertEquals("No Email address", channel(log, NotificationChannel.EMAIL).getErrorMessage());
        assertEquals("No SMS address", channel(log, NotificationChannel.SMS).getErrorMessage());
        assertTrue(outbox.claim("worker-a", 10).isEmpty());
    }

    @Test
    public void aChannelThatIsNotSetUpFailsTheMessageAtOnce() {
        email.setUp = false;
        NotificationLog log = outbox.enqueue(emailTo("kila@example.org", "ACC-003"));

        assertEquals(OverallStatus.FAILED, log.getOverallStatus());
        NotificationLogChannel row = channel(log, NotificationChannel.EMAIL);
        assertEquals(ChannelStatus.FAILED, row.getStatus());
        assertEquals("Email is not set up", row.getErrorMessage());
        assertTrue(outbox.claim("worker-a", 10).isEmpty());
    }

    @Test
    public void theWorkerSendsAClaimedRowAndRecordsTheAttempt() {
        NotificationLog log = outbox.enqueue(emailTo("kila@example.org", "ACC-004"));

        List<Claim> claims = outbox.claim("worker-a", 10);
        assertEquals(1, claims.size());
        assertEquals(log.getId(), claims.get(0).logId());
        assertTrue("a claimed row is leased to one worker", outbox.claim("worker-b", 10).isEmpty());

        outbox.attempt(claims.get(0), "worker-a");

        assertEquals(1, email.sent.size());
        assertEquals("kila@example.org", email.sent.get(0).getAddress());
        NotificationLogChannel row = channel(log, NotificationChannel.EMAIL);
        assertEquals(ChannelStatus.SUCCESS, row.getStatus());
        assertEquals(1, row.getAttempts());
        assertEquals("msg-1", row.getGatewayMessageId());
        assertNull(row.getLockedBy());
        SentMessageDTO detail = outbox.detail(log.getId()).orElseThrow();
        assertEquals("SENT", detail.getStatus());
        assertEquals(1, detail.getAttempts().size());
        assertTrue(detail.getAttempts().get(0).isSucceeded());
        DeliveryChannelDTO channelRow = channelService.get(Channel.EMAIL).orElseThrow();
        assertEquals("READY", channelRow.getStatus());
        assertNotNull(channelRow.getLastSuccessAt());
    }

    @Test
    public void aFailedSendIsRetriedAfterTheConfiguredDelaysAndEveryAttemptIsKept() {
        email.failure = new DeliveryException(DeliveryException.Code.UNREACHABLE,
                "Could not reach smtp.example.org on port 587");
        NotificationLog log = outbox.enqueue(emailTo("kila@example.org", "ACC-005"));
        int[] expectedDelayMinutes = { 1, 5, 15 };

        for (int attempt = 1; attempt <= 3; attempt++) {
            List<Claim> claims = outbox.claim("worker-a", 10);
            assertEquals("attempt " + attempt + " is due", 1, claims.size());
            Instant before = Instant.now();
            outbox.attempt(claims.get(0), "worker-a");
            NotificationLogChannel row = channel(log, NotificationChannel.EMAIL);
            assertEquals(ChannelStatus.QUEUED, row.getStatus());
            assertEquals(attempt, row.getAttempts());
            assertEquals("Could not reach smtp.example.org on port 587", row.getErrorMessage());
            long waitSeconds = (row.getNextAttemptAt().toInstant().getEpochSecond() - before.getEpochSecond());
            assertTrue(
                    "next attempt about " + expectedDelayMinutes[attempt - 1] + " min away, was " + waitSeconds + " s",
                    Math.abs(waitSeconds - 60L * expectedDelayMinutes[attempt - 1]) <= 5);
            assertTrue("not due before its time", outbox.claim("worker-a", 10).isEmpty());
            jdbcTemplate.update(
                    "UPDATE clinlims.notification_log_channel SET next_attempt_at = ?"
                            + " WHERE notification_log_id = ?",
                    Timestamp.from(Instant.now().minusSeconds(1)), log.getId());
        }

        List<Claim> last = outbox.claim("worker-a", 10);
        assertEquals(1, last.size());
        outbox.attempt(last.get(0), "worker-a");
        NotificationLogChannel row = channel(log, NotificationChannel.EMAIL);
        assertEquals(ChannelStatus.FAILED, row.getStatus());
        assertEquals(4, row.getAttempts());
        assertNull(row.getNextAttemptAt());
        assertEquals(OverallStatus.FAILED, reload(log.getId()).getOverallStatus());
        SentMessageDTO detail = outbox.detail(log.getId()).orElseThrow();
        assertEquals(4, detail.getAttempts().size());
        assertFalse(detail.getAttempts().get(3).isSucceeded());
        assertEquals("FAILING", channelService.get(Channel.EMAIL).orElseThrow().getStatus());
        assertTrue(email.sent.isEmpty());
    }

    @Test
    public void resendQueuesTheSameTextAgainLinkedToTheSourceAndRefusesAQueuedOne() {
        NotificationLog source = outbox.enqueue(emailTo("kila@example.org", "ACC-006"));
        try {
            outbox.resend(source.getId(), ADMIN);
            fail("a queued message cannot be resent");
        } catch (IllegalArgumentException expected) {
            // still queued
        }
        outbox.attempt(outbox.claim("worker-a", 10).get(0), "worker-a");

        NotificationLog again = outbox.resend(source.getId(), ADMIN);

        assertEquals(source.getId(), again.getResentFromId());
        assertEquals(OverallStatus.QUEUED, again.getOverallStatus());
        assertEquals(source.getRenderedSubject(), again.getRenderedSubject());
        assertEquals(source.getRenderedMessage(), again.getRenderedMessage());
        assertEquals("kila@example.org", again.getRecipientEmail());
        SentMessageDTO detail = outbox.detail(again.getId()).orElseThrow();
        assertFalse("a resend names who asked for it", detail.isAutomatic());
    }

    @Test
    public void theListFiltersByProductStatusChannelLabNumberAndRecipientAndPages() {
        for (int i = 0; i < 3; i++) {
            outbox.enqueue(emailTo("kila@example.org", "ACC-10" + i));
        }
        outbox.enqueue(new DeliveryRequest().setProduct(Product.ALERT).setEventCode("ALERT").setRuleName("Cholera")
                .setRecipientType(NotificationRecipientType.TYPED_ADDRESS).setRecipientDisplayName("+675 7000 0000")
                .setRecipientPhone("+67570000000").setSubject("Alert").setMessage("Cholera RDT positive")
                .setReferenceAccession("ACC-200").addChannel(NotificationChannel.SMS));
        outbox.attempt(outbox.claim("worker-a", 1).get(0), "worker-a");

        SentMessagesPageDTO all = outbox.list(new DeliveryQueueSearch(), 1, 2);
        assertEquals(4, all.getTotal());
        assertEquals(2, all.getRows().size());
        SentMessagesPageDTO page2 = outbox.list(new DeliveryQueueSearch(), 2, 2);
        assertEquals(2, page2.getRows().size());

        DeliveryQueueSearch alerts = new DeliveryQueueSearch();
        alerts.setProduct(Product.ALERT);
        assertEquals(1, outbox.list(alerts, 1, 10).getTotal());
        assertEquals("Cholera", outbox.list(alerts, 1, 10).getRows().get(0).getRuleName());

        DeliveryQueueSearch sent = new DeliveryQueueSearch();
        sent.setStatus(OverallStatus.SENT);
        assertEquals(1, outbox.list(sent, 1, 10).getTotal());

        DeliveryQueueSearch sms = new DeliveryQueueSearch();
        sms.setChannel(NotificationChannel.SMS);
        assertEquals(1, outbox.list(sms, 1, 10).getTotal());

        DeliveryQueueSearch labNo = new DeliveryQueueSearch();
        labNo.setLabNumber("ACC-101");
        assertEquals(1, outbox.list(labNo, 1, 10).getTotal());

        DeliveryQueueSearch recipient = new DeliveryQueueSearch();
        recipient.setRecipient("kila@");
        assertEquals(3, outbox.list(recipient, 1, 10).getTotal());
    }

    @Test
    public void aChannelTestIsRecordedUnderProductTestWithItsOutcome() {
        DeliveryChannelService.TestOutcome ok = channelService.sendTest(Channel.EMAIL, "me@example.org", ADMIN);
        assertTrue(ok.success());
        assertEquals("Test message sent to me@example.org.", ok.message());
        SentMessageDTO row = outbox.detail(ok.sentMessageId()).orElseThrow();
        assertEquals("TEST", row.getProduct());
        assertEquals("SENT", row.getStatus());

        email.failure = new DeliveryException(DeliveryException.Code.AUTHENTICATION,
                "Authentication failed: check the username and password");
        DeliveryChannelService.TestOutcome refused = channelService.sendTest(Channel.EMAIL, "me@example.org", ADMIN);
        assertFalse(refused.success());
        assertEquals("Authentication failed: check the username and password", refused.message());
        assertEquals("FAILED", outbox.detail(refused.sentMessageId()).orElseThrow().getStatus());
        assertEquals("FAILING", channelService.get(Channel.EMAIL).orElseThrow().getStatus());
    }

    @Test
    public void theChannelSecretNeverComesBackAndABlankOneKeepsTheStoredValue() {
        DeliveryChannelDTO wanted = channelService.get(Channel.EMAIL).orElseThrow();
        wanted.setSecret("s3cret");
        wanted.setUsername("lab");
        DeliveryChannelDTO saved = channelService.save(Channel.EMAIL, wanted, ADMIN);
        assertNull(saved.getSecret());
        assertTrue(saved.isSecretSet());

        saved.setSecret("");
        saved.setSenderName("CPHL");
        DeliveryChannelDTO kept = channelService.save(Channel.EMAIL, saved, ADMIN);
        assertTrue(kept.isSecretSet());
        assertEquals("CPHL", kept.getSenderName());
        String stored = jdbcTemplate
                .queryForObject("SELECT secret FROM clinlims.delivery_channel WHERE channel = 'EMAIL'", String.class);
        assertNotNull("the secret stays stored", stored);
    }

    /**
     * Fixtures replace site_information mid-suite, so the settings rows can be
     * missing or empty; the defaults answer instead of an error.
     */
    @Test
    public void settingsFallBackToTheDefaultsWhenTheRowsAreEmpty() {
        jdbcTemplate.update("UPDATE clinlims.site_information SET value = NULL WHERE name IN"
                + " ('delivery.retryDelays', 'delivery.defaultFormat', 'delivery.location')");
        try {
            DeliverySettings settings = settingsService.get();
            assertEquals("MASKED", settings.getDefaultFormat());
            assertEquals("LOCAL", settings.getLocation());
            assertEquals(DeliverySettings.DEFAULT_RETRY_DELAYS, settingsService.retryDelays());

            DeliverySettings blank = new DeliverySettings();
            blank.setDefaultFormat(null);
            try {
                settingsService.save(blank, ADMIN);
                throw new AssertionError("a missing format is refused, not a server error");
            } catch (IllegalArgumentException expected) {
                assertEquals("defaultFormat", expected.getMessage());
            }
        } finally {
            jdbcTemplate.update(
                    "UPDATE clinlims.site_information SET value = '1,5,15' WHERE name = 'delivery.retryDelays'");
            jdbcTemplate.update(
                    "UPDATE clinlims.site_information SET value = 'MASKED' WHERE name = 'delivery.defaultFormat'");
            jdbcTemplate
                    .update("UPDATE clinlims.site_information SET value = 'LOCAL' WHERE name = 'delivery.location'");
        }
    }

    /**
     * The batch lease lapsed while earlier sends ran and the other worker claimed
     * the row: the first worker finds its lease gone and does not send, so the
     * message goes out once.
     */
    @Test
    public void aRowWhoseLeaseWasTakenOverIsNotSentTwice() {
        NotificationLog queued = outbox.enqueue(emailTo("clinic@example.org", "ACC-LEASE"));
        List<Claim> claims = outbox.claim("worker-a", 10);
        assertEquals(1, claims.size());
        jdbcTemplate.update("UPDATE clinlims.notification_log_channel SET locked_by = 'worker-b',"
                + " locked_until = now() + interval '2 minutes' WHERE notification_log_id = ?", queued.getId());

        outbox.attempt(claims.get(0), "worker-a");

        assertTrue("the worker that lost the lease sends nothing", email.sent.isEmpty());
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT status, attempts, locked_by FROM clinlims.notification_log_channel WHERE notification_log_id = ?",
                queued.getId());
        assertEquals("QUEUED", row.get("status"));
        assertEquals(0, ((Number) row.get("attempts")).intValue());
        assertEquals("worker-b", row.get("locked_by"));
    }

    /**
     * Resend retries the channels that did not get through; a message every channel
     * delivered is sent again on all of them.
     */
    @Test
    public void resendQueuesOnlyTheChannelsThatDidNotGetThrough() {
        DeliveryRequest both = emailTo("clinic@example.org", "ACC-RESEND").setRecipientPhone("+67570000001")
                .addChannel(NotificationChannel.WHATSAPP);
        NotificationLog source = outbox.enqueue(both);
        jdbcTemplate.update("UPDATE clinlims.notification_log_channel SET status = 'SUCCESS', locked_by = NULL"
                + " WHERE notification_log_id = ? AND channel = 'EMAIL'", source.getId());
        jdbcTemplate.update("UPDATE clinlims.notification_log SET overall_status = 'FAILED' WHERE id = ?",
                source.getId());

        NotificationLog again = outbox.resend(source.getId(), ADMIN);

        assertEquals(1, again.getChannels().size());
        assertEquals(NotificationChannel.WHATSAPP, again.getChannels().iterator().next().getChannel());

        jdbcTemplate.update("UPDATE clinlims.notification_log_channel SET status = 'SUCCESS', locked_by = NULL"
                + " WHERE notification_log_id = ?", source.getId());
        jdbcTemplate.update("UPDATE clinlims.notification_log SET overall_status = 'SENT' WHERE id = ?",
                source.getId());
        NotificationLog everything = outbox.resend(source.getId(), ADMIN);
        assertEquals(2, everything.getChannels().size());
    }
}
