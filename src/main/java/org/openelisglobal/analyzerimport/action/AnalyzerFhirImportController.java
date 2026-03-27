package org.openelisglobal.analyzerimport.action;

import ca.uhn.fhir.context.FhirContext;
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
import org.openelisglobal.analyzerresults.service.AnalyzerResultsService;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
public class AnalyzerFhirImportController {

    private static final String CLASS_NAME = "AnalyzerFhirImportController";

    @Autowired
    private AnalyzerResultsService analyzerResultsService;

    @Autowired
    private AnalyzerService analyzerService;

    @Autowired
    private FhirContext fhirContext;

    @PostMapping(value = "/analyzer/fhir", consumes = "application/fhir+json")
    public ResponseEntity<Map<String, Object>> importFhirBundle(@RequestBody String bundleJson,
            @RequestHeader(value = "X-Analyzer-Id", required = false) String analyzerId) {

        Map<String, Object> response = new LinkedHashMap<>();

        try {
            Bundle bundle = fhirContext.newJsonParser().parseResource(Bundle.class, bundleJson);

            if (bundle.getType() != Bundle.BundleType.TRANSACTION && bundle.getType() != Bundle.BundleType.BATCH) {
                response.put("success", false);
                response.put("error", "Bundle type must be 'transaction' or 'batch', got: " + bundle.getType());
                return ResponseEntity.badRequest().body(response);
            }

            // Resolve analyzer
            Analyzer analyzer = null;
            if (analyzerId != null && !analyzerId.isBlank()) {
                analyzer = analyzerService.get(analyzerId);
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
                response.put("error", "Bundle contains no Observation resources with results");
                return ResponseEntity.badRequest().body(response);
            }

            String userId = "1"; // System user for bridge imports
            analyzerResultsService.insertAnalyzerResults(results, userId);

            response.put("success", true);
            response.put("resultsInserted", results.size());
            response.put("analyzerId", analyzer != null ? analyzer.getId() : null);
            LogEvent.logInfo(CLASS_NAME, "importFhirBundle", "Inserted " + results.size() + " results from FHIR Bundle"
                    + (analyzer != null ? " for analyzer " + analyzer.getName() : ""));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            LogEvent.logError(CLASS_NAME, "importFhirBundle", "FHIR Bundle import failed: " + e.getMessage());
            response.put("success", false);
            response.put("error", "FHIR Bundle import failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
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

        // Test name from code
        if (obs.hasCode() && obs.getCode().hasCoding()) {
            var coding = obs.getCode().getCodingFirstRep();
            ar.setTestName(coding.getCode() != null ? coding.getCode() : coding.getDisplay());
        } else if (obs.hasCode() && obs.getCode().hasText()) {
            ar.setTestName(obs.getCode().getText());
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

        ar.setTestId("-1"); // Resolved later during result review
        ar.setCompleteDate(new Timestamp(System.currentTimeMillis()));

        return ar;
    }
}
