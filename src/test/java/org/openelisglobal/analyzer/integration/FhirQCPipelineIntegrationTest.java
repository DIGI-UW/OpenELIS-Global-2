package org.openelisglobal.analyzer.integration;

import static org.junit.Assert.*;

import ca.uhn.fhir.context.FhirContext;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Specimen;
import org.hl7.fhir.r4.model.StringType;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzerimport.action.AnalyzerFhirImportController;
import org.openelisglobal.analyzerimport.util.AnalyzerTestNameCache;
import org.openelisglobal.qc.dao.QCResultDAO;
import org.openelisglobal.qc.dao.QCRuleViolationDAO;
import org.openelisglobal.qc.service.QCControlLotService;
import org.openelisglobal.qc.valueholder.QCControlLot;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCRuleViolation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Integration test for the full FHIR QC pipeline:
 *
 * <pre>
 * FHIR Bundle (with QC-tagged Observation)
 *   -> AnalyzerFhirImportController
 *   -> AnalyzerResults staging (isControl=true)
 *   -> QCResultProcessingService (lot lookup by accession)
 *   -> QCResultService.createQCResult (z-score calculation)
 *   -> QCResultCreatedEvent (async)
 *   -> WestgardRuleEvaluationService (rule evaluation)
 *   -> QCRuleViolation creation
 * </pre>
 *
 * <p>
 * Test data (fhir-qc-pipeline.xml):
 * <ul>
 * <li>Analyzer id=1 ("TEST-FHIR-ANALYZER")</li>
 * <li>Test id=1 ("Glucose"), mapped as "GLU" on analyzer 1</li>
 * <li>Control lot "lot-fhir-001" (ACTIVE, lot_number="QC-LOT-2025-GLU",
 * mean=100.0, SD=5.0)</li>
 * <li>Westgard rules: 1_3s (REJECTION) + 1_2s (WARNING)</li>
 * </ul>
 */
public class FhirQCPipelineIntegrationTest extends BaseWebContextSensitiveTest {

    private static final int ASYNC_WAIT_MS = 1500;

    private final FhirContext realFhirContext = FhirContext.forR4();

    @Autowired
    private AnalyzerFhirImportController controller;

    @Autowired
    private QCResultDAO qcResultDAO;

    @Autowired
    private QCRuleViolationDAO violationDAO;

