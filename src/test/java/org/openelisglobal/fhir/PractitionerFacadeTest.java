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
    public void readPractitioner_shouldIncludeGivenName() throws Exception {
        Provider existingProvider = providerService.get("1");
        String practitionerUuid = existingProvider.getFhirUuidAsString();

        MockHttpServletRequest request = buildFhirRequest("GET", "/Practitioner/" + practitionerUuid);
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());
        JsonNode jsonResponse = objectMapper.readTree(response.getContentAsString());
        JsonNode nameArray = jsonResponse.get("name");
        assertNotNull(nameArray);
        JsonNode givenArray = nameArray.get(0).get("given");
        assertNotNull("Given name array should be present", givenArray);
        assertEquals("John", givenArray.get(0).asText());
    }

    @Test
    public void readPractitioner_inactiveProvider_shouldStillBeReturned() throws Exception {
        Provider inactiveProvider = providerService.get("2");
        assertNotNull(inactiveProvider);
        assertFalse(inactiveProvider.getActive());
        String practitionerUuid = inactiveProvider.getFhirUuidAsString();

        MockHttpServletRequest request = buildFhirRequest("GET", "/Practitioner/" + practitionerUuid);
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());
        JsonNode jsonResponse = objectMapper.readTree(response.getContentAsString());
        assertEquals("Practitioner", jsonResponse.get("resourceType").asText());
        assertEquals(practitionerUuid, jsonResponse.get("id").asText());
    }

    @Test
    public void updatePractitioner_withNonExistentId_shouldReturn404() throws Exception {
        String nonExistentUuid = "00000000-0000-0000-0000-000000000000";
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
    public void deletePractitioner_withNonExistentId_shouldReturn404() throws Exception {
        String nonExistentUuid = "00000000-0000-0000-0000-000000000000";
        MockHttpServletRequest request = buildFhirRequest("DELETE", "/Practitioner/" + nonExistentUuid);
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(404, response.getStatus());
    }

    @Test
    public void createPractitioner_shouldBeReadableAfterCreation() throws Exception {
        List<Provider> providers = providerService.getAll();
        providers.forEach(p -> p.setSysUserId("1"));
        providerService.deleteAll(providers);
        List<Person> people = personService.getAll();
        people.forEach(p -> p.setSysUserId("1"));
        personService.deleteAll(people);

        MockHttpServletRequest createRequest = buildRequest("POST");
        String practitionerJson = """
                {
                  "resourceType": "Practitioner",
                  "active": true,
                  "name": [
                    {
                      "use": "official",
                      "family": "Kamau",
                      "given": ["Wanjiku"]
                    }
                  ],
                  "telecom": [
                    {
                      "system": "email",
                      "value": "wanjiku.kamau@example.com",
                      "use": "work"
                    }
                  ]
                }
                """;
        createRequest.setContent(practitionerJson.getBytes());
        MockHttpServletResponse createResponse = new MockHttpServletResponse();
        fhirServlet.service(createRequest, createResponse);

        assertEquals(201, createResponse.getStatus());
        JsonNode created = objectMapper.readTree(createResponse.getContentAsString());
        String newUuid = created.get("id").asText();
        assertNotNull(newUuid);

        MockHttpServletRequest readRequest = buildFhirRequest("GET", "/Practitioner/" + newUuid);
        MockHttpServletResponse readResponse = new MockHttpServletResponse();
        fhirServlet.service(readRequest, readResponse);

        assertEquals(200, readResponse.getStatus());
        JsonNode readJson = objectMapper.readTree(readResponse.getContentAsString());
        assertEquals("Practitioner", readJson.get("resourceType").asText());
        assertEquals(newUuid, readJson.get("id").asText());
        assertEquals("Kamau", readJson.get("name").get(0).get("family").asText());
    }

    @Test
    public void updatePractitioner_shouldPersistNewGivenName() throws Exception {
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
                      "family": "Doe",
                      "given": ["UpdatedGiven"]
                    }
                  ]
                }
                """.formatted(practitionerUuid);
        request.setContent(updateJson.getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());
        Provider updatedProvider = providerService.get("1");
        Person updatedPerson = personService.get(updatedProvider.getPerson().getId());
        assertEquals("UpdatedGiven", updatedPerson.getFirstName());
    }

    @Test
    public void deletePractitioner_thenRecordShouldBeInactive() throws Exception {
        Provider existingProvider = providerService.get("1");
        String practitionerUuid = existingProvider.getFhirUuidAsString();

        MockHttpServletRequest deleteRequest = buildFhirRequest("DELETE", "/Practitioner/" + practitionerUuid);
        MockHttpServletResponse deleteResponse = new MockHttpServletResponse();
        fhirServlet.service(deleteRequest, deleteResponse);

        assertEquals(204, deleteResponse.getStatus());
        Provider deletedProvider = providerService.get("1");
        assertNotNull(deletedProvider);
        assertFalse(deletedProvider.getActive());
    }

    @Test
    public void createPractitioner_withInvalidBody_shouldReturn400() throws Exception {
        List<Provider> providers = providerService.getAll();
        providers.forEach(p -> p.setSysUserId("1"));
        providerService.deleteAll(providers);
        List<Person> people = personService.getAll();
        people.forEach(p -> p.setSysUserId("1"));
        personService.deleteAll(people);

        MockHttpServletRequest request = buildRequest("POST");
        // sending plain text instead of a FHIR resource
        request.setContent("this is not valid fhir json".getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);

        assertTrue("Should return a 4xx error for invalid input",
                response.getStatus() >= 400 && response.getStatus() < 500);
    }

    @Test
    public void createPractitioner_withMultipleTelecom_shouldPersistEmail() throws Exception {
        List<Provider> providers = providerService.getAll();
        providers.forEach(p -> p.setSysUserId("1"));
        providerService.deleteAll(providers);
        List<Person> people = personService.getAll();
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
                      "family": "Mukasa",
                      "given": ["David"]
                    }
                  ],
                  "telecom": [
                    {
                      "system": "phone",
                      "value": "+256700000000",
                      "use": "work"
                    },
                    {
                      "system": "email",
                      "value": "david.mukasa@example.com",
                      "use": "work"
                    }
                  ]
                }
                """;
        request.setContent(practitionerJson.getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);

        assertEquals(201, response.getStatus());

        List<Person> savedPeople = personService.getAll();
        assertFalse("At least one person should be saved", savedPeople.isEmpty());
        assertEquals("david.mukasa@example.com", savedPeople.get(0).getEmail());
    }

    @Test
    public void deletePractitioner_twice_shouldReturn204BothTimes() throws Exception {
        Provider existingProvider = providerService.get("1");
        String practitionerUuid = existingProvider.getFhirUuidAsString();

        MockHttpServletRequest firstDelete = buildFhirRequest("DELETE", "/Practitioner/" + practitionerUuid);
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        fhirServlet.service(firstDelete, firstResponse);
        assertEquals(204, firstResponse.getStatus());

        MockHttpServletRequest secondDelete = buildFhirRequest("DELETE", "/Practitioner/" + practitionerUuid);
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        fhirServlet.service(secondDelete, secondResponse);
        assertEquals(204, secondResponse.getStatus());
    }
}
