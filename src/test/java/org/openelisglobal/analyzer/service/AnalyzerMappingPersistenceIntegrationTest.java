package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.sql.DataSource;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzer.dao.AnalyzerActivationRecordDAO;
import org.openelisglobal.analyzer.dao.AnalyzerDAO;
import org.openelisglobal.analyzer.dao.AnalyzerMappingConfirmationDAO;
import org.openelisglobal.analyzer.dao.AnalyzerMappingDAO;
import org.openelisglobal.analyzer.dao.AnalyzerMappingResultDAO;
import org.openelisglobal.analyzer.dao.AnalyzerMappingTestDAO;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerMapping;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingConfirmation;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingOrigin;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingState;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfilePin;
import org.openelisglobal.analyzerresults.service.AnalyzerResultsService;
import org.openelisglobal.audittrail.daoimpl.AuditTrailServiceImpl;
import org.openelisglobal.history.service.HistoryService;
import org.openelisglobal.qc.service.QCControlLotService;
import org.openelisglobal.qc.valueholder.QCControlLot;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.testresultcomponent.service.TestResultComponentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

public class AnalyzerMappingPersistenceIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String PROFILE_FINGERPRINT = "sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static final String RECOGNITION_FINGERPRINT = "sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";

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
    private AnalyzerMappingDAO mappingDAO;

    @Autowired
    private AnalyzerMappingTestDAO mappingTestDAO;

    @Autowired
    private AnalyzerMappingResultDAO mappingResultDAO;

    @Autowired
    private AnalyzerMappingConfirmationDAO confirmationDAO;

    @Autowired
    private AnalyzerMappingConfirmationService confirmationService;

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
    public void savedMappingAndConfirmationReloadFromPostgres() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.executeWithoutResult(status -> {
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            String testId = jdbc.queryForObject("SELECT nextval('test_seq')", Long.class).toString();
            String resultOptionId = jdbc.queryForObject("SELECT nextval('test_result_seq')", Long.class).toString();
            jdbc.update(
                    "INSERT INTO test (id, name, description, guid, is_active, is_reportable, orderable, "
                            + "lastupdated) VALUES (?, ?, ?, ?, 'Y', 'Y', TRUE, CURRENT_TIMESTAMP)",
                    Long.valueOf(testId), "Analyzer mapping persistence test", "Analyzer mapping persistence test",
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
            // The local test carries no LOINC code, so no default can bind and the operator
            // decides.
            when(mappingCatalogService.searchActiveTests(null))
                    .thenReturn(List.of(new AnalyzerMappingCatalogService.TestOption(testId,
                            "Analyzer mapping persistence test", "TEST", List.of())));
            when(mappingCatalogService.getActiveResultOptions(testId)).thenReturn(List.of(
                    new AnalyzerMappingCatalogService.ResultOption(resultOptionId, "POSITIVE", "Positive", List.of())));

            AuditTrailServiceImpl auditTrailService = auditTrail();
            String profileId = "site.persistence." + UUID.randomUUID();
            AnalyzerMappingService mappingService = mappingService(testService, testResultService,
                    mappingCatalogService, auditTrailService, profileId);
            AnalyzerMappingConfirmationService confirmationService = new AnalyzerMappingConfirmationServiceImpl(
                    confirmationDAO, auditTrailService, systemUserService, mappingCatalogService);

            Analyzer analyzer = insertAnalyzer("Persistence analyzer", Analyzer.AnalyzerStatus.VALIDATION);
            AnalyzerMappingSnapshot initial = mappingService.assignProfile(analyzer, profileId, 1, TEST_SYS_USER_ID);
            AnalyzerMappingDraft decisions = new AnalyzerMappingDraft(
                    List.of(new AnalyzerMappingTestDraft("RAW-A", AnalyzerMappingState.BOUND, testId, null, null,
                            AnalyzerMappingOrigin.OVERRIDE)),
                    List.of(new AnalyzerMappingResultDraft("RAW-A", "POS", AnalyzerMappingState.BOUND, resultOptionId,
                            null, AnalyzerMappingOrigin.OVERRIDE)));
            AnalyzerMappingSnapshot saved = mappingService.appendRevision(analyzer, decisions, TEST_SYS_USER_ID);
            analyzer.setMapping(saved.mapping());
            analyzerDAO.update(analyzer);
            AnalyzerMappingConfirmationRequest request = new AnalyzerMappingConfirmationRequest(
                    saved.mapping().getMappingFingerprint(), RECOGNITION_FINGERPRINT,
                    List.of(new AnalyzerMappingSourceRow("RAW-A", null), new AnalyzerMappingSourceRow("RAW-A", "POS")),
                    List.of());
            confirmationService.confirm(saved, RECOGNITION_FINGERPRINT, request, TEST_SYS_USER_ID);
            var storedVerification = confirmationDAO.findByMappingId(saved.mapping().getId()).orElseThrow();
            analyzer.setBridgeConnectionId("bridge-" + UUID.randomUUID());
            analyzerDAO.update(analyzer);

            AnalyzerActivationRecordService activationRecordService = new AnalyzerActivationRecordServiceImpl(
                    activationRecordDAO, auditTrailService);
            AnalyzerProfilePin pin = saved.mapping().getProfilePin();
            ObjectNode firstAcknowledgement = runtimeAcknowledgement(analyzer, pin, "activate-1", 1);
            var firstRecord = activationRecordService.retain(analyzer, saved.mapping(), storedVerification,
                    firstAcknowledgement, "ACTIVE", TEST_SYS_USER_ID);
            ObjectNode secondAcknowledgement = runtimeAcknowledgement(analyzer, pin, "activate-2", 2);
            var latestRecord = activationRecordService.retain(analyzer, saved.mapping(), storedVerification,
                    secondAcknowledgement, "ACTIVE", TEST_SYS_USER_ID);
            analyzer.setLatestActivationRecord(latestRecord);
            analyzer.setStatus(Analyzer.AnalyzerStatus.ACTIVE);
            analyzerDAO.update(analyzer);

            entityManager.flush();
            entityManager.clear();

            AnalyzerMappingSnapshot reloaded = mappingService.findLatestByAnalyzerId(analyzer.getId()).orElseThrow();
            assertEquals(2, reloaded.mapping().getRevisionNumber());
            assertEquals(saved.mapping().getMappingFingerprint(), reloaded.mapping().getMappingFingerprint());
            assertEquals(profileId, reloaded.mapping().getProfileId());
            assertEquals(PROFILE_FINGERPRINT, reloaded.mapping().getProfileFingerprint());
            assertEquals(initial.mapping().getId(), reloaded.mapping().getSupersedes().getId());
            assertEquals(AnalyzerMappingState.BOUND, reloaded.tests().get(0).getMappingState());
            assertEquals(AnalyzerMappingOrigin.OVERRIDE, reloaded.tests().get(0).getOrigin());
            assertEquals(testId, reloaded.tests().get(0).getTestId());
            assertEquals(AnalyzerMappingState.BOUND, reloaded.results().get(0).getMappingState());
            assertEquals(resultOptionId, reloaded.results().get(0).getTestResultId());

            AnalyzerMappingSnapshot firstRevision = mappingService.findById(initial.mapping().getId()).orElseThrow();
            assertEquals(AnalyzerMappingState.UNRESOLVED, firstRevision.tests().get(0).getMappingState());
            assertEquals(AnalyzerMappingOrigin.DEFAULT, firstRevision.tests().get(0).getOrigin());
            assertEquals(AnalyzerUnresolvedReason.NO_MATCH, firstRevision.tests().get(0).getUnresolvedReason());

            AnalyzerMappingConfirmationView confirmation = confirmationService.getStatus(reloaded,
                    RECOGNITION_FINGERPRINT);
            var storedConfirmation = confirmationDAO.findByMappingId(reloaded.mapping().getId()).orElseThrow();
            assertEquals(AnalyzerMappingConfirmationView.State.CURRENT, confirmation.state());
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

    @Test
    public void anAssayTurnedOffUnderItsInstrumentCodeIsKeptByTheRevision() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.executeWithoutResult(status -> {
            String profileId = "site.assays." + UUID.randomUUID();
            AnalyzerMappingService mappingService = mappingService(mock(TestService.class),
                    mock(TestResultService.class), mock(AnalyzerMappingCatalogService.class), auditTrail(), profileId);
            Analyzer analyzer = insertAnalyzer("Assay analyzer", Analyzer.AnalyzerStatus.SETUP);
            mappingService.assignProfile(analyzer, profileId, 1, TEST_SYS_USER_ID);

            mappingService.appendRevision(analyzer, new AnalyzerMappingDraft(
                    List.of(new AnalyzerMappingTestDraft("RAW-A", AnalyzerMappingState.UNRESOLVED, null)
                            .withAssay(false, "MTB")),
                    List.of(new AnalyzerMappingResultDraft("RAW-A", "POS", AnalyzerMappingState.UNRESOLVED, null))),
                    TEST_SYS_USER_ID);
            entityManager.flush();
            entityManager.clear();

            var row = mappingService.findLatestByAnalyzerId(analyzer.getId()).orElseThrow().tests().get(0);
            assertFalse(row.isEnabled());
            assertEquals("MTB", row.getInstrumentCode());
            status.setRollbackOnly();
        });
    }

    @Test
    public void aMappingCanReturnToTheContentOfAnEarlierRevision() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.executeWithoutResult(status -> {
            String profileId = "site.revert." + UUID.randomUUID();
            AnalyzerMappingService mappingService = mappingService(mock(TestService.class),
                    mock(TestResultService.class), mock(AnalyzerMappingCatalogService.class), auditTrail(), profileId);

            Analyzer analyzer = insertAnalyzer("Revert analyzer", Analyzer.AnalyzerStatus.SETUP);
            AnalyzerMappingSnapshot initial = mappingService.assignProfile(analyzer, profileId, 1, TEST_SYS_USER_ID);
            AnalyzerMappingDraft excluded = new AnalyzerMappingDraft(
                    List.of(new AnalyzerMappingTestDraft("RAW-A", AnalyzerMappingState.EXCLUDED, null)),
                    List.of(new AnalyzerMappingResultDraft("RAW-A", "POS", AnalyzerMappingState.EXCLUDED, null)));
            AnalyzerMappingSnapshot changed = mappingService.appendRevision(analyzer, excluded, TEST_SYS_USER_ID);
            // RAW-A matched no local test, so the first revision has the assay off.
            AnalyzerMappingDraft restoredContent = new AnalyzerMappingDraft(
                    List.of(new AnalyzerMappingTestDraft("RAW-A", AnalyzerMappingState.UNRESOLVED, null)
                            .withAssay(false, null)),
                    List.of(new AnalyzerMappingResultDraft("RAW-A", "POS", AnalyzerMappingState.UNRESOLVED, null)));

            AnalyzerMappingSnapshot restored = mappingService.appendRevision(analyzer, restoredContent,
                    TEST_SYS_USER_ID);
            entityManager.flush();
            entityManager.clear();

            AnalyzerMappingSnapshot current = mappingService.findLatestByAnalyzerId(analyzer.getId()).orElseThrow();
            assertEquals(1, initial.mapping().getRevisionNumber());
            assertEquals(2, changed.mapping().getRevisionNumber());
            assertEquals(3, restored.mapping().getRevisionNumber());
            assertFalse(initial.mapping().getId().equals(restored.mapping().getId()));
            assertEquals(initial.mapping().getMappingFingerprint(), restored.mapping().getMappingFingerprint());
            assertEquals(restored.mapping().getId(), current.mapping().getId());
            status.setRollbackOnly();
        });
    }

    @Test
    public void twoAnalyzersOnTheSameProfileKeepIndependentMappings() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.executeWithoutResult(status -> {
            String profileId = "site.independent." + UUID.randomUUID();
            AnalyzerMappingService mappingService = mappingService(mock(TestService.class),
                    mock(TestResultService.class), mock(AnalyzerMappingCatalogService.class), auditTrail(), profileId);
            Analyzer first = insertAnalyzer("Independent analyzer 1", Analyzer.AnalyzerStatus.SETUP);
            Analyzer second = insertAnalyzer("Independent analyzer 2", Analyzer.AnalyzerStatus.SETUP);
            first.setMapping(mappingService.assignProfile(first, profileId, 1, TEST_SYS_USER_ID).mapping());
            second.setMapping(mappingService.assignProfile(second, profileId, 1, TEST_SYS_USER_ID).mapping());
            analyzerDAO.update(first);
            analyzerDAO.update(second);

            mappingService.appendRevision(first,
                    new AnalyzerMappingDraft(
                            List.of(new AnalyzerMappingTestDraft("RAW-A", AnalyzerMappingState.EXCLUDED, null, null,
                                    null, AnalyzerMappingOrigin.OVERRIDE)),
                            List.of(new AnalyzerMappingResultDraft("RAW-A", "POS", AnalyzerMappingState.EXCLUDED, null,
                                    null, AnalyzerMappingOrigin.OVERRIDE))),
                    TEST_SYS_USER_ID);
            entityManager.flush();
            entityManager.clear();

            AnalyzerMappingSnapshot editedFirst = mappingService.findLatestByAnalyzerId(first.getId()).orElseThrow();
            AnalyzerMappingSnapshot untouchedSecond = mappingService.findLatestByAnalyzerId(second.getId())
                    .orElseThrow();
            assertEquals(2, editedFirst.mapping().getRevisionNumber());
            assertEquals(AnalyzerMappingState.EXCLUDED, editedFirst.tests().get(0).getMappingState());
            assertEquals(1, untouchedSecond.mapping().getRevisionNumber());
            assertEquals(AnalyzerMappingState.UNRESOLVED, untouchedSecond.tests().get(0).getMappingState());
            assertEquals(List.of(first.getId(), second.getId()), mappingDAO.findAnalyzersInForceOnProfile(profileId)
                    .stream().map(Analyzer::getId).sorted().toList());
            status.setRollbackOnly();
        });
    }

    @Test
    public void bridgeConnectionReferencePersistsAfterReloadingTheLocalAnalyzer() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        String analyzerId = transaction.execute(status -> {
            Analyzer analyzer = insertAnalyzer("Connection reference persistence test", Analyzer.AnalyzerStatus.SETUP);
            AnalyzerMapping revision = insertMapping(analyzer, 1, "site.connection." + UUID.randomUUID(),
                    "sha256:" + "c".repeat(64));
            analyzer.setMapping(revision);
            analyzerDAO.update(analyzer);
            entityManager.flush();
            return analyzer.getId();
        });

        try {
            String connectionId = "bridge-" + UUID.randomUUID();
            AnalyzerInstanceState attached = analyzerInstanceLocalStateService.attachBridgeConnection(analyzerId,
                    connectionId, TEST_SYS_USER_ID);

            assertEquals(connectionId, attached.bridgeConnectionId());
            String persistedConnectionId = transaction
                    .execute(status -> analyzerDAO.get(analyzerId).orElseThrow().getBridgeConnectionId());
            assertEquals(connectionId, persistedConnectionId);
        } finally {
            deleteAnalyzer(transaction, analyzerId);
        }
    }

    @Test
    public void anAnalyzerWithNoMappingStaysVisibleAndCarriesNoProfile() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        String analyzerId = transaction.execute(status -> {
            Analyzer analyzer = insertAnalyzer("Unmapped migrated analyzer", Analyzer.AnalyzerStatus.INACTIVE);
            entityManager.flush();
            return analyzer.getId();
        });

        try {
            AnalyzerInstanceState state = analyzerInstanceLocalStateService.get(analyzerId);

            assertEquals("", state.profileId());
            assertEquals(0, state.profileRevision());
            assertEquals(Analyzer.AnalyzerStatus.INACTIVE, state.status());
            assertNull(transaction.execute(status -> analyzerDAO.get(analyzerId).orElseThrow().getMapping()));
        } finally {
            deleteAnalyzer(transaction, analyzerId);
        }
    }

    @Test
    public void reviewedMappingRevisionPersistsAfterReloadingTheLocalAnalyzer() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        MappingSelectionFixture fixture = transaction.execute(status -> {
            Analyzer analyzer = insertAnalyzer("Mapping selection persistence test", Analyzer.AnalyzerStatus.SETUP);
            // The confirmation service reads this profile from the test Bridge catalog.
            String profileId = "site.unknown-capable";
            String profileFingerprint = "sha256:" + "1".repeat(64);
            AnalyzerMapping initial = insertMapping(analyzer, 1, profileId, 3, profileFingerprint,
                    "sha256:" + "c".repeat(64));
            AnalyzerMapping reviewed = insertMapping(analyzer, 2, profileId, 3, profileFingerprint,
                    "sha256:" + "d".repeat(64));
            analyzer.setMapping(initial);
            analyzerDAO.update(analyzer);

            confirmationService.confirm(new AnalyzerMappingSnapshot(reviewed, List.of(), List.of()),
                    "sha256:" + "2".repeat(64), new AnalyzerMappingConfirmationRequest(reviewed.getMappingFingerprint(),
                            "sha256:" + "2".repeat(64), List.of(), List.of()),
                    TEST_SYS_USER_ID);
            entityManager.flush();
            return new MappingSelectionFixture(analyzer.getId(), reviewed.getId(), reviewed.getMappingFingerprint());
        });

        try {
            analyzerInstanceLocalStateService.applyMapping(fixture.analyzerId(), fixture.reviewedMappingId(), 2,
                    fixture.reviewedFingerprint(), TEST_SYS_USER_ID);

            String persistedMappingId = transaction
                    .execute(status -> analyzerDAO.get(fixture.analyzerId()).orElseThrow().getMapping().getId());
            assertEquals(fixture.reviewedMappingId(), persistedMappingId);
        } finally {
            deleteAnalyzer(transaction, fixture.analyzerId());
        }
    }

    @Test
    public void connectionProbeReadsThePinnedProfileAfterTheAnalyzerTransactionCloses() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        String profileId = "site.probe." + UUID.randomUUID();
        ProbeFixture fixture = transaction.execute(status -> {
            Analyzer analyzer = insertAnalyzer("Connection probe persistence test", Analyzer.AnalyzerStatus.SETUP);
            analyzer.setMapping(insertMapping(analyzer, 1, profileId, "sha256:" + "e".repeat(64)));
            analyzer.setBridgeConnectionId("bridge-" + UUID.randomUUID());
            analyzerDAO.update(analyzer);
            entityManager.flush();
            return new ProbeFixture(analyzer.getId(), analyzer.getBridgeConnectionId(), profileId);
        });

        try {
            BridgeAnalyzerConnectionClient bridgeClient = mock(BridgeAnalyzerConnectionClient.class);
            ObjectNode connection = probeDocument(fixture, false);
            ObjectNode evidence = probeDocument(fixture, true);
            when(bridgeClient.getConnection(fixture.connectionId())).thenReturn(connection);
            when(bridgeClient.probe(fixture.connectionId(), 1, "probe-after-transaction")).thenReturn(evidence);
            AnalyzerConnectionProbeService probeService = new AnalyzerConnectionProbeService(analyzerService,
                    bridgeClient, () -> "probe-after-transaction");

            AnalyzerConnectionProbeView result = probeService.probe(fixture.analyzerId());

            assertEquals("SUCCEEDED", result.status());
            assertEquals(fixture.profileId(), result.profileRef().profileId());
        } finally {
            deleteAnalyzer(transaction, fixture.analyzerId());
        }
    }

    @Test
    public void activationAndDeactivationPersistExactBridgeAcknowledgementsWithoutChangingTheLoadedVersion() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.executeWithoutResult(status -> {
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            String qcTestId = jdbc.queryForObject("SELECT nextval('test_seq')", Long.class).toString();
            jdbc.update(
                    "INSERT INTO test (id, name, description, guid, is_active, is_reportable, orderable, "
                            + "lastupdated) VALUES (?, ?, ?, ?, 'Y', 'Y', TRUE, CURRENT_TIMESTAMP)",
                    Long.valueOf(qcTestId), "Analyzer activation QC independence test",
                    "Analyzer activation QC independence test", UUID.randomUUID());

            String profileId = "site.activation." + UUID.randomUUID();
            Analyzer analyzer = insertAnalyzer("Activation persistence test", Analyzer.AnalyzerStatus.SETUP);
            AnalyzerMapping revision = insertMapping(analyzer, 1, profileId, "sha256:" + "c".repeat(64));
            AnalyzerMappingConfirmation confirmation = insertConfirmation(revision, profileId);
            analyzer.setMapping(revision);
            analyzer.setBridgeConnectionId("bridge-" + UUID.randomUUID());
            analyzerDAO.update(analyzer);
            entityManager.flush();
            entityManager.clear();

            AnalyzerMappingSnapshot snapshot = new AnalyzerMappingSnapshot(revision, List.of(), List.of());
            BridgeProfileCatalogService profileCatalogService = mock(BridgeProfileCatalogService.class);
            AnalyzerMappingService mappingService = mock(AnalyzerMappingService.class);
            AnalyzerMappingConfirmationService confirmationService = mock(AnalyzerMappingConfirmationService.class);
            TestSectionService testSectionService = mock(TestSectionService.class);
            BridgeAnalyzerConnectionClient bridgeClient = mock(BridgeAnalyzerConnectionClient.class);
            when(profileCatalogService.getProfile(profileId, 1)).thenReturn(
                    new BridgeProfileCatalog.ProfileRevision(profile(profileId), new ObjectMapper().createObjectNode(),
                            new BridgeProfileCatalog.ControlRecognitionSummary(RECOGNITION_FINGERPRINT, "NONE",
                                    "No automated control recognition", true, List.of())));
            when(mappingService.findById(revision.getId())).thenReturn(java.util.Optional.of(snapshot));
            when(confirmationService.assessCurrent(snapshot, RECOGNITION_FINGERPRINT))
                    .thenReturn(AnalyzerMappingVerificationAssessment.current(confirmation));
            when(confirmationService.findForMapping(revision.getId())).thenReturn(java.util.Optional.of(confirmation));
            TestSection activeUnit = new TestSection();
            activeUnit.setId("1");
            activeUnit.setIsActive("Y");
            when(testSectionService.get("1")).thenReturn(activeUnit);

            AnalyzerProfilePin pin = revision.getProfilePin();
            ObjectNode connection = connectionDocument(analyzer, pin);
            ObjectNode acknowledgement = runtimeAcknowledgement(analyzer, pin, "activate-persistence", 1);
            when(bridgeClient.getConnection(analyzer.getBridgeConnectionId())).thenReturn(connection);
            when(bridgeClient.applyRuntimeCommand(analyzer.getBridgeConnectionId(), 1, "ACTIVATE",
                    "activate-persistence")).thenReturn(acknowledgement);

            AuditTrailServiceImpl auditTrailService = auditTrail();
            AnalyzerActivationRecordService activationRecordService = new AnalyzerActivationRecordServiceImpl(
                    activationRecordDAO, auditTrailService);
            AnalyzerActivationService activationService = new AnalyzerActivationServiceImpl(analyzerService,
                    profileCatalogService, mappingService, confirmationService, testSectionService, bridgeClient,
                    activationRecordService, java.time.Clock.systemUTC(), () -> "activate-persistence",
                    () -> "deactivate-persistence");

            AnalyzerActivationResult readinessBeforeQc = activationService.readiness(analyzer.getId());
            String confirmationIdBeforeQc = confirmationDAO.findByMappingId(revision.getId()).orElseThrow().getId();

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
            String confirmationIdAfterQc = confirmationDAO.findByMappingId(revision.getId()).orElseThrow().getId();
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

            ObjectNode deactivationAcknowledgement = runtimeAcknowledgement(reloaded, pin, "deactivate-persistence",
                    "DEACTIVATE", "INACTIVE", 2);
            when(bridgeClient.applyRuntimeCommand(reloaded.getBridgeConnectionId(), 1, "DEACTIVATE",
                    "deactivate-persistence")).thenReturn(deactivationAcknowledgement);

            AnalyzerDeactivationResult deactivation = activationService.deactivate(reloaded.getId(), TEST_SYS_USER_ID);
            entityManager.flush();
            entityManager.clear();

            Analyzer deactivated = analyzerDAO.get(analyzer.getId()).orElseThrow();
            assertTrue(deactivation.deactivated());
            assertEquals(Analyzer.AnalyzerStatus.INACTIVE, deactivated.getStatus());
            assertFalse(deactivated.isActive());
            assertEquals(2, activationRecordDAO.findByAnalyzerId(analyzer.getId()).size());
            status.setRollbackOnly();
        });
    }

    /**
     * Two operators save the same analyzer's mapping from the same loaded revision.
     * Both read it before either writes, which the catalog lookup between the read
     * and the insert holds open. The later save must be refused as stale, not fail
     * on the revision number or land on top of a revision it never loaded. The data
     * is committed so both transactions really contend.
     */
    @Test
    public void overlappingSavesOfOneMappingRefuseTheStaleOne() throws Exception {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        String profileId = "site.save-race." + UUID.randomUUID();
        AnalyzerMappingService mappingService = mappingService(mock(TestService.class), mock(TestResultService.class),
                mock(AnalyzerMappingCatalogService.class), auditTrail(), profileId);
        Analyzer analyzer = transaction.execute(status -> {
            Analyzer inserted = insertAnalyzer("Mapping save race test", Analyzer.AnalyzerStatus.SETUP);
            inserted.setMapping(mappingService.assignProfile(inserted, profileId, 1, TEST_SYS_USER_ID).mapping());
            analyzerDAO.update(inserted);
            return inserted;
        });
        String loadedFingerprint = mappingDAO.findLatestByAnalyzerId(analyzer.getId()).orElseThrow()
                .getMappingFingerprint();

        CyclicBarrier bothRead = new CyclicBarrier(2);
        BridgeProfileCatalogService catalog = mock(BridgeProfileCatalogService.class);
        when(catalog.getProfile(profileId, 1)).thenAnswer(invocation -> {
            try {
                bothRead.await(2, TimeUnit.SECONDS);
            } catch (Exception serialized) {
                // Only one save reaches this point at a time once saves are serialized.
            }
            return new BridgeProfileCatalog.ProfileRevision(profile(profileId), new ObjectMapper().createObjectNode(),
                    new BridgeProfileCatalog.ControlRecognitionSummary(RECOGNITION_FINGERPRINT, "NONE",
                            "No automated control recognition", true, List.of()));
        });
        AnalyzerMappingEditorService editor = new AnalyzerMappingEditorServiceImpl(analyzerService, catalog,
                mappingService, mock(AnalyzerMappingCatalogService.class), confirmationService,
                mock(AnalyzerResultsService.class),
                new AnalyzerMappingDefaults(mock(AnalyzerMappingCatalogService.class), mock(TestResultService.class)));
        AnalyzerMappingUpdate exclude = new AnalyzerMappingUpdate(loadedFingerprint,
                List.of(new AnalyzerMappingTestDraft("RAW-A", AnalyzerMappingState.EXCLUDED, null)),
                List.of(new AnalyzerMappingResultDraft("RAW-A", "POS", AnalyzerMappingState.EXCLUDED, null)));
        Callable<AnalyzerMappingView> save = () -> transaction
                .execute(status -> editor.saveMapping(analyzer.getId(), exclude, TEST_SYS_USER_ID));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<AnalyzerMappingView> first = executor.submit(save);
            Future<AnalyzerMappingView> second = executor.submit(save);
            List<Throwable> refusals = new java.util.ArrayList<>();
            for (Future<AnalyzerMappingView> outcome : List.of(first, second)) {
                try {
                    outcome.get(30, TimeUnit.SECONDS);
                } catch (ExecutionException refused) {
                    refusals.add(refused.getCause());
                }
            }

            assertEquals(1, refusals.size());
            assertTrue("the later save is refused as stale, got " + refusals.get(0),
                    refusals.get(0) instanceof IllegalArgumentException);
            assertEquals(2, mappingDAO.findLatestByAnalyzerId(analyzer.getId()).orElseThrow().getRevisionNumber());
        } finally {
            executor.shutdownNow();
            deleteAnalyzer(transaction, analyzer.getId());
        }
    }

    /**
     * Two activations of one analyzer overlap: the first to reach the Bridge gets
     * APPLIED and the second ALREADY_APPLIED. Unless lifecycle transitions on the
     * analyzer row are serialized, the second commits first, the first then fails
     * its version check and compensates by deactivating the runtime that the
     * committed row records as active. The data is committed so both transactions
     * really contend for the row.
     */
    @Test
    public void overlappingActivationsLeaveTheBridgeRuntimeActive() throws Exception {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        String profileId = "site.activation-race." + UUID.randomUUID();
        ActivationRaceFixture fixture = transaction.execute(status -> {
            Analyzer analyzer = insertAnalyzer("Activation race test", Analyzer.AnalyzerStatus.SETUP);
            AnalyzerMapping revision = insertMapping(analyzer, 1, profileId, "sha256:" + "c".repeat(64));
            AnalyzerMappingConfirmation confirmation = insertConfirmation(revision, profileId);
            analyzer.setMapping(revision);
            analyzer.setBridgeConnectionId("bridge-" + UUID.randomUUID());
            analyzerDAO.update(analyzer);
            return new ActivationRaceFixture(analyzer, revision, confirmation);
        });
        Analyzer analyzer = fixture.analyzer();

        try {
            AnalyzerMappingSnapshot snapshot = new AnalyzerMappingSnapshot(fixture.revision(), List.of(), List.of());
            BridgeProfileCatalogService profileCatalogService = mock(BridgeProfileCatalogService.class);
            AnalyzerMappingService mappingService = mock(AnalyzerMappingService.class);
            AnalyzerMappingConfirmationService confirmationService = mock(AnalyzerMappingConfirmationService.class);
            TestSectionService testSectionService = mock(TestSectionService.class);
            BridgeAnalyzerConnectionClient bridgeClient = mock(BridgeAnalyzerConnectionClient.class);
            when(profileCatalogService.getProfile(profileId, 1)).thenReturn(
                    new BridgeProfileCatalog.ProfileRevision(profile(profileId), new ObjectMapper().createObjectNode(),
                            new BridgeProfileCatalog.ControlRecognitionSummary(RECOGNITION_FINGERPRINT, "NONE",
                                    "No automated control recognition", true, List.of())));
            when(mappingService.findById(fixture.revision().getId())).thenReturn(java.util.Optional.of(snapshot));
            when(confirmationService.assessCurrent(snapshot, RECOGNITION_FINGERPRINT))
                    .thenReturn(AnalyzerMappingVerificationAssessment.current(fixture.confirmation()));
            TestSection activeUnit = new TestSection();
            activeUnit.setId("1");
            activeUnit.setIsActive("Y");
            when(testSectionService.get("1")).thenReturn(activeUnit);
            when(bridgeClient.getConnection(analyzer.getBridgeConnectionId()))
                    .thenReturn(connectionDocument(analyzer, fixture.revision().getProfilePin()));

            AtomicReference<String> runtimeState = new AtomicReference<>("INACTIVE");
            List<String> bridgeCommands = new CopyOnWriteArrayList<>();
            CountDownLatch firstActivationInBridge = new CountDownLatch(1);
            CountDownLatch secondActivationFinished = new CountDownLatch(1);
            when(bridgeClient.applyRuntimeCommand(eq(analyzer.getBridgeConnectionId()), eq(1), anyString(),
                    anyString())).thenAnswer(invocation -> {
                        String action = invocation.getArgument(2);
                        String commandId = invocation.getArgument(3);
                        bridgeCommands.add(action);
                        String state = "DEACTIVATE".equals(action) ? "INACTIVE" : "ACTIVE";
                        boolean changed = !state.equals(runtimeState.getAndSet(state));
                        if (changed && "ACTIVATE".equals(action)) {
                            firstActivationInBridge.countDown();
                            secondActivationFinished.await(3, TimeUnit.SECONDS);
                        }
                        ObjectNode acknowledgement = runtimeAcknowledgement(analyzer,
                                fixture.revision().getProfilePin(), commandId, action, state, bridgeCommands.size());
                        acknowledgement.put("outcome", changed ? "APPLIED" : "ALREADY_APPLIED");
                        return acknowledgement;
                    });

            AuditTrailServiceImpl auditTrailService = new AuditTrailServiceImpl();
            ReflectionTestUtils.setField(auditTrailService, "referenceTablesService", referenceTablesService);
            ReflectionTestUtils.setField(auditTrailService, "historyService", historyService);
            AnalyzerActivationService activationService = new AnalyzerActivationServiceImpl(analyzerService,
                    profileCatalogService, mappingService, confirmationService, testSectionService, bridgeClient,
                    new AnalyzerActivationRecordServiceImpl(activationRecordDAO, auditTrailService),
                    java.time.Clock.systemUTC(), () -> UUID.randomUUID().toString(),
                    () -> UUID.randomUUID().toString());
            Callable<String> activation = () -> {
                try {
                    AnalyzerActivationResult result = transaction
                            .execute(status -> activationService.activate(analyzer.getId(), TEST_SYS_USER_ID));
                    return result.activated() ? "activated" : "blocked " + result.blockers();
                } catch (RuntimeException exception) {
                    return exception.getClass().getSimpleName();
                }
            };

            ExecutorService executor = Executors.newFixedThreadPool(2);
            String firstOutcome;
            String secondOutcome;
            try {
                Future<String> first = executor.submit(activation);
                assertTrue("The first activation never reached the Bridge",
                        firstActivationInBridge.await(30, TimeUnit.SECONDS));
                Future<String> second = executor.submit(() -> {
                    try {
                        return activation.call();
                    } finally {
                        secondActivationFinished.countDown();
                    }
                });
                firstOutcome = first.get(60, TimeUnit.SECONDS);
                secondOutcome = second.get(60, TimeUnit.SECONDS);
            } finally {
                executor.shutdownNow();
            }

            assertEquals("The Bridge must not be told to deactivate a runtime recorded as active",
                    List.of("ACTIVATE", "ACTIVATE"), bridgeCommands);
            assertEquals("ACTIVE", runtimeState.get());
            assertEquals("activated", firstOutcome);
            assertEquals("activated", secondOutcome);
            assertEquals(Analyzer.AnalyzerStatus.ACTIVE,
                    transaction.execute(status -> analyzerDAO.get(analyzer.getId()).orElseThrow()).getStatus());
        } finally {
            deleteAnalyzer(transaction, analyzer.getId());
        }
    }

    private record ActivationRaceFixture(Analyzer analyzer, AnalyzerMapping revision,
            AnalyzerMappingConfirmation confirmation) {
    }

    private record MappingSelectionFixture(String analyzerId, String reviewedMappingId, String reviewedFingerprint) {
    }

    private record ProbeFixture(String analyzerId, String connectionId, String profileId) {
    }

    private AuditTrailServiceImpl auditTrail() {
        AuditTrailServiceImpl auditTrailService = new AuditTrailServiceImpl();
        ReflectionTestUtils.setField(auditTrailService, "referenceTablesService", referenceTablesService);
        ReflectionTestUtils.setField(auditTrailService, "historyService", historyService);
        return auditTrailService;
    }

    /**
     * The real mapping service over the real DAOs, with the Bridge catalog serving
     * the one fixture profile.
     */
    private AnalyzerMappingService mappingService(TestService testService, TestResultService testResultService,
            AnalyzerMappingCatalogService mappingCatalogService, AuditTrailServiceImpl auditTrailService,
            String profileId) {
        BridgeProfileCatalogService profileCatalogService = mock(BridgeProfileCatalogService.class);
        when(profileCatalogService.getCatalog()).thenReturn(new BridgeProfileCatalog("1.0", PROFILE_FINGERPRINT,
                List.of(new BridgeProfileCatalog.ProfileRevision(profile(profileId),
                        new ObjectMapper().createObjectNode()))));
        return new AnalyzerMappingServiceImpl(mappingDAO, mappingTestDAO, mappingResultDAO, auditTrailService,
                testService, testResultService, mock(TestResultComponentService.class),
                new AnalyzerMappingDefaults(mappingCatalogService, testResultService), profileCatalogService);
    }

    private Analyzer insertAnalyzer(String name, Analyzer.AnalyzerStatus status) {
        Analyzer analyzer = new Analyzer();
        analyzer.ensureFhirUuid();
        analyzer.setName(name);
        analyzer.setStatus(status);
        analyzer.setActive(false);
        analyzer.setTestUnitIds(List.of("1"));
        analyzer.setSysUserId(TEST_SYS_USER_ID);
        analyzerDAO.insert(analyzer);
        return analyzer;
    }

    private AnalyzerMapping insertMapping(Analyzer analyzer, int revisionNumber, String profileId, String fingerprint) {
        return insertMapping(analyzer, revisionNumber, profileId, 1, PROFILE_FINGERPRINT, fingerprint);
    }

    private AnalyzerMapping insertMapping(Analyzer analyzer, int revisionNumber, String profileId, int profileRevision,
            String profileFingerprint, String fingerprint) {
        AnalyzerMapping revision = new AnalyzerMapping();
        revision.setAnalyzer(analyzer);
        revision.setRevisionNumber(revisionNumber);
        revision.setProfileId(profileId);
        revision.setProfileRevision(profileRevision);
        revision.setProfileFingerprint(profileFingerprint);
        revision.setMappingFingerprint(fingerprint);
        revision.setCreatedBy(TEST_SYS_USER_ID);
        revision.setSysUserId(TEST_SYS_USER_ID);
        mappingDAO.insert(revision);
        return revision;
    }

    private AnalyzerMappingConfirmation insertConfirmation(AnalyzerMapping revision, String profileId) {
        AnalyzerMappingConfirmation confirmation = new AnalyzerMappingConfirmation();
        confirmation.setMapping(revision);
        confirmation.setProfileId(profileId);
        confirmation.setProfileRevision(1);
        confirmation.setProfileRevisionFingerprint(PROFILE_FINGERPRINT);
        confirmation.setMappingFingerprint(revision.getMappingFingerprint());
        confirmation.setRecognitionFingerprint(RECOGNITION_FINGERPRINT);
        confirmation.setConfirmedRowsJson("[]");
        confirmation.setExcludedRowsJson("[]");
        confirmation.setConfirmedBy(TEST_SYS_USER_ID);
        confirmation.setSysUserId(TEST_SYS_USER_ID);
        confirmationDAO.insert(confirmation);
        return confirmation;
    }

    /**
     * An analyzer and its mapping point at each other, so the references are
     * cleared before either is deleted.
     */
    private void deleteAnalyzer(TransactionTemplate transaction, String analyzerId) {
        transaction.executeWithoutResult(status -> {
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            Long id = Long.valueOf(analyzerId);
            jdbc.update("UPDATE analyzer SET latest_activation_record_id = NULL, mapping_id = NULL WHERE id = ?", id);
            jdbc.update("DELETE FROM analyzer_activation_record WHERE analyzer_id = ?", id);
            String mappingIds = "(SELECT id FROM analyzer_mapping WHERE analyzer_id = ?)";
            jdbc.update("DELETE FROM analyzer_mapping_confirmation WHERE mapping_id IN " + mappingIds, id);
            jdbc.update("DELETE FROM analyzer_mapping_result WHERE mapping_id IN " + mappingIds, id);
            jdbc.update("DELETE FROM analyzer_mapping_test WHERE mapping_id IN " + mappingIds, id);
            jdbc.update("UPDATE analyzer_mapping SET supersedes_mapping_id = NULL WHERE analyzer_id = ?", id);
            jdbc.update("DELETE FROM analyzer_mapping WHERE analyzer_id = ?", id);
            jdbc.update("DELETE FROM analyzer WHERE id = ?", id);
        });
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
                          "values":["POS"],
                          "value_codes":{"POS":[{"system":"http://loinc.org","code":"LA6576-8"}]}
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

    private static ObjectNode runtimeAcknowledgement(Analyzer analyzer, AnalyzerProfilePin profile, String commandId,
            int runtimeRevision) {
        return runtimeAcknowledgement(analyzer, profile, commandId, "ACTIVATE", "ACTIVE", runtimeRevision);
    }

    private static ObjectNode runtimeAcknowledgement(Analyzer analyzer, AnalyzerProfilePin profile, String commandId,
            String action, String runtimeState, int runtimeRevision) {
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

    private static ObjectNode connectionDocument(Analyzer analyzer, AnalyzerProfilePin profile) {
        ObjectNode connection = new ObjectMapper().createObjectNode();
        connection.put("schemaVersion", "1.0");
        connection.put("connectionId", analyzer.getBridgeConnectionId());
        connection.put("clientAnalyzerId", analyzer.getId());
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
