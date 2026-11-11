package org.openelisglobal.dataexchange.fhir.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.hibernate.ObjectNotFoundException;
import org.hl7.fhir.r4.model.Bundle;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException;
import org.openelisglobal.dataexchange.fhir.form.FhirReplayRequest;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/fhir")
@PreAuthorize("hasRole('ADMIN')")
public class FhirReplayRestController extends BaseRestController {

    private final FhirTransformService fhirTransformService;

    public FhirReplayRestController(@Lazy FhirTransformService fhirTransformService) {
        this.fhirTransformService = fhirTransformService;
    }

    @PostMapping(value = "/replay", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> replay(@Valid @RequestBody FhirReplayRequest request) {
        List<String> sampleIds = request.getSampleIds().stream().distinct().toList();
        try {
            Bundle response = fhirTransformService.transformPersistObjectsUnderSamples(sampleIds).get(120,
                    TimeUnit.SECONDS);
            if (response == null || response.getType() != Bundle.BundleType.TRANSACTIONRESPONSE
                    || response.getEntry().isEmpty()
                    || response.getEntry().stream()
                            .anyMatch(entry -> !entry.hasResponse() || !entry.getResponse().hasStatus()
                                    || !entry.getResponse().getStatus().matches("2[0-9]{2}( .*)?"))) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .body(Map.of("error", "FHIR_REPLAY_INVALID_RESPONSE"));
            }
            return ResponseEntity.ok(
                    Map.of("status", "completed", "sampleIds", sampleIds, "resourceCount", response.getEntry().size()));
        } catch (ExecutionException e) {
            LogEvent.logError(e);
            if (e.getCause() instanceof ObjectNotFoundException) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "FHIR_REPLAY_SAMPLE_NOT_FOUND"));
            }
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", "FHIR_REPLAY_FAILED"));
        } catch (FhirLocalPersistingException e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", "FHIR_REPLAY_FAILED"));
        } catch (TimeoutException e) {
            // The worker may still finish; a timeout must not be reported as completion.
            return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                    .body(Map.of("error", "FHIR_REPLAY_COMPLETION_UNKNOWN"));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "FHIR_REPLAY_COMPLETION_UNKNOWN"));
        }
    }
}
