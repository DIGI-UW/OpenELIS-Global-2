package org.openelisglobal.dataexchange.fhir.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Base64;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.hl7.fhir.r4.model.ResourceType;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.service.FhirApiWorkflowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fhir")
public class InternalFhirApi {

    @Autowired
    CloseableHttpClient httpClient;

    @Autowired
    FhirConfig fhirConfig;

    @Autowired
    FhirApiWorkflowService fhirApiWorkflowService;

    private static final String[] ALLOWED_FIELDS = new String[] { "resourceType" };

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @GetMapping("/**")
    public ResponseEntity<Object> recieveGetFhirRequests(HttpServletRequest request) {

        String method = "recieveGetFhirRequests";

        try {

            String requestUri = request.getRequestURI();
            String contextPath = request.getContextPath();
            String path = requestUri.substring(contextPath.length());
            String fhirPath = path.replaceFirst("/fhir", "");

            LogEvent.logDebug(this.getClass().getSimpleName(), method,
                    "Received GET FHIR request for path: " + fhirPath);

            StringBuilder targetUrl = new StringBuilder(fhirConfig.getLocalFhirStorePath());

            if (!fhirConfig.getLocalFhirStorePath().endsWith("/") && !fhirPath.startsWith("/")) {
                targetUrl.append("/");
            }
            targetUrl.append(fhirPath);

            if (request.getQueryString() != null) {
                targetUrl.append("?").append(request.getQueryString());
            }

            HttpGet httpGet = new HttpGet(targetUrl.toString());
            String username = fhirConfig.getUsername();
            String password = fhirConfig.getPassword();

            if (username != null && !username.isBlank()) {
                String encoding = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
                httpGet.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + encoding);
            }

            httpGet.setHeader(HttpHeaders.ACCEPT, "application/json");

            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {

                int statusCode = response.getStatusLine().getStatusCode();
                String body = EntityUtils.toString(response.getEntity());

                LogEvent.logDebug(this.getClass().getSimpleName(), method,
                        "FHIR store responded with status: " + statusCode);

                ObjectMapper mapper = new ObjectMapper();
                Object json = mapper.readValue(body, Object.class);

                return ResponseEntity.status(statusCode).contentType(MediaType.APPLICATION_JSON).body(json);
            }

        } catch (IOException e) {

            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "I/O error while calling local FHIR store: " + e.getMessage());

            return ResponseEntity.internalServerError().body("Error communicating with FHIR store");

        } catch (Exception e) {

            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Unexpected error in GET FHIR request: " + e.getMessage());

            return ResponseEntity.internalServerError().body("Unexpected server error");
        }
    }

    @PostMapping(value = "/**")
    public String receivePostFhirRequest(HttpServletRequest request) {

        String method = "receivePostFhirRequest";

        LogEvent.logDebug(this.getClass().getSimpleName(), method, "Forwarding POST FHIR request");

        String queryString = request.getQueryString() != null ? "?" + request.getQueryString() : "";

        return "forward:" + request.getServletPath().replaceFirst("fhir", "fhir/facade")
                + (request.getPathInfo() != null ? request.getPathInfo() : "") + queryString;
    }

    @PutMapping(value = "/{resourceType}/**")
    public ResponseEntity<String> receiveFhirRequest(@PathVariable("resourceType") ResourceType resourceType) {

        String method = "receiveFhirRequest";

        try {

            LogEvent.logDebug(this.getClass().getSimpleName(), method,
                    "Received workflow notification for resource type: " + resourceType);

            if (resourceType == null) {

                LogEvent.logError(this.getClass().getSimpleName(), method, "ResourceType is null");

                return ResponseEntity.badRequest().body("ResourceType must be provided");
            }

            fhirApiWorkflowService.processWorkflow(resourceType);

            LogEvent.logInfo(this.getClass().getSimpleName(), method,
                    "Workflow successfully triggered for resource type: " + resourceType);

            return ResponseEntity.ok("");

        } catch (Exception e) {

            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Error processing workflow for resource type " + resourceType + ": " + e.getMessage());

            return ResponseEntity.internalServerError().body("Error processing workflow");
        }
    }
}
