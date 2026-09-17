package org.openelisglobal.analyzerimport.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.openelisglobal.analyzer.AnalyzerTestProfileCatalog.RECEIPT_PROFILE_ID;

import ca.uhn.fhir.context.FhirContext;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Device;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.StringType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzer.dao.AnalyzerProfileBindingDAO;
import org.openelisglobal.analyzer.form.AnalyzerInstanceRequest;
import org.openelisglobal.analyzer.service.AnalyzerInstanceLocalStateService;
import org.openelisglobal.analyzer.service.AnalyzerInstanceState;
import org.openelisglobal.analyzer.service.AnalyzerSiteBindingConfirmationRequest;
import org.openelisglobal.analyzer.service.AnalyzerSiteBindingSourceRow;
import org.openelisglobal.analyzer.service.AnalyzerSiteBindingTestDraft;
import org.openelisglobal.analyzer.service.AnalyzerTypeMappingService;
import org.openelisglobal.analyzer.service.AnalyzerTypeMappingUpdate;
import org.openelisglobal.analyzer.service.AnalyzerTypeMappingView;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingMappingState;
import org.openelisglobal.analyzerresults.service.AnalyzerResultsService;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.qc.service.QCControlLotService;
import org.openelisglobal.qc.valueholder.QCControlLot;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.TestSection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Committed database tests: the importer owns its transaction, including
 * rollback and concurrent receipt protection. Setup uses real local services.
 * SQL is used only to inspect persisted state and remove this test's owned
 * records afterward.
 */
public class AnalyzerNormalizedResultImportIntegrationTest extends BaseWebContextSensitiveTest {
    private static final String EXTENSION = "https://openelis-global.org/fhir/StructureDefinition/analyzer-";
    private static final String ACCESSION = "ACC-UNKNOWN-TEST-001";
    private static final Path FIXTURES = Path.of("tools", "openelis-analyzer-bridge", "contracts", "analyzer", "v1",
            "fixtures");

    @Autowired
    private AnalyzerNormalizedResultImportService importService;
    @Autowired
    private DataSource dataSource;
    @Autowired
    private FhirContext fhirContext;
    @Autowired
    private AnalyzerInstanceLocalStateService instances;
    @Autowired
    private AnalyzerTypeMappingService mappings;
    @Autowired
    private AnalyzerProfileBindingDAO profileBindings;
    @Autowired
    private TestService tests;
    @Autowired
    private TestSectionService labUnits;
    @Autowired
    private LocalizationService localizations;
    @Autowired
    private QCControlLotService controlLots;
    @Autowired
    private AnalyzerResultsService results;

    private static final List<String> OWNED_TABLES = List.of("test", "test_section", "localization", "analyzer",
            "analyzer_results", "analyzer_delivery_receipt", "analyzer_profile_binding", "analyzer_site_binding",
            "analyzer_site_binding_revision", "analyzer_site_binding_test", "analyzer_site_binding_result",
            "analyzer_site_binding_confirmation", "qc_control_lot", "qc_statistics", "qc_result",
            "westgard_rule_config", "history");
    private JdbcTemplate jdbc;
    private Map<String, Integer> initialRowCounts;
    private AnalyzerInstanceState analyzer;
    private String testId;
    private String labUnitId;
    private String localizationId;
    private String controlLotId;
    private String profileBindingId;
    private String siteBindingId;
    private final List<String> removedStagingIds = new ArrayList<>();

