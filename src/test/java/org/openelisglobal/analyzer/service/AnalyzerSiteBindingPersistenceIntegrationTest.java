package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzer.dao.AnalyzerActivationRecordDAO;
import org.openelisglobal.analyzer.dao.AnalyzerDAO;
import org.openelisglobal.analyzer.dao.AnalyzerProfileBindingDAO;
import org.openelisglobal.analyzer.dao.AnalyzerSiteBindingConfirmationDAO;
import org.openelisglobal.analyzer.dao.AnalyzerSiteBindingDAO;
import org.openelisglobal.analyzer.dao.AnalyzerSiteBindingResultDAO;
import org.openelisglobal.analyzer.dao.AnalyzerSiteBindingRevisionDAO;
import org.openelisglobal.analyzer.dao.AnalyzerSiteBindingTestDAO;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingMappingState;
import org.openelisglobal.audittrail.daoimpl.AuditTrailServiceImpl;
import org.openelisglobal.history.service.HistoryService;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

public class AnalyzerSiteBindingPersistenceIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String PROFILE_FINGERPRINT = "sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static final String RECOGNITION_FINGERPRINT = "sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";

    @Autowired
    private AnalyzerProfileBindingDAO profileBindingDAO;

    @Autowired
    private AnalyzerDAO analyzerDAO;

    @Autowired
    private AnalyzerActivationRecordDAO activationRecordDAO;

    @Autowired
    private AnalyzerSiteBindingDAO siteBindingDAO;

    @Autowired
    private AnalyzerSiteBindingRevisionDAO revisionDAO;

    @Autowired
    private AnalyzerSiteBindingTestDAO siteBindingTestDAO;

    @Autowired
    private AnalyzerSiteBindingResultDAO siteBindingResultDAO;

    @Autowired
    private AnalyzerSiteBindingConfirmationDAO confirmationDAO;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private ReferenceTablesService referenceTablesService;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    public void savedCatalogBindingsAndConfirmationReloadFromPostgres() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.executeWithoutResult(status -> {
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            String testId = jdbc.queryForObject("SELECT nextval('test_seq')", Long.class).toString();
            String resultOptionId = jdbc.queryForObject("SELECT nextval('test_result_seq')", Long.class).toString();
            jdbc.update(
                    "INSERT INTO test (id, name, description, guid, is_active, is_reportable, orderable, "
                            + "lastupdated) VALUES (?, ?, ?, ?, 'Y', 'Y', TRUE, CURRENT_TIMESTAMP)",
                    Long.valueOf(testId), "Analyzer binding persistence test", "Analyzer binding persistence test",
                    UUID.randomUUID());
            jdbc.update("INSERT INTO test_result (id, test_id, tst_rslt_type, value, sort_order, is_active, "
                    + "is_normal, lastupdated) VALUES (?, ?, 'D', 'POSITIVE', 1, TRUE, TRUE, CURRENT_TIMESTAMP)",
                    Long.valueOf(resultOptionId), Long.valueOf(testId));

            org.openelisglobal.test.valueholder.Test test = new org.openelisglobal.test.valueholder.Test();
            test.setId(testId);
            test.setIsActive("Y");
            TestResult resultOption = new TestResult();
            resultOption.setId(resultOptionId);
            resultOption.setIsActive(true);
            resultOption.setTestResultType("D");
            resultOption.setTest(test);

            TestService testService = mock(TestService.class);
            TestResultService testResultService = mock(TestResultService.class);
            SystemUserService systemUserService = mock(SystemUserService.class);
            AnalyzerMappingCatalogService mappingCatalogService = mock(AnalyzerMappingCatalogService.class);
            SystemUser actor = new SystemUser();
            actor.setId(TEST_SYS_USER_ID);
            actor.setFirstName("Integration");
            actor.setLastName("Reviewer");
            when(testService.get(testId)).thenReturn(test);
            when(testResultService.get(resultOptionId)).thenReturn(resultOption);
            when(systemUserService.getUserById(TEST_SYS_USER_ID)).thenReturn(actor);
            when(mappingCatalogService.searchActiveTests(null))
                    .thenReturn(List.of(new AnalyzerMappingCatalogService.TestOption(testId,
                            "Analyzer binding persistence test", "TEST", List.of())));
            when(mappingCatalogService.getActiveResultOptions(testId)).thenReturn(
                    List.of(new AnalyzerMappingCatalogService.ResultOption(resultOptionId, "POSITIVE", "Positive")));

            AuditTrailServiceImpl auditTrailService = new AuditTrailServiceImpl();
            ReflectionTestUtils.setField(auditTrailService, "referenceTablesService", referenceTablesService);
            ReflectionTestUtils.setField(auditTrailService, "historyService", historyService);
            AnalyzerSiteBindingService siteBindingService = new AnalyzerSiteBindingServiceImpl(siteBindingDAO,
                    revisionDAO, siteBindingTestDAO, siteBindingResultDAO, auditTrailService, testService,
                    testResultService);
            AnalyzerSiteBindingConfirmationService confirmationService = new AnalyzerSiteBindingConfirmationServiceImpl(
                    confirmationDAO, auditTrailService, systemUserService, mappingCatalogService);

            String profileId = "site.persistence." + UUID.randomUUID();
            AnalyzerProfileBinding profileBinding = new AnalyzerProfileBinding();
            profileBinding.setProfileId(profileId);
            profileBinding.setProfileRevision(1);
            profileBinding.setProfileFingerprint(PROFILE_FINGERPRINT);
            profileBinding.setSysUserId(TEST_SYS_USER_ID);
            profileBindingDAO.insert(profileBinding);

            ObjectNode profile = profile(profileId);
            AnalyzerSiteBindingSnapshot initial = siteBindingService.resolveInitialRevision(profileBinding, profile,
                    TEST_SYS_USER_ID);
            AnalyzerSiteBindingDraft decisions = new AnalyzerSiteBindingDraft(
                    List.of(new AnalyzerSiteBindingTestDraft("RAW-A", AnalyzerSiteBindingMappingState.BOUND, testId)),
                    List.of(new AnalyzerSiteBindingResultDraft("RAW-A", "POS", AnalyzerSiteBindingMappingState.BOUND,
                            resultOptionId)));
            AnalyzerSiteBindingSnapshot saved = siteBindingService.appendRevision(initial.binding(), decisions,
                    TEST_SYS_USER_ID);
            AnalyzerSiteBindingConfirmationRequest request = new AnalyzerSiteBindingConfirmationRequest(
                    saved.revision().getBindingFingerprint(), RECOGNITION_FINGERPRINT,
                    List.of(new AnalyzerSiteBindingSourceRow("RAW-A", null),
                            new AnalyzerSiteBindingSourceRow("RAW-A", "POS")),
                    List.of());
            confirmationService.confirm(saved, RECOGNITION_FINGERPRINT, request, TEST_SYS_USER_ID);
            var storedVerification = confirmationDAO.findByRevisionId(saved.revision().getId()).orElseThrow();

            Analyzer analyzer = new Analyzer();
            analyzer.setName("Persistence analyzer");
            analyzer.setStatus(Analyzer.AnalyzerStatus.VALIDATION);
            analyzer.setSiteBindingRevision(saved.revision());
            analyzer.setTestUnitIds(List.of("1"));
            analyzer.setBridgeConnectionId("bridge-" + UUID.randomUUID());
            analyzer.setSysUserId(TEST_SYS_USER_ID);
            analyzerDAO.insert(analyzer);

            AnalyzerActivationRecordService activationRecordService = new AnalyzerActivationRecordServiceImpl(
                    activationRecordDAO, auditTrailService);
            ObjectNode firstAcknowledgement = runtimeAcknowledgement(analyzer, profileBinding, "activate-1", 1);
            var firstRecord = activationRecordService.retain(analyzer, saved.revision(), storedVerification,
                    firstAcknowledgement, "ACTIVE", TEST_SYS_USER_ID);
            ObjectNode secondAcknowledgement = runtimeAcknowledgement(analyzer, profileBinding, "activate-2", 2);
            var latestRecord = activationRecordService.retain(analyzer, saved.revision(), storedVerification,
                    secondAcknowledgement, "ACTIVE", TEST_SYS_USER_ID);
            analyzer.setLatestActivationRecord(latestRecord);
            analyzer.setStatus(Analyzer.AnalyzerStatus.ACTIVE);
            analyzerDAO.update(analyzer);

            entityManager.flush();
            entityManager.clear();

            AnalyzerSiteBindingSnapshot reloaded = siteBindingService
                    .findCurrentByProfileBindingId(profileBinding.getId()).orElseThrow();
            assertEquals(2, reloaded.revision().getRevisionNumber());
            assertEquals(saved.revision().getBindingFingerprint(), reloaded.revision().getBindingFingerprint());
            assertEquals(AnalyzerSiteBindingMappingState.BOUND, reloaded.tests().get(0).getMappingState());
            assertEquals(testId, reloaded.tests().get(0).getTestId());
            assertEquals(AnalyzerSiteBindingMappingState.BOUND, reloaded.results().get(0).getMappingState());
            assertEquals(resultOptionId, reloaded.results().get(0).getTestResultId());

            AnalyzerSiteBindingConfirmationView confirmation = confirmationService.getStatus(reloaded,
                    RECOGNITION_FINGERPRINT);
            var storedConfirmation = confirmationDAO.findByRevisionId(reloaded.revision().getId()).orElseThrow();
            assertEquals(AnalyzerSiteBindingConfirmationView.State.CURRENT, confirmation.state());
            assertEquals(PROFILE_FINGERPRINT, storedConfirmation.getProfileRevisionFingerprint());
            assertNotNull(storedConfirmation.getAuditEventId());
            assertEquals(TEST_SYS_USER_ID, confirmation.confirmedBy());
            assertEquals("Integration Reviewer", confirmation.confirmedByDisplayName());
            assertNotNull(confirmation.confirmedAt());
            assertEquals(request.confirmedRows(), confirmation.confirmedRows());
            assertTrue(confirmation.excludedRows().isEmpty());

            var retainedRecords = activationRecordDAO.findByAnalyzerId(analyzer.getId());
            assertEquals(2, retainedRecords.size());
            assertEquals(firstRecord.getId(), retainedRecords.get(0).getId());
            assertEquals(firstAcknowledgement, parseJson(retainedRecords.get(0).getRuntimeAcknowledgementJson()));
            Analyzer reloadedAnalyzer = analyzerDAO.get(analyzer.getId()).orElseThrow();
            assertEquals(latestRecord.getId(), reloadedAnalyzer.getLatestActivationRecord().getId());

            status.setRollbackOnly();
        });
    }

    private static ObjectNode profile(String profileId) {
        try {
            ObjectNode profile = (ObjectNode) new ObjectMapper().readTree("""
                    {
                      "profileMeta":{"id":"placeholder","displayName":"Persistence Test Analyzer"},
                      "protocol":{"name":"ASTM","version":"LIS2-A2"},
                      "communication":{"mode":"ANALYZER_INITIATED","supports_lis_initiated":false},
                      "configDefaults":{"connectionRole":"SERVER","defaultTransport":"TCP/IP"},
                      "catalog":{
                        "revision":1,
                        "revisionFingerprint":"sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                        "source":"SITE",
                        "status":"ACTIVE"
                      },
                      "default_test_mappings":[
                        {
                          "test_code":"RAW-A",
                          "loinc":"94500-6",
                          "result_type":"qualitative",
                          "values":["POS"]
                        }
                      ]
                    }
                    """);
            ((ObjectNode) profile.path("profileMeta")).put("id", profileId);
            return profile;
        } catch (Exception exception) {
            throw new AssertionError("Cannot build analyzer profile fixture", exception);
        }
    }

    private static ObjectNode runtimeAcknowledgement(Analyzer analyzer, AnalyzerProfileBinding profile,
            String commandId, int runtimeRevision) {
        ObjectNode acknowledgement = new ObjectMapper().createObjectNode();
        acknowledgement.put("schemaVersion", "1.0");
        acknowledgement.put("commandId", commandId);
        acknowledgement.put("action", "ACTIVATE");
        acknowledgement.put("outcome", "APPLIED");
        acknowledgement.put("connectionId", analyzer.getBridgeConnectionId());
        ObjectNode profileRef = acknowledgement.putObject("profileRef");
        profileRef.put("profileId", profile.getProfileId());
        profileRef.put("revision", profile.getProfileRevision());
        profileRef.put("fingerprint", profile.getProfileFingerprint());
        acknowledgement.put("configRevision", 1);
        acknowledgement.put("configFingerprint", "sha256:" + "c".repeat(64));
        acknowledgement.put("runtimeRevision", runtimeRevision);
        acknowledgement.put("runtimeFingerprint", "sha256:" + "d".repeat(64));
        acknowledgement.put("desiredRuntimeState", "ACTIVE");
        acknowledgement.put("actualRuntimeState", "ACTIVE");
        acknowledgement.putArray("blockers");
        acknowledgement.put("acknowledgedAt", "2026-08-24T19:05:05Z");
        return acknowledgement;
    }

    private static ObjectNode parseJson(String value) {
        try {
            return (ObjectNode) new ObjectMapper().readTree(value);
        } catch (Exception exception) {
            throw new AssertionError("Cannot parse retained activation document", exception);
        }
    }
}
