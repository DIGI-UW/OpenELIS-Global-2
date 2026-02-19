package org.openelisglobal.dataexchange.fhir.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
        return forwardGetRequest(request);
    }

    @PostMapping("/**")
    public void receivePostFhirRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        forwardToFacade(request, response);
    }

    @PutMapping("/{resourceType}/**")
    public void receivePutFhirRequest(@PathVariable("resourceType") ResourceType resourceType,
            HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        forwardToFacade(request, response);
    }

    private String extractFhirPath(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = requestUri.substring(contextPath.length());
        return path.replaceFirst("/fhir", "");
    }

    private String buildQueryPath(String base, String fhirPath, String queryString) {
        StringBuilder url = new StringBuilder(base);
        if (!base.endsWith("/") && !fhirPath.startsWith("/")) {
            url.append("/");
        }
        url.append(fhirPath);
        if (queryString != null && !queryString.isBlank()) {
            url.append("?").append(queryString);
        }
        return url.toString();
    }

    private ResponseEntity<Object> forwardGetRequest(HttpServletRequest request) {
        String method = "forwardGetRequest";
        try {
            String fhirPath = extractFhirPath(request);
            LogEvent.logDebug(this.getClass().getSimpleName(), method,
                    "Received GET FHIR request for path: " + fhirPath);

            String targetUrl = buildQueryPath(fhirConfig.getLocalFhirStorePath(), fhirPath, request.getQueryString());

            HttpGet httpGet = new HttpGet(targetUrl);
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

    private void forwardToFacade(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String method = "forwardToFacade";
        try {
            String fhirPath = extractFhirPath(request);
            LogEvent.logDebug(this.getClass().getSimpleName(), method, "Forwarding FHIR request for path: " + fhirPath);

            String targetUrl = buildQueryPath("/fhir/facade", fhirPath, request.getQueryString());

            LogEvent.logDebug(this.getClass().getSimpleName(), method, "Forwarding to: " + targetUrl);

            RequestDispatcher dispatcher = request.getRequestDispatcher(targetUrl);
            dispatcher.forward(request, response);
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), method, "Error forwarding request: " + e.getMessage());

            if (!response.isCommitted()) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Error processing FHIR request: " + e.getMessage());
            }
        }
    }
}
