package org.openelisglobal.dataexchange.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.apache.commons.validator.GenericValidator;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.itech.fhir.dataexport.core.service.FhirClientFetcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class FhirUtil implements FhirClientFetcher {

    @Autowired
    private FhirConfig fhirConfig;
    @Autowired
    private FhirContext fhirContext;

    @Autowired
    private FhirClientFactory clients;
    @Autowired
    @Qualifier("partnerFhirHttpClient")
    private CloseableHttpClient partnerHttpClient;

    @Override
    public IGenericClient getFhirClient(String fhirStorePath) {
        return clients.create(fhirStorePath);
    }

    public IGenericClient getLocalFhirClient() {
        return clients.create(fhirConfig.getLocalFhirStorePath());
    }

    public IParser getFhirParser() {
        return fhirContext.newJsonParser();
    }

    public IGenericClient getFhirClient(String fhirStorePath, String token) {
        return clients.createBearer(fhirStorePath, token);
    }

    public IGenericClient getFhirClient(String fhirStorePath, String username, String password) {
        return clients.create(fhirStorePath, username, password);
    }

    public String getAccessToken(String authUrl, String authUserName, String authPassword) throws IOException {
        URI endpoint = URI.create(authUrl);
        FhirClientFactory.requireHttps(endpoint);
        if (endpoint.getHost() == null || endpoint.getUserInfo() != null || endpoint.getFragment() != null) {
            throw new IllegalArgumentException("Invalid FHIR authentication endpoint");
        }
        HttpPost httpPost = new HttpPost(authUrl);

        String json = new ObjectMapper().createObjectNode().put("username", authUserName).put("password", authPassword)
                .toString();
        StringEntity entity = new StringEntity(json, StandardCharsets.UTF_8);
        httpPost.setEntity(entity);
        httpPost.setHeader("Accept", "application/json");
        httpPost.setHeader("Content-type", "application/json");
        ObjectMapper mapper = new ObjectMapper();
        JsonNode response = mapper.createObjectNode();
        try (CloseableHttpResponse res = partnerHttpClient.execute(httpPost)) {
            if (res.getStatusLine().getStatusCode() != 200) {
                throw new IOException("FHIR authentication endpoint did not accept the request");
            }
            response = res.getEntity() == null ? null
                    : mapper.readTree(EntityUtils.toString(res.getEntity(), StandardCharsets.UTF_8));
        }
        JsonNode token = response == null ? null : response.get("access_token");
        if (token == null || !token.isTextual() || token.asText().isBlank()) {
            throw new IOException("FHIR authentication endpoint returned no access token");
        }
        return token.asText();
    }

    /**
     * Extracts the sample domain code (H, E, V) from a ServiceRequest's category
     * codings using the {@code {oeFhirSystem}/samp_domain} system.
     *
     * @return the domain code, or {@code null} if no domain category is present
     */
    public String getSampleDomain(ServiceRequest serviceRequest) {
        if (serviceRequest == null) {
            return null;
        }
        String domainSystem = fhirConfig.getOeFhirSystem() + "/samp_domain";
        for (CodeableConcept category : serviceRequest.getCategory()) {
            for (Coding coding : category.getCoding()) {
                if (domainSystem.equals(coding.getSystem())) {
                    return coding.getCode();
                }
            }
        }
        return null;
    }

    /**
     * True when the reference points at a Patient by id. The subject of an
     * environmental or vector ServiceRequest is a Location or a contained Group,
     * which must not be read as a Patient.
     */
    public static boolean refersToPatient(Reference reference) {
        return reference != null
                && ResourceType.Patient.name().equals(reference.getReferenceElement().getResourceType())
                && !GenericValidator.isBlankOrNull(reference.getReferenceElement().getIdPart());
    }

}
