package org.openelisglobal.analyzerimport.action;

import ca.uhn.fhir.context.FhirContext;
import jakarta.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Specimen;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzerimport.util.AnalyzerTestNameCache;
import org.openelisglobal.analyzerimport.util.MappedTestName;
import org.openelisglobal.analyzerresults.service.AnalyzerResultsService;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * FHIR R4 Bundle import endpoint for analyzer results.
 *
 * <p>
 * Receives a FHIR R4 transaction Bundle containing DiagnosticReport,
 * Observation, and Specimen resources from the analyzer bridge. Maps
 * Observations to {@link AnalyzerResults} staging rows — the same table used by
 * HL7, ASTM, and FILE import paths.
 *
 * <p>
 * This is the unified entry point for all analyzer results regardless of source
 * protocol. The bridge handles all format-specific parsing (HL7 v2, ASTM
 * LIS2-A2, Excel, CSV) and normalizes to FHIR R4.
 *
 * <p>
 * FHIR → AnalyzerResults mapping:
 * <ul>
 * <li>Specimen.identifier → accessionNumber</li>
 * <li>Observation.code.coding[0].code → testName</li>
 * <li>Observation.value[x] → result</li>
 * <li>Observation.valueQuantity.unit → units</li>
 * <li>X-Analyzer-Id header → analyzerId</li>
 * </ul>
 */
@RestController
public class AnalyzerFhirImportController extends org.openelisglobal.common.rest.BaseRestController {

    private static final String CLASS_NAME = "AnalyzerFhirImportController";

    @Autowired
    private AnalyzerResultsService analyzerResultsService;

    @Autowired
    private AnalyzerService analyzerService;

    @Autowired
    private FhirContext fhirContext;

    @PostMapping(value = "/analyzer/fhir", consumes = { "application/fhir+json", MediaType.APPLICATION_JSON_VALUE,
            MediaType.ALL_VALUE })
    public ResponseEntity<Map<String, Object>> importFhirBundle(HttpServletRequest request,
            @RequestHeader(value = "X-Analyzer-Id", required = false) String analyzerId) {

        Map<String, Object> response = new LinkedHashMap<>();

        try {
            String bundleJson = new String(request.getInputStream().readAllBytes(),
                    java.nio.charset.StandardCharsets.UTF_8);
            Bundle bundle = fhirContext.newJsonParser().parseResource(Bundle.class, bundleJson);

            if (bundle.getType() != Bundle.BundleType.TRANSACTION && bundle.getType() != Bundle.BundleType.BATCH) {
                response.put("success", false);
                response.put("error", "analyzer.fhirImport.error.invalidBundleType");
                response.put("errorKey", "analyzer.fhirImport.error.invalidBundleType");
                response.put("errorArgs", Map.of("bundleType", String.valueOf(bundle.getType())));
                return ResponseEntity.badRequest().body(response);
            }

            // Resolve analyzer from bridge identifier (composite like "MINDRAY-BC-5380")
            Analyzer analyzer = null;
            if (analyzerId != null && !analyzerId.isBlank()) {
                // Try numeric ID first (direct DB lookup)
                if (isNumericId(analyzerId)) {
                    analyzer = analyzerService.get(analyzerId);
                }
                if (analyzer == null) {
                    analyzer = analyzerService.findByIdentifierPatternMatch(analyzerId).orElse(null);
                }
                if (analyzer == null) {
                    // Try by name
                    analyzer = analyzerService.getByName(analyzerId).orElse(null);
                }
            }

            // Collect Specimen identifiers for accession number lookup
            Map<String, String> specimenAccessions = new LinkedHashMap<>();
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                Resource resource = entry.getResource();
                if (resource instanceof Specimen specimen) {
                    String fullUrl = entry.getFullUrl();
                    String accession = null;
                    if (specimen.hasIdentifier()) {
                        accession = specimen.getIdentifierFirstRep().getValue();
                    }
                    if (accession != null && fullUrl != null) {
                        specimenAccessions.put(fullUrl, accession);
                    }
                }
            }

            // Map Observations to AnalyzerResults
            List<AnalyzerResults> results = new ArrayList<>();
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                Resource resource = entry.getResource();
                if (resource instanceof Observation obs) {
                    AnalyzerResults ar = mapObservationToAnalyzerResult(obs, specimenAccessions, analyzer);
                    if (ar != null) {
                        results.add(ar);
                    }
                }
            }

            if (results.isEmpty()) {
                response.put("success", false);
                response.put("error", "analyzer.fhirImport.error.noObservations");
                response.put("errorKey", "analyzer.fhirImport.error.noObservations");
                return ResponseEntity.badRequest().body(response);
            }

            String userId = getSysUserId(request);
            if (userId == null) {
                LogEvent.logWarn(CLASS_NAME, "importFhirBundle",
                        "Could not resolve sysUserId from request — using system default");
                userId = "1";
            }
            analyzerResultsService.insertAnalyzerResults(results, userId);

