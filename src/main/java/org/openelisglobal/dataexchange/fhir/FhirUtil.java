package org.openelisglobal.dataexchange.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.validator.GenericValidator;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.itech.fhir.dataexport.core.service.FhirClientFetcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FhirUtil implements FhirClientFetcher {

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
        httpPost.setHeader("Accept", "application/json");
        httpPost.setHeader("Content-type", "application/json");
        ObjectMapper mapper = new ObjectMapper();
        JsonNode response = mapper.createObjectNode();
        try (CloseableHttpResponse res = closeableHttpClient.execute(httpPost)) {
            if (res.getStatusLine().getStatusCode() == 200) {
                response = mapper.readTree(EntityUtils.toString(res.getEntity(), StandardCharsets.UTF_8));
            }
        }
        return response.get("access_token").asText();
    }
}
