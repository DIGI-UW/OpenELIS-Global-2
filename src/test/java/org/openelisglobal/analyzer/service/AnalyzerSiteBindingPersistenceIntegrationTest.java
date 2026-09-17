package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzer.AnalyzerTestProfileCatalog;
import org.openelisglobal.analyzer.dao.AnalyzerActivationRecordDAO;
import org.openelisglobal.analyzer.dao.AnalyzerDAO;
import org.openelisglobal.analyzer.dao.AnalyzerProfileBindingDAO;
import org.openelisglobal.analyzer.dao.AnalyzerSiteBindingConfirmationDAO;
import org.openelisglobal.analyzer.dao.AnalyzerSiteBindingDAO;
import org.openelisglobal.analyzer.dao.AnalyzerSiteBindingRevisionDAO;
import org.openelisglobal.analyzer.form.AnalyzerInstanceRequest;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingMappingState;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingRevision;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.dictionarycategory.service.DictionaryCategoryService;
import org.openelisglobal.dictionarycategory.valueholder.DictionaryCategory;
import org.openelisglobal.history.service.HistoryService;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.qc.service.QCControlLotService;
import org.openelisglobal.qc.valueholder.QCControlLot;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Local services and transactions run for real; only Bridge HTTP responses are
 * substituted.
 */
