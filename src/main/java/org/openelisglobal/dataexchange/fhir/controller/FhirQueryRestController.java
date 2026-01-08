package org.openelisglobal.dataexchange.fhir.controller;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import liquibase.repackaged.org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.internationalization.MessageUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/fhir")
public class FhirQueryRestController extends BaseController {

    @Override
    protected String getPageTitleKey() {
        return MessageUtil.getContextualKey("fhir.query.title");
    }

    @Override
    protected String getPageSubtitleKey() {
        return MessageUtil.getContextualKey("fhir.query.subtitle");
    }

    @Autowired
    private FhirUtil fhirUtil;

    @Autowired
    private FhirConfig fhirConfig;

    @Override
    protected String findLocalForward(String forward) {
        return "PageNotFound";
    }

    /**
     * Query FHIR resources by resource type with search parameters. Supports
     * standard FHIR search parameters passed as query parameters.
     * 
     * @param resourceType The FHIR resource type (e.g., Patient, Questionnaire,
     *                     ServiceRequest)
     * @param count        Maximum number of results to return
     * @param includeTotal Whether to include total count in response
     * @param request      HTTP request object (for extracting query parameters)
     * @return Bundle containing matching resources
     */
    @GetMapping(value = "/{resourceType}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> queryFhirResources(@PathVariable("resourceType") String resourceType,
            @RequestParam(required = false) Integer count,
            @RequestParam(required = false, defaultValue = "false") boolean includeTotal, HttpServletRequest request) {

        try {
            if (StringUtils.isBlank(fhirConfig.getLocalFhirStorePath())) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "FHIR store path is not configured");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
            }

            IGenericClient fhirClient = fhirUtil.getLocalFhirClient();

            // Build search URL for generic queries (since .where() with string param names
            // doesn't work)
            StringBuilder searchUrl = new StringBuilder();
            searchUrl.append(fhirConfig.getLocalFhirStorePath()).append("/").append(resourceType).append("?");

