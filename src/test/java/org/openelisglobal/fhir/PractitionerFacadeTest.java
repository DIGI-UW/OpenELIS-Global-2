package org.openelisglobal.fhir;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.RestfulServer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    }

    private MockHttpServletRequest buildRequest(String method) {
        List<Provider> providers = providerService.getAll();
        providerService.deleteAll(providers);
        List<Person> people = personService.getAll();
        personService.deleteAll(people);

        MockHttpServletRequest request = new MockHttpServletRequest();

        request.setMethod(method);

        request.setContextPath("");
        request.setServletPath("/fhir/facade");
        request.setPathInfo("/Practitioner");

        request.setRequestURI("/fhir/facade/Practitioner");

        request.setContentType("application/fhir+json");
        request.addHeader("Accept", "application/fhir+json");

        return request;
    }

    @Test
    public void createPractitioner_shouldReturnSuccess() throws Exception {
        List<Provider> providers = providerService.getAll();
        providerService.deleteAll(providers);
        List<Person> people = personService.getAll();
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

        MockHttpServletRequest request = new MockHttpServletRequest();

        request.setMethod("PUT");
        request.setContextPath("");
        request.setServletPath("/fhir/facade");
        request.setPathInfo("/Practitioner/" + practitionerUuid);
        request.setRequestURI("/fhir/facade/Practitioner/" + practitionerUuid);

        request.setContentType("application/fhir+json");
        request.addHeader("Accept", "application/fhir+json");

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

}
