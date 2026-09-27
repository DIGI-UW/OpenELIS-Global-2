package org.openelisglobal.analyzerimport.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.StringType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzer.service.AnalyzerInstanceLocalStateService;
import org.openelisglobal.analyzer.service.AnalyzerSiteBindingConfirmationRequest;
import org.openelisglobal.analyzer.service.AnalyzerSiteBindingConfirmationService;
import org.openelisglobal.analyzer.service.AnalyzerSiteBindingConfirmationView;
import org.openelisglobal.analyzer.service.AnalyzerSiteBindingDraft;
import org.openelisglobal.analyzer.service.AnalyzerSiteBindingResultDraft;
import org.openelisglobal.analyzer.service.AnalyzerSiteBindingService;
import org.openelisglobal.analyzer.service.AnalyzerSiteBindingSnapshot;
import org.openelisglobal.analyzer.service.AnalyzerSiteBindingSourceRow;
import org.openelisglobal.analyzer.service.AnalyzerSiteBindingTestDraft;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingMappingState;
import org.openelisglobal.analyzerresults.service.AnalyzerResultsService;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.audittrail.daoimpl.AuditTrailServiceImpl;
import org.openelisglobal.history.service.HistoryService;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

public class AnalyzerNormalizedResultImportIntegrationTest extends BaseWebContextSensitiveTest {

    private static final long PROFILE_BINDING_ID = 98401L;
    private static final long SITE_BINDING_ID = 98402L;
    private static final long SITE_BINDING_REVISION_ID = 98403L;
    private static final long ANALYZER_ID = 98404L;
    private static final long TEST_ID = 98405L;
    private static final long RESULT_OPTION_ID = 98406L;
    private static final String QC_LOT_ID = "receipt-qc-lot";
    private static final String CONNECTION_ID = "bridge-connection-7f3c";
    private static final String ACCESSION = "ACC-UNKNOWN-TEST-001";
    private static final Path FIXTURE = Path.of("tools", "openelis-analyzer-bridge", "contracts", "analyzer", "v1",
            "fixtures", "normalized-unknown-test.fhir.json");
    private static final FhirContext REAL_FHIR = FhirContext.forR4();

    @Autowired
    private AnalyzerNormalizedResultImportService importService;
    @Autowired
    private DataSource dataSource;
    @Autowired
    private FhirContext fhirContext;