    @Autowired
    private QCControlLotService controlLotService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        // Replace the mocked FhirContext with a real one so the controller can
        // parse FHIR R4 bundles.
        ReflectionTestUtils.setField(controller, "fhirContext", realFhirContext);
        executeDataSetWithStateManagement("testdata/fhir-qc-pipeline.xml");
        // The analyzer test-code -> test mapping lives in a static singleton cache
        // (AnalyzerTestNameCache) that lazy-loads once per JVM. In a full-suite run
        // an earlier test can prime it before this dataset is inserted, so "GLU"
        // resolves to nothing and the pipeline processes 0 QC results. Rebuild it
        // from the freshly-loaded dataset so this test is order-independent.
        AnalyzerTestNameCache.getInstance().reloadCache();
    }

    /**
     * A FHIR bundle with a QC-tagged Observation whose value (120) exceeds 3SD
     * (mean=100, SD=5 -> z=4.0) should:
     * <ol>
     * <li>Be accepted by the controller (HTTP 200)</li>
     * <li>Create a QCResult with z-score = 4.0</li>
     * <li>Trigger 2 Westgard violations: 1_3s REJECTION + 1_2s WARNING</li>
     * <li>Update the QCResult status to REJECTED</li>
     * </ol>
     *
     * <p>
     * The accession number ("QC-LOT-2025-GLU") matches the configured control lot's
     * lot_number, and "GLU" maps to test id=1 via analyzer_test_map.
     */
    @Test
    public void fhirBundle_withQCObservationExceeding3SD_createsViolations() throws InterruptedException {
        // Arrange: build FHIR bundle with a QC result of 120 mg/dL
        String bundleJson = buildQCFhirBundle("QC-LOT-2025-GLU", "GLU", new BigDecimal("120.0"), "mg/dL");

        System.out.println("bundleJson: " + bundleJson);

        // Act: POST to the FHIR import endpoint
        ResponseEntity<Map<String, Object>> response = postFhirBundle(bundleJson, "1");

        // Assert: controller accepted the bundle
        assertEquals("HTTP status should be 200", 200, response.getStatusCode().value());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("success should be true", true, body.get("success"));
        assertEquals("Should insert 1 result", 1, body.get("resultsInserted"));
        assertEquals("Should process 1 QC result", 1, body.get("qcResultsProcessed"));

        // Assert: QCResult was created with correct z-score
        // z = (120 - 100) / 5 = 4.0
        List<QCResult> qcResults = qcResultDAO.getAll();
        assertEquals("Should have exactly 1 QC result", 1, qcResults.size());

        QCResult qcResult = qcResults.get(0);
        assertEquals("Result value should be 120.0", 0,
                new BigDecimal("120.00000").compareTo(qcResult.getResultValue()));
        assertEquals("Z-score should be 4.0000", 0, new BigDecimal("4.0000").compareTo(qcResult.getZScore()));
        assertEquals("Control lot should be lot-fhir-001", "lot-fhir-001", qcResult.getControlLotId());
        assertEquals("1", qcResult.getTestId());
        assertEquals("1", qcResult.getInstrumentId());
        assertEquals("Unit should be mg/dL", "mg/dL", qcResult.getUnitOfMeasure());

        // Wait for async event processing (event -> rule evaluation -> violations)
        Thread.sleep(ASYNC_WAIT_MS);

        // Assert: Westgard rules triggered violations
        List<QCRuleViolation> violations = violationDAO.findByInstrument("1");
        assertEquals("Should have 2 violations (1_3s + 1_2s)", 2, violations.size());

        // Verify REJECTION violation (1_3s)
        List<QCRuleViolation> rejections = violations.stream().filter(v -> "REJECTION".equals(v.getSeverity()))
                .collect(Collectors.toList());
        assertEquals("Should have 1 REJECTION violation", 1, rejections.size());
        QCRuleViolation rejection = rejections.get(0);
        assertEquals("REJECTION rule code should be 1₃ₛ", "1₃ₛ", rejection.getRuleCode());
        assertEquals("REJECTION status should be UNRESOLVED", "UNRESOLVED", rejection.getResolutionStatus());
        assertEquals("REJECTION triggering result should match QC result ID", qcResult.getId(),
                rejection.getTriggeringResultId());

        // Verify WARNING violation (1_2s)
        List<QCRuleViolation> warnings = violations.stream().filter(v -> "WARNING".equals(v.getSeverity()))
                .collect(Collectors.toList());
        assertEquals("Should have 1 WARNING violation", 1, warnings.size());
        assertEquals("WARNING rule code should be 1₂ₛ", "1₂ₛ", warnings.get(0).getRuleCode());
        assertEquals("WARNING triggering result should match QC result ID", qcResult.getId(),
                warnings.get(0).getTriggeringResultId());

        // Assert: QCResult status updated to REJECTED by event listener
        QCResult updated = qcResultDAO.get(qcResult.getId()).orElseThrow();
        assertEquals("Result status should be REJECTED after 1_3s violation", "REJECTED", updated.getResultStatus());
    }

    /**
     * Proves discrimination: an abnormal QC result triggers violations, then a
     * normal QC result (z=0.4) produces NO additional violations. Without the
     * precondition, asserting "0 violations for the second result" would be a false
     * positive (would also pass if the pipeline was completely broken).
     */
    @Test
    public void fhirBundle_withNormalQCResult_createsNoViolations() throws InterruptedException {
        // Precondition: send an abnormal result to prove the pipeline works
        String abnormalBundle = buildQCFhirBundle("QC-LOT-2025-GLU", "GLU", new BigDecimal("112.0"), "mg/dL");
        postFhirBundle(abnormalBundle, "1");
        Thread.sleep(ASYNC_WAIT_MS);

        List<QCRuleViolation> preconditionViolations = violationDAO.findByInstrument("1");
        assertEquals("Precondition: pipeline must produce 1 WARNING violation", 1, preconditionViolations.size());
        assertEquals("Precondition: violation severity should be WARNING", "WARNING",
                preconditionViolations.get(0).getSeverity());

        // Act: send a normal result (value=102, z=(102-100)/5 = 0.4)
        String normalBundle = buildQCFhirBundle("QC-LOT-2025-GLU", "GLU", new BigDecimal("102.0"), "mg/dL");
        ResponseEntity<Map<String, Object>> response = postFhirBundle(normalBundle, "1");

        assertEquals(200, response.getStatusCode().value());
        Thread.sleep(ASYNC_WAIT_MS);

        // Assert: still only 1 violation (the precondition one)
        List<QCRuleViolation> allViolations = violationDAO.findByInstrument("1");
        assertEquals("Should still have only 1 violation from precondition", 1, allViolations.size());

        // Assert: the normal result has status ACCEPTED
        List<QCResult> qcResults = qcResultDAO.getAll();
        assertEquals("Should have 2 QC results total", 2, qcResults.size());
        QCResult normalResult = qcResults.stream()
                .filter(r -> r.getResultValue().compareTo(new BigDecimal("102.00000")) == 0).findFirst().orElseThrow();
        // Wait and re-read for async status update
        QCResult updatedNormal = qcResultDAO.get(normalResult.getId()).orElseThrow();
        assertEquals("Normal result should be ACCEPTED", "ACCEPTED", updatedNormal.getResultStatus());
    }

    /**
     * When the QC Observation's accession number doesn't match any configured
     * control lot's lotNumber, but exactly one ACTIVE lot exists for the (testId,
     * instrumentId) pair, the controller should fall back to that single active lot
     * and create a QCResult against it. This Tier 2 fallback is documented in
     * {@code QCResultProcessingServiceImpl.findMatchingControlLot} and exists
     * specifically for FILE analyzers whose specimen IDs (e.g., CNEG001, NTC001)
     * are per-run identifiers unrelated to lot numbers.
     */
    @Test
    public void fhirBundle_withUnmatchedLot_fallsBackToSingleActiveLot() {
        // Arrange: accession = "UNKNOWN-LOT" (no strict match to lot_number
        // "QC-LOT-2025-GLU"), but fixture has exactly 1 ACTIVE lot for
        // (testId=1, instrumentId=1) → Tier 2 fallback applies.
        String bundle = buildQCFhirBundle("UNKNOWN-LOT", "GLU", new BigDecimal("102.0"), "mg/dL");

        // Act
        ResponseEntity<Map<String, Object>> response = postFhirBundle(bundle, "1");

        // Assert: controller accepted the bundle
        assertEquals(200, response.getStatusCode().value());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = response.getBody();
        assertEquals("Should insert 1 staging result", 1, body.get("resultsInserted"));
        assertEquals("QC processing should be attempted", 1, body.get("qcResultsProcessed"));

        // Assert: QCResult WAS created via Tier 2 fallback against the single
        // active lot (lot_number="QC-LOT-2025-GLU", not the "UNKNOWN-LOT"
        // accession). Value 102 is within 1SD of mean=100,SD=5 → ACCEPTED,
        // no violation.
        List<QCResult> qcResults = qcResultDAO.getAll();
        assertEquals("Fallback lot should yield 1 QC result", 1, qcResults.size());
        List<QCRuleViolation> violations = violationDAO.findByInstrument("1");
        assertEquals("In-range fallback result should not produce a violation", 0, violations.size());
    }

    /**
     * A non-QC (patient) Observation in the FHIR bundle should NOT trigger any QC
     * processing.
     */
    @Test
    public void fhirBundle_withPatientObservation_doesNotTriggerQCProcessing() {
        // Arrange: build a FHIR bundle WITHOUT the QC meta tag
        String bundle = buildPatientFhirBundle("ACC-001", "GLU", new BigDecimal("95.0"), "mg/dL");

        // Act
        ResponseEntity<Map<String, Object>> response = postFhirBundle(bundle, "1");

        // Assert
        assertEquals(200, response.getStatusCode().value());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = response.getBody();
        assertEquals("Should insert 1 staging result", 1, body.get("resultsInserted"));
        assertEquals("Should process 0 QC results", 0, body.get("qcResultsProcessed"));

        List<QCResult> qcResults = qcResultDAO.getAll();
        assertEquals("No QC result should be created for patient sample", 0, qcResults.size());
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * POST a FHIR JSON bundle to the controller, simulating a bridge request.
     */
    private ResponseEntity<Map<String, Object>> postFhirBundle(String bundleJson, String analyzerId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent(bundleJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        request.setContentType("application/fhir+json");
        return controller.importFhirBundle(request, analyzerId);
    }

    /**
     * Build a FHIR R4 transaction Bundle with a single QC-tagged Observation.
     */
    private String buildQCFhirBundle(String accessionNumber, String testCode, BigDecimal value, String unit) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.TRANSACTION);

        // Specimen entry
        String specimenUrl = "urn:uuid:" + UUID.randomUUID();
        Specimen specimen = new Specimen();
        specimen.addIdentifier().setValue(accessionNumber);
        bundle.addEntry().setFullUrl(specimenUrl).setResource(specimen).getRequest().setMethod(Bundle.HTTPVerb.POST)
                .setUrl("Specimen");

        // QC Observation entry
        Observation obs = new Observation();
        obs.getMeta().addTag(new Coding().setSystem("http://openelis-global.org/fhir/tags").setCode("QC")
                .setDisplay("Quality Control"));
        obs.getCode().addCoding().setCode(testCode).setDisplay(testCode);
        obs.setValue(new Quantity().setValue(value).setUnit(unit));
        obs.getSpecimen().setReference(specimenUrl);
        obs.setEffective(new org.hl7.fhir.r4.model.DateTimeType(java.util.Date.from(java.time.Instant.now())));

        bundle.addEntry().setFullUrl("urn:uuid:" + UUID.randomUUID()).setResource(obs).getRequest()
                .setMethod(Bundle.HTTPVerb.POST).setUrl("Observation");

        return realFhirContext.newJsonParser().encodeResourceToString(bundle);
    }

    /**
     * Tier 2 disambiguation via FHIR controlLevel extension. With multiple usable
     * lots on (testId=1, instrumentId=1) — the seeded ACTIVE lot
     * (controlLevel=NORMAL) plus an additional ESTABLISHMENT lot (controlLevel=LPC)
     * inserted at runtime — a bundle whose Observation carries the
     * qc/control-level=LPC extension must resolve to the LPC lot, NOT the seeded
     * NORMAL one. Without the extension, resolution would be ambiguous (Tier 3
     * single-active is bypassed because there are 2 usable lots).
     *
     * <p>
     * The extra lot is ESTABLISHMENT specifically so it doesn't need statistics
     * (z-score is null during establishment per QCResultServiceImpl) — keeps the
     * fixture light.
     */
    @Test
    public void fhirBundle_withControlLevelExtension_disambiguatesAmongMultipleUsableLots() {
        String lpcLotId = insertEstablishmentLot("LOT-LPC-INT", "LPC", "1", "1");

        // Bundle accession is unrelated to either lot's lot_number, so Tier 1
        // can't match. Without the controlLevel extension, Tier 3 also can't
        // pick because there are 2 usable lots.
        String bundle = buildQCFhirBundleWithExtensions("UNRELATED-ACC", "GLU", new BigDecimal("100.0"), "mg/dL", null,
                "LPC");

        ResponseEntity<Map<String, Object>> response = postFhirBundle(bundle, "1");
        assertEquals(200, response.getStatusCode().value());

        List<QCResult> qcResults = qcResultDAO.getAll();
        assertEquals("Should have exactly 1 QC result resolved via controlLevel", 1, qcResults.size());
        assertEquals("controlLevel extension must select the LPC lot, not the seeded NORMAL lot", lpcLotId,
                qcResults.get(0).getControlLotId());
    }

    /**
     * Tier 1 lot-number override. Bundle accession is unrelated to either lot, but
     * the qc/lot-number extension explicitly names a usable lot — resolution must
     * pick that lot regardless of accession.
     */
    @Test
    public void fhirBundle_withLotNumberExtension_picksExactLotIgnoringAccession() {
        String extraLotId = insertEstablishmentLot("LOT-EXTRA-INT", "HPC", "1", "1");

        String bundle = buildQCFhirBundleWithExtensions("UNRELATED-ACC-2", "GLU", new BigDecimal("100.0"), "mg/dL",
                "LOT-EXTRA-INT", null);

        ResponseEntity<Map<String, Object>> response = postFhirBundle(bundle, "1");
        assertEquals(200, response.getStatusCode().value());

        List<QCResult> qcResults = qcResultDAO.getAll();
        assertEquals("Should have exactly 1 QC result resolved via lotNumber", 1, qcResults.size());
        assertEquals("lot-number extension must select LOT-EXTRA-INT, ignoring accession", extraLotId,
                qcResults.get(0).getControlLotId());
    }

    /**
     * Tier 2 ambiguity surfaces. Two ACTIVE/ESTABLISHMENT lots share controlLevel
     * for the same (test, instrument) — schema does not enforce uniqueness — and
     * the resolver must refuse rather than pick the first by DAO order.
     */
    @Test
    public void fhirBundle_withControlLevelExtension_butAmbiguousMatch_doesNotResolve() {
        insertEstablishmentLot("LOT-LPC-DUP-A", "LPC", "1", "1");
        insertEstablishmentLot("LOT-LPC-DUP-B", "LPC", "1", "1");

        String bundle = buildQCFhirBundleWithExtensions("UNRELATED-ACC-3", "GLU", new BigDecimal("100.0"), "mg/dL",
                null, "LPC");

        ResponseEntity<Map<String, Object>> response = postFhirBundle(bundle, "1");
        // Controller still accepts the bundle — staging insert succeeds.
        assertEquals(200, response.getStatusCode().value());

        // No QCResult — resolver returned null on ambiguous match, surfaced
        // as a logged error rather than silent first-match selection.
        List<QCResult> qcResults = qcResultDAO.getAll();
        assertEquals("Ambiguous controlLevel must NOT create a QC result", 0, qcResults.size());
    }

    /**
     * Insert a minimal ESTABLISHMENT lot at runtime so the test can exercise
     * multi-lot scenarios without modifying the shared fixture. ESTABLISHMENT
     * status skips the z-score precondition in QCResultServiceImpl.
     */
    private String insertEstablishmentLot(String lotNumber, String controlLevel, String testId, String instrumentId) {
        QCControlLot lot = new QCControlLot();
        lot.setId(UUID.randomUUID().toString());
        lot.setFhirUuid(UUID.randomUUID());
        lot.setLotNumber(lotNumber);
        lot.setControlLevel(controlLevel);
        lot.setProductName("test-product-" + lotNumber);
        lot.setManufacturer("test-mfr");
        lot.setStatus("ESTABLISHMENT");
        lot.setTestId(testId);
        lot.setInstrumentId(instrumentId);
        lot.setCalculationMethod("INITIAL_RUNS");
        lot.setInitialRunsCount(20);
        lot.setActivationDate(new Timestamp(System.currentTimeMillis()));
        lot.setSystemUserId(1);
        lot.setSysUserId("1");
        return controlLotService.insert(lot);
    }

    /**
     * Build a FHIR R4 transaction Bundle with a QC-tagged Observation carrying
     * optional qc/lot-number and qc/control-level extensions.
     */
    private String buildQCFhirBundleWithExtensions(String accessionNumber, String testCode, BigDecimal value,
            String unit, String lotNumberExt, String controlLevelExt) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.TRANSACTION);

        String specimenUrl = "urn:uuid:" + UUID.randomUUID();
        Specimen specimen = new Specimen();
        specimen.addIdentifier().setValue(accessionNumber);
        bundle.addEntry().setFullUrl(specimenUrl).setResource(specimen).getRequest().setMethod(Bundle.HTTPVerb.POST)
                .setUrl("Specimen");

        Observation obs = new Observation();
        obs.getMeta().addTag(new Coding().setSystem("http://openelis-global.org/fhir/tags").setCode("QC")
                .setDisplay("Quality Control"));
        obs.getCode().addCoding().setCode(testCode).setDisplay(testCode);
        obs.setValue(new Quantity().setValue(value).setUnit(unit));
        obs.getSpecimen().setReference(specimenUrl);
        obs.setEffective(new org.hl7.fhir.r4.model.DateTimeType(java.util.Date.from(java.time.Instant.now())));

        if (lotNumberExt != null) {
            Extension ext = new Extension();
            ext.setUrl("http://openelis-global.org/fhir/qc/lot-number");
            ext.setValue(new StringType(lotNumberExt));
            obs.addExtension(ext);
        }
        if (controlLevelExt != null) {
            Extension ext = new Extension();
            ext.setUrl("http://openelis-global.org/fhir/qc/control-level");
            ext.setValue(new StringType(controlLevelExt));
            obs.addExtension(ext);
        }

        bundle.addEntry().setFullUrl("urn:uuid:" + UUID.randomUUID()).setResource(obs).getRequest()
                .setMethod(Bundle.HTTPVerb.POST).setUrl("Observation");

        return realFhirContext.newJsonParser().encodeResourceToString(bundle);
    }

    /**
     * Build a FHIR R4 transaction Bundle with a single patient (non-QC) Observation
     * — no QC meta tag.
     */
    private String buildPatientFhirBundle(String accessionNumber, String testCode, BigDecimal value, String unit) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.TRANSACTION);

        String specimenUrl = "urn:uuid:" + UUID.randomUUID();
        Specimen specimen = new Specimen();
        specimen.addIdentifier().setValue(accessionNumber);
        bundle.addEntry().setFullUrl(specimenUrl).setResource(specimen).getRequest().setMethod(Bundle.HTTPVerb.POST)
                .setUrl("Specimen");

        Observation obs = new Observation();
        // No QC meta tag — this is a patient observation
        obs.getCode().addCoding().setCode(testCode).setDisplay(testCode);
        obs.setValue(new Quantity().setValue(value).setUnit(unit));
        obs.getSpecimen().setReference(specimenUrl);

        bundle.addEntry().setFullUrl("urn:uuid:" + UUID.randomUUID()).setResource(obs).getRequest()
                .setMethod(Bundle.HTTPVerb.POST).setUrl("Observation");

        return realFhirContext.newJsonParser().encodeResourceToString(bundle);
    }
}