            response.put("success", true);
            response.put("resultsInserted", results.size());
            response.put("analyzerId", analyzer != null ? analyzer.getId() : null);
            LogEvent.logInfo(CLASS_NAME, "importFhirBundle", "Inserted " + results.size() + " results from FHIR Bundle"
                    + (analyzer != null ? " for analyzer " + analyzer.getName() : ""));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            LogEvent.logError(CLASS_NAME, "importFhirBundle",
                    "FHIR Bundle import failed: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            response.put("success", false);
            response.put("error", "analyzer.fhirImport.error.importFailed");
            response.put("errorKey", "analyzer.fhirImport.error.importFailed");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    private boolean isNumericId(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return !value.isEmpty();
    }

    private AnalyzerResults mapObservationToAnalyzerResult(Observation obs, Map<String, String> specimenAccessions,
            Analyzer analyzer) {

        AnalyzerResults ar = new AnalyzerResults();

        // Analyzer ID
        if (analyzer != null) {
            ar.setAnalyzerId(analyzer.getId());
        }

        // Accession number from Specimen reference
        if (obs.hasSpecimen()) {
            String specimenRef = obs.getSpecimen().getReference();
            String accession = specimenAccessions.get(specimenRef);
            if (accession != null) {
                ar.setAccessionNumber(accession);
            }
        }
        // Fallback: check Observation.subject.identifier
        if (ar.getAccessionNumber() == null && obs.hasSubject() && obs.getSubject().hasIdentifier()) {
            ar.setAccessionNumber(obs.getSubject().getIdentifier().getValue());
        }

        if (ar.getAccessionNumber() == null) {
            LogEvent.logWarn(CLASS_NAME, "mapObservationToAnalyzerResult",
                    "Observation has no accession number — skipping");
            return null;
        }

        // Test code from Observation.code (raw analyzer code, e.g., "WBC")
        String testCode = null;
        if (obs.hasCode() && obs.getCode().hasCoding()) {
            var coding = obs.getCode().getCodingFirstRep();
            testCode = coding.getCode() != null ? coding.getCode() : coding.getDisplay();
        } else if (obs.hasCode() && obs.getCode().hasText()) {
            testCode = obs.getCode().getText();
        }

        LogEvent.logInfo(CLASS_NAME, "mapObservationToAnalyzerResult", "accession=" + ar.getAccessionNumber()
                + " testCode=" + testCode + " analyzerId=" + (analyzer != null ? analyzer.getId() : "null"));

        // Map raw test code → OE test ID via the cache (uses per-analyzer ID index).
        // On cache miss, force a reload and retry — the afterCommit cache refresh
        // may have run in a stale transaction context and missed newly-committed data.
        if (testCode != null && analyzer != null) {
            MappedTestName mapped = AnalyzerTestNameCache.getInstance().getMappedTestByAnalyzerId(analyzer.getId(),
                    testCode);
            if (mapped == null) {
                LogEvent.logInfo(CLASS_NAME, "mapObservationToAnalyzerResult", "Cache miss for analyzer "
                        + analyzer.getId() + " testCode=" + testCode + " — forcing reload and retry");
                AnalyzerTestNameCache.getInstance().reloadCache();
                mapped = AnalyzerTestNameCache.getInstance().getMappedTestByAnalyzerId(analyzer.getId(), testCode);
            }
            if (mapped != null && mapped.getTestId() != null && !"-1".equals(mapped.getTestId())) {
                ar.setTestId(mapped.getTestId());
                ar.setTestName(mapped.getOpenElisTestName());
            } else {
                ar.setTestName(testCode);
                ar.setReadOnly(true);
            }
        } else {
            ar.setTestName(testCode);
        }

        // Result value
        if (obs.hasValueQuantity()) {
            ar.setResult(obs.getValueQuantity().getValue().toPlainString());
            ar.setUnits(obs.getValueQuantity().getUnit());
            ar.setResultType("N");
        } else if (obs.hasValueStringType()) {
            ar.setResult(obs.getValueStringType().getValue());
            ar.setResultType("A");
        } else if (obs.hasValueCodeableConcept()) {
            ar.setResult(obs.getValueCodeableConcept().hasText() ? obs.getValueCodeableConcept().getText()
                    : obs.getValueCodeableConcept().getCodingFirstRep().getDisplay());
            ar.setResultType("A");
        }

        if (ar.getResult() == null) {
            LogEvent.logWarn(CLASS_NAME, "mapObservationToAnalyzerResult", "Observation has no value — skipping");
            return null;
        }

        // QC/control flag from bridge tag
        if (obs.hasMeta() && obs.getMeta().hasTag()) {
            boolean isQc = obs.getMeta().getTag().stream().anyMatch(t -> "QC".equals(t.getCode()));
            ar.setIsControl(isQc);
        }

        // Completion timestamp from effectiveDateTime
        if (obs.hasEffectiveDateTimeType()) {
            try {
                ar.setCompleteDate(new Timestamp(obs.getEffectiveDateTimeType().getValue().getTime()));
            } catch (Exception e) {
                ar.setCompleteDate(new Timestamp(System.currentTimeMillis()));
            }
        } else {
            ar.setCompleteDate(new Timestamp(System.currentTimeMillis()));
        }

        // testId is set by the mapping lookup above; if unmapped, it stays null
        // and readOnly=true so the result shows as "configuration needed"
        if (ar.getTestId() == null) {
            ar.setReadOnly(true);
        }

        return ar;
    }
}