@ContextConfiguration(classes = AnalyzerSiteBindingPersistenceIntegrationTest.BridgeTransportConfig.class)
@TestPropertySource(properties = "analyzer.bridge.url=http://bridge.test")
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
    private AnalyzerInstanceLocalStateService analyzerInstanceLocalStateService;

    @Autowired
    private QCControlLotService controlLotService;

    @Autowired
    private AnalyzerService analyzerService;

    @Autowired
    private AnalyzerSiteBindingDAO siteBindingDAO;

    @Autowired
    private AnalyzerSiteBindingRevisionDAO revisionDAO;

    @Autowired
    private AnalyzerSiteBindingConfirmationDAO confirmationDAO;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private AnalyzerSiteBindingService siteBindingService;
    @Autowired
    private AnalyzerSiteBindingConfirmationService confirmationService;
    @Autowired
    private AnalyzerActivationRecordService activationRecordService;
    @Autowired
    private TestService testService;
    @Autowired
    private TestResultService testResultService;
    @Autowired
    private SystemUserService systemUserService;
    @Autowired
    private DictionaryService dictionaryService;
    @Autowired
    private DictionaryCategoryService dictionaryCategoryService;

    @Autowired
    private AnalyzerActivationService activationService;
    @Autowired
    private AnalyzerConnectionProbeService probeService;
    @Autowired
    private BridgeHttpClient bridgeHttpClient;
    @Autowired
    private TestSectionService testSectionService;
    @Autowired
    private LocalizationService localizationService;

    // A lite configuration registered only by this test. It is deliberately not
    // a component-scanned @Configuration, so other suites retain their own HTTP
    // boundary.
    public static class BridgeTransportConfig {
        @Bean
        @Primary
        public BridgeHttpClient persistenceBridgeHttpClient() {
            return mock(BridgeHttpClient.class);
        }
    }

    private Map<String, Integer> initialCounts;

    @Before
    public void prepareExternalTransportAndSnapshotOwnedTables() {
        reset(bridgeHttpClient);
        initialCounts = fixtureTableCounts();
    }

    @After
    public void verifyFixtureCleanup() {
        reset(bridgeHttpClient);
        assertEquals("Persistence tests must leave no owned records or history behind", initialCounts,
                fixtureTableCounts());
    }

    private Map<String, Integer> fixtureTableCounts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String table : List.of("analyzer", "analyzer_profile_binding", "analyzer_site_binding",
                "analyzer_site_binding_revision", "analyzer_site_binding_test", "analyzer_site_binding_result",
                "analyzer_site_binding_confirmation", "analyzer_activation_record", "system_user", "test",
                "test_result", "dictionary", "dictionary_category", "test_section", "localization", "qc_control_lot",
                "qc_statistics", "westgard_rule_config", "history")) {
            counts.put(table, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM clinlims." + table, Integer.class));
        }
        return counts;
    }

    @Test
    public void savedCatalogBindingsAndConfirmationReloadFromPostgres() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.executeWithoutResult(status -> {
            org.openelisglobal.test.valueholder.Test test = createTest("Analyzer binding persistence test");
            String testId = test.getId();
            DictionaryCategory category = new DictionaryCategory();
            category.setCategoryName("Persistence result values");
            category.setDescription("Owned by the persistence test");
            category.setLocalAbbreviation("PERSIST");
            category.setSysUserId(TEST_SYS_USER_ID);
            dictionaryCategoryService.insert(category);
            Dictionary positive = new Dictionary();
            positive.setDictionaryCategory(category);
            positive.setDictEntry("Positive");
            positive.setIsActive("Y");
            positive.setSysUserId(TEST_SYS_USER_ID);
            dictionaryService.insert(positive);
            TestResult resultOption = new TestResult();
            resultOption.setTest(test);
            resultOption.setTestResultType("D");
            resultOption.setValue(positive.getId());
            resultOption.setIsActive(true);
            resultOption.setSysUserId(TEST_SYS_USER_ID);
            String resultOptionId = testResultService.insert(resultOption);
            SystemUser actor = new SystemUser();
            actor.setLoginName("reviewer-" + UUID.randomUUID().toString().substring(0, 12));
            actor.setFirstName("Integration");
            actor.setLastName("Reviewer");
            actor.setIsActive("Y");
            actor.setIsEmployee("Y");
            actor.setSysUserId(TEST_SYS_USER_ID);
            String reviewerId = systemUserService.insert(actor);

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
            confirmationService.confirm(saved, RECOGNITION_FINGERPRINT, request, reviewerId);
            var storedVerification = confirmationDAO.findByRevisionId(saved.revision().getId()).orElseThrow();

            Analyzer analyzer = new Analyzer();
            analyzer.setName("Persistence analyzer");
            analyzer.setStatus(Analyzer.AnalyzerStatus.VALIDATION);
            analyzer.setSiteBindingRevision(saved.revision());
            analyzer.setTestUnitIds(List.of("1"));
            analyzer.setBridgeConnectionId("bridge-" + UUID.randomUUID());
            analyzer.setSysUserId(TEST_SYS_USER_ID);
            analyzerDAO.insert(analyzer);

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
            assertEquals(reviewerId, confirmation.confirmedBy());
            assertEquals(reviewerId, historyService.get(storedConfirmation.getAuditEventId()).getSysUserId());
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

    @Test
    public void sharedMappingCanReturnToTheContentOfAnEarlierRevision() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.executeWithoutResult(status -> {
            String profileId = "site.revert." + UUID.randomUUID();
            AnalyzerProfileBinding profileBinding = new AnalyzerProfileBinding();
            profileBinding.setProfileId(profileId);
            profileBinding.setProfileRevision(1);
            profileBinding.setProfileFingerprint(PROFILE_FINGERPRINT);
            profileBinding.setSysUserId(TEST_SYS_USER_ID);
            profileBindingDAO.insert(profileBinding);

            AnalyzerSiteBindingSnapshot initial = siteBindingService.resolveInitialRevision(profileBinding,
                    profile(profileId), TEST_SYS_USER_ID);
            AnalyzerSiteBindingDraft excluded = new AnalyzerSiteBindingDraft(
                    List.of(new AnalyzerSiteBindingTestDraft("RAW-A", AnalyzerSiteBindingMappingState.EXCLUDED, null)),
                    List.of(new AnalyzerSiteBindingResultDraft("RAW-A", "POS", AnalyzerSiteBindingMappingState.EXCLUDED,
                            null)));
            AnalyzerSiteBindingSnapshot changed = siteBindingService.appendRevision(initial.binding(), excluded,
                    TEST_SYS_USER_ID);
            AnalyzerSiteBindingDraft restoredContent = new AnalyzerSiteBindingDraft(
                    List.of(new AnalyzerSiteBindingTestDraft("RAW-A", AnalyzerSiteBindingMappingState.UNRESOLVED,
                            null)),
                    List.of(new AnalyzerSiteBindingResultDraft("RAW-A", "POS",
                            AnalyzerSiteBindingMappingState.UNRESOLVED, null)));

            AnalyzerSiteBindingSnapshot restored = siteBindingService.appendRevision(initial.binding(), restoredContent,
                    TEST_SYS_USER_ID);
            entityManager.flush();
            entityManager.clear();

            AnalyzerSiteBindingSnapshot current = siteBindingService
                    .findCurrentByProfileBindingId(profileBinding.getId()).orElseThrow();
            assertEquals(2, changed.revision().getRevisionNumber());
            assertEquals(3, restored.revision().getRevisionNumber());
            assertFalse(initial.revision().getId().equals(restored.revision().getId()));
            assertEquals(initial.revision().getBindingFingerprint(), restored.revision().getBindingFingerprint());
            assertEquals(restored.revision().getId(), current.revision().getId());
            status.setRollbackOnly();
        });
    }

    @Test
    public void bridgeConnectionReferencePersistsAfterReloadingTheLocalAnalyzer() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        ConnectionFixture fixture = transaction.execute(status -> {
            String profileId = "site.connection." + UUID.randomUUID();
            AnalyzerProfileBinding profileBinding = new AnalyzerProfileBinding();
            profileBinding.setProfileId(profileId);
            profileBinding.setProfileRevision(1);
            profileBinding.setProfileFingerprint(PROFILE_FINGERPRINT);
            profileBinding.setSysUserId(TEST_SYS_USER_ID);
            profileBindingDAO.insert(profileBinding);

            AnalyzerSiteBinding binding = new AnalyzerSiteBinding();
            binding.setProfileBinding(profileBinding);
            binding.setCreatedBy(TEST_SYS_USER_ID);
            binding.setSysUserId(TEST_SYS_USER_ID);
            siteBindingDAO.insert(binding);

            AnalyzerSiteBindingRevision revision = new AnalyzerSiteBindingRevision();
            revision.setSiteBinding(binding);
            revision.setRevisionNumber(1);
            revision.setBindingFingerprint("sha256:" + "c".repeat(64));
            revision.setCreatedBy(TEST_SYS_USER_ID);
            revision.setSysUserId(TEST_SYS_USER_ID);
            revisionDAO.insert(revision);

            Analyzer analyzer = new Analyzer();
            analyzer.ensureFhirUuid();
            analyzer.setName("Connection reference persistence test");
            analyzer.setStatus(Analyzer.AnalyzerStatus.SETUP);
            analyzer.setActive(false);
            analyzer.setSiteBindingRevision(revision);
            analyzer.setTestUnitIds(List.of("1"));
            analyzer.setSysUserId(TEST_SYS_USER_ID);
            analyzerDAO.insert(analyzer);

            entityManager.flush();
            return new ConnectionFixture(analyzer.getId(), revision.getId(), binding.getId(), profileBinding.getId());
        });

        try {
            String connectionId = "bridge-" + UUID.randomUUID();
            AnalyzerInstanceState attached = analyzerInstanceLocalStateService
                    .attachBridgeConnection(fixture.analyzerId(), connectionId, TEST_SYS_USER_ID);

            assertEquals(connectionId, attached.bridgeConnectionId());
            String persistedConnectionId = transaction
                    .execute(status -> analyzerDAO.get(fixture.analyzerId()).orElseThrow().getBridgeConnectionId());
            assertEquals(connectionId, persistedConnectionId);
        } finally {
            transaction.executeWithoutResult(status -> {
                JdbcTemplate jdbc = new JdbcTemplate(dataSource);
                jdbc.update("DELETE FROM history WHERE reference_id = ? AND reference_table = "
                        + "(SELECT id FROM reference_tables WHERE name = 'analyzer')", fixture.analyzerId());
                jdbc.update("DELETE FROM analyzer WHERE id = ?", Long.valueOf(fixture.analyzerId()));
                jdbc.update("DELETE FROM analyzer_site_binding_revision WHERE id = ?",
                        Long.valueOf(fixture.revisionId()));
                jdbc.update("DELETE FROM analyzer_site_binding WHERE id = ?", Long.valueOf(fixture.bindingId()));
                jdbc.update("DELETE FROM analyzer_profile_binding WHERE id = ?",
                        Long.valueOf(fixture.profileBindingId()));
            });
        }
    }

    @Test
    public void reviewedSharedBindingRevisionPersistsAfterReloadingTheLocalAnalyzer() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        BindingSelectionFixture fixture = transaction.execute(status -> {
            String profileId = "site.selection." + UUID.randomUUID();
            AnalyzerProfileBinding profileBinding = new AnalyzerProfileBinding();
            profileBinding.setProfileId(profileId);
            profileBinding.setProfileRevision(1);
            profileBinding.setProfileFingerprint(PROFILE_FINGERPRINT);
            profileBinding.setSysUserId(TEST_SYS_USER_ID);
            profileBindingDAO.insert(profileBinding);

            AnalyzerSiteBinding binding = new AnalyzerSiteBinding();
            binding.setProfileBinding(profileBinding);
            binding.setCreatedBy(TEST_SYS_USER_ID);
            binding.setSysUserId(TEST_SYS_USER_ID);
            siteBindingDAO.insert(binding);

            AnalyzerSiteBindingRevision initial = bindingRevision(binding, 1, "sha256:" + "c".repeat(64));
            AnalyzerSiteBindingRevision reviewed = bindingRevision(binding, 2, "sha256:" + "d".repeat(64));

            Analyzer analyzer = new Analyzer();
            analyzer.ensureFhirUuid();
            analyzer.setName("Binding selection persistence test");
            analyzer.setStatus(Analyzer.AnalyzerStatus.SETUP);
            analyzer.setActive(false);
            analyzer.setSiteBindingRevision(initial);
            analyzer.setTestUnitIds(List.of("1"));
            analyzer.setSysUserId(TEST_SYS_USER_ID);
            analyzerDAO.insert(analyzer);
            entityManager.flush();
            return new BindingSelectionFixture(analyzer.getId(), initial.getId(), reviewed.getId(), binding.getId(),
                    profileBinding.getId(), reviewed.getBindingFingerprint());
        });

        try {
            analyzerInstanceLocalStateService.selectSiteBindingRevision(fixture.analyzerId(), fixture.bindingId(), 2,
                    fixture.reviewedFingerprint(), TEST_SYS_USER_ID);

            String persistedRevisionId = transaction.execute(
                    status -> analyzerDAO.get(fixture.analyzerId()).orElseThrow().getSiteBindingRevision().getId());
            assertEquals(fixture.reviewedRevisionId(), persistedRevisionId);
        } finally {
            transaction.executeWithoutResult(status -> {
                JdbcTemplate jdbc = new JdbcTemplate(dataSource);
                jdbc.update("DELETE FROM history WHERE reference_id = ? AND reference_table = "
                        + "(SELECT id FROM reference_tables WHERE name = 'analyzer')", fixture.analyzerId());
                jdbc.update("DELETE FROM analyzer WHERE id = ?", Long.valueOf(fixture.analyzerId()));
                jdbc.update("DELETE FROM analyzer_site_binding_revision WHERE id = ?",
                        Long.valueOf(fixture.reviewedRevisionId()));
                jdbc.update("DELETE FROM analyzer_site_binding_revision WHERE id = ?",
                        Long.valueOf(fixture.initialRevisionId()));
                jdbc.update("DELETE FROM analyzer_site_binding WHERE id = ?", Long.valueOf(fixture.bindingId()));
                jdbc.update("DELETE FROM analyzer_profile_binding WHERE id = ?",
                        Long.valueOf(fixture.profileBindingId()));
            });
        }
    }

    @Test
    public void connectionProbeReadsThePinnedProfileAfterTheAnalyzerTransactionCloses() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        ProbeFixture fixture = transaction.execute(status -> {
            String profileId = "site.probe." + UUID.randomUUID();
            AnalyzerProfileBinding profileBinding = new AnalyzerProfileBinding();
            profileBinding.setProfileId(profileId);
            profileBinding.setProfileRevision(1);
            profileBinding.setProfileFingerprint(PROFILE_FINGERPRINT);
            profileBinding.setSysUserId(TEST_SYS_USER_ID);
            profileBindingDAO.insert(profileBinding);

            AnalyzerSiteBinding binding = new AnalyzerSiteBinding();
            binding.setProfileBinding(profileBinding);
            binding.setCreatedBy(TEST_SYS_USER_ID);
            binding.setSysUserId(TEST_SYS_USER_ID);
            siteBindingDAO.insert(binding);

            AnalyzerSiteBindingRevision revision = bindingRevision(binding, 1, "sha256:" + "e".repeat(64));
            Analyzer analyzer = new Analyzer();
            analyzer.ensureFhirUuid();
            analyzer.setName("Connection probe persistence test");
            analyzer.setStatus(Analyzer.AnalyzerStatus.SETUP);
            analyzer.setActive(false);
            analyzer.setSiteBindingRevision(revision);
            analyzer.setTestUnitIds(List.of("1"));
            analyzer.setBridgeConnectionId("bridge-" + UUID.randomUUID());
            analyzer.setSysUserId(TEST_SYS_USER_ID);
            analyzerDAO.insert(analyzer);
            entityManager.flush();
            return new ProbeFixture(analyzer.getId(), analyzer.getBridgeConnectionId(), revision.getId(),
                    binding.getId(), profileBinding.getId(), profileId);
        });

        try {
            ObjectNode connection = probeDocument(fixture, false);
            ObjectNode evidence = probeDocument(fixture, true);
            stubGet(connectionUrl(fixture.connectionId()), connection);
            stubPost(connectionUrl(fixture.connectionId()) + "/probe", request -> {
                assertEquals("1.0", request.path("schemaVersion").asText());
                assertEquals(fixture.connectionId(), request.path("connectionId").asText());
                assertEquals(1, request.path("expectedConfigRevision").asInt());
                UUID.fromString(request.path("requestId").asText());
                return evidence.deepCopy().put("requestId", request.path("requestId").asText());
            });
            AnalyzerConnectionProbeView result = probeService.probe(fixture.analyzerId());

            assertEquals("SUCCEEDED", result.status());
            assertEquals(fixture.profileId(), result.profileRef().profileId());
        } finally {
            transaction.executeWithoutResult(status -> {
                JdbcTemplate jdbc = new JdbcTemplate(dataSource);
                jdbc.update("DELETE FROM history WHERE reference_id = ? AND reference_table = "
                        + "(SELECT id FROM reference_tables WHERE name = 'analyzer')", fixture.analyzerId());
                jdbc.update("DELETE FROM analyzer WHERE id = ?", Long.valueOf(fixture.analyzerId()));
                jdbc.update("DELETE FROM analyzer_site_binding_revision WHERE id = ?",
                        Long.valueOf(fixture.revisionId()));
                jdbc.update("DELETE FROM analyzer_site_binding WHERE id = ?", Long.valueOf(fixture.bindingId()));
                jdbc.update("DELETE FROM analyzer_profile_binding WHERE id = ?",
                        Long.valueOf(fixture.profileBindingId()));
            });
        }
    }

    @Test
    public void activationAndDeactivationPersistExactBridgeAcknowledgementsWithoutChangingTheLoadedVersion() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.executeWithoutResult(status -> {
            String qcTestId = createTest("Analyzer activation QC independence test").getId();
            AnalyzerInstanceRequest request = new AnalyzerInstanceRequest();
            request.setName("Activation persistence test");
            request.setProfileId(AnalyzerTestProfileCatalog.PROFILE_ID);
            request.setProfileRevision(AnalyzerTestProfileCatalog.PROFILE_REVISION);
            request.setTestUnitIds(List.of(createLabUnit()));
            AnalyzerInstanceState created = analyzerInstanceLocalStateService.create(request, TEST_SYS_USER_ID);
            analyzerInstanceLocalStateService.attachBridgeConnection(created.analyzerId(),
                    "bridge-" + UUID.randomUUID(), TEST_SYS_USER_ID);
            Analyzer analyzer = analyzerService.getWithBinding(created.analyzerId()).orElseThrow();
            AnalyzerProfileBinding profileBinding = analyzer.getPinnedProfileBinding();
            AnalyzerSiteBindingRevision revision = analyzer.getSiteBindingRevision();
            AnalyzerSiteBindingSnapshot snapshot = siteBindingService.findByRevisionId(revision.getId()).orElseThrow();
            confirmationService.confirm(snapshot, AnalyzerTestProfileCatalog.RECOGNITION_FINGERPRINT,
                    new AnalyzerSiteBindingConfirmationRequest(revision.getBindingFingerprint(),
                            AnalyzerTestProfileCatalog.RECOGNITION_FINGERPRINT, List.of(), List.of()),
                    TEST_SYS_USER_ID);
            entityManager.flush();
            entityManager.clear();

            stubGet(connectionUrl(analyzer.getBridgeConnectionId()), connectionDocument(analyzer, profileBinding));
            List<ObjectNode> acknowledgements = new ArrayList<>();
            stubPost(connectionUrl(analyzer.getBridgeConnectionId()) + "/runtime", command -> {
                assertEquals("1.0", command.path("schemaVersion").asText());
                assertEquals(analyzer.getBridgeConnectionId(), command.path("connectionId").asText());
                assertEquals(1, command.path("expectedConfigRevision").asInt());
                String action = acknowledgements.isEmpty() ? "ACTIVATE" : "DEACTIVATE";
                assertEquals(action, command.path("action").asText());
                UUID.fromString(command.path("commandId").asText());
                ObjectNode acknowledgement = runtimeAcknowledgement(analyzer, profileBinding,
                        command.path("commandId").asText(), action, "ACTIVATE".equals(action) ? "ACTIVE" : "INACTIVE",
                        acknowledgements.size() + 1);
                acknowledgements.add(acknowledgement);
                return acknowledgement;
            });

            AnalyzerActivationResult readinessBeforeQc = activationService.readiness(analyzer.getId());
            String confirmationIdBeforeQc = confirmationDAO.findByRevisionId(revision.getId()).orElseThrow().getId();

            QCControlLot controlLot = new QCControlLot();
            controlLot.setId(UUID.randomUUID().toString());
            controlLot.setLotNumber("ACTIVATION-INDEPENDENCE-" + UUID.randomUUID());
            controlLot.setProductName("Activation independence control");
            controlLot.setControlLevel("NORMAL");
            controlLot.setTestId(qcTestId);
            controlLot.setInstrumentId(analyzer.getId());
            controlLot.setCalculationMethod("MANUFACTURER_FIXED");
            controlLot.setManufacturerMean(100.0);
            controlLot.setManufacturerStdDev(5.0);
            controlLot.setActivationDate(new java.sql.Timestamp(System.currentTimeMillis()));
            controlLot.setSystemUserId(Integer.valueOf(TEST_SYS_USER_ID));
            controlLotService.createControlLot(controlLot);

            AnalyzerActivationResult readinessAfterQc = activationService.readiness(analyzer.getId());
            String confirmationIdAfterQc = confirmationDAO.findByRevisionId(revision.getId()).orElseThrow().getId();
            assertTrue(readinessBeforeQc.ready());
            assertTrue(readinessAfterQc.ready());
            assertEquals(confirmationIdBeforeQc, confirmationIdAfterQc);

            AnalyzerActivationResult result = activationService.activate(analyzer.getId(), TEST_SYS_USER_ID);
            entityManager.flush();
            entityManager.clear();

            Analyzer reloaded = analyzerDAO.get(analyzer.getId()).orElseThrow();
            assertTrue(result.activated());
            assertEquals(Analyzer.AnalyzerStatus.ACTIVE, reloaded.getStatus());
            assertTrue(reloaded.isActive());
            assertNotNull(reloaded.getLatestActivationRecord());

            AnalyzerDeactivationResult deactivation = activationService.deactivate(reloaded.getId(), TEST_SYS_USER_ID);
            entityManager.flush();
            entityManager.clear();

            Analyzer deactivated = analyzerDAO.get(analyzer.getId()).orElseThrow();
            assertTrue(deactivation.deactivated());
            assertEquals(Analyzer.AnalyzerStatus.INACTIVE, deactivated.getStatus());
            assertFalse(deactivated.isActive());
            var retained = activationRecordDAO.findByAnalyzerId(analyzer.getId());
            assertEquals(2, acknowledgements.size());
            assertEquals(2, retained.size());
            assertEquals(acknowledgements.get(0), parseJson(retained.get(0).getRuntimeAcknowledgementJson()));
            assertEquals(acknowledgements.get(1), parseJson(retained.get(1).getRuntimeAcknowledgementJson()));
            status.setRollbackOnly();
        });
    }

    private String createLabUnit() {
        Localization localization = new Localization();
        localization.setDescription("test section name");
        localization.setLocalizedValue("en", "Activation persistence");
        localization.setSysUserId(TEST_SYS_USER_ID);
        localizationService.insert(localization);
        TestSection unit = new TestSection();
        unit.setLocalization(localization);
        unit.setTestSectionName("Activation test");
        unit.setDescription("Owned by the activation persistence test");
        unit.setIsActive("Y");
        unit.setSysUserId(TEST_SYS_USER_ID);
        return testSectionService.insert(unit);
    }

    private static String connectionUrl(String connectionId) {
        return "http://bridge.test/api/connections/" + connectionId;
    }

    private void stubGet(String url, ObjectNode response) {
        try {
            when(bridgeHttpClient.get(eq(url), eq(Duration.ofSeconds(10))))
                    .thenReturn(new BridgeHttpClient.BridgeResponse(200, response.toString()));
        } catch (IOException exception) {
            throw new AssertionError("Cannot configure external transport fixture", exception);
        }
    }

    private void stubPost(String url, Function<ObjectNode, ObjectNode> response) {
        try {
            // Command/request IDs are generated by the real service. The callback
            // validates the complete request before returning matching evidence.
            when(bridgeHttpClient.post(eq(url), argThat(body -> body != null), eq(Duration.ofSeconds(10))))
                    .thenAnswer(call -> new BridgeHttpClient.BridgeResponse(200,
                            response.apply(parseJson(call.getArgument(1))).toString()));
        } catch (IOException exception) {
            throw new AssertionError("Cannot configure external transport fixture", exception);
        }
    }

    private org.openelisglobal.test.valueholder.Test createTest(String description) {
        org.openelisglobal.test.valueholder.Test test = new org.openelisglobal.test.valueholder.Test();
        test.setName(description);
        test.setDescription(description);
        test.setGuid(UUID.randomUUID().toString());
        test.setIsActive("Y");
        test.setIsReportable("Y");
        test.setSysUserId(TEST_SYS_USER_ID);
        testService.insert(test);
        return test;
    }

    private record ConnectionFixture(String analyzerId, String revisionId, String bindingId, String profileBindingId) {
    }

    private record BindingSelectionFixture(String analyzerId, String initialRevisionId, String reviewedRevisionId,
            String bindingId, String profileBindingId, String reviewedFingerprint) {
    }

    private record ProbeFixture(String analyzerId, String connectionId, String revisionId, String bindingId,
            String profileBindingId, String profileId) {
    }

    private AnalyzerSiteBindingRevision bindingRevision(AnalyzerSiteBinding binding, int revisionNumber,
            String fingerprint) {
        AnalyzerSiteBindingRevision revision = new AnalyzerSiteBindingRevision();
        revision.setSiteBinding(binding);
        revision.setRevisionNumber(revisionNumber);
        revision.setBindingFingerprint(fingerprint);
        revision.setCreatedBy(TEST_SYS_USER_ID);
        revision.setSysUserId(TEST_SYS_USER_ID);
        revisionDAO.insert(revision);
        return revision;
    }

    private static ObjectNode profile(String profileId) {
        try {
            ObjectNode profile = (ObjectNode) new ObjectMapper().readTree("""
                    {
                      "profileMeta":{"id":"placeholder","displayName":"Persistence Test Analyzer"},
                      "protocol":{"name":"ASTM","version":"LIS2-A2"},
                      "communication":{"mode":"ANALYZER_INITIATED","supports_lis_initiated":false},
                      "configDefaults":{"connectionRole":"SERVER","transport":"TCP/IP"},
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
        return runtimeAcknowledgement(analyzer, profile, commandId, "ACTIVATE", "ACTIVE", runtimeRevision);
    }

    private static ObjectNode runtimeAcknowledgement(Analyzer analyzer, AnalyzerProfileBinding profile,
            String commandId, String action, String runtimeState, int runtimeRevision) {
        ObjectNode acknowledgement = new ObjectMapper().createObjectNode();
        acknowledgement.put("schemaVersion", "1.0");
        acknowledgement.put("commandId", commandId);
        acknowledgement.put("action", action);
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
        acknowledgement.put("desiredRuntimeState", runtimeState);
        acknowledgement.put("actualRuntimeState", runtimeState);
        acknowledgement.putArray("blockers");
        acknowledgement.put("acknowledgedAt", "2026-08-24T19:05:05Z");
        return acknowledgement;
    }

    private static ObjectNode connectionDocument(Analyzer analyzer, AnalyzerProfileBinding profile) {
        ObjectNode connection = new ObjectMapper().createObjectNode();
        connection.put("schemaVersion", "1.0");
        connection.put("connectionId", analyzer.getBridgeConnectionId());
        connection.put("clientAnalyzerId", analyzer.getId());
        connection.put("displayName", analyzer.getName());
        connection.putArray("fields");
        connection.put("desiredRuntimeState", "INACTIVE");
        connection.put("actualRuntimeState", "INACTIVE");
        connection.put("updatedAt", "2026-08-25T16:00:00Z");
        connection.putObject("profileRef").put("profileId", profile.getProfileId())
                .put("revision", profile.getProfileRevision()).put("fingerprint", profile.getProfileFingerprint());
        connection.put("configRevision", 1);
        connection.put("configFingerprint", "sha256:" + "c".repeat(64));
        connection.putObject("readiness").put("ready", true).putArray("blockers");
        return connection;
    }

    private static ObjectNode probeDocument(ProbeFixture fixture, boolean evidence) {
        ObjectNode document = new ObjectMapper().createObjectNode();
        document.put("schemaVersion", "1.0");
        document.put("connectionId", fixture.connectionId());
        if (evidence) {
            document.put("requestId", "probe-after-transaction");
        } else {
            document.put("clientAnalyzerId", fixture.analyzerId());
            document.put("displayName", "Connection probe persistence test");
            document.putArray("fields");
            document.put("desiredRuntimeState", "INACTIVE");
            document.put("actualRuntimeState", "INACTIVE");
            document.put("updatedAt", "2026-08-25T16:00:00Z");
            document.putObject("readiness").put("ready", true).putArray("blockers");
        }
        document.putObject("profileRef").put("profileId", fixture.profileId()).put("revision", 1).put("fingerprint",
                PROFILE_FINGERPRINT);
        document.put("configRevision", 1);
        document.put("configFingerprint", "sha256:" + "f".repeat(64));
        if (evidence) {
            document.put("nonMutating", true);
            document.put("status", "SUCCEEDED");
            document.put("startedAt", "2026-08-25T16:00:00Z");
            document.put("completedAt", "2026-08-25T16:00:01Z");
            document.putArray("checks");
        }
        return document;
    }

    private static ObjectNode parseJson(String value) {
        try {
            return (ObjectNode) new ObjectMapper().readTree(value);
        } catch (Exception exception) {
            throw new AssertionError("Cannot parse retained activation document", exception);
        }
    }
}
