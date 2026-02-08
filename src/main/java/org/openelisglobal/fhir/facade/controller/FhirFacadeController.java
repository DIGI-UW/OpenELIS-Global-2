package org.openelisglobal.fhir.facade.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.openelisglobal.fhir.facade.service.FhirFacadeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fhir")
public class FhirFacadeController {

    private static final String FHIR_JSON = "application/fhir+json";

    @Autowired
    private FhirFacadeService fhirFacadeService;

    @GetMapping(value = "/metadata", produces = { FHIR_JSON, MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<String> metadata() {
        String body = fhirFacadeService.getCapabilityStatement();
        return buildResponse(body);
    }

    @GetMapping(value = "/{resourceType}", produces = { FHIR_JSON, MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<String> search(@PathVariable String resourceType, @RequestHeader HttpHeaders headers,
            HttpServletRequest request) {
        String body = fhirFacadeService.proxyGetRequest(resourceType, request.getQueryString(), headers);
        return buildResponse(body);
    }

    @GetMapping(value = "/{resourceType}/{id}", produces = { FHIR_JSON, MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<String> read(@PathVariable String resourceType, @PathVariable String id,
            @RequestHeader HttpHeaders headers) {
        String body = fhirFacadeService.proxyGetRequest(resourceType + "/" + id, null, headers);
        return buildResponse(body);
    }

    @GetMapping(value = "/{resourceType}/{id}/_history", produces = { FHIR_JSON, MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<String> history(@PathVariable String resourceType, @PathVariable String id,
            @RequestHeader HttpHeaders headers, HttpServletRequest request) {
        String body = fhirFacadeService.proxyGetRequest(resourceType + "/" + id + "/_history", request.getQueryString(),
                headers);
        return buildResponse(body);
    }

    @GetMapping(value = "/{resourceType}/{id}/_history/{vid}", produces = { FHIR_JSON,
            MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<String> vread(@PathVariable String resourceType, @PathVariable String id,
            @PathVariable String vid, @RequestHeader HttpHeaders headers) {
        String body = fhirFacadeService.proxyGetRequest(resourceType + "/" + id + "/_history/" + vid, null, headers);
        return buildResponse(body);
    }

    @PostMapping(value = "", produces = { FHIR_JSON, MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<String> transaction(HttpServletRequest request) throws IOException {
        String requestBody = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String body = fhirFacadeService.processTransactionBundle(requestBody);
        return buildResponse(body);
    }

    @PostMapping(value = "/{resourceType}", produces = { FHIR_JSON, MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<String> create(@PathVariable String resourceType, HttpServletRequest request)
            throws IOException {
        String requestBody = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String body = fhirFacadeService.createResource(resourceType, requestBody);
        return buildResponse(body);
    }

    @PutMapping(value = "/{resourceType}/{id}", produces = { FHIR_JSON, MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<String> update(@PathVariable String resourceType, @PathVariable String id,
            HttpServletRequest request) throws IOException {
        String requestBody = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String body = fhirFacadeService.updateResource(resourceType, id, requestBody);
        return buildResponse(body);
    }

    @DeleteMapping(value = "/{resourceType}/{id}", produces = { FHIR_JSON, MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<String> delete(@PathVariable String resourceType, @PathVariable String id,
            @RequestHeader HttpHeaders headers) {
        String body = fhirFacadeService.proxyDeleteRequest(resourceType + "/" + id, headers);
        return buildResponse(body);
    }

    private ResponseEntity<String> buildResponse(String body) {
        return ResponseEntity.status(fhirFacadeService.getLastResponseStatus())
                .contentType(MediaType.parseMediaType(FHIR_JSON)).body(body);
    }
}
