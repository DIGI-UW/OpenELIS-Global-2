package org.openelisglobal.fhir.facade.provider;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fhir")
public class FhirProxyController {

    private static final String FHIR_JSON = "application/fhir+json";

    @Autowired
    private CloseableHttpClient httpClient;

    @Value("${org.openelisglobal.fhirstore.uri:}")
    private String hapiServerUrl;

    @PostConstruct
    public void init() {
        if (hapiServerUrl != null && hapiServerUrl.endsWith("/")) {
            hapiServerUrl = hapiServerUrl.substring(0, hapiServerUrl.length() - 1);
        }
        LogEvent.logInfo(this.getClass().getSimpleName(), "init",
                "FHIR Proxy initialized, forwarding to: " + hapiServerUrl);
    }

    @GetMapping(value = "/{resourceType}", produces = { FHIR_JSON, MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<String> searchResource(@PathVariable String resourceType, @RequestHeader HttpHeaders headers,
            HttpServletRequest request) {
        return forwardGetRequest(resourceType, request.getQueryString(), headers);
    }

    @GetMapping(value = "/{resourceType}/{id}", produces = { FHIR_JSON, MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<String> readResource(@PathVariable String resourceType, @PathVariable String id,
            @RequestHeader HttpHeaders headers) {
        return forwardGetRequest(resourceType + "/" + id, null, headers);
    }

    @GetMapping(value = "/{resourceType}/{id}/_history", produces = { FHIR_JSON, MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<String> historyResource(@PathVariable String resourceType, @PathVariable String id,
            @RequestHeader HttpHeaders headers, HttpServletRequest request) {
        return forwardGetRequest(resourceType + "/" + id + "/_history", request.getQueryString(), headers);
    }

    @GetMapping(value = "/{resourceType}/{id}/_history/{vid}", produces = { FHIR_JSON,
            MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<String> vreadResource(@PathVariable String resourceType, @PathVariable String id,
            @PathVariable String vid, @RequestHeader HttpHeaders headers) {
        return forwardGetRequest(resourceType + "/" + id + "/_history/" + vid, null, headers);
    }

    private ResponseEntity<String> forwardGetRequest(String path, String queryString, HttpHeaders headers) {
        if (hapiServerUrl == null || hapiServerUrl.isEmpty()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("{\"error\": \"HAPI FHIR server URL not configured\"}");
        }

        String targetUrl = hapiServerUrl + "/" + path;
        if (queryString != null && !queryString.isEmpty()) {
            targetUrl += "?" + queryString;
        }

        LogEvent.logInfo(this.getClass().getSimpleName(), "forwardGetRequest", "Forwarding to: " + targetUrl);

        try {
            HttpGet request = new HttpGet(targetUrl);
            if (headers.getFirst(HttpHeaders.AUTHORIZATION) != null) {
                request.setHeader(HttpHeaders.AUTHORIZATION, headers.getFirst(HttpHeaders.AUTHORIZATION));
            }
            request.setHeader(HttpHeaders.ACCEPT, FHIR_JSON);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String body = response.getEntity() != null ? EntityUtils.toString(response.getEntity()) : "";
                return ResponseEntity.status(statusCode).contentType(MediaType.parseMediaType(FHIR_JSON)).body(body);
            }
        } catch (IOException e) {
            LogEvent.logError(this.getClass().getSimpleName(), "forwardGetRequest",
                    "Error forwarding request: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("{\"error\": \"Failed to connect to HAPI FHIR server: " + e.getMessage() + "\"}");
        }
    }
}