    @Autowired
    private AnalyzerSiteBindingService bindings;
    @Autowired
    private AnalyzerSiteBindingConfirmationService confirmations;
    @Autowired
    private AnalyzerInstanceLocalStateService localState;
    @Autowired
    private AnalyzerResultsService resultsService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private HistoryService historyService;
    @Autowired
    private ReferenceTablesService referenceTablesService;
    private Object confirmationTarget;
    private Object previousAuditTrail;
    private JdbcTemplate jdbc;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        when(fhirContext.newJsonParser()).thenAnswer(invocation -> REAL_FHIR.newJsonParser());
        // Confirmation is valid only with a durable audit event; AppTestConfig mocks
        // audit.
        var audit = new AuditTrailServiceImpl();
        ReflectionTestUtils.setField(audit, "historyService", historyService);
        ReflectionTestUtils.setField(audit, "referenceTablesService", referenceTablesService);
        confirmationTarget = AopTestUtils.getUltimateTargetObject(confirmations);
        previousAuditTrail = ReflectionTestUtils.getField(confirmationTarget, "auditTrailService");
        ReflectionTestUtils.setField(confirmationTarget, "auditTrailService", audit);
        jdbc = new JdbcTemplate(dataSource);
        cleanup();
        jdbc.update(
                "INSERT INTO clinlims.analyzer_profile_binding"
                        + " (id, profile_id, profile_revision, profile_fingerprint, last_updated)"
                        + " VALUES (?, 'site.unknown-capable', 3, ?, NOW())",
                PROFILE_BINDING_ID, "sha256:" + "1".repeat(64));
        jdbc.update("INSERT INTO clinlims.analyzer_site_binding"
                + " (id, profile_binding_id, created_by, created_at, last_updated) VALUES (?, ?, '1', NOW(), NOW())",
                SITE_BINDING_ID, PROFILE_BINDING_ID);
        jdbc.update("INSERT INTO clinlims.analyzer_site_binding_revision"
                + " (id, site_binding_id, revision_number, binding_fingerprint, created_by, created_at, last_updated)"
                + " VALUES (?, ?, 1, ?, '1', NOW(), NOW())", SITE_BINDING_REVISION_ID, SITE_BINDING_ID,
                "sha256:" + "2".repeat(64));
        jdbc.update(
                "INSERT INTO clinlims.analyzer"
                        + " (id, name, is_active, bridge_connection_id, site_binding_revision_id, last_updated)"
                        + " VALUES (?, 'Normalized import test', true, ?, ?, NOW())",
                ANALYZER_ID, CONNECTION_ID, SITE_BINDING_REVISION_ID);
    }

    @After
    public void tearDown() {
        try {
            cleanup();
        } finally {
            if (confirmationTarget != null) {
                ReflectionTestUtils.setField(confirmationTarget, "auditTrailService", previousAuditTrail);
            }
        }
    }

    @Test
    public void controlReplayDoesNotCreateAnotherOperationalQcResultAfterStagingRemoval() throws Exception {
        Bundle bundle = prepareControl(true);
        AnalyzerNormalizedResultImportSummary accepted = importService.importBundle(bundle, "1");
        assertEquals(1, accepted.controlResultsProcessed());
        jdbc.update("DELETE FROM clinlims.analyzer_results WHERE analyzer_id = ?", ANALYZER_ID);
        assertEquals(accepted, importService.importBundle(bundle, "1"));
        assertEquals(Integer.valueOf(1), jdbc.queryForObject(
                "SELECT COUNT(*) FROM clinlims.qc_result WHERE control_lot_id = ?", Integer.class, QC_LOT_ID));
        assertEquals(Integer.valueOf(0), jdbc.queryForObject(
                "SELECT COUNT(*) FROM clinlims.analyzer_results WHERE analyzer_id = ?", Integer.class, ANALYZER_ID));
    }

    @Test
    public void qcFailureRollsBackStagingAndReceiptAndAllowsASuccessfulRetry() throws Exception {
        Bundle bundle = prepareControl(false);
        assertThrows(IllegalArgumentException.class, () -> importService.importBundle(bundle, "1"));
        assertEquals(Integer.valueOf(0), jdbc.queryForObject(
                "SELECT COUNT(*) FROM clinlims.analyzer_results WHERE analyzer_id = ?", Integer.class, ANALYZER_ID));
        assertEquals(Integer.valueOf(0),
                jdbc.queryForObject("SELECT COUNT(*) FROM clinlims.analyzer_delivery_receipt WHERE connection_id = ?",
                        Integer.class, CONNECTION_ID));
        addQcStatistics();
        assertEquals(1, importService.importBundle(bundle, "1").controlResultsProcessed());
        assertEquals(Integer.valueOf(1), jdbc.queryForObject(
                "SELECT COUNT(*) FROM clinlims.qc_result WHERE control_lot_id = ?", Integer.class, QC_LOT_ID));
    }

    @Test
    public void simultaneousCopiesCommitOnlyOneReceiptAndStagingRow() throws Exception {
        String payload = Files.readString(FIXTURE);
        CountDownLatch start = new CountDownLatch(1);
        try (var workers = Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<AnalyzerNormalizedResultImportSummary> delivery = () -> {
                if (!start.await(10, TimeUnit.SECONDS))
                    throw new IllegalStateException("Delivery barrier timed out");
                return importService.importBundle(REAL_FHIR.newJsonParser().parseResource(Bundle.class, payload), "1");
            };
            var first = workers.submit(delivery);
            var second = workers.submit(delivery);
            start.countDown();
            assertEquals(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS));
        }
        assertEquals(Integer.valueOf(1),
                jdbc.queryForObject("SELECT COUNT(*) FROM clinlims.analyzer_delivery_receipt WHERE connection_id = ?",
                        Integer.class, CONNECTION_ID));
        assertEquals(Integer.valueOf(1), jdbc.queryForObject(
                "SELECT COUNT(*) FROM clinlims.analyzer_results WHERE analyzer_id = ?", Integer.class, ANALYZER_ID));
    }

    @Test
    public void aNewMessageIdProducesADistinctReceipt() throws Exception {
        Bundle bundle = REAL_FHIR.newJsonParser().parseResource(Bundle.class, Files.readString(FIXTURE));
        importService.importBundle(bundle, "1");
        bundle.getIdentifier().setValue("second-delivery");
        importService.importBundle(bundle, "1");
        assertEquals(Integer.valueOf(2),
                jdbc.queryForObject("SELECT COUNT(*) FROM clinlims.analyzer_delivery_receipt WHERE connection_id = ?",
                        Integer.class, CONNECTION_ID));
    }

    @Test
    public void invalidProfileDoesNotCommitAReceipt() throws Exception {
        Bundle bundle = REAL_FHIR.newJsonParser().parseResource(Bundle.class, Files.readString(FIXTURE));
        jdbc.update("UPDATE clinlims.analyzer_profile_binding SET profile_revision = 4 WHERE id = ?",
                PROFILE_BINDING_ID);
        assertThrows(AnalyzerNormalizedResultImportException.class, () -> importService.importBundle(bundle, "1"));
        assertEquals(Integer.valueOf(0),
                jdbc.queryForObject("SELECT COUNT(*) FROM clinlims.analyzer_delivery_receipt WHERE connection_id = ?",
                        Integer.class, CONNECTION_ID));
    }

    @Test
    public void acceptedDeliveryIsNotRestagedAfterStagingRowsAreRemoved() throws Exception {
        Bundle bundle = REAL_FHIR.newJsonParser().parseResource(Bundle.class, Files.readString(FIXTURE));
        AnalyzerNormalizedResultImportSummary accepted = importService.importBundle(bundle, "1");
        jdbc.update("DELETE FROM clinlims.analyzer_results WHERE analyzer_id = ?", ANALYZER_ID);
        jdbc.update("UPDATE clinlims.analyzer_profile_binding SET profile_revision = 4 WHERE id = ?",
                PROFILE_BINDING_ID);

        AnalyzerNormalizedResultImportSummary replay = importService.importBundle(bundle, "1");

        assertEquals(accepted, replay);
        assertEquals(Integer.valueOf(0), jdbc.queryForObject(
                "SELECT COUNT(*) FROM clinlims.analyzer_results WHERE analyzer_id = ?", Integer.class, ANALYZER_ID));
    }

    @Test
    public void unknownResultIsHeldWithExactBridgeSourceEvidence() throws Exception {
        Bundle bundle = REAL_FHIR.newJsonParser().parseResource(Bundle.class, Files.readString(FIXTURE));

        AnalyzerNormalizedResultImportSummary summary = importService.importBundle(bundle, "1");

        assertEquals(String.valueOf(ANALYZER_ID), summary.analyzerId());
        assertEquals(1, summary.resultsStaged());
        assertEquals(1, summary.resultsHeld());
        assertEquals(AnalyzerResults.IMPORT_ISSUE_UNKNOWN_TEST,
                jdbc.queryForObject(
                        "SELECT import_issue_reason FROM clinlims.analyzer_results"
                                + " WHERE analyzer_id = ? AND accession_number = ?",
                        String.class, ANALYZER_ID, ACCESSION));
        assertEquals(CONNECTION_ID,
                jdbc.queryForObject(
                        "SELECT source_connection_id FROM clinlims.analyzer_results"
                                + " WHERE analyzer_id = ? AND accession_number = ?",
                        String.class, ANALYZER_ID, ACCESSION));
        assertEquals("VENDOR-NEW-42",
                jdbc.queryForObject(
                        "SELECT raw_test_code FROM clinlims.analyzer_results"
                                + " WHERE analyzer_id = ? AND accession_number = ?",
                        String.class, ANALYZER_ID, ACCESSION));
        assertEquals("PATIENT",
                jdbc.queryForObject(
                        "SELECT result_classification FROM clinlims.analyzer_results"
                                + " WHERE analyzer_id = ? AND accession_number = ?",
                        String.class, ANALYZER_ID, ACCESSION));
        assertNotNull(jdbc.queryForObject("SELECT source_payload FROM clinlims.analyzer_results"
                + " WHERE analyzer_id = ? AND accession_number = ?", String.class, ANALYZER_ID, ACCESSION));
    }

    @Test
    public void heldResultRecoversInPlaceOnlyAfterExplicitAdoptionAndDoesNotReplay() throws Exception {
        Bundle bundle = REAL_FHIR.newJsonParser().parseResource(Bundle.class, Files.readString(FIXTURE));
        AnalyzerNormalizedResultImportSummary receipt = importService.importBundle(bundle, "1");
        assertEquals(1, receipt.resultsHeld());
        AnalyzerResults original = resultsService.getResultsbyAnalyzer(String.valueOf(ANALYZER_ID)).get(0);
        String id = original.getId();
        String payload = original.getSourcePayload();
        assertTrue(original.isReadOnly());

        // Seed the local clinical catalog, then use real mapping/confirmation/adoption
        // services.
        jdbc.update(
                "INSERT INTO clinlims.test (id, guid, name, description, is_active, is_reportable, orderable, lastupdated)"
                        + " VALUES (?, ?, 'Recovery test', 'Recovery test', 'Y', 'Y', true, NOW())",
                TEST_ID, UUID.randomUUID().toString());
        AnalyzerSiteBindingSnapshot previous = bindings.findByRevisionId(String.valueOf(SITE_BINDING_REVISION_ID))
                .orElseThrow();
        AnalyzerSiteBindingSnapshot updated = bindings.appendRevision(previous.binding(),
                new AnalyzerSiteBindingDraft(List.of(new AnalyzerSiteBindingTestDraft(original.getRawTestCode(),
                        AnalyzerSiteBindingMappingState.BOUND, String.valueOf(TEST_ID))), List.of()),
                "1");
        confirm(updated, bundle);
        assertEquals(0, importService.recoverHeldMappingResults(String.valueOf(ANALYZER_ID), "1"));
        assertTrue(resultsService.get(id).isReadOnly());

        localState.selectSiteBindingRevision(String.valueOf(ANALYZER_ID), updated.binding().getId(),
                updated.revision().getRevisionNumber(), updated.revision().getBindingFingerprint(), "1");
        AnalyzerResults recovered = resultsService.get(id);
        assertFalse(recovered.isReadOnly());
        assertNull(recovered.getImportIssueReason());
        assertEquals(String.valueOf(TEST_ID), recovered.getTestId());
        assertEquals(original.getRawResultValue(), recovered.getResult());
        assertEquals(payload, recovered.getSourcePayload());
        assertEquals(original.getSourceMessageId(), recovered.getSourceMessageId());
        assertEquals(0, importService.recoverHeldMappingResults(String.valueOf(ANALYZER_ID), "1"));
        assertEquals(receipt, importService.importBundle(bundle, "1"));
        assertEquals(1, resultsService.getResultsbyAnalyzer(String.valueOf(ANALYZER_ID)).size());
    }

    @Test
    public void mappedAndUnknownObservationsInOneDeliveryAreRetainedIndependently() throws Exception {
        bindTest("KNOWN");
        Bundle bundle = REAL_FHIR.newJsonParser().parseResource(Bundle.class, Files.readString(FIXTURE));
        Observation unknown = bundle.getEntry().stream().map(entry -> entry.getResource())
                .filter(Observation.class::isInstance).map(Observation.class::cast).findFirst().orElseThrow();
        Observation known = unknown.copy();
        known.getCode().getCoding().stream().filter(code -> code.getSystem().endsWith("analyzer-raw-code"))
                .forEach(code -> code.setCode("KNOWN"));
        bundle.addEntry().setFullUrl("urn:uuid:known-result").setResource(known);
        confirm(bindings.findByRevisionId(String.valueOf(SITE_BINDING_REVISION_ID)).orElseThrow(), bundle);
        AnalyzerNormalizedResultImportSummary receipt = importService.importBundle(bundle, "1");
        assertEquals(2, receipt.resultsStaged());
        assertEquals(1, receipt.resultsHeld());
        List<AnalyzerResults> rows = resultsService.getResultsbyAnalyzer(String.valueOf(ANALYZER_ID));
        assertEquals(2, rows.size());
        assertEquals(1, rows.stream().filter(row -> !row.isReadOnly()).count());
        AnalyzerResults held = rows.stream().filter(AnalyzerResults::isReadOnly).findFirst().orElseThrow();
        assertEquals("VENDOR-NEW-42", held.getRawTestCode());
        assertEquals(AnalyzerResults.IMPORT_ISSUE_UNKNOWN_TEST, held.getImportIssueReason());
        assertEquals(receipt, importService.importBundle(bundle, "1"));
        assertEquals(2, resultsService.getResultsbyAnalyzer(String.valueOf(ANALYZER_ID)).size());
    }

    @Test
    public void unresolvedQualitativeValueRecoversWithRawValuePreservedAfterAdoption() throws Exception {
        bindTest("VENDOR-NEW-42");
        jdbc.update("INSERT INTO clinlims.test_result"
                + " (id, test_id, tst_rslt_type, value, is_active, sort_order, lastupdated)"
                + " VALUES (?, ?, 'D', 'Positive', true, 1, NOW())", RESULT_OPTION_ID, TEST_ID);
        var originalBinding = bindings.findByRevisionId(String.valueOf(SITE_BINDING_REVISION_ID)).orElseThrow();
        var partial = bindings.appendRevision(originalBinding.binding(),
                new AnalyzerSiteBindingDraft(
                        List.of(new AnalyzerSiteBindingTestDraft("VENDOR-NEW-42", AnalyzerSiteBindingMappingState.BOUND,
                                String.valueOf(TEST_ID))),
                        List.of(new AnalyzerSiteBindingResultDraft("VENDOR-NEW-42", "DETECTED",
                                AnalyzerSiteBindingMappingState.UNRESOLVED, null))),
                "1");
        Bundle bundle = REAL_FHIR.newJsonParser().parseResource(Bundle.class, Files.readString(FIXTURE));
        Observation observation = bundle.getEntry().stream().map(entry -> entry.getResource())
                .filter(Observation.class::isInstance).map(Observation.class::cast).findFirst().orElseThrow();
        observation.setValue(new StringType("DETECTED"));
        confirm(partial, bundle);
        localState.selectSiteBindingRevision(String.valueOf(ANALYZER_ID), partial.binding().getId(),
                partial.revision().getRevisionNumber(), partial.revision().getBindingFingerprint(), "1");
        var receipt = importService.importBundle(bundle, "1");
        assertEquals(1, receipt.resultsHeld());
        AnalyzerResults held = resultsService.getResultsbyAnalyzer(String.valueOf(ANALYZER_ID)).get(0);
        assertEquals(AnalyzerResults.IMPORT_ISSUE_RESULT_MAPPING_NOT_READY, held.getImportIssueReason());

        var corrected = bindings.appendRevision(partial.binding(),
                new AnalyzerSiteBindingDraft(
                        List.of(new AnalyzerSiteBindingTestDraft("VENDOR-NEW-42", AnalyzerSiteBindingMappingState.BOUND,
                                String.valueOf(TEST_ID))),
                        List.of(new AnalyzerSiteBindingResultDraft("VENDOR-NEW-42", "DETECTED",
                                AnalyzerSiteBindingMappingState.BOUND, String.valueOf(RESULT_OPTION_ID)))),
                "1");
        confirm(corrected, bundle);
        assertEquals(0, importService.recoverHeldMappingResults(String.valueOf(ANALYZER_ID), "1"));
        localState.selectSiteBindingRevision(String.valueOf(ANALYZER_ID), corrected.binding().getId(),
                corrected.revision().getRevisionNumber(), corrected.revision().getBindingFingerprint(), "1");
        AnalyzerResults recovered = resultsService.get(held.getId());
        assertFalse(recovered.isReadOnly());
        assertEquals("Positive", recovered.getResult());
        assertEquals("D", recovered.getResultType());
        assertEquals("DETECTED", recovered.getRawResultValue());
        assertEquals(held.getSourcePayload(), recovered.getSourcePayload());
        assertEquals(0, importService.recoverHeldMappingResults(String.valueOf(ANALYZER_ID), "1"));
        assertEquals(receipt, importService.importBundle(bundle, "1"));
        assertEquals(1, resultsService.getResultsbyAnalyzer(String.valueOf(ANALYZER_ID)).size());
    }

    private void confirm(AnalyzerSiteBindingSnapshot candidate, Bundle bundle) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            AnalyzerSiteBindingSnapshot binding = bindings.findByRevisionId(candidate.revision().getId()).orElseThrow();
            Observation observation = bundle.getEntry().stream().map(entry -> entry.getResource())
                    .filter(Observation.class::isInstance).map(Observation.class::cast).findFirst().orElseThrow();
            String fingerprint = observation
                    .getExtensionByUrl(
                            "https://openelis-global.org/fhir/StructureDefinition/analyzer-control-recognition")
                    .getExtensionByUrl("recognitionFingerprint").getValue().primitiveValue();
            var tests = new ArrayList<AnalyzerSiteBindingSourceRow>();
            binding.tests().stream().filter(row -> row.getMappingState() == AnalyzerSiteBindingMappingState.BOUND)
                    .map(row -> new AnalyzerSiteBindingSourceRow(row.getId().getSourceRowKey(), null))
                    .forEach(tests::add);
            binding.results().stream().filter(row -> row.getMappingState() == AnalyzerSiteBindingMappingState.BOUND)
                    .map(row -> new AnalyzerSiteBindingSourceRow(row.getId().getSourceRowKey(),
                            row.getId().getRawValue()))
                    .forEach(tests::add);
            assertEquals(AnalyzerSiteBindingConfirmationView.State.CURRENT,
                    confirmations
                            .confirm(binding, fingerprint,
                                    new AnalyzerSiteBindingConfirmationRequest(
                                            binding.revision().getBindingFingerprint(), fingerprint, tests, List.of()),
                                    "1")
                            .state());
            assertTrue(confirmations.assessCurrent(binding, fingerprint).currentConfirmation().isPresent());
        });
    }

    private void bindTest(String sourceCode) {
        jdbc.update(
                "INSERT INTO clinlims.test (id, guid, name, description, is_active, is_reportable, orderable, lastupdated)"
                        + " VALUES (?, ?, 'Receipt test', 'Receipt test', 'Y', 'Y', true, NOW())",
                TEST_ID, UUID.randomUUID().toString());
        jdbc.update("INSERT INTO clinlims.analyzer_site_binding_test"
                + " (site_binding_revision_id, source_row_key, mapping_state, test_id, last_updated)"
                + " VALUES (?, ?, 'BOUND', ?, NOW())", SITE_BINDING_REVISION_ID, sourceCode, TEST_ID);
    }

    private Bundle prepareControl(boolean withStatistics) throws Exception {
        bindTest("WBC");
        jdbc.update(
                "UPDATE clinlims.analyzer_profile_binding SET profile_id = 'site.mock-hematology', profile_revision = 1 WHERE id = ?",
                PROFILE_BINDING_ID);
        jdbc.update("INSERT INTO clinlims.qc_control_lot"
                + " (id, fhir_uuid, product_name, lot_number, manufacturer, control_level, test_id, instrument_id,"
                + " calculation_method, initial_runs_count, manufacturer_mean, manufacturer_std_dev, activation_date,"
                + " expiration_date, status, sys_user_id, last_updated)"
                + " VALUES (?, ?::uuid, 'Receipt control', 'LOT-WBC-2026-08', 'Test', 'NORMAL', ?, ?,"
                + " 'INITIAL_RUNS', 20, 7.1, 1, NOW(), '2099-12-31', 'ACTIVE', 1, NOW())", QC_LOT_ID,
                UUID.randomUUID().toString(), TEST_ID, ANALYZER_ID);
        if (withStatistics)
            addQcStatistics();
        Bundle bundle = REAL_FHIR.newJsonParser().parseResource(Bundle.class,
                Files.readString(FIXTURE.resolveSibling("normalized-qc.fhir.json")));
        confirm(bindings.findByRevisionId(String.valueOf(SITE_BINDING_REVISION_ID)).orElseThrow(), bundle);
        return bundle;
    }

    private void addQcStatistics() {
        jdbc.update("INSERT INTO clinlims.qc_statistics"
                + " (id, control_lot_id, calculation_date, mean, standard_deviation, num_values, calculation_method,"
                + " validity_start, sys_user_id, last_updated) VALUES ('receipt-qc-stats', ?, NOW(), 7.1, 1, 20,"
                + " 'INITIAL_RUNS', NOW(), 1, NOW())", QC_LOT_ID);
    }

    private void cleanup() {
        if (jdbc == null) {
            return;
        }
        jdbc.update("DELETE FROM clinlims.analyzer_results WHERE analyzer_id = ?", ANALYZER_ID);
        jdbc.update("DELETE FROM clinlims.qc_result WHERE control_lot_id = ?", QC_LOT_ID);
        jdbc.update("DELETE FROM clinlims.qc_statistics WHERE control_lot_id = ?", QC_LOT_ID);
        jdbc.update("DELETE FROM clinlims.qc_control_lot WHERE id = ?", QC_LOT_ID);
        jdbc.update("DELETE FROM clinlims.qc_control_lot WHERE id = 'receipt-qc-other'");
        jdbc.update(
                "DELETE FROM clinlims.analyzer_site_binding_confirmation WHERE site_binding_revision_id IN"
                        + " (SELECT id FROM clinlims.analyzer_site_binding_revision WHERE site_binding_id = ?)",
                SITE_BINDING_ID);
        jdbc.update(
                "DELETE FROM clinlims.analyzer_site_binding_result WHERE site_binding_revision_id IN"
                        + " (SELECT id FROM clinlims.analyzer_site_binding_revision WHERE site_binding_id = ?)",
                SITE_BINDING_ID);
        jdbc.update(
                "DELETE FROM clinlims.analyzer_site_binding_test WHERE site_binding_revision_id IN"
                        + " (SELECT id FROM clinlims.analyzer_site_binding_revision WHERE site_binding_id = ?)",
                SITE_BINDING_ID);
        jdbc.update("DELETE FROM clinlims.test_result WHERE id = ?", RESULT_OPTION_ID);
        jdbc.update("DELETE FROM clinlims.test WHERE id = ?", TEST_ID);
        jdbc.update("DELETE FROM clinlims.analyzer_delivery_receipt WHERE analyzer_id = ?", ANALYZER_ID);
        jdbc.update("DELETE FROM clinlims.analyzer WHERE id = ?", ANALYZER_ID);
        jdbc.update("DELETE FROM clinlims.analyzer_site_binding_revision WHERE site_binding_id = ?", SITE_BINDING_ID);
        jdbc.update("DELETE FROM clinlims.analyzer_site_binding WHERE id = ?", SITE_BINDING_ID);
        jdbc.update("DELETE FROM clinlims.analyzer_profile_binding WHERE id = ?", PROFILE_BINDING_ID);
    }
}
