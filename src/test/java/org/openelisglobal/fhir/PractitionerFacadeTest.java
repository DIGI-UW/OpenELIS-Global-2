package org.openelisglobal.fhir;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.RestfulServer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
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

    private RestfulServer fhirServlet;
    private ObjectMapper objectMapper;
    @Autowired
    private PersonService personService;
    @Autowired
    private ProviderService providerService;

    @Autowired
    private PractitionerProvider practitionerProvider;

    @Autowired
    private MockServletContext servletContext;

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

    private MockHttpServletRequest buildRequest(String method) {
        List<Provider> providers = providerService.getAll();
        // Fixture-loaded rows have null sys_user_id; audit emit rejects deletes
        // without one, so stamp before fanning out the deleteAll calls.
        providers.forEach(p -> p.setSysUserId("1"));
        providerService.deleteAll(providers);
        List<Person> people = personService.getAll();
        people.forEach(p -> p.setSysUserId("1"));
        personService.deleteAll(people);

        return buildFhirRequest(method, "/Practitioner");
    }

    @Test
    public void readPractitioner_shouldReturnPractitionerResource() throws Exception {
        Provider existingProvider = providerService.get("1");
        assertNotNull("Test provider with id=1 must exist", existingProvider);
        String practitionerUuid = existingProvider.getFhirUuidAsString();

        MockHttpServletRequest request = buildFhirRequest("GET", "/Practitioner/" + practitionerUuid);
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());

        JsonNode jsonResponse = objectMapper.readTree(response.getContentAsString());
        assertEquals("Practitioner", jsonResponse.get("resourceType").asText());
        assertEquals(practitionerUuid, jsonResponse.get("id").asText());

        // Verify name from test data (John Doe, person_id=1)
        assertTrue("Response should contain name array", jsonResponse.has("name"));
        JsonNode nameArray = jsonResponse.get("name");
        assertTrue("Name array should not be empty", nameArray.size() > 0);
        assertEquals("Doe", nameArray.get(0).get("family").asText());
    }

    @Test
    public void readPractitioner_withNonExistentId_shouldReturn404() throws Exception {
        String nonExistentUuid = "00000000-0000-0000-0000-000000000000";
        MockHttpServletRequest request = buildFhirRequest("GET", "/Practitioner/" + nonExistentUuid);
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(404, response.getStatus());

        JsonNode jsonResponse = objectMapper.readTree(response.getContentAsString());
        assertEquals("OperationOutcome", jsonResponse.get("resourceType").asText());
    }

    @Test
    public void createPractitioner_shouldReturnSuccess() throws Exception {
        List<Provider> providers = providerService.getAll();
        providers.forEach(p -> p.setSysUserId("1"));
        providerService.deleteAll(providers);
        List<Person> people = personService.getAll();
        // Fixture-loaded persons have null sys_user_id; audit emit rejects
        // deletes without one (PersonServiceImpl has auditTrailLog=true).
        people.forEach(p -> p.setSysUserId("1"));
        personService.deleteAll(people);

        MockHttpServletRequest request = buildRequest("POST");

        String practitionerJson = """
                {
                  "resourceType": "Practitioner",
                  "active": true,
                  "name": [
                    {
                      "use": "official",
                      "family": "Patric",
                      "given": ["Onyango"]
                    }
                  ],
                  "telecom": [
                    {
                      "system": "email",
                      "value": "patric.onyango@example.com",
                      "use": "work"
                    }
                  ]
                }
                """;

        request.setContent(practitionerJson.getBytes());

        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(201, response.getStatus());
        assertEquals("application/fhir+json;charset=UTF-8", response.getContentType());

        JsonNode jsonResponse = objectMapper.readTree(response.getContentAsString());

        assertEquals("Practitioner", jsonResponse.get("resourceType").asText());
        assertNotNull(jsonResponse.get("id"));
        List<Provider> savedProviders = providerService.getAll();
        List<Person> savedPeople = personService.getAll();

        assertTrue(savedProviders.size() > 0);
        assertTrue(savedPeople.size() > 0);
        assertEquals("patric.onyango@example.com", savedPeople.get(0).getEmail());
        assertTrue(savedProviders.get(0).getActive());

    }

    @Test
    public void updatePractitioner_shouldReturnSuccess() throws Exception {

        Provider existingProvider = providerService.get("1");
        String practitionerUuid = existingProvider.getFhirUuidAsString();

        MockHttpServletRequest request = buildFhirRequest("PUT", "/Practitioner/" + practitionerUuid);

        String updateJson = """
                {
                  "resourceType": "Practitioner",
                  "id": "%s",
                  "active": true,
                  "name": [
                    {
                      "use": "official",
                      "family": "Betty",
                      "given": ["Mpologoma"]
                    }
                  ],
                  "telecom": [
                    {
                      "system": "email",
                      "value": "bety.namatovu@example.com",
                      "use": "work"
                    }
                  ]
                }
                """.formatted(practitionerUuid);

        request.setContent(updateJson.getBytes());

        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());

        JsonNode jsonResponse = objectMapper.readTree(response.getContentAsString());

        assertEquals("Practitioner", jsonResponse.get("resourceType").asText());

        assertEquals(practitionerUuid, jsonResponse.get("id").asText());

        Provider updatedProvider = providerService.get("1");
        Person updatedPerson = personService.get(updatedProvider.getPerson().getId());

        assertEquals("bety.namatovu@example.com", updatedPerson.getEmail());

        assertTrue(updatedProvider.getActive());
    }

    @Test
    public void deletePractitoner_shouldSetPractitionerInActive() throws ServletException, IOException {

        Provider existingProvider = providerService.get("1");
        String practitionerUuid = existingProvider.getFhirUuidAsString();
        MockHttpServletRequest request = buildFhirRequest("DELETE", "/Practitioner/" + practitionerUuid);

        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(204, response.getStatus());
        Provider deletedProvider = providerService.get("1");
        Person deletedPerson = personService.get(deletedProvider.getPerson().getId());

        assertEquals("john@gmail.com", deletedPerson.getEmail());

        assertFalse(deletedProvider.getActive());
    }

    @Test
    public void searchPractitioner_withoutParamsShouldReturnAllPractitioners() throws Exception {

        MockHttpServletRequest request = buildFhirRequest("GET", "/Practitioner");

        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());

        JsonNode bundle = objectMapper.readTree(response.getContentAsString());

        assertEquals("Bundle", bundle.get("resourceType").asText());
        assertEquals("searchset", bundle.get("type").asText());
        assertEquals(2, bundle.get("total").asInt());

        assertTrue(bundle.has("entry"));
        assertEquals(2, bundle.get("entry").size());

        for (JsonNode entry : bundle.get("entry")) {
            JsonNode practitioner = entry.get("resource");

            assertEquals("Practitioner", practitioner.get("resourceType").asText());

            assertNotNull(practitioner.get("id"));
            assertTrue(practitioner.has("name"));
        }
    }

    @Test
    public void searchPractitioner_byFamilyName_shouldReturnMatchingPractitioner() throws Exception {

        MockHttpServletRequest request = buildFhirRequest("GET", "/Practitioner");

        request.addParameter("family", "Doe");

        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());

        JsonNode bundle = objectMapper.readTree(response.getContentAsString());

        assertEquals("Bundle", bundle.get("resourceType").asText());
        assertEquals(1, bundle.get("total").asInt());
        assertEquals(1, bundle.get("entry").size());

        JsonNode practitioner = bundle.get("entry").get(0).get("resource");

        assertEquals("Practitioner", practitioner.get("resourceType").asText());

        assertEquals("Doe", practitioner.get("name").get(0).get("family").asText());

        assertEquals("John", practitioner.get("name").get(0).get("given").get(0).asText());

        assertTrue(practitioner.get("active").asBoolean());
    }

    @Test
    public void searchPractitioner_byGivenName_shouldReturnMatchingPractitioner() throws Exception {

        MockHttpServletRequest request = buildFhirRequest("GET", "/Practitioner");

        request.addParameter("given", "James");

        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());

        JsonNode bundle = objectMapper.readTree(response.getContentAsString());

        assertEquals(1, bundle.get("total").asInt());
        assertEquals(1, bundle.get("entry").size());

        JsonNode practitioner = bundle.get("entry").get(0).get("resource");

        assertEquals("Practitioner", practitioner.get("resourceType").asText());

        assertEquals("James", practitioner.get("name").get(0).get("given").get(0).asText());

        assertEquals("Mulizi", practitioner.get("name").get(0).get("family").asText());

        assertFalse(practitioner.get("active").asBoolean());
    }

    @Test
    public void searchPractitioner_withUnknownGivenName_shouldReturnEmptyBundle() throws Exception {

        MockHttpServletRequest request = buildFhirRequest("GET", "/Practitioner");

        request.addParameter("given", "NotExisting");

        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());

        JsonNode bundle = objectMapper.readTree(response.getContentAsString());

        assertEquals("Bundle", bundle.get("resourceType").asText());
        assertEquals("searchset", bundle.get("type").asText());
        assertEquals(0, bundle.get("total").asInt());

        if (bundle.has("entry")) {
            assertEquals(0, bundle.get("entry").size());
        }
    }

    @Test
    public void searchPractitioner_byIdentifier_shouldReturnPractitioner() throws Exception {

        Provider provider = providerService.get("1");

        String identifier = provider.getFhirUuidAsString();

        MockHttpServletRequest request = buildFhirRequest("GET", "/Practitioner");

        request.addParameter("identifier", identifier);

        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());

        JsonNode bundle = objectMapper.readTree(response.getContentAsString());

        assertEquals("Bundle", bundle.get("resourceType").asText());
        assertEquals(1, bundle.get("total").asInt());
        assertEquals(1, bundle.get("entry").size());

        JsonNode practitioner = bundle.get("entry").get(0).get("resource");

        assertEquals(identifier, practitioner.get("id").asText());

        assertEquals("Practitioner", practitioner.get("resourceType").asText());
    }

    @Test
    public void searchPractitioner_byId_shouldReturnPractitioner() throws Exception {

        Provider provider = providerService.get("1");

        MockHttpServletRequest request = buildFhirRequest("GET", "/Practitioner");

        request.addParameter("_id", provider.getFhirUuidAsString());

        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());

        JsonNode bundle = objectMapper.readTree(response.getContentAsString());

        assertEquals(1, bundle.get("total").asInt());
    }

}