            // Add search parameters from query string
            Map<String, String[]> parameterMap = request.getParameterMap();
            boolean firstParam = true;
            for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
                String paramName = entry.getKey();
                // Skip internal parameters
                if ("count".equals(paramName) || "includeTotal".equals(paramName)) {
                    continue;
                }
                String[] values = entry.getValue();
                if (values != null && values.length > 0) {
                    for (String value : values) {
                        if (!firstParam) {
                            searchUrl.append("&");
                        }
                        searchUrl.append(URLEncoder.encode(paramName, StandardCharsets.UTF_8)).append("=")
                                .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
                        firstParam = false;
                    }
                }
            }

            // Add count if specified
            if (count != null && count > 0) {
                if (!firstParam) {
                    searchUrl.append("&");
                }
                searchUrl.append("_count=").append(count);
                firstParam = false;
            }

            // Add total if requested
            if (includeTotal) {
                if (!firstParam) {
                    searchUrl.append("&");
                }
                searchUrl.append("_total=accurate");
            }

            // Execute search using fetch operation with URL
            Bundle bundle = (Bundle) fhirClient.fetchResourceFromUrl(Bundle.class, searchUrl.toString());

            return ResponseEntity.ok(bundle);

        } catch (Exception e) {
            LogEvent.logError("Failed to query FHIR resources: " + resourceType, e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to query FHIR resources: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get a specific FHIR resource by ID.
     * 
     * @param resourceType The FHIR resource type
     * @param resourceId   The resource ID
     * @return The FHIR resource
     */
    @GetMapping(value = "/{resourceType}/{resourceId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getFhirResource(@PathVariable("resourceType") String resourceType,
            @PathVariable("resourceId") String resourceId) {

        try {
            if (StringUtils.isBlank(fhirConfig.getLocalFhirStorePath())) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "FHIR store path is not configured");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
            }

            IGenericClient fhirClient = fhirUtil.getLocalFhirClient();
            org.hl7.fhir.instance.model.api.IBaseResource resource = fhirClient.read().resource(resourceType)
                    .withId(resourceId).execute();

            return ResponseEntity.ok(resource);

        } catch (Exception e) {
            LogEvent.logError("Failed to get FHIR resource: " + resourceType + "/" + resourceId, e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get FHIR resource: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }

    /**
     * Execute a FHIR search using a POST request with a search parameter map.
     * Useful for complex queries or when search parameters exceed URL length
     * limits.
     * 
     * @param resourceType The FHIR resource type
     * @param searchParams Map of search parameter names to values
     * @param count        Maximum number of results to return
     * @param includeTotal Whether to include total count in response
     * @return Bundle containing matching resources
     */
    @PostMapping(value = "/{resourceType}/_search", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> searchFhirResources(@PathVariable("resourceType") String resourceType,
            @RequestBody(required = false) Map<String, Object> searchParams,
            @RequestParam(required = false) Integer count,
            @RequestParam(required = false, defaultValue = "false") boolean includeTotal) {

        try {
            if (StringUtils.isBlank(fhirConfig.getLocalFhirStorePath())) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "FHIR store path is not configured");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
            }

            IGenericClient fhirClient = fhirUtil.getLocalFhirClient();

            // Build search URL for generic queries
            StringBuilder searchUrl = new StringBuilder();
            searchUrl.append(fhirConfig.getLocalFhirStorePath()).append("/").append(resourceType).append("?");

            // Add search parameters from request body
            boolean firstParam = true;
            if (searchParams != null) {
                for (Map.Entry<String, Object> entry : searchParams.entrySet()) {
                    String paramName = entry.getKey();
                    // Skip internal parameters
                    if ("count".equals(paramName) || "includeTotal".equals(paramName)) {
                        continue;
                    }
                    Object value = entry.getValue();
                    if (value != null) {
                        if (value instanceof String) {
                            if (!firstParam) {
                                searchUrl.append("&");
                            }
                            searchUrl.append(URLEncoder.encode(paramName, StandardCharsets.UTF_8)).append("=")
                                    .append(URLEncoder.encode((String) value, StandardCharsets.UTF_8));
                            firstParam = false;
                        } else if (value instanceof java.util.List) {
                            @SuppressWarnings("unchecked")
                            java.util.List<String> values = (java.util.List<String>) value;
                            for (String v : values) {
                                if (!firstParam) {
                                    searchUrl.append("&");
                                }
                                searchUrl.append(URLEncoder.encode(paramName, StandardCharsets.UTF_8)).append("=")
                                        .append(URLEncoder.encode(v, StandardCharsets.UTF_8));
                                firstParam = false;
                            }
                        } else {
                            if (!firstParam) {
                                searchUrl.append("&");
                            }
                            searchUrl.append(URLEncoder.encode(paramName, StandardCharsets.UTF_8)).append("=")
                                    .append(URLEncoder.encode(value.toString(), StandardCharsets.UTF_8));
                            firstParam = false;
                        }
                    }
                }
            }

            // Add count if specified
            if (count != null && count > 0) {
                if (!firstParam) {
                    searchUrl.append("&");
                }
                searchUrl.append("_count=").append(count);
                firstParam = false;
            }

            // Add total if requested
            if (includeTotal) {
                if (!firstParam) {
                    searchUrl.append("&");
                }
                searchUrl.append("_total=accurate");
            }

            // Execute search using fetch operation with URL
            Bundle bundle = (Bundle) fhirClient.fetchResourceFromUrl(Bundle.class, searchUrl.toString());

            return ResponseEntity.ok(bundle);

        } catch (Exception e) {
            LogEvent.logError("Failed to search FHIR resources: " + resourceType, e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to search FHIR resources: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Execute a raw FHIR query using the FHIR search URL format. This allows more
     * flexibility for advanced queries.
     * 
     * @param resourceType The FHIR resource type
     * @param queryString  The raw FHIR search query string (e.g.,
     *                     "name=John&birthdate=ge2020")
     * @return Bundle containing matching resources
     */
    @GetMapping(value = "/{resourceType}/_search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> searchFhirResourcesRaw(@PathVariable("resourceType") String resourceType,
            @RequestParam(required = false) String queryString, HttpServletRequest request) {

        try {
            if (StringUtils.isBlank(fhirConfig.getLocalFhirStorePath())) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "FHIR store path is not configured");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
            }

            IGenericClient fhirClient = fhirUtil.getLocalFhirClient();

            // Build search URL for generic queries
            StringBuilder searchUrl = new StringBuilder();
            searchUrl.append(fhirConfig.getLocalFhirStorePath()).append("/").append(resourceType).append("?");

            // Add all query parameters as search parameters
            Map<String, String[]> parameterMap = request.getParameterMap();
            boolean firstParam = true;
            for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
                String paramName = entry.getKey();
                String[] values = entry.getValue();
                if (values != null && values.length > 0) {
                    for (String value : values) {
                        if (!firstParam) {
                            searchUrl.append("&");
                        }
                        searchUrl.append(URLEncoder.encode(paramName, StandardCharsets.UTF_8)).append("=")
                                .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
                        firstParam = false;
                    }
                }
            }

            // Execute search using fetch operation with URL
            Bundle bundle = (Bundle) fhirClient.fetchResourceFromUrl(Bundle.class, searchUrl.toString());

            return ResponseEntity.ok(bundle);

        } catch (Exception e) {
            LogEvent.logError("Failed to search FHIR resources: " + resourceType, e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to search FHIR resources: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

}