    @Before
    public void createConfirmedAndAdoptedMapping() {
        jdbc = new JdbcTemplate(dataSource);
        initialRowCounts = rowCounts();
        assertFalse("Receipt fixtures must not leave their profile behind",
                profileBindings.findByProfileIdAndRevision(RECEIPT_PROFILE_ID, 1).isPresent());

        org.openelisglobal.test.valueholder.Test test = new org.openelisglobal.test.valueholder.Test();
        test.setName("Receipt WBC " + UUID.randomUUID());
        test.setDescription("Receipt integration WBC");
        test.setGuid(UUID.randomUUID().toString());
        test.setIsActive("Y");
        test.setIsReportable("Y");
        test.setSysUserId(TEST_SYS_USER_ID);
        testId = tests.insert(test);

        Localization localization = new Localization();
        localization.setDescription("test section name");
        localization.setLocalizedValue("en", "Receipt integration");
        localization.setSysUserId(TEST_SYS_USER_ID);
        localizationId = localizations.insert(localization);
        TestSection unit = new TestSection();
        unit.setLocalization(localization);
        unit.setTestSectionName("Receipt integration");
        unit.setDescription("Owned by the receipt integration test");
        unit.setIsActive("Y");
        unit.setSysUserId(TEST_SYS_USER_ID);
        labUnitId = labUnits.insert(unit);

        AnalyzerInstanceRequest request = new AnalyzerInstanceRequest();
        request.setName("Receipt analyzer " + UUID.randomUUID());
        request.setProfileId(RECEIPT_PROFILE_ID);
        request.setProfileRevision(1);
        request.setTestUnitIds(List.of(labUnitId));
        analyzer = instances.create(request, TEST_SYS_USER_ID);
        analyzer = instances.attachBridgeConnection(analyzer.analyzerId(), "receipt-" + UUID.randomUUID(),
                TEST_SYS_USER_ID);
        profileBindingId = profileBindings.findByProfileIdAndRevision(RECEIPT_PROFILE_ID, 1).orElseThrow().getId();
        AnalyzerTypeMappingView initial = mappings.getMapping(RECEIPT_PROFILE_ID, 1);
        siteBindingId = initial.siteBindingId();
        AnalyzerTypeMappingView mapped = mappings.saveMapping(RECEIPT_PROFILE_ID, 1,
                new AnalyzerTypeMappingUpdate(initial.bindingFingerprint(),
                        List.of(new AnalyzerSiteBindingTestDraft("WBC", AnalyzerSiteBindingMappingState.BOUND, testId)),
                        List.of()),
                TEST_SYS_USER_ID);
        mappings.confirmMapping(RECEIPT_PROFILE_ID, 1,
                new AnalyzerSiteBindingConfirmationRequest(mapped.bindingFingerprint(),
                        mapped.controlRecognition().recognitionFingerprint(),
                        List.of(new AnalyzerSiteBindingSourceRow("WBC", null)), List.of()),
                TEST_SYS_USER_ID);
        instances.selectSiteBindingRevision(analyzer.analyzerId(), mapped.siteBindingId(), mapped.siteBindingRevision(),
                mapped.bindingFingerprint(), TEST_SYS_USER_ID);
    }

    @Test
    public void controlReplayDoesNotCreateAnotherOperationalQcResultAfterStagingRemoval() throws Exception {
        Bundle bundle = prepareControl();
        AnalyzerNormalizedResultImportSummary accepted = importService.importBundle(bundle, TEST_SYS_USER_ID);
        assertEquals(1, accepted.controlResultsProcessed());
        assertEquals(1, stagingCount());
        removeStaging();
        assertEquals(accepted, importService.importBundle(bundle, TEST_SYS_USER_ID));
        assertEquals(1, controlResultCount());
        assertEquals(1, receiptCount());
        assertEquals(0, stagingCount());
    }

    @Test
    public void qcFailureRollsBackStagingAndReceiptAndAllowsASuccessfulRetry() throws Exception {
        Bundle bundle = prepareControl();
        // Valid lot/statistics; a numeric overflow makes the actual QC write fail
        // after staging. Retrying corrects the message, not a broken dependency.
        setControlValue(bundle, "10000000000");
        int historyBefore = jdbc.queryForObject("SELECT COUNT(*) FROM clinlims.history", Integer.class);
        RuntimeException failure = assertThrows(RuntimeException.class,
                () -> importService.importBundle(bundle, TEST_SYS_USER_ID));
        Throwable cause = failure;
        while (cause != null && !(cause instanceof SQLException)) {
            cause = cause.getCause();
        }
        assertNotNull("The failure must reach PostgreSQL's numeric constraint", cause);
        assertEquals("22003", ((SQLException) cause).getSQLState());
        assertEquals(0, stagingCount());
        assertEquals(0, receiptCount());
        assertEquals(0, controlResultCount());
        assertEquals(historyBefore,
                jdbc.queryForObject("SELECT COUNT(*) FROM clinlims.history", Integer.class).intValue());
        setControlValue(bundle, "7.1");
        assertEquals(1, importService.importBundle(bundle, TEST_SYS_USER_ID).controlResultsProcessed());
        assertEquals(1, controlResultCount());
        assertEquals(1, stagingCount());
        assertEquals(1, receiptCount());
        assertEquals(0,
                new BigDecimal("7.1").compareTo(
                        jdbc.queryForObject("SELECT result_value FROM clinlims.qc_result WHERE control_lot_id = ?",
                                BigDecimal.class, controlLotId)));
    }

