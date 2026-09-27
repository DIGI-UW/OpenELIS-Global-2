package org.openelisglobal.analyzerimport.action;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.hl7.fhir.r4.model.Bundle;
import org.openelisglobal.analyzerimport.service.AnalyzerNormalizedResultImportException;
import org.openelisglobal.analyzerimport.service.AnalyzerNormalizedResultImportService;
import org.openelisglobal.analyzerimport.service.AnalyzerNormalizedResultImportSummary;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Receives the versioned normalized result contract emitted by Analyzer Bridge.
 */
@RestController
public class AnalyzerFhirImportController extends BaseRestController {

    private static final String CLASS_NAME = "AnalyzerFhirImportController";

    @Autowired
    private FhirContext fhirContext;

    @Autowired
    private AnalyzerNormalizedResultImportService importService;

    @PostMapping(value = "/analyzer/fhir", consumes = { "application/fhir+json", MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<Map<String, Object>> importFhirBundle(HttpServletRequest request) {
        try {
            String body = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            Bundle bundle = fhirContext.newJsonParser().parseResource(Bundle.class, body);
            String actor = getSysUserId(request);
            AnalyzerNormalizedResultImportSummary summary = importService.importBundle(bundle,
                    actor == null || actor.isBlank() ? "1" : actor);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("analyzerId", summary.analyzerId());
            response.put("resultsStaged", summary.resultsStaged());
            response.put("resultsHeld", summary.resultsHeld());
            response.put("controlResultsProcessed", summary.controlResultsProcessed());
            return ResponseEntity.ok(response);
        } catch (AnalyzerNormalizedResultImportException exception) {
            return error(HttpStatus.UNPROCESSABLE_ENTITY, exception.getErrorKey(), exception.getMessage());
        } catch (DataFormatException exception) {
            return error(HttpStatus.BAD_REQUEST, "analyzer.fhirImport.error.invalidPayload",
                    "Request body is not valid FHIR JSON");
        } catch (IllegalArgumentException exception) {
            return error(HttpStatus.BAD_REQUEST, "analyzer.fhirImport.error.invalidContract", exception.getMessage());
        } catch (Exception exception) {
            LogEvent.logError(exception);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "analyzer.fhirImport.error.importFailed",
                    "Analyzer result import failed");
        }
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String errorKey, String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", false);
        response.put("error", message);
        response.put("errorKey", errorKey);
        LogEvent.logWarn(CLASS_NAME, "importFhirBundle", errorKey + ": " + message);
        return ResponseEntity.status(status).body(response);
    }
}
