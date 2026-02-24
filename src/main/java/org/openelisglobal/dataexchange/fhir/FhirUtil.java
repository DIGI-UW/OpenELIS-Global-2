package org.openelisglobal.dataexchange.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import jakarta.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.apache.commons.validator.GenericValidator;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.itech.fhir.dataexport.core.service.FhirClientFetcher;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Component
public class FhirUtil implements FhirClientFetcher {

    @Autowired
    CloseableHttpClient httpClient;

    @Autowired
    private FhirConfig fhirConfig;
    @Autowired
    private FhirContext fhirContext;
    @Autowired
    private CloseableHttpClient closeableHttpClient;

    @Override
    public IGenericClient getFhirClient(String fhirStorePath) {
        IGenericClient fhirClient = fhirContext.newRestfulGenericClient(fhirStorePath);
        if (!GenericValidator.isBlankOrNull(fhirConfig.getUsername())) {
            IClientInterceptor authInterceptor = new BasicAuthInterceptor(fhirConfig.getUsername(),
                    fhirConfig.getPassword());
            fhirClient.registerInterceptor(authInterceptor);
        }

        return fhirClient;
    }

    public IGenericClient getLocalFhirClient() {
        IGenericClient fhirClient = fhirContext.newRestfulGenericClient(fhirConfig.getLocalFhirStorePath());
        if (!GenericValidator.isBlankOrNull(fhirConfig.getUsername())) {
            IClientInterceptor authInterceptor = new BasicAuthInterceptor(fhirConfig.getUsername(),
                    fhirConfig.getPassword());
            fhirClient.registerInterceptor(authInterceptor);
        }
        return fhirClient;
    }

    public IParser getFhirParser() {
        return fhirContext.newJsonParser();
    }

    public IGenericClient getFhirClient(String fhirStorePath, String token) {
        IGenericClient fhirClient = fhirContext.newRestfulGenericClient(fhirStorePath);
        BearerTokenAuthInterceptor authInterceptor = new BearerTokenAuthInterceptor(token);
        fhirClient.registerInterceptor(authInterceptor);
        return fhirClient;
    }

    public IGenericClient getFhirClient(String fhirStorePath, String username, String password) {
        IGenericClient fhirClient = fhirContext.newRestfulGenericClient(fhirStorePath);
        BasicAuthInterceptor authInterceptor = new BasicAuthInterceptor(username, password);
        fhirClient.registerInterceptor(authInterceptor);
        return fhirClient;
    }

    public String getAccessToken(String authUrl, String authUserName, String authPassowrd) throws IOException {
        HttpPost httpPost = new HttpPost(authUrl);

        String json = String.format("{\"username\":\"%s\",\"password\":\"%s\"}", authUserName, authPassowrd);
        StringEntity entity = new StringEntity(json);
        httpPost.setEntity(entity);
        httpPost.setHeader("Accept", "application/fhir+json");
        httpPost.setHeader("Content-type", "application/fhir+json");
        ObjectMapper mapper = new ObjectMapper();
        JsonNode response = mapper.createObjectNode();
        try (CloseableHttpResponse res = closeableHttpClient.execute(httpPost)) {
            if (res.getStatusLine().getStatusCode() == 200) {
                response = mapper.readTree(EntityUtils.toString(res.getEntity(), StandardCharsets.UTF_8));
            }
        }
        return response.get("access_token").asText();
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

public Bundle forwardSearchToFhirStore(HttpServletRequest request) {
    String method = "forwardSearchToFhirStore";
    try {
        String fhirPath = extractFhirPath(request);
        LogEvent.logDebug(this.getClass().getSimpleName(), method,
                "Received FHIR search request for path: " + fhirPath);

        String targetUrl = buildQueryPath(fhirConfig.getLocalFhirStorePath(), fhirPath, request.getQueryString());

        HttpGet httpGet = new HttpGet(targetUrl);
        addAuthHeader(httpGet);
        httpGet.setHeader(HttpHeaders.ACCEPT, "application/fhir+json");

        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            int statusCode = response.getStatusLine().getStatusCode();
            String body = EntityUtils.toString(response.getEntity());

            LogEvent.logDebug(this.getClass().getSimpleName(), method,
                    "FHIR store responded with status: " + statusCode);

            if (statusCode >= 200 && statusCode < 300) {
                // Parse the response as a FHIR Bundle
                return getFhirParser().parseResource(Bundle.class, body);
            } else {
                LogEvent.logError(this.getClass().getSimpleName(), method,
                    "FHIR store returned error: " + statusCode + " - " + body);
                throw new RuntimeException("FHIR store returned error: " + statusCode);
            }
        }
    } catch (IOException e) {
        LogEvent.logError(this.getClass().getSimpleName(), method,
                "I/O error while calling local FHIR store: " + e.getMessage());
        throw new RuntimeException("Error communicating with FHIR store", e);
    }
}

    private void addAuthHeader(org.apache.http.HttpRequest httpRequest) {
        String username = fhirConfig.getUsername();
        String password = fhirConfig.getPassword();
        if (username != null && !username.isBlank()) {
            String encoding = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
            httpRequest.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + encoding);
        }
    }
}
