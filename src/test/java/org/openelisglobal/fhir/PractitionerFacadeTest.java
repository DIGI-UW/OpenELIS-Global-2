package org.openelisglobal.fhir;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.RestfulServer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.fhir.providers.PractitionerProvider;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.provider.service.ProviderService;
import org.openelisglobal.provider.valueholder.Provider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;

public class PractitionerFacadeTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ProviderService providerService;
    @Autowired
    private PersonService personService;
    @Autowired
    private PractitionerProvider practitionerProvider;
    @Autowired
    private MockServletContext servletContext;

    private RestfulServer fhirServlet;
    private ObjectMapper objectMapper;

    @Before
    public void setUp() throws Exception {
        fhirServlet = new RestfulServer(FhirContext.forR4());
        fhirServlet.setResourceProviders(Arrays.asList(practitionerProvider));

        MockServletConfig servletConfig = new MockServletConfig(servletContext);
        servletConfig.addInitParameter("name", "FhirServlet");
        fhirServlet.init(servletConfig);

        objectMapper = new ObjectMapper();
        executeDataSetWithStateManagement("testdata/provider.xml");
        ensureReferenceTables("PROVIDER", "PERSON");
        executeDataSetWithStateManagement("testdata/system-user.xml");
    }

    private void prepareCleanSlate() throws Exception {
        cleanRowsInCurrentConnection(new String[] { "provider", "person" });
    }

    @Test
    public void readPractitioner_shouldReturn200() throws Exception {
        Provider provider = providerService.get("1");
        String uuid = provider.getFhirUuidAsString();

        MockHttpServletRequest request = buildFhirRequest("GET", "/Practitioner/" + uuid);
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());
    }

    @Test
    public void readPractitioner_shouldMapResourceTypeAndId() throws Exception {
        Provider provider = providerService.get("1");
        String uuid = provider.getFhirUuidAsString();

        MockHttpServletRequest request = buildFhirRequest("GET", "/Practitioner/" + uuid);
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        JsonNode json = objectMapper.readTree(response.getContentAsString());
        assertEquals("Practitioner", json.get("resourceType").asText());
        assertEquals(uuid, json.get("id").asText());
    }

    @Test
    public void readPractitioner_shouldIncludeGivenName() throws Exception {
        Provider provider = providerService.get("1");
        String uuid = provider.getFhirUuidAsString();

        MockHttpServletRequest request = buildFhirRequest("GET", "/Practitioner/" + uuid);
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        JsonNode json = objectMapper.readTree(response.getContentAsString());
        assertEquals("John", json.get("name").get(0).get("given").get(0).asText());
    }

    @Test
    public void readPractitioner_shouldMapFamilyName() throws Exception {
        Provider provider = providerService.get("1");
        String uuid = provider.getFhirUuidAsString();

        MockHttpServletRequest request = buildFhirRequest("GET", "/Practitioner/" + uuid);
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        JsonNode json = objectMapper.readTree(response.getContentAsString());
        assertEquals("Doe", json.get("name").get(0).get("family").asText());
    }

    @Test
    public void readPractitioner_withNonExistentId_shouldReturn404() throws Exception {
        String nonExistentUuid = UUID.randomUUID().toString();

        MockHttpServletRequest request = buildFhirRequest("GET", "/Practitioner/" + nonExistentUuid);
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(404, response.getStatus());
    }

    @Test
    public void readPractitioner_withNonExistentId_shouldReturnOperationOutcome() throws Exception {
        String nonExistentUuid = UUID.randomUUID().toString();

        MockHttpServletRequest request = buildFhirRequest("GET", "/Practitioner/" + nonExistentUuid);
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        JsonNode json = objectMapper.readTree(response.getContentAsString());
        assertEquals("OperationOutcome", json.get("resourceType").asText());
    }

    @Test
    public void createPractitioner_shouldReturn201() throws Exception {
        prepareCleanSlate();
        MockHttpServletRequest request = buildFhirRequest("POST", "/Practitioner");
        String practitionerJson = """
                {
                  "resourceType": "Practitioner",
                  "active": true,
                  "name": [{"use": "official", "family": "Patric", "given": ["Onyango"]}]
                }
                """;
        request.setContent(practitionerJson.getBytes());

        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(201, response.getStatus());
    }

    @Test
    public void createPractitioner_withEmail_shouldReturn201() throws Exception {
        prepareCleanSlate();
        MockHttpServletRequest request = buildFhirRequest("POST", "/Practitioner");
        String practitionerJson = """
                {
                  "resourceType": "Practitioner",
                  "active": true,
                  "name": [{"use": "official", "family": "Mukasa", "given": ["David"]}],
                  "telecom": [{"system": "email", "value": "david.mukasa@example.com", "use": "work"}]
                }
                """;
        request.setContent(practitionerJson.getBytes());

        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(201, response.getStatus());
    }

    @Test
    public void createPractitioner_shouldPersistEmail() throws Exception {
        prepareCleanSlate();
        MockHttpServletRequest request = buildFhirRequest("POST", "/Practitioner");
        String practitionerJson = """
                {
                  "resourceType": "Practitioner",
                  "active": true,
                  "name": [{"use": "official", "family": "Mukasa", "given": ["David"]}],
                  "telecom": [{"system": "email", "value": "david.mukasa@example.com", "use": "work"}]
                }
                """;
        request.setContent(practitionerJson.getBytes());

        MockHttpServletResponse createResponse = new MockHttpServletResponse();

        fhirServlet.service(request, createResponse);

        String createdId = objectMapper.readTree(createResponse.getContentAsString()).get("id").asText();
        Provider savedProvider = providerService.getAllMatching("fhirUuid", UUID.fromString(createdId)).get(0);
        Person savedPerson = personService.get(savedProvider.getPerson().getId());
        assertEquals("david.mukasa@example.com", savedPerson.getEmail());
    }

    @Test
    public void updatePractitioner_shouldReturn200() throws Exception {
        Provider provider = providerService.get("1");
        String uuid = provider.getFhirUuidAsString();

        MockHttpServletRequest request = buildFhirRequest("PUT", "/Practitioner/" + uuid);
        String updateJson = """
                {
                  "resourceType": "Practitioner",
                  "id": "%s",
                  "active": true,
                  "name": [{"use": "official", "family": "Betty", "given": ["Mpologoma"]}],
                  "telecom": [{"system": "email", "value": "betty.namatovu@example.com", "use": "work"}]
                }
                """.formatted(uuid);
        request.setContent(updateJson.getBytes());

        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());
    }

    @Test
    public void updatePractitioner_shouldPersistNewGivenName() throws Exception {
        Provider provider = providerService.get("1");
        String uuid = provider.getFhirUuidAsString();

        MockHttpServletRequest request = buildFhirRequest("PUT", "/Practitioner/" + uuid);
        String updateJson = """
                {
                  "resourceType": "Practitioner",
                  "id": "%s",
                  "active": true,
                  "name": [{"use": "official", "family": "Doe", "given": ["UpdatedGiven"]}]
                }
                """.formatted(uuid);
        request.setContent(updateJson.getBytes());

        fhirServlet.service(request, new MockHttpServletResponse());

        Provider updatedProvider = providerService.get("1");
        Person updatedPerson = personService.get(updatedProvider.getPerson().getId());

        assertEquals("UpdatedGiven", updatedPerson.getFirstName());
    }

    @Test
    public void updatePractitioner_withNonExistentId_shouldReturn404() throws Exception {
        String nonExistentUuid = UUID.randomUUID().toString();

        MockHttpServletRequest request = buildFhirRequest("PUT", "/Practitioner/" + nonExistentUuid);
        String updateJson = """
                {
                  "resourceType": "Practitioner",
                  "id": "%s",
                  "name": [
                    {
                      "use": "official",
                      "family": "Ghost",
                      "given": ["Nobody"]
                    }
                  ]
                }
                """.formatted(nonExistentUuid);
        request.setContent(updateJson.getBytes());

        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(404, response.getStatus());
    }

    @Test
    public void deletePractitioner_shouldReturn204() throws ServletException, IOException {
        Provider provider = providerService.get("1");
        String uuid = provider.getFhirUuidAsString();

        MockHttpServletRequest request = buildFhirRequest("DELETE", "/Practitioner/" + uuid);
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(204, response.getStatus());
    }

    @Test
    public void deletePractitioner_withNonExistentId_shouldReturn404() throws ServletException, IOException {
        String nonExistentUuid = UUID.randomUUID().toString();

        MockHttpServletRequest request = buildFhirRequest("DELETE", "/Practitioner/" + nonExistentUuid);
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(404, response.getStatus());
    }

    @Test
    public void deletePractitioner_shouldSetProviderInactive() throws Exception {
        Provider provider = providerService.get("1");
        String uuid = provider.getFhirUuidAsString();

        MockHttpServletRequest request = buildFhirRequest("DELETE", "/Practitioner/" + uuid);

        fhirServlet.service(request, new MockHttpServletResponse());

        Provider deletedProvider = providerService.get("1");

        assertNotNull(deletedProvider);
        assertFalse(deletedProvider.getActive());
    }
}
