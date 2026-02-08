package org.openelisglobal.fhir.facade.service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementKind;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestComponent;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestResourceComponent;
import org.hl7.fhir.r4.model.CapabilityStatement.RestfulCapabilityMode;
import org.hl7.fhir.r4.model.CapabilityStatement.TypeRestfulInteraction;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Enumerations.FHIRVersion;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;
import org.hl7.fhir.r4.model.Resource;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceServiceImpl.FhirOperations;
import org.openelisglobal.fhir.facade.FhirFacadeServlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class FhirFacadeServiceImpl implements FhirFacadeService {

    private static final String FHIR_JSON = "application/fhir+json";

    @Autowired
    private CloseableHttpClient httpClient;

    @Autowired
    private FhirPersistanceService fhirPersistanceService;

    @Value("${org.openelisglobal.fhirstore.uri:}")
    private String hapiServerUrl;

    private int lastResponseStatus;

    @PostConstruct
    public void init() {
        if (hapiServerUrl != null && hapiServerUrl.endsWith("/")) {
            hapiServerUrl = hapiServerUrl.substring(0, hapiServerUrl.length() - 1);
        }
        LogEvent.logInfo(this.getClass().getSimpleName(), "init", "FHIR Facade initialized: " + hapiServerUrl);
    }

    @Override
    public int getLastResponseStatus() {
        return lastResponseStatus;
    }

    @Override
    public String proxyGetRequest(String path, String queryString, HttpHeaders headers) {
        if (hapiServerUrl == null || hapiServerUrl.isEmpty()) {
            lastResponseStatus = HttpStatus.SERVICE_UNAVAILABLE.value();
            return createErrorJson("HAPI FHIR server URL not configured");
        }

        String targetUrl = buildTargetUrl(path, queryString);
        LogEvent.logInfo(this.getClass().getSimpleName(), "proxyGetRequest", "Forwarding GET to: " + targetUrl);

        try {
            HttpGet request = new HttpGet(targetUrl);
            copyAuthHeader(headers, request);
            request.setHeader(HttpHeaders.ACCEPT, FHIR_JSON);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                lastResponseStatus = response.getStatusLine().getStatusCode();
                return response.getEntity() != null ? EntityUtils.toString(response.getEntity()) : "";
            }
        } catch (IOException e) {
            LogEvent.logError(this.getClass().getSimpleName(), "proxyGetRequest", e.getMessage());
            lastResponseStatus = HttpStatus.BAD_GATEWAY.value();
            return createErrorJson("Failed to connect to HAPI FHIR server: " + e.getMessage());
        }
    }

    @Override
    public String proxyDeleteRequest(String path, HttpHeaders headers) {
        if (hapiServerUrl == null || hapiServerUrl.isEmpty()) {
            lastResponseStatus = HttpStatus.SERVICE_UNAVAILABLE.value();
            return createErrorJson("HAPI FHIR server URL not configured");
        }

        String targetUrl = buildTargetUrl(path, null);
        LogEvent.logInfo(this.getClass().getSimpleName(), "proxyDeleteRequest", "Forwarding DELETE to: " + targetUrl);

        try {
            HttpDelete request = new HttpDelete(targetUrl);
            copyAuthHeader(headers, request);
            request.setHeader(HttpHeaders.ACCEPT, FHIR_JSON);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                lastResponseStatus = response.getStatusLine().getStatusCode();
                return response.getEntity() != null ? EntityUtils.toString(response.getEntity()) : "";
            }
        } catch (IOException e) {
            LogEvent.logError(this.getClass().getSimpleName(), "proxyDeleteRequest", e.getMessage());
            lastResponseStatus = HttpStatus.BAD_GATEWAY.value();
            return createErrorJson("Failed to connect to HAPI FHIR server: " + e.getMessage());
        }
    }

    @Override
    public String createResource(String resourceType, String body) {
        LogEvent.logInfo(this.getClass().getSimpleName(), "createResource", "Creating " + resourceType);

        try {
            Resource resource = (Resource) FhirFacadeServlet.getJsonParser().parseResource(body);
            Bundle result = fhirPersistanceService.createFhirResourceInFhirStore(resource);
            lastResponseStatus = HttpStatus.CREATED.value();
            return FhirFacadeServlet.getJsonParser().encodeResourceToString(result);
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "createResource", e.getMessage());
            lastResponseStatus = HttpStatus.BAD_REQUEST.value();
            return createOperationOutcome(IssueSeverity.ERROR, "Failed to create resource: " + e.getMessage());
        }
    }

    @Override
    public String updateResource(String resourceType, String id, String body) {
        LogEvent.logInfo(this.getClass().getSimpleName(), "updateResource", "Updating " + resourceType + "/" + id);

        try {
            Resource resource = (Resource) FhirFacadeServlet.getJsonParser().parseResource(body);
            if (resource.getId() == null || resource.getId().isEmpty()) {
                resource.setId(id);
            }
            Bundle result = fhirPersistanceService.updateFhirResourceInFhirStore(resource);
            lastResponseStatus = HttpStatus.OK.value();
            return FhirFacadeServlet.getJsonParser().encodeResourceToString(result);
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "updateResource", e.getMessage());
            lastResponseStatus = HttpStatus.BAD_REQUEST.value();
            return createOperationOutcome(IssueSeverity.ERROR, "Failed to update resource: " + e.getMessage());
        }
    }

    @Override
    public String processTransactionBundle(String bundleJson) {
        LogEvent.logInfo(this.getClass().getSimpleName(), "processTransactionBundle", "Processing transaction bundle");

        try {
            Bundle bundle = FhirFacadeServlet.getJsonParser().parseResource(Bundle.class, bundleJson);
            if (bundle.getType() != Bundle.BundleType.TRANSACTION && bundle.getType() != Bundle.BundleType.BATCH) {
                lastResponseStatus = HttpStatus.BAD_REQUEST.value();
                return createOperationOutcome(IssueSeverity.ERROR, "Bundle type must be 'transaction' or 'batch'");
            }

            Bundle result = fhirPersistanceService.createUpdateFhirResourcesInFhirStore(new FhirOperations());

            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                Resource resource = entry.getResource();
                if (resource != null) {
                    Bundle.HTTPVerb method = entry.getRequest().getMethod();
                    if (method == Bundle.HTTPVerb.POST) {
                        fhirPersistanceService.createFhirResourceInFhirStore(resource);
                    } else if (method == Bundle.HTTPVerb.PUT) {
                        fhirPersistanceService.updateFhirResourceInFhirStore(resource);
                    }
                }
            }

            lastResponseStatus = HttpStatus.OK.value();
            return FhirFacadeServlet.getJsonParser().encodeResourceToString(result);
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "processTransactionBundle", e.getMessage());
            lastResponseStatus = HttpStatus.BAD_REQUEST.value();
            return createOperationOutcome(IssueSeverity.ERROR, "Failed to process bundle: " + e.getMessage());
        }
    }

    @Override
    public String getCapabilityStatement() {
        CapabilityStatement cs = new CapabilityStatement();
        cs.setStatus(PublicationStatus.ACTIVE);
        cs.setKind(CapabilityStatementKind.INSTANCE);
        cs.setFhirVersion(FHIRVersion._4_0_1);
        cs.setFormat(Arrays.asList(new CodeType(FHIR_JSON), new CodeType("application/fhir+xml")));
        cs.setDate(new Date());

        cs.getSoftware().setName("OpenELIS Global FHIR Facade").setVersion("1.0.0");

        CapabilityStatementRestComponent rest = cs.addRest();
        rest.setMode(RestfulCapabilityMode.SERVER);

        String[] resourceTypes = { "Patient", "Practitioner", "Organization", "Location", "Specimen", "Observation",
                "DiagnosticReport", "ServiceRequest", "Task" };

        for (String resourceType : resourceTypes) {
            CapabilityStatementRestResourceComponent resource = rest.addResource();
            resource.setType(resourceType);
            resource.addInteraction().setCode(TypeRestfulInteraction.READ);
            resource.addInteraction().setCode(TypeRestfulInteraction.SEARCHTYPE);
            resource.addInteraction().setCode(TypeRestfulInteraction.CREATE);
            resource.addInteraction().setCode(TypeRestfulInteraction.UPDATE);
            resource.addInteraction().setCode(TypeRestfulInteraction.DELETE);
        }

        lastResponseStatus = HttpStatus.OK.value();
        return FhirFacadeServlet.getJsonParser().encodeResourceToString(cs);
    }

    private String buildTargetUrl(String path, String queryString) {
        String url = hapiServerUrl + "/" + path;
        if (queryString != null && !queryString.isEmpty()) {
            url += "?" + queryString;
        }
        return url;
    }

    private void copyAuthHeader(HttpHeaders headers, org.apache.http.HttpRequest request) {
        String auth = headers.getFirst(HttpHeaders.AUTHORIZATION);
        if (auth != null) {
            request.setHeader(HttpHeaders.AUTHORIZATION, auth);
        }
    }

    private String createErrorJson(String message) {
        return "{\"error\": \"" + message.replace("\"", "'") + "\"}";
    }

    private String createOperationOutcome(IssueSeverity severity, String message) {
        OperationOutcome outcome = new OperationOutcome();
        outcome.addIssue().setSeverity(severity).setCode(IssueType.EXCEPTION).setDiagnostics(message);
        return FhirFacadeServlet.getJsonParser().encodeResourceToString(outcome);
    }
}
