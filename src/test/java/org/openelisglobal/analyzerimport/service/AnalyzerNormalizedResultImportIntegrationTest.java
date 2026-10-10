package org.openelisglobal.analyzerimport.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Quantity;
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
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingState;
import org.openelisglobal.analyzerresults.service.AnalyzerResultsService;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

public class AnalyzerNormalizedResultImportIntegrationTest extends BaseWebContextSensitiveTest {

    private static final long MAPPING_ID = 98403L;
    private static final long ANALYZER_ID = 98404L;
    private static final long TEST_ID = 98405L;
    private static final long RESULT_OPTION_ID = 98406L;
    private static final long OTHER_TEST_ID = 98407L;
    private static final long THIRD_TEST_ID = 98408L;
    private static final long NEGATIVE_OPTION_ID = 98409L;
    private static final long DETECTED_ENTRY_ID = 98420L;
    private static final long NOT_DETECTED_ENTRY_ID = 98421L;
    private static final String QC_LOT_ID = "receipt-qc-lot";
    private static final String CONNECTION_ID = "bridge-connection-7f3c";
    private static final String ACCESSION = "ACC-UNKNOWN-TEST-001";
    private static final Path FIXTURE = Path.of("tools", "openelis-analyzer-bridge", "contracts", "analyzer", "v1",
            "fixtures", "normalized-unknown-test.fhir.json");
    private static final FhirContext REAL_FHIR = FhirContext.forR4();
    private static final String RAW_VALUE = "https://openelis-global.org/fhir/StructureDefinition/analyzer-raw-value";
    private static final String V2_SUBID = "http://hl7.org/fhir/StructureDefinition/observation-v2-subid";
    private static final String INTERPRETATION = "http://terminology.hl7.org/CodeSystem/v3-ObservationInterpretation";

    @Autowired
    private AnalyzerNormalizedResultImportService importService;
    @Autowired
    private DataSource dataSource;

    @Autowired
    private AnalyzerMappingService bindings;
    @Autowired
    private AnalyzerService analyzerService;
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
        jdbc.update("INSERT INTO clinlims.analyzer (id, name, is_active, bridge_connection_id, last_updated)"
                + " VALUES (?, 'Normalized import test', true, ?, NOW())", ANALYZER_ID, CONNECTION_ID);
        jdbc.update(
                "INSERT INTO clinlims.analyzer_mapping"
                        + " (id, analyzer_id, revision_number, profile_id, profile_revision, profile_fingerprint,"
                        + " mapping_fingerprint, created_by, created_at, last_updated)"
                        + " VALUES (?, ?, 1, 'site.unknown-capable', 3, ?, ?, '1', NOW(), NOW())",
                MAPPING_ID, ANALYZER_ID, "sha256:" + "1".repeat(64), "sha256:" + "2".repeat(64));
        jdbc.update("UPDATE clinlims.analyzer SET mapping_id = ? WHERE id = ?", MAPPING_ID, ANALYZER_ID);
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

        AnalyzerMappingSnapshot binding = bindings.findById(String.valueOf(MAPPING_ID)).orElseThrow();
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
        jdbc.update("UPDATE clinlims.analyzer_mapping SET profile_id = 'site.other-analyzer' WHERE id = ?", MAPPING_ID);
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
        jdbc.update("UPDATE clinlims.analyzer_mapping SET profile_id = 'site.other-analyzer' WHERE id = ?", MAPPING_ID);

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
        AnalyzerMappingSnapshot previous = bindings.findById(String.valueOf(MAPPING_ID)).orElseThrow();
        AnalyzerMappingSnapshot updated = bindings.appendRevision(analyzer(),
                new AnalyzerMappingDraft(List.of(new AnalyzerMappingTestDraft(original.getRawTestCode(),
                        AnalyzerMappingState.BOUND, String.valueOf(TEST_ID))), List.of()),
                "1");
        assertThrows(IllegalArgumentException.class,
                () -> localState.applyMapping(String.valueOf(ANALYZER_ID), updated.mapping().getId(),
                        updated.mapping().getRevisionNumber(), updated.mapping().getMappingFingerprint(), "1"));
        assertTrue("rejecting an unconfirmed revision must not release held results",
                resultsService.get(id).isReadOnly());