    @Test
    public void simultaneousCopiesCommitOnlyOneReceiptAndStagingRow() throws Exception {
        String payload = fhirContext.newJsonParser()
                .encodeResourceToString(bundle("normalized-unknown-test.fhir.json"));
        CountDownLatch start = new CountDownLatch(1);
        try (var workers = Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<AnalyzerNormalizedResultImportSummary> delivery = () -> {
                if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Delivery barrier timed out");
                }
                return importService.importBundle(fhirContext.newJsonParser().parseResource(Bundle.class, payload),
                        TEST_SYS_USER_ID);
            };
            var first = workers.submit(delivery);
            var second = workers.submit(delivery);
            start.countDown();
            AnalyzerNormalizedResultImportSummary accepted = first.get(20, TimeUnit.SECONDS);
            assertEquals(accepted, second.get(20, TimeUnit.SECONDS));
            assertEquals(1, accepted.resultsStaged());
            assertEquals(1, accepted.resultsHeld());
        }
        assertEquals(1, receiptCount());
        assertEquals(1, stagingCount());
    }

    @Test
    public void aNewMessageIdProducesADistinctReceipt() throws Exception {
        Bundle bundle = bundle("normalized-unknown-test.fhir.json");
        String firstMessage = bundle.getIdentifier().getValue();
        importService.importBundle(bundle, TEST_SYS_USER_ID);
        bundle.getIdentifier().setValue("second-delivery");
        importService.importBundle(bundle, TEST_SYS_USER_ID);
        assertEquals(2, receiptCount());
        // Clinical duplicate detection retains the original held row even though
        // the transport deliveries have different receipts.
        assertEquals(1, stagingCount());
        assertEquals(firstMessage, results.getResultsbyAnalyzer(analyzer.analyzerId()).get(0).getSourceMessageId());
    }

    @Test
    public void invalidProfileDoesNotCommitAReceipt() throws Exception {
        Bundle bundle = bundle("normalized-unknown-test.fhir.json");
        device(bundle).getExtensionByUrl(EXTENSION + "profile-revision").setValue(new IntegerType(2));
        AnalyzerNormalizedResultImportException failure = assertThrows(AnalyzerNormalizedResultImportException.class,
                () -> importService.importBundle(bundle, TEST_SYS_USER_ID));
        assertEquals("Normalized traffic profile does not match the analyzer pin", failure.getMessage());
        assertEquals(0, receiptCount());
        assertEquals(0, stagingCount());
    }

    @Test
    public void acceptedDeliveryIsNotRestagedAfterStagingRowsAreRemoved() throws Exception {
        Bundle bundle = bundle("normalized-unknown-test.fhir.json");
        AnalyzerNormalizedResultImportSummary accepted = importService.importBundle(bundle, TEST_SYS_USER_ID);
        assertEquals(1, stagingCount());
        removeStaging();
        // A receipt is checked before the current profile match. Changing the
        // incoming metadata proves this without mutating an immutable profile.
        device(bundle).getExtensionByUrl(EXTENSION + "profile-revision").setValue(new IntegerType(2));
        assertEquals(accepted, importService.importBundle(bundle, TEST_SYS_USER_ID));
        assertEquals(0, stagingCount());
        assertEquals(1, receiptCount());
    }

    @Test
    public void unknownResultIsHeldWithExactBridgeSourceEvidence() throws Exception {
        Bundle bundle = bundle("normalized-unknown-test.fhir.json");
        AnalyzerNormalizedResultImportSummary summary = importService.importBundle(bundle, TEST_SYS_USER_ID);
        assertEquals(analyzer.analyzerId(), summary.analyzerId());
        assertEquals(1, summary.resultsStaged());
        assertEquals(1, summary.resultsHeld());
        AnalyzerResults row = results.getResultsbyAnalyzer(analyzer.analyzerId()).get(0);
        assertEquals(AnalyzerResults.IMPORT_ISSUE_UNKNOWN_TEST, row.getImportIssueReason());
        assertEquals(analyzer.bridgeConnectionId(), row.getSourceConnectionId());
        assertEquals(bundle.getIdentifier().getValue(), row.getSourceMessageId());
        assertEquals(ACCESSION, row.getAccessionNumber());
        assertEquals(RECEIPT_PROFILE_ID, row.getSourceProfileId());
        assertEquals(Integer.valueOf(1), row.getSourceProfileRevision());
        assertEquals("VENDOR-NEW-42", row.getRawTestCode());
        assertEquals("DETECTED", row.getRawResultValue());
        assertEquals("PATIENT", row.getResultClassification());
        Observation source = bundle.getEntry().stream().map(Bundle.BundleEntryComponent::getResource)
                .filter(Observation.class::isInstance).map(Observation.class::cast).findFirst().orElseThrow();
        assertEquals(fhirContext.newJsonParser().encodeResourceToString(source), row.getSourcePayload());
    }

    private Bundle prepareControl() throws Exception {
        QCControlLot lot = new QCControlLot();
        controlLotId = UUID.randomUUID().toString();
        lot.setId(controlLotId);
        lot.setProductName("Receipt control");
        lot.setLotNumber("receipt-" + UUID.randomUUID());
        lot.setManufacturer("Test");
        lot.setControlLevel("NORMAL");
        lot.setTestId(testId);
        lot.setInstrumentId(analyzer.analyzerId());
        lot.setCalculationMethod("MANUFACTURER_FIXED");
        lot.setManufacturerMean(7.1);
        lot.setManufacturerStdDev(1.0);
        lot.setExpirationDate(Timestamp.valueOf("2099-12-31 00:00:00"));
        lot.setSystemUserId(Integer.valueOf(TEST_SYS_USER_ID));
        lot.setSysUserId(TEST_SYS_USER_ID);
        controlLots.createControlLot(lot);
        assertEquals("ACTIVE", controlLots.get(controlLotId).getStatus());
        assertEquals(Integer.valueOf(1), jdbc.queryForObject(
                "SELECT COUNT(*) FROM clinlims.qc_statistics WHERE control_lot_id = ?", Integer.class, controlLotId));
        Bundle bundle = bundle("normalized-qc.fhir.json");
        bundle.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).filter(Observation.class::isInstance)
                .map(Observation.class::cast)
                .forEach(observation -> observation.getExtensionByUrl("http://openelis-global.org/fhir/qc/lot-number")
                        .setValue(new StringType(lot.getLotNumber())));
        return bundle;
    }

    private Bundle bundle(String fixture) throws Exception {
        Bundle bundle = fhirContext.newJsonParser().parseResource(Bundle.class,
                Files.readString(FIXTURES.resolve(fixture)));
        bundle.getIdentifier().setValue(UUID.randomUUID().toString());
        Device device = device(bundle);
        device.getIdentifier().stream().filter(id -> id.getSystem().endsWith("analyzer-connection-id"))
                .forEach(id -> id.setValue(analyzer.bridgeConnectionId()));
        device.getIdentifier().stream().filter(id -> id.getSystem().endsWith("/analyzer-id"))
                .forEach(id -> id.setValue(analyzer.analyzerId()));
        device.getExtensionByUrl(EXTENSION + "profile-id").setValue(new StringType(RECEIPT_PROFILE_ID));
        device.getExtensionByUrl(EXTENSION + "profile-revision").setValue(new IntegerType(1));
        device.getExtensionByUrl(EXTENSION + "source-protocol").setValue(new CodeType("ASTM"));
        bundle.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).filter(Observation.class::isInstance)
                .map(Observation.class::cast).forEach(observation -> observation
                        .getExtensionByUrl(EXTENSION + "source-transport").setValue(new CodeType("TCP")));
        return bundle;
    }

    private Device device(Bundle bundle) {
        return bundle.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).filter(Device.class::isInstance)
                .map(Device.class::cast).findFirst().orElseThrow();
    }

    private void setControlValue(Bundle bundle, String value) {
        Observation observation = bundle.getEntry().stream().map(Bundle.BundleEntryComponent::getResource)
                .filter(Observation.class::isInstance).map(Observation.class::cast).findFirst().orElseThrow();
        observation.getValueQuantity().setValue(new BigDecimal(value));
        observation.getExtensionByUrl(EXTENSION + "raw-value").setValue(new StringType(value));
    }

    private int receiptCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM clinlims.analyzer_delivery_receipt WHERE connection_id = ?",
                Integer.class, analyzer.bridgeConnectionId());
    }

    private int stagingCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM clinlims.analyzer_results WHERE analyzer_id = ?::numeric",
                Integer.class, analyzer.analyzerId());
    }

    private int controlResultCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM clinlims.qc_result WHERE control_lot_id = ?", Integer.class,
                controlLotId);
    }

    private void removeStaging() {
        for (AnalyzerResults row : results.getResultsbyAnalyzer(analyzer.analyzerId())) {
            row.setSysUserId(TEST_SYS_USER_ID);
            results.delete(row);
            removedStagingIds.add(row.getId());
        }
    }

    @After
    public void removeOwnedFixtures() {
        if (jdbc == null) {
            return;
        }
        if (analyzer != null) {
            // History references are polymorphic; remove only this fixture's
            // owned records and history, never truncate or repair shared seeds.
            for (String id : jdbc.queryForList(
                    "SELECT id::text FROM clinlims.analyzer_results WHERE analyzer_id = ?::numeric", String.class,
                    analyzer.analyzerId())) {
                deleteHistory("analyzer_results", id);
            }
            removedStagingIds.forEach(id -> deleteHistory("analyzer_results", id));
            jdbc.update("DELETE FROM clinlims.analyzer_results WHERE analyzer_id = ?::numeric", analyzer.analyzerId());
            jdbc.update("DELETE FROM clinlims.qc_result WHERE control_lot_id = ?", controlLotId);
            jdbc.update("DELETE FROM clinlims.qc_statistics WHERE control_lot_id = ?", controlLotId);
            jdbc.update("DELETE FROM clinlims.qc_control_lot WHERE id = ?", controlLotId);
            jdbc.update("DELETE FROM clinlims.westgard_rule_config WHERE test_id = ?::numeric", testId);
            jdbc.update("DELETE FROM clinlims.analyzer_delivery_receipt WHERE analyzer_id = ?::numeric",
                    analyzer.analyzerId());
            jdbc.update("DELETE FROM clinlims.analyzer WHERE id = ?::numeric", analyzer.analyzerId());
            deleteHistory("analyzer", analyzer.analyzerId());
        }
        if (siteBindingId != null) {
            List<String> revisions = jdbc.queryForList(
                    "SELECT id::text FROM clinlims.analyzer_site_binding_revision WHERE site_binding_id = ?::numeric ORDER BY revision_number DESC",
                    String.class, siteBindingId);
            for (String revision : revisions) {
                List<String> confirmations = jdbc.queryForList(
                        "SELECT id::text FROM clinlims.analyzer_site_binding_confirmation WHERE site_binding_revision_id = ?::numeric",
                        String.class, revision);
                jdbc.update(
                        "DELETE FROM clinlims.analyzer_site_binding_confirmation WHERE site_binding_revision_id = ?::numeric",
                        revision);
                confirmations.forEach(id -> deleteHistory("analyzer_site_binding_confirmation", id));
                jdbc.update(
                        "DELETE FROM clinlims.analyzer_site_binding_result WHERE site_binding_revision_id = ?::numeric",
                        revision);
                jdbc.update(
                        "DELETE FROM clinlims.analyzer_site_binding_test WHERE site_binding_revision_id = ?::numeric",
                        revision);
                jdbc.update("DELETE FROM clinlims.analyzer_site_binding_revision WHERE id = ?::numeric", revision);
                deleteHistory("analyzer_site_binding_revision", revision);
            }
            jdbc.update("DELETE FROM clinlims.analyzer_site_binding WHERE id = ?::numeric", siteBindingId);
        }
        jdbc.update("DELETE FROM clinlims.analyzer_profile_binding WHERE id = ?::numeric", profileBindingId);
        jdbc.update("DELETE FROM clinlims.test WHERE id = ?::numeric", testId);
        jdbc.update("DELETE FROM clinlims.test_section WHERE id = ?::numeric", labUnitId);
        jdbc.update("DELETE FROM clinlims.localization WHERE id = ?::numeric", localizationId);
        deleteHistory("test", testId);
        deleteHistory("test_section", labUnitId);
        deleteHistory("localization", localizationId);
        assertEquals("Committed receipt fixtures must leave no owned rows or history behind", initialRowCounts,
                rowCounts());
    }

    private Map<String, Integer> rowCounts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String table : OWNED_TABLES) {
            counts.put(table, jdbc.queryForObject("SELECT COUNT(*) FROM clinlims." + table, Integer.class));
        }
        return counts;
    }

    private void deleteHistory(String table, String referenceId) {
        jdbc.update("DELETE FROM clinlims.history WHERE reference_id = ? AND reference_table IN"
                + " (SELECT id FROM clinlims.reference_tables WHERE lower(name) = ?)", referenceId, table);
    }
}
