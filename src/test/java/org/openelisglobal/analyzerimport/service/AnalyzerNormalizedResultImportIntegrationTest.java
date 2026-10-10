package org.openelisglobal.analyzerimport.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
import org.openelisglobal.analyzer.service.AnalyzerMappingConfirmationRequest;
import org.openelisglobal.analyzer.service.AnalyzerMappingConfirmationService;
import org.openelisglobal.analyzer.service.AnalyzerMappingConfirmationView;
import org.openelisglobal.analyzer.service.AnalyzerMappingDraft;
import org.openelisglobal.analyzer.service.AnalyzerMappingResultDraft;
import org.openelisglobal.analyzer.service.AnalyzerMappingService;
import org.openelisglobal.analyzer.service.AnalyzerMappingSnapshot;
import org.openelisglobal.analyzer.service.AnalyzerMappingSourceRow;
import org.openelisglobal.analyzer.service.AnalyzerMappingTestDraft;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingState;
import org.openelisglobal.analyzerresults.service.AnalyzerResultsService;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

public class AnalyzerNormalizedResultImportIntegrationTest extends BaseWebContextSensitiveTest {

    private static final long PROFILE_BINDING_ID = 98401L;
    private static final long SITE_BINDING_ID = 98402L;
    private static final long SITE_BINDING_REVISION_ID = 98403L;
    private static final long ANALYZER_ID = 98404L;
    private static final long TEST_ID = 98405L;
    private static final long RESULT_OPTION_ID = 98406L;
    private static final long OTHER_TEST_ID = 98407L;
    private static final long THIRD_TEST_ID = 98408L;
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
    private AnalyzerMappingService bindings;
    @Autowired
    private AnalyzerMappingConfirmationService confirmations;
    @Autowired
    private AnalyzerInstanceLocalStateService localState;
    @Autowired
    private AnalyzerResultsService resultsService;
    @Autowired
    private AnalyzerDeliveryBundleService bundleService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private JdbcTemplate jdbc;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
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
        cleanup();
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
    public void heldControlRecoveryKeepsItsOriginalLotAndDoesNotDuplicateQc() throws Exception {
        Bundle bundle = prepareControl(true, false);
        AnalyzerNormalizedResultImportSummary receipt = importService.importBundle(bundle, "1");
        assertEquals(1, receipt.resultsHeld());
        AnalyzerResults held = resultsService.getResultsbyAnalyzer(String.valueOf(ANALYZER_ID)).get(0);
        String originalId = held.getId();
        assertTrue(held.getSourcePayload().contains("LOT-WBC-2026-08"));
        assertTrue(held.isReadOnly());
        assertEquals(Integer.valueOf(0), jdbc.queryForObject(
                "SELECT COUNT(*) FROM clinlims.qc_result WHERE control_lot_id = ?", Integer.class, QC_LOT_ID));

        AnalyzerMappingSnapshot binding = bindings.findByRevisionId(String.valueOf(SITE_BINDING_REVISION_ID))
                .orElseThrow();
        confirm(binding, bundle);
        assertEquals(1, importService.recoverHeldMappingResults(String.valueOf(ANALYZER_ID), "1"));

        AnalyzerResults recovered = resultsService.get(originalId);
        assertFalse(recovered.isReadOnly());
        assertTrue(recovered.getSourcePayload().contains("LOT-WBC-2026-08"));
        assertEquals(Integer.valueOf(1), jdbc.queryForObject(
                "SELECT COUNT(*) FROM clinlims.qc_result WHERE control_lot_id = ?", Integer.class, QC_LOT_ID));
        assertEquals(receipt, importService.importBundle(bundle, "1"));
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
    public void theDeliveryBundleStaysReadableAfterItsStagedResultsAreReviewed() throws Exception {
        Bundle bundle = REAL_FHIR.newJsonParser().parseResource(Bundle.class, Files.readString(FIXTURE));
        AnalyzerNormalizedResultImportSummary accepted = importService.importBundle(bundle, "1");
        String messageId = jdbc.queryForObject("SELECT message_id FROM clinlims.analyzer_delivery_receipt WHERE id = ?",
                String.class, accepted.receiptId());

        jdbc.update("DELETE FROM clinlims.analyzer_results WHERE analyzer_id = ?", ANALYZER_ID);

        String json = bundleService.getBundle(accepted.receiptId()).orElseThrow();
        Bundle kept = REAL_FHIR.newJsonParser().parseResource(Bundle.class, json);
        assertEquals(bundle.getEntry().size(), kept.getEntry().size());
        assertEquals(accepted.receiptId(), bundleService.findReceiptId(CONNECTION_ID, messageId).orElseThrow());
        assertEquals(Optional.empty(), bundleService.getBundle("no-such-receipt"));
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
        AnalyzerMappingSnapshot previous = bindings.findByRevisionId(String.valueOf(SITE_BINDING_REVISION_ID))
                .orElseThrow();
        AnalyzerMappingSnapshot updated = bindings.appendRevision(previous.binding(),
                new AnalyzerMappingDraft(List.of(new AnalyzerMappingTestDraft(original.getRawTestCode(),
                        AnalyzerMappingState.BOUND, String.valueOf(TEST_ID))), List.of()),
                "1");
        assertThrows(IllegalArgumentException.class,
                () -> localState.selectSiteBindingRevision(String.valueOf(ANALYZER_ID), updated.binding().getId(),
                        updated.revision().getRevisionNumber(), updated.revision().getBindingFingerprint(), "1"));
        assertTrue("rejecting an unconfirmed revision must not release held results",
                resultsService.get(id).isReadOnly());

        confirm(updated, bundle);
        assertTrue("confirmation alone must not replay held results", resultsService.get(id).isReadOnly());
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
                new AnalyzerMappingDraft(
                        List.of(new AnalyzerMappingTestDraft("VENDOR-NEW-42", AnalyzerMappingState.BOUND,
                                String.valueOf(TEST_ID))),
                        List.of(new AnalyzerMappingResultDraft("VENDOR-NEW-42", "DETECTED",
                                AnalyzerMappingState.UNRESOLVED, null))),
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
                new AnalyzerMappingDraft(
                        List.of(new AnalyzerMappingTestDraft("VENDOR-NEW-42", AnalyzerMappingState.BOUND,
                                String.valueOf(TEST_ID))),
                        List.of(new AnalyzerMappingResultDraft("VENDOR-NEW-42", "DETECTED", AnalyzerMappingState.BOUND,
                                String.valueOf(RESULT_OPTION_ID)))),
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

    @Test
    public void inactiveTestHoldsOnlyItsObservationAndCanRecoverInPlace() throws Exception {
        assertCatalogChangeIsLocal(false);
    }

    @Test
    public void inactiveAnswerHoldsOnlyItsObservationAndCanRecoverInPlace() throws Exception {
        assertCatalogChangeIsLocal(true);
    }

    private void assertCatalogChangeIsLocal(boolean deactivateAnswer) throws Exception {
        bindTest("A", TEST_ID);
        bindTest("B", OTHER_TEST_ID);
        bindTest("C", THIRD_TEST_ID);
        if (deactivateAnswer) {
            jdbc.update("INSERT INTO clinlims.test_result"
                    + " (id, test_id, tst_rslt_type, value, is_active, sort_order, lastupdated)"
                    + " VALUES (?, ?, 'D', 'Positive', true, 1, NOW())", RESULT_OPTION_ID, THIRD_TEST_ID);
            jdbc.update("INSERT INTO clinlims.analyzer_site_binding_result"
                    + " (site_binding_revision_id, source_row_key, raw_value, mapping_state, test_result_id, last_updated)"
                    + " VALUES (?, 'C', 'DETECTED', 'BOUND', ?, NOW())", SITE_BINDING_REVISION_ID, RESULT_OPTION_ID);
        }
        Bundle bundle = REAL_FHIR.newJsonParser().parseResource(Bundle.class, Files.readString(FIXTURE));
        Observation first = bundle.getEntry().stream().map(entry -> entry.getResource())
                .filter(Observation.class::isInstance).map(Observation.class::cast).findFirst().orElseThrow();
        first.getCode().getCodingFirstRep().setCode("A");
        for (String code : List.of("B", "C")) {
            Observation other = first.copy();
            other.getCode().getCodingFirstRep().setCode(code);
            bundle.addEntry().setFullUrl("urn:uuid:result-" + code).setResource(other);
        }
        confirm(bindings.findByRevisionId(String.valueOf(SITE_BINDING_REVISION_ID)).orElseThrow(), bundle);
        if (deactivateAnswer) {
            jdbc.update("UPDATE clinlims.test_result SET is_active = false WHERE id = ?", RESULT_OPTION_ID);
        } else {
            jdbc.update("UPDATE clinlims.test SET is_active = 'N' WHERE id = ?", THIRD_TEST_ID);
        }

        var receipt = importService.importBundle(bundle, "1");
        assertEquals(3, receipt.resultsStaged());
        assertEquals("An invalid C mapping must not hold A and B", 1, receipt.resultsHeld());
        List<AnalyzerResults> rows = resultsService.getResultsbyAnalyzer(String.valueOf(ANALYZER_ID));
        assertEquals(3, rows.size());
        for (AnalyzerResults row : rows) {
            assertEquals(row.getRawTestCode().equals("C"), row.isReadOnly());
            assertEquals("DETECTED", row.getRawResultValue());
        }
        AnalyzerResults held = rows.stream().filter(AnalyzerResults::isReadOnly).findFirst().orElseThrow();
        assertEquals(deactivateAnswer ? AnalyzerResults.IMPORT_ISSUE_INVALID_RESULT_MAPPING
                : AnalyzerResults.IMPORT_ISSUE_TEST_MAPPING_NOT_READY, held.getImportIssueReason());
        String heldId = held.getId();
        String originalPayload = held.getSourcePayload();

        if (deactivateAnswer) {
            jdbc.update("UPDATE clinlims.test_result SET is_active = true WHERE id = ?", RESULT_OPTION_ID);
        } else {
            jdbc.update("UPDATE clinlims.test SET is_active = 'Y' WHERE id = ?", THIRD_TEST_ID);
        }
        assertEquals(1, importService.recoverHeldMappingResults(String.valueOf(ANALYZER_ID), "1"));
        AnalyzerResults recovered = resultsService.get(heldId);
        assertFalse(recovered.isReadOnly());
        assertEquals(String.valueOf(THIRD_TEST_ID), recovered.getTestId());
        assertEquals(deactivateAnswer ? "Positive" : "DETECTED", recovered.getResult());
        assertEquals(originalPayload, recovered.getSourcePayload());
        assertEquals(0, importService.recoverHeldMappingResults(String.valueOf(ANALYZER_ID), "1"));
        assertEquals(receipt, importService.importBundle(bundle, "1"));
        assertEquals(3, resultsService.getResultsbyAnalyzer(String.valueOf(ANALYZER_ID)).size());
    }

    private void confirm(AnalyzerMappingSnapshot candidate, Bundle bundle) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            AnalyzerMappingSnapshot binding = bindings.findByRevisionId(candidate.revision().getId()).orElseThrow();
            Observation observation = bundle.getEntry().stream().map(entry -> entry.getResource())
                    .filter(Observation.class::isInstance).map(Observation.class::cast).findFirst().orElseThrow();
            String fingerprint = observation
                    .getExtensionByUrl(
                            "https://openelis-global.org/fhir/StructureDefinition/analyzer-control-recognition")
                    .getExtensionByUrl("recognitionFingerprint").getValue().primitiveValue();
            var tests = new ArrayList<AnalyzerMappingSourceRow>();
            binding.tests().stream().filter(row -> row.getMappingState() == AnalyzerMappingState.BOUND)
                    .map(row -> new AnalyzerMappingSourceRow(row.getId().getSourceRowKey(), null)).forEach(tests::add);
            binding.results().stream().filter(row -> row.getMappingState() == AnalyzerMappingState.BOUND)
                    .map(row -> new AnalyzerMappingSourceRow(row.getId().getSourceRowKey(), row.getId().getRawValue()))
                    .forEach(tests::add);
            assertEquals(AnalyzerMappingConfirmationView.State.CURRENT,
                    confirmations
                            .confirm(binding, fingerprint, new AnalyzerMappingConfirmationRequest(
                                    binding.revision().getBindingFingerprint(), fingerprint, tests, List.of()), "1")
                            .state());
            assertTrue(confirmations.assessCurrent(binding, fingerprint).currentConfirmation().isPresent());
        });
    }

    private void bindTest(String sourceCode) {
        bindTest(sourceCode, TEST_ID);
    }

    private void bindTest(String sourceCode, long testId) {
        jdbc.update(
                "INSERT INTO clinlims.test (id, guid, name, description, is_active, is_reportable, orderable, lastupdated)"
                        + " VALUES (?, ?, ?, ?, 'Y', 'Y', true, NOW())",
                testId, UUID.randomUUID().toString(), "Receipt test " + sourceCode, "Receipt test " + sourceCode);
        jdbc.update("INSERT INTO clinlims.analyzer_site_binding_test"
                + " (site_binding_revision_id, source_row_key, mapping_state, test_id, last_updated)"
                + " VALUES (?, ?, 'BOUND', ?, NOW())", SITE_BINDING_REVISION_ID, sourceCode, testId);
    }

    private Bundle prepareControl(boolean withStatistics) throws Exception {
        return prepareControl(withStatistics, true);
    }

    private Bundle prepareControl(boolean withStatistics, boolean confirmed) throws Exception {
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
        if (confirmed)
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
        jdbc.update("DELETE FROM clinlims.test WHERE id IN (?, ?, ?)", TEST_ID, OTHER_TEST_ID, THIRD_TEST_ID);
        jdbc.update("DELETE FROM clinlims.analyzer_delivery_receipt WHERE analyzer_id = ?", ANALYZER_ID);
        jdbc.update("DELETE FROM clinlims.analyzer WHERE id = ?", ANALYZER_ID);
        jdbc.update("DELETE FROM clinlims.analyzer_site_binding_revision WHERE site_binding_id = ?", SITE_BINDING_ID);
        jdbc.update("DELETE FROM clinlims.analyzer_site_binding WHERE id = ?", SITE_BINDING_ID);
        jdbc.update("DELETE FROM clinlims.analyzer_profile_binding WHERE id = ?", PROFILE_BINDING_ID);
    }
}