        confirm(updated, bundle);
        assertTrue("confirmation alone must not replay held results", resultsService.get(id).isReadOnly());
        localState.applyMapping(String.valueOf(ANALYZER_ID), updated.mapping().getId(),
                updated.mapping().getRevisionNumber(), updated.mapping().getMappingFingerprint(), "1");
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
        confirm(bindings.findById(String.valueOf(MAPPING_ID)).orElseThrow(), bundle);
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
        var partial = bindings.appendRevision(analyzer(),
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
        localState.applyMapping(String.valueOf(ANALYZER_ID), partial.mapping().getId(),
                partial.mapping().getRevisionNumber(), partial.mapping().getMappingFingerprint(), "1");
        var receipt = importService.importBundle(bundle, "1");
        assertEquals(1, receipt.resultsHeld());
        AnalyzerResults held = resultsService.getResultsbyAnalyzer(String.valueOf(ANALYZER_ID)).get(0);
        assertEquals(AnalyzerResults.IMPORT_ISSUE_RESULT_MAPPING_NOT_READY, held.getImportIssueReason());

        var corrected = bindings.appendRevision(analyzer(),
                new AnalyzerMappingDraft(
                        List.of(new AnalyzerMappingTestDraft("VENDOR-NEW-42", AnalyzerMappingState.BOUND,
                                String.valueOf(TEST_ID))),
                        List.of(new AnalyzerMappingResultDraft("VENDOR-NEW-42", "DETECTED", AnalyzerMappingState.BOUND,
                                String.valueOf(RESULT_OPTION_ID)))),
                "1");
        confirm(corrected, bundle);
        assertEquals(0, importService.recoverHeldMappingResults(String.valueOf(ANALYZER_ID), "1"));
        localState.applyMapping(String.valueOf(ANALYZER_ID), corrected.mapping().getId(),
                corrected.mapping().getRevisionNumber(), corrected.mapping().getMappingFingerprint(), "1");
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
    public void aMultiRecordRunLandsItsMainResultOnTheTestAndTheRestOnComponentsOfTheSameTest() throws Exception {
        // One cartridge run reports a main result and six more records; the mapping
        // sends the main one to the test itself and each of the others to a component.
        bindTest("HIV-VL", TEST_ID);
        List<String> componentCodes = List.of("LOG", "HIV-1", "Ct", "EndPt", "IQS-H", "IQS-L");
        for (int order = 0; order < componentCodes.size(); order++) {
            String code = componentCodes.get(order);
            jdbc.update(
                    "INSERT INTO clinlims.test_result_component"
                            + " (id, test_id, code, label, display_order, is_active) VALUES (?, ?, ?, ?, ?, 'Y')",
                    "comp-" + code, TEST_ID, code, code, order + 1);
            jdbc.update("INSERT INTO clinlims.analyzer_mapping_test"
                    + " (mapping_id, source_row_key, mapping_state, test_id, component_id, last_updated)"
                    + " VALUES (?, ?, 'BOUND', ?, ?, NOW())", MAPPING_ID, code, TEST_ID, "comp-" + code);
        }
        Bundle bundle = REAL_FHIR.newJsonParser().parseResource(Bundle.class, Files.readString(FIXTURE));
        Observation main = bundle.getEntry().stream().map(entry -> entry.getResource())
                .filter(Observation.class::isInstance).map(Observation.class::cast).findFirst().orElseThrow();
        main.getCode().getCodingFirstRep().setCode("HIV-VL");
        for (String code : componentCodes) {
            Observation record = main.copy();
            record.getCode().getCodingFirstRep().setCode(code);
            bundle.addEntry().setFullUrl("urn:uuid:result-" + code).setResource(record);
        }
        confirm(bindings.findById(String.valueOf(MAPPING_ID)).orElseThrow(), bundle);

        importService.importBundle(bundle, "1");

        List<AnalyzerResults> staged = resultsService.getResultsbyAnalyzer(String.valueOf(ANALYZER_ID));
        assertEquals(7, staged.size());
        assertEquals(Set.of(String.valueOf(TEST_ID)),
                staged.stream().map(AnalyzerResults::getTestId).collect(Collectors.toSet()));
        assertEquals(1, staged.stream().map(AnalyzerResults::getAccessionNumber).distinct().count());
        AnalyzerResults mainRow = staged.stream().filter(row -> "HIV-VL".equals(row.getRawTestCode())).findFirst()
                .orElseThrow();
        assertNull("the main result belongs to the test itself", mainRow.getComponentId());
        for (String code : componentCodes) {
            AnalyzerResults row = staged.stream().filter(candidate -> code.equals(candidate.getRawTestCode()))
                    .findFirst().orElseThrow();
            assertEquals("comp-" + code, row.getComponentId());
            assertFalse(code + " is mapped, so it is not held", row.isReadOnly());
        }
    }

    // Cepheid 303-0251 §2.1.1, quantified: R|1 "^1009.64", R|2 LOG "^3.00", R|3
    // HIV-1 "POS^", R|4 HIV-1 Ct "^33.0", all under host test code HIVVL.
    @Test
    public void aQuantifiedViralLoadLandsEachRecordOnItsOwnTarget() throws Exception {
        bindViralLoadRecords();
        Bundle bundle = viralLoadBundle(number(record(null, "^1009.64"), "1009.64", null),
                number(record("&LOG", "^3.00"), "3.00", null), call(record("HIV-1", "POS^"), "POS"),
                number(record("HIV-1&Ct", "^33.0"), "33.0", null));
        confirm(bindings.findById(String.valueOf(MAPPING_ID)).orElseThrow(), bundle);

        importService.importBundle(bundle, "1");

        List<AnalyzerResults> staged = resultsService.getResultsbyAnalyzer(String.valueOf(ANALYZER_ID));
        assertEquals(4, staged.size());
        assertTrue("nothing is held", staged.stream().noneMatch(AnalyzerResults::isReadOnly));
        assertEquals("1009.64", stagedOn(staged, null).getResult());
        assertEquals("3.00", stagedOn(staged, "comp-LOG").getResult());
        assertEquals("POS", stagedOn(staged, "comp-HIV-1").getResult());
        assertEquals("33.0", stagedOn(staged, "comp-HIV-1-Ct").getResult());
        assertTrue("the instrument reported no call",
                staged.stream().noneMatch(row -> "comp-call".equals(row.getComponentId())));
    }

    @Test
    public void aStagedResultKeepsTheInstrumentsFlagAssayAndOperator() throws Exception {
        bindViralLoadRecords();
        Observation main = number(record(null, "^1009.64"), "1009.64", null);
        main.addInterpretation().addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/v3-ObservationInterpretation").setCode("H")
                .setDisplay("H");
        main.getMethod().setText("Xpert HIV-1 Viral Load").addExtension(
                "https://openelis-global.org/fhir/StructureDefinition/analyzer-assay-version", new StringType("4"));
        main.addPerformer().setDisplay("Operator 12");
        Bundle bundle = viralLoadBundle(main);
        confirm(bindings.findById(String.valueOf(MAPPING_ID)).orElseThrow(), bundle);

        importService.importBundle(bundle, "1");

        AnalyzerResults staged = stagedOn(resultsService.getResultsbyAnalyzer(String.valueOf(ANALYZER_ID)), null);
        assertEquals("H", staged.getInstrumentFlags());
        assertEquals("Xpert HIV-1 Viral Load", staged.getAssayName());
        assertEquals("4", staged.getAssayVersion());
        assertEquals("Operator 12", staged.getInstrumentOperator());
    }

    // Cepheid 303-0251 §2.1.1, below range: R|1 "DETECTED^" with R.7 "<" and R.6
    // "40.00 to 10000000.00"; R|2 LOG "^" with "<" and "1.60 to 7.00".
    @Test
    public void aBelowRangeViralLoadStagesTheLimitWithItsComparatorAndTheCallOnTheCallComponent() throws Exception {
        bindViralLoadRecords();
        Observation main = number(record(null, "DETECTED^"), "40", Quantity.QuantityComparator.LESS_THAN);
        main.addInterpretation(new CodeableConcept(new Coding(INTERPRETATION, "DET", "Detected")).setText("DETECTED"));
        Bundle bundle = viralLoadBundle(main,
                number(record("&LOG", "^"), "1.60", Quantity.QuantityComparator.LESS_THAN));
        confirm(bindings.findById(String.valueOf(MAPPING_ID)).orElseThrow(), bundle);

        importService.importBundle(bundle, "1");

        List<AnalyzerResults> staged = resultsService.getResultsbyAnalyzer(String.valueOf(ANALYZER_ID));
        assertEquals(3, staged.size());
        assertTrue("nothing is held", staged.stream().noneMatch(AnalyzerResults::isReadOnly));
        assertEquals("<40", stagedOn(staged, null).getResult());
        assertEquals("DETECTED", stagedOn(staged, "comp-call").getResult());
        assertEquals("<1.60", stagedOn(staged, "comp-LOG").getResult());
        assertEquals("the raw text stays as the instrument sent it", "DETECTED^",
                stagedOn(staged, null).getRawResultValue());
    }

    // Cepheid 303-0251 §2.1.1, not detected: R|1 "NOT DETECTED^", no number.
    @Test
    public void aNotDetectedViralLoadFillsOnlyTheCall() throws Exception {
        bindViralLoadRecords();
        Bundle bundle = viralLoadBundle(call(record(null, "NOT DETECTED^"), "NOT DETECTED"));
        confirm(bindings.findById(String.valueOf(MAPPING_ID)).orElseThrow(), bundle);

        importService.importBundle(bundle, "1");

        List<AnalyzerResults> staged = resultsService.getResultsbyAnalyzer(String.valueOf(ANALYZER_ID));
        assertEquals(1, staged.size());
        assertEquals("comp-call", staged.get(0).getComponentId());
        assertEquals("NOT DETECTED", staged.get(0).getResult());
        assertFalse(staged.get(0).isReadOnly());
    }

    // Cepheid 303-0251 §2.1.1, above range: R|1 "DETECTED^" with R.7 ">" and the
    // R.6 upper limit 10000000.00.
    @Test
    public void anAboveRangeViralLoadStagesTheUpperLimitWithItsComparator() throws Exception {
        bindViralLoadRecords();
        Observation main = number(record(null, "DETECTED^"), "10000000", Quantity.QuantityComparator.GREATER_THAN);
        main.addInterpretation(new CodeableConcept(new Coding(INTERPRETATION, "DET", "Detected")).setText("DETECTED"));
        Bundle bundle = viralLoadBundle(main);
        confirm(bindings.findById(String.valueOf(MAPPING_ID)).orElseThrow(), bundle);

        importService.importBundle(bundle, "1");

        List<AnalyzerResults> staged = resultsService.getResultsbyAnalyzer(String.valueOf(ANALYZER_ID));
        assertEquals(2, staged.size());
        assertEquals(">10000000", stagedOn(staged, null).getResult());
        assertEquals("DETECTED", stagedOn(staged, "comp-call").getResult());
    }

    // Cepheid 303-0251 §2.1.1, invalid: R|1 "INVALID^", no number.
    @Test
    public void anInvalidViralLoadFillsOnlyTheCall() throws Exception {
        bindViralLoadRecords();
        Bundle bundle = viralLoadBundle(call(record(null, "INVALID^"), "INVALID"));
        confirm(bindings.findById(String.valueOf(MAPPING_ID)).orElseThrow(), bundle);

        importService.importBundle(bundle, "1");

        List<AnalyzerResults> staged = resultsService.getResultsbyAnalyzer(String.valueOf(ANALYZER_ID));
        assertEquals(1, staged.size());
        assertEquals("comp-call", staged.get(0).getComponentId());
        assertEquals("INVALID", staged.get(0).getResult());
    }

    // Cepheid 303-0251 §2.1.1, error: R|1 "ERROR^" with a C record; step 6 sends no
    // value, dataAbsentReason "error" and the C record as a note.
    @Test
    public void anErrorViralLoadIsHeldAsAFailedRunWithTheInstrumentsNote() throws Exception {
        bindViralLoadRecords();
        Observation main = record(null, "ERROR^");
        main.setValue(null);
        main.setDataAbsentReason(new CodeableConcept(
                new Coding("http://terminology.hl7.org/CodeSystem/data-absent-reason", "error", "Error")));
        main.addNote().setText("Error 2097: Assay-Specific Termination Error #2: 47, 8, 1, 0");
        Bundle bundle = viralLoadBundle(main);
        confirm(bindings.findById(String.valueOf(MAPPING_ID)).orElseThrow(), bundle);

        importService.importBundle(bundle, "1");

        List<AnalyzerResults> staged = resultsService.getResultsbyAnalyzer(String.valueOf(ANALYZER_ID));
        assertEquals(1, staged.size());
        assertTrue(staged.get(0).isReadOnly());
        assertEquals(AnalyzerResults.IMPORT_ISSUE_RUN_FAILED, staged.get(0).getImportIssueReason());
        assertTrue(staged.get(0).getInstrumentNote().contains("Error 2097"));
    }

    @Test
    public void aRecordWhoseSubIdentityIsNotMappedIsHeldAsAnUnknownTest() throws Exception {
        bindViralLoadRecords();
        // An instrument completes every record of a run at the same time (R.13).
        DateTimeType completed = new DateTimeType("2026-10-06T09:15:00Z");
        Bundle bundle = viralLoadBundle(
                number(record(null, "^1009.64"), "1009.64", null).setEffective(completed.copy()),
                number(record("HIV-1&EndPt", "^257.0"), "257.0", null).setEffective(completed.copy()));
        confirm(bindings.findById(String.valueOf(MAPPING_ID)).orElseThrow(), bundle);

        importService.importBundle(bundle, "1");

        List<AnalyzerResults> staged = resultsService.getResultsbyAnalyzer(String.valueOf(ANALYZER_ID));
        assertEquals(2, staged.size());
        AnalyzerResults endPoint = staged.stream().filter(AnalyzerResults::isReadOnly).findFirst().orElseThrow();
        assertEquals(AnalyzerResults.IMPORT_ISSUE_UNKNOWN_TEST, endPoint.getImportIssueReason());
        assertEquals("HIVVL", endPoint.getRawTestCode());
        assertEquals("HIV-1&EndPt", endPoint.getRawSubIdentity());
        assertEquals("", stagedOn(staged, null).getRawSubIdentity());
        assertFalse("the mapped main record still lands", stagedOn(staged, null).isReadOnly());
    }

    @Test
    public void theNumberIsNeverHeldAsAnUnknownAnswerWhenTheCallHasAnswers() throws Exception {
        bindViralLoadRecords();
        jdbc.update("INSERT INTO clinlims.test_result"
                + " (id, test_id, tst_rslt_type, value, is_active, sort_order, lastupdated)"
                + " VALUES (?, ?, 'D', 'Not detected', true, 1, NOW())", NEGATIVE_OPTION_ID, TEST_ID);
        jdbc.update("INSERT INTO clinlims.analyzer_mapping_result"
                + " (mapping_id, source_row_key, sub_identity, raw_value, mapping_state, test_result_id, last_updated)"
                + " VALUES (?, 'HIVVL', '', 'NOT DETECTED', 'BOUND', ?, NOW())", MAPPING_ID, NEGATIVE_OPTION_ID);
        Bundle bundle = viralLoadBundle(number(record(null, "^1009.64"), "1009.64", null));
        confirm(bindings.findById(String.valueOf(MAPPING_ID)).orElseThrow(), bundle);

        importService.importBundle(bundle, "1");

        AnalyzerResults number = resultsService.getResultsbyAnalyzer(String.valueOf(ANALYZER_ID)).get(0);
        assertFalse(number.getImportIssueReason(), number.isReadOnly());
        assertEquals("1009.64", number.getResult());
    }

    /**
     * As the Bridge sends Cepheid's below-range viral load: the raw value is the
     * call alone, and the baseline profile maps that call. The call's answer
     * belongs on the call component; the number keeps "<40".
     */
    @Test
    public void aBelowRangeViralLoadKeepsItsNumberWhenItsCallIsMapped() throws Exception {
        bindViralLoadRecords();
        jdbc.update("INSERT INTO clinlims.test_result"
                + " (id, test_id, tst_rslt_type, value, is_active, sort_order, component_id, lastupdated)"
                + " VALUES (?, ?, 'D', 'Detected', true, 1, 'comp-call', NOW())", RESULT_OPTION_ID, TEST_ID);
        jdbc.update("INSERT INTO clinlims.analyzer_mapping_result"
                + " (mapping_id, source_row_key, sub_identity, raw_value, mapping_state, test_result_id, last_updated)"
                + " VALUES (?, 'HIVVL', '', 'DETECTED', 'BOUND', ?, NOW())", MAPPING_ID, RESULT_OPTION_ID);
        Observation main = number(record(null, "DETECTED"), "40", Quantity.QuantityComparator.LESS_THAN);
        main.addInterpretation(new CodeableConcept(new Coding(INTERPRETATION, "DET", "Detected")).setText("DETECTED"));
        Bundle bundle = viralLoadBundle(main);
        confirm(bindings.findById(String.valueOf(MAPPING_ID)).orElseThrow(), bundle);

        importService.importBundle(bundle, "1");

        List<AnalyzerResults> staged = resultsService.getResultsbyAnalyzer(String.valueOf(ANALYZER_ID));
        assertEquals("the number and the call, once each", 2, staged.size());
        assertEquals("<40", stagedOn(staged, null).getResult());
        assertEquals("N", stagedOn(staged, null).getResultType());
        assertEquals("Detected", stagedOn(staged, "comp-call").getResult());
    }

    @Test
    public void aBelowRangeRecordHeldWholeRecoversIntoItsNumberAndItsCallWithoutDuplicates() throws Exception {
        bindTest("HIVVL", TEST_ID);
        jdbc.update("INSERT INTO clinlims.test_result_component"
                + " (id, test_id, code, label, display_order, is_active) VALUES ('comp-call', ?, 'call', 'call', 1, 'Y')",
                TEST_ID);
        var unresolved = bindings.appendRevision(analyzer(), new AnalyzerMappingDraft(
                List.of(new AnalyzerMappingTestDraft("HIVVL", AnalyzerMappingState.UNRESOLVED, null)), List.of()), "1");
        Observation main = number(record(null, "DETECTED^"), "40", Quantity.QuantityComparator.LESS_THAN);
        main.addInterpretation(new CodeableConcept(new Coding(INTERPRETATION, "DET", "Detected")).setText("DETECTED"));
        Bundle bundle = viralLoadBundle(main);
        confirm(unresolved, bundle);
        localState.applyMapping(String.valueOf(ANALYZER_ID), unresolved.mapping().getId(),
                unresolved.mapping().getRevisionNumber(), unresolved.mapping().getMappingFingerprint(), "1");
        importService.importBundle(bundle, "1");
        List<AnalyzerResults> held = resultsService.getResultsbyAnalyzer(String.valueOf(ANALYZER_ID));
        assertEquals("held whole while its record is unmapped", 1, held.size());
        assertTrue(held.get(0).isReadOnly());

        var bound = bindings.appendRevision(analyzer(),
                new AnalyzerMappingDraft(List.of(new AnalyzerMappingTestDraft("HIVVL", AnalyzerMappingState.BOUND,
                        String.valueOf(TEST_ID), null, null, null, "", "comp-call")), List.of()),
                "1");
        confirm(bound, bundle);
        localState.applyMapping(String.valueOf(ANALYZER_ID), bound.mapping().getId(),
                bound.mapping().getRevisionNumber(), bound.mapping().getMappingFingerprint(), "1");

        List<AnalyzerResults> recovered = resultsService.getResultsbyAnalyzer(String.valueOf(ANALYZER_ID));
        assertEquals("the number and the call, once each", 2, recovered.size());
        assertTrue(recovered.stream().noneMatch(AnalyzerResults::isReadOnly));
        assertEquals("<40", stagedOn(recovered, null).getResult());
        assertEquals("DETECTED", stagedOn(recovered, "comp-call").getResult());
        assertEquals(0, importService.recoverHeldMappingResults(String.valueOf(ANALYZER_ID), "1"));
        assertEquals(2, resultsService.getResultsbyAnalyzer(String.valueOf(ANALYZER_ID)).size());
    }

    /**
     * The HIVVL records of one Cepheid run mapped as one test: the main record's
     * number on the primary and its call on a call component; LOG and the HIV-1
     * records on components of their own.
     */
    private void bindViralLoadRecords() {
        bindTest("HIVVL", TEST_ID);
        for (String code : List.of("call", "LOG", "HIV-1", "HIV-1-Ct")) {
            jdbc.update(
                    "INSERT INTO clinlims.test_result_component"
                            + " (id, test_id, code, label, display_order, is_active) VALUES (?, ?, ?, ?, 1, 'Y')",
                    "comp-" + code, TEST_ID, code, code);
        }
        jdbc.update("UPDATE clinlims.analyzer_mapping_test SET call_component_id = 'comp-call'"
                + " WHERE mapping_id = ? AND source_row_key = 'HIVVL' AND sub_identity = ''", MAPPING_ID);
        for (String[] record : List.of(new String[] { "&LOG", "comp-LOG" }, new String[] { "HIV-1", "comp-HIV-1" },
                new String[] { "HIV-1&Ct", "comp-HIV-1-Ct" })) {
            jdbc.update("INSERT INTO clinlims.analyzer_mapping_test"
                    + " (mapping_id, source_row_key, sub_identity, mapping_state, test_id, component_id, last_updated)"
                    + " VALUES (?, 'HIVVL', ?, 'BOUND', ?, ?, NOW())", MAPPING_ID, record[0], TEST_ID, record[1]);
        }
    }

    /** One record of the run as step 6 emits it, before its value is set. */
    private Observation record(String subIdentity, String rawText) throws Exception {
        Bundle fixture = REAL_FHIR.newJsonParser().parseResource(Bundle.class, Files.readString(FIXTURE));
        Observation record = fixture.getEntry().stream().map(entry -> entry.getResource())
                .filter(Observation.class::isInstance).map(Observation.class::cast).findFirst().orElseThrow();
        record.getCode().getCodingFirstRep().setCode("HIVVL").setDisplay("HIVVL");
        record.getExtensionByUrl(RAW_VALUE).setValue(new StringType(rawText));
        if (subIdentity != null) {
            record.addExtension().setUrl(V2_SUBID).addExtension("original-sub-identifier", new StringType(subIdentity));
        }
        return record;
    }

    private Observation number(Observation record, String value, Quantity.QuantityComparator comparator) {
        Quantity quantity = new Quantity().setValue(new BigDecimal(value)).setUnit("copies/mL");
        if (comparator != null) {
            quantity.setComparator(comparator);
        }
        return record.setValue(quantity);
    }

    private Observation call(Observation record, String text) {
        return record.setValue(new StringType(text));
    }

    private Bundle viralLoadBundle(Observation... records) throws Exception {
        Bundle bundle = REAL_FHIR.newJsonParser().parseResource(Bundle.class, Files.readString(FIXTURE));
        bundle.getEntry().removeIf(entry -> entry.getResource() instanceof Observation);
        for (int index = 0; index < records.length; index++) {
            bundle.addEntry().setFullUrl("urn:uuid:hivvl-record-" + index).setResource(records[index]);
        }
        return bundle;
    }

    private static AnalyzerResults stagedOn(List<AnalyzerResults> staged, String componentId) {
        return staged.stream().filter(row -> java.util.Objects.equals(componentId, row.getComponentId())).findFirst()
                .orElseThrow(() -> new AssertionError("nothing staged on " + componentId));
    }

    @Test
    public void aPositiveControlThatGivesItsExpectedAnswerIsRecordedAsAPassingQcResult() throws Exception {
        Bundle bundle = prepareQualitativeControl("POS", true);

        AnalyzerNormalizedResultImportSummary summary = importService.importBundle(bundle, "1");

        assertEquals(1, summary.controlResultsProcessed());
        assertEquals(0, summary.resultsHeld());
        assertEquals(Integer.valueOf(1),
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM clinlims.qc_result WHERE control_lot_id = ?"
                                + " AND source = 'ASTM' AND qualitative_outcome = 'PASS' AND result_value IS NULL",
                        Integer.class, QC_LOT_ID));
    }

