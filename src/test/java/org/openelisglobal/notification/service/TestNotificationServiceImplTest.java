package org.openelisglobal.notification.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.Optional;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.delivery.dto.DeliveryChannelDTO;
import org.openelisglobal.delivery.service.DeliveryChannelService;
import org.openelisglobal.delivery.valueholder.DeliveryChannel.Channel;
import org.openelisglobal.notification.valueholder.AnalysisNotificationConfig;
import org.openelisglobal.notification.valueholder.NotificationConfigOption;
import org.openelisglobal.notification.valueholder.NotificationConfigOption.NotificationMethod;
import org.openelisglobal.notification.valueholder.NotificationConfigOption.NotificationNature;
import org.openelisglobal.notification.valueholder.NotificationConfigOption.NotificationPersonType;
import org.openelisglobal.notification.valueholder.NotificationPayloadTemplate;
import org.openelisglobal.notification.valueholder.NotificationPayloadTemplate.NotificationPayloadType;
import org.openelisglobal.result.service.ResultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The per-test "results ready" notice queues through the outbox when the
 * laboratory has set the Email channel up, and queues nothing before that.
 * AppTestConfig keeps the implementation out of the context, so one is wired by
 * hand against the real outbox and the mocked configuration services.
 */
public class TestNotificationServiceImplTest extends BaseWebContextSensitiveTest {

    private static final String ADMIN = "9301";
    private static final String RESULT = "9301";
    private static final String ANALYSIS = "9301";
    private static final String PATIENT_EMAIL = "mary.kila@example.org";

    @Autowired
    private ResultService resultService;
    @Autowired
    private AnalysisNotificationConfigService analysisNotificationConfigService;
    @Autowired
    private DeliveryChannelService channelService;
    @Autowired
    private NotificationPayloadTemplateService payloadTemplateService;
    @Autowired
    private PlatformTransactionManager transactionManager;

    private TestNotificationServiceImpl service;

    @Before
    public void loadOrder() throws Exception {
        executeDataSetWithStateManagement("testdata/patient-report-v1.xml");
        cleanRows();
        jdbcTemplate.update("UPDATE clinlims.person SET email = ? WHERE id = 9301", PATIENT_EMAIL);
        ensureTestResultTemplate();
        service = new TestNotificationServiceImpl();
        webApplicationContext.getAutowireCapableBeanFactory().autowireBean(service);

        AnalysisNotificationConfig config = new AnalysisNotificationConfig();
        config.setAnalysis(resultService.get(RESULT).getAnalysis());
        NotificationConfigOption option = config.getOptionFor(NotificationNature.RESULT_VALIDATION,
                NotificationMethod.EMAIL, NotificationPersonType.PATIENT);
        option.setActive(true);
        Mockito.reset(analysisNotificationConfigService);
        Mockito.when(analysisNotificationConfigService.getAnalysisNotificationConfigForAnalysisId(ANALYSIS))
                .thenReturn(Optional.of(config));
    }

    @After
    public void cleanUp() {
        Mockito.reset(analysisNotificationConfigService);
        cleanRows();
    }

    private void cleanRows() {
        jdbcTemplate.update("DELETE FROM clinlims.notification_log_attempt");
        jdbcTemplate.update("DELETE FROM clinlims.notification_log_channel");
        jdbcTemplate.update("DELETE FROM clinlims.notification_log");
        jdbcTemplate.update("DELETE FROM clinlims.client_results_view WHERE result_id = ?", Integer.valueOf(RESULT));
        jdbcTemplate.update("UPDATE clinlims.delivery_channel SET host = NULL, port = NULL, username = NULL,"
                + " secret = NULL, sender_address = NULL WHERE channel = 'EMAIL'");
    }

    private void ensureTestResultTemplate() {
        if (payloadTemplateService
                .getSystemDefaultPayloadTemplateForType(NotificationPayloadType.TEST_RESULT) == null) {
            DefaultPayloadTemplates.Default factory = DefaultPayloadTemplates
                    .forType(NotificationPayloadType.TEST_RESULT);
            NotificationPayloadTemplate template = new NotificationPayloadTemplate();
            template.setType(NotificationPayloadType.TEST_RESULT);
            template.setSubjectTemplate(factory.getSubject());
            template.setMessageTemplate(factory.getMessage());
            template.setSysUserId(ADMIN);
            payloadTemplateService.save(template);
        }
    }

    /**
     * Production calls the service inside its own transaction; the hand-wired
     * instance gets a frame here so the result's lazy associations can load.
     */
    private void notifyReleased() {
        TransactionTemplate frame = new TransactionTemplate(transactionManager);
        frame.execute(status -> {
            service.createAndSendNotificationsToConfiguredSources(NotificationNature.RESULT_VALIDATION,
                    resultService.get(RESULT));
            return null;
        });
    }

    private int noticeRows() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM clinlims.notification_log WHERE product = 'NOTICE'",
                Integer.class);
    }

    @Test
    public void aReleasedResultQueuesAReadyNoticeToThePatientOnceEmailIsSetUp() {
        DeliveryChannelDTO email = new DeliveryChannelDTO();
        email.setActive(true);
        email.setHost("smtp.example.org");
        email.setPort(587);
        email.setSecurity("STARTTLS");
        email.setSenderAddress("lab@example.org");
        channelService.save(Channel.EMAIL, email, ADMIN);
        String template = jdbcTemplate.queryForObject(
                "SELECT message_template FROM clinlims.notification_payload_template WHERE type = 'TEST_RESULT'",
                String.class);
        jdbcTemplate.update(
                "UPDATE clinlims.notification_payload_template SET message_template = ?"
                        + " WHERE type = 'TEST_RESULT'",
                "Dear [patientFirstName] [patientLastNameInitial], [testName] is ready.");
        try {
            notifyReleased();
        } finally {
            jdbcTemplate.update("UPDATE clinlims.notification_payload_template SET message_template = ?"
                    + " WHERE type = 'TEST_RESULT'", template);
        }

        assertEquals(1, noticeRows());
        Map<String, Object> row = jdbcTemplate.queryForMap("SELECT recipient_type, recipient_email,"
                + " reference_accession, overall_status, rendered_message FROM clinlims.notification_log"
                + " WHERE product = 'NOTICE'");
        assertEquals("PATIENT", row.get("recipient_type"));
        assertEquals(PATIENT_EMAIL, row.get("recipient_email"));
        assertEquals("V1RPT0000009301", row.get("reference_accession"));
        assertEquals("QUEUED", row.get("overall_status"));
        String message = String.valueOf(row.get("rendered_message"));
        assertFalse("a template that names the patient renders without the name: " + message,
                message.contains("Mary") || message.contains("Kila") || message.contains("K,"));
        assertTrue(message, message.contains("is ready."));
    }

    @Test
    public void nothingIsQueuedWhileTheEmailChannelIsNotSetUp() {
        notifyReleased();

        assertEquals(0, noticeRows());
    }
}
