package org.openelisglobal.fhir.facade.provider;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;
import org.hl7.fhir.r4.model.Resource;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;
import org.openelisglobal.fhir.facade.FhirFacadeServlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fhir")
public class FhirWriteController {

    private static final String FHIR_JSON = "application/fhir+json";

    @Autowired
    private FhirPersistanceService fhirPersistanceService;

    @PostMapping(value = "/{resourceType}", produces = { FHIR_JSON, MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<String> createResource(@PathVariable String resourceType, HttpServletRequest request) {
        LogEvent.logInfo(this.getClass().getSimpleName(), "createResource", "Creating " + resourceType);

        try {
            String body = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            Resource resource = (Resource) FhirFacadeServlet.getJsonParser().parseResource(body);
            Bundle result = fhirPersistanceService.createFhirResourceInFhirStore(resource);

            String json = FhirFacadeServlet.getJsonParser().encodeResourceToString(result);
            return ResponseEntity.status(HttpStatus.CREATED).contentType(MediaType.parseMediaType(FHIR_JSON))
                    .body(json);
        } catch (IOException e) {
            LogEvent.logError(this.getClass().getSimpleName(), "createResource", "IO Error: " + e.getMessage());
            return createErrorResponse(HttpStatus.BAD_REQUEST, "Failed to read request body: " + e.getMessage());
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "createResource", "Error: " + e.getMessage());
            return createErrorResponse(HttpStatus.BAD_REQUEST, "Failed to create resource: " + e.getMessage());
        }
    }

    @PutMapping(value = "/{resourceType}/{id}", produces = { FHIR_JSON, MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<String> updateResource(@PathVariable String resourceType, @PathVariable String id,
            HttpServletRequest request) {
        LogEvent.logInfo(this.getClass().getSimpleName(), "updateResource", "Updating " + resourceType + "/" + id);

        try {
            String body = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            Resource resource = (Resource) FhirFacadeServlet.getJsonParser().parseResource(body);
            if (resource.getId() == null || resource.getId().isEmpty()) {
                resource.setId(id);
            }
            Bundle result = fhirPersistanceService.updateFhirResourceInFhirStore(resource);

            String json = FhirFacadeServlet.getJsonParser().encodeResourceToString(result);
            return ResponseEntity.ok().contentType(MediaType.parseMediaType(FHIR_JSON)).body(json);
        } catch (IOException e) {
            LogEvent.logError(this.getClass().getSimpleName(), "updateResource", "IO Error: " + e.getMessage());
            return createErrorResponse(HttpStatus.BAD_REQUEST, "Failed to read request body: " + e.getMessage());
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "updateResource", "Error: " + e.getMessage());
            return createErrorResponse(HttpStatus.BAD_REQUEST, "Failed to update resource: " + e.getMessage());
        }
    }

    private ResponseEntity<String> createErrorResponse(HttpStatus status, String message) {
        OperationOutcome outcome = new OperationOutcome();
        outcome.addIssue().setSeverity(status.is4xxClientError() ? IssueSeverity.WARNING : IssueSeverity.ERROR)
                .setCode(IssueType.EXCEPTION).setDiagnostics(message);

        String json = FhirFacadeServlet.getJsonParser().encodeResourceToString(outcome);
        return ResponseEntity.status(status).contentType(MediaType.parseMediaType(FHIR_JSON)).body(json);
    }
}