    @Test
    public void aQualitativeControlWithNoQcTargetIsHeldWithItsReasonAndRecordsNoQc() throws Exception {
        Bundle bundle = prepareQualitativeControl("POS", false);

        AnalyzerNormalizedResultImportSummary summary = importService.importBundle(bundle, "1");

        assertEquals(0, summary.controlResultsProcessed());
        assertEquals(1, summary.resultsHeld());
        AnalyzerResults held = resultsService.getResultsbyAnalyzer(String.valueOf(ANALYZER_ID)).get(0);
        assertTrue(held.isReadOnly());
        assertEquals(AnalyzerResults.IMPORT_ISSUE_QC_TARGET_MISSING, held.getImportIssueReason());
        assertEquals(Integer.valueOf(0), jdbc.queryForObject(
                "SELECT COUNT(*) FROM clinlims.qc_result WHERE control_lot_id = ?", Integer.class, QC_LOT_ID));
    }

    @Test
    public void aRunWithNoValueIsHeldAsAFailedRunCarryingTheInstrumentsNote() throws Exception {
        bindTest("VENDOR-NEW-42");
        Bundle bundle = REAL_FHIR.newJsonParser().parseResource(Bundle.class, Files.readString(FIXTURE));
        Observation run = bundle.getEntry().stream().map(entry -> entry.getResource())
                .filter(Observation.class::isInstance).map(Observation.class::cast).findFirst().orElseThrow();
        run.setValue(null);
        run.getDataAbsentReason().addCoding().setSystem("http://terminology.hl7.org/CodeSystem/data-absent-reason")
                .setCode("error");
        run.getExtensionByUrl("https://openelis-global.org/fhir/StructureDefinition/analyzer-raw-value")
                .setValue(new StringType("ERROR"));
        run.addNote().setText("Error 2008: pressure abort");
        confirm(bindings.findById(String.valueOf(MAPPING_ID)).orElseThrow(), bundle);

        AnalyzerNormalizedResultImportSummary summary = importService.importBundle(bundle, "1");

        assertEquals(1, summary.resultsHeld());
        AnalyzerResults held = resultsService.getResultsbyAnalyzer(String.valueOf(ANALYZER_ID)).get(0);
        assertTrue("a failed run is never a patient result", held.isReadOnly());
        assertEquals(AnalyzerResults.IMPORT_ISSUE_RUN_FAILED, held.getImportIssueReason());
        assertEquals("ERROR", held.getRawResultValue());
        assertEquals("Error 2008: pressure abort", held.getInstrumentNote());
        assertEquals("the test is known, so the failure can be recorded on it", String.valueOf(TEST_ID),
                held.getTestId());
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
            jdbc.update("INSERT INTO clinlims.analyzer_mapping_result"
                    + " (mapping_id, source_row_key, raw_value, mapping_state, test_result_id, last_updated)"
                    + " VALUES (?, 'C', 'DETECTED', 'BOUND', ?, NOW())", MAPPING_ID, RESULT_OPTION_ID);
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
        confirm(bindings.findById(String.valueOf(MAPPING_ID)).orElseThrow(), bundle);
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

    private Analyzer analyzer() {
        return analyzerService.getWithMapping(String.valueOf(ANALYZER_ID)).orElseThrow();
    }

    private void confirm(AnalyzerMappingSnapshot candidate, Bundle bundle) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            AnalyzerMappingSnapshot binding = bindings.findById(candidate.mapping().getId()).orElseThrow();
            Observation observation = bundle.getEntry().stream().map(entry -> entry.getResource())
                    .filter(Observation.class::isInstance).map(Observation.class::cast).findFirst().orElseThrow();
            String fingerprint = observation
                    .getExtensionByUrl(
                            "https://openelis-global.org/fhir/StructureDefinition/analyzer-control-recognition")
                    .getExtensionByUrl("recognitionFingerprint").getValue().primitiveValue();
            var tests = new ArrayList<AnalyzerMappingSourceRow>();
            binding.tests().stream().filter(row -> row.getMappingState() == AnalyzerMappingState.BOUND)
                    .map(row -> new AnalyzerMappingSourceRow(row.getId().getSourceRowKey(), null,
                            row.getId().getSubIdentity()))
                    .forEach(tests::add);
            binding.results().stream().filter(row -> row.getMappingState() == AnalyzerMappingState.BOUND)
                    .map(row -> new AnalyzerMappingSourceRow(row.getId().getSourceRowKey(), row.getId().getRawValue(),
                            row.getId().getSubIdentity()))
                    .forEach(tests::add);
            assertEquals(AnalyzerMappingConfirmationView.State.CURRENT,
                    confirmations
                            .confirm(binding, fingerprint, new AnalyzerMappingConfirmationRequest(
                                    binding.mapping().getMappingFingerprint(), fingerprint, tests, List.of()), "1")
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
        jdbc.update("INSERT INTO clinlims.analyzer_mapping_test"
                + " (mapping_id, source_row_key, mapping_state, test_id, last_updated)"
                + " VALUES (?, ?, 'BOUND', ?, NOW())", MAPPING_ID, sourceCode, testId);
    }

    private Bundle prepareControl(boolean withStatistics) throws Exception {
        return prepareControl(withStatistics, true);
    }

    private Bundle prepareControl(boolean withStatistics, boolean confirmed) throws Exception {
        bindTest("WBC");
        jdbc.update(
                "UPDATE clinlims.analyzer_mapping SET profile_id = 'site.mock-hematology', profile_revision = 1 WHERE id = ?",
                MAPPING_ID);
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
            confirm(bindings.findById(String.valueOf(MAPPING_ID)).orElseThrow(), bundle);
        return bundle;
    }

    /**
     * A control that reports Detected or Not detected: the analyzer's mapping turns
     * POS and NEG into those answers, and the Test Catalog QC target for the lot's
     * level expects Detected when {@code withTarget}.
     */
    private Bundle prepareQualitativeControl(String reported, boolean withTarget) throws Exception {
        Bundle bundle = prepareControl(false, false);
        jdbc.update(
                "INSERT INTO clinlims.dictionary (id, dict_entry, is_active, lastupdated)"
                        + " VALUES (?, 'Detected', 'Y', NOW()), (?, 'Not detected', 'Y', NOW())",
                DETECTED_ENTRY_ID, NOT_DETECTED_ENTRY_ID);
        jdbc.update(
                "INSERT INTO clinlims.test_result"
                        + " (id, test_id, tst_rslt_type, value, is_active, sort_order, lastupdated)"
                        + " VALUES (?, ?, 'D', ?, true, 1, NOW()), (?, ?, 'D', ?, true, 2, NOW())",
                RESULT_OPTION_ID, TEST_ID, String.valueOf(DETECTED_ENTRY_ID), NEGATIVE_OPTION_ID, TEST_ID,
                String.valueOf(NOT_DETECTED_ENTRY_ID));
        jdbc.update(
                "INSERT INTO clinlims.analyzer_mapping_result"
                        + " (mapping_id, source_row_key, raw_value, mapping_state, test_result_id, last_updated)"
                        + " VALUES (?, 'WBC', 'POS', 'BOUND', ?, NOW()), (?, 'WBC', 'NEG', 'BOUND', ?, NOW())",
                MAPPING_ID, RESULT_OPTION_ID, MAPPING_ID, NEGATIVE_OPTION_ID);
        if (withTarget) {
            jdbc.update(
                    "INSERT INTO clinlims.test_qc_target"
                            + " (id, test_id, control_level, expected_dict_result_id, is_active, last_updated)"
                            + " VALUES ('qualitative-target', ?, 'NORMAL', ?, true, NOW())",
                    TEST_ID, DETECTED_ENTRY_ID);
        }
        Observation control = bundle.getEntry().stream().map(entry -> entry.getResource())
                .filter(Observation.class::isInstance).map(Observation.class::cast).findFirst().orElseThrow();
        control.setValue(new StringType(reported));
        control.getExtensionByUrl("https://openelis-global.org/fhir/StructureDefinition/analyzer-raw-value")
                .setValue(new StringType(reported));
        confirm(bindings.findById(String.valueOf(MAPPING_ID)).orElseThrow(), bundle);
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
        jdbc.update("UPDATE clinlims.analyzer SET mapping_id = NULL WHERE id = ?", ANALYZER_ID);
        String mappingIds = "(SELECT id FROM clinlims.analyzer_mapping WHERE analyzer_id = ?)";
        jdbc.update("DELETE FROM clinlims.analyzer_mapping_confirmation WHERE mapping_id IN " + mappingIds,
                ANALYZER_ID);
        jdbc.update("DELETE FROM clinlims.analyzer_mapping_result WHERE mapping_id IN " + mappingIds, ANALYZER_ID);
        jdbc.update("DELETE FROM clinlims.analyzer_mapping_test WHERE mapping_id IN " + mappingIds, ANALYZER_ID);
        jdbc.update("UPDATE clinlims.analyzer_mapping SET supersedes_mapping_id = NULL WHERE analyzer_id = ?",
                ANALYZER_ID);
        jdbc.update("DELETE FROM clinlims.analyzer_mapping WHERE analyzer_id = ?", ANALYZER_ID);
        jdbc.update("DELETE FROM clinlims.test_qc_target WHERE test_id = ?", TEST_ID);
        jdbc.update("DELETE FROM clinlims.test_result WHERE id IN (?, ?)", RESULT_OPTION_ID, NEGATIVE_OPTION_ID);
        jdbc.update("DELETE FROM clinlims.dictionary WHERE id IN (?, ?)", DETECTED_ENTRY_ID, NOT_DETECTED_ENTRY_ID);
        jdbc.update("DELETE FROM clinlims.test_result_component WHERE test_id IN (?, ?, ?)", TEST_ID, OTHER_TEST_ID,
                THIRD_TEST_ID);
        jdbc.update("DELETE FROM clinlims.test WHERE id IN (?, ?, ?)", TEST_ID, OTHER_TEST_ID, THIRD_TEST_ID);
        jdbc.update("DELETE FROM clinlims.analyzer_delivery_receipt WHERE analyzer_id = ?", ANALYZER_ID);
        jdbc.update("DELETE FROM clinlims.analyzer WHERE id = ?", ANALYZER_ID);
    }
}
