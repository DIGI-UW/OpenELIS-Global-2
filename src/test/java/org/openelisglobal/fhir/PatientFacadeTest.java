package org.openelisglobal.fhir;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.RestfulServer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.fhir.providers.PatientProvider;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.person.service.PersonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;

public class PatientFacadeTest extends BaseWebContextSensitiveTest {

    @Autowired
    private PatientService patientService;

    @Autowired
    private PersonService personService;

    @Autowired
    private PatientProvider patientProvider;

    @Autowired
    private MockServletContext servletContext;

    private RestfulServer fhirServlet;
    private ObjectMapper objectMapper;

    @Before
    public void setUp() throws Exception {
        fhirServlet = new RestfulServer(FhirContext.forR4());
        fhirServlet.setResourceProviders(Arrays.asList(patientProvider));

        MockServletConfig servletConfig = new MockServletConfig(servletContext);
        servletConfig.addInitParameter("name", "FhirServlet");
        fhirServlet.init(servletConfig);

        objectMapper = new ObjectMapper();

        executeDataSetWithStateManagement("testdata/facade-patient.xml");
    }

    private MockHttpServletRequest buildRequest(String method, String pathInfo) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod(method);
        request.setContextPath("");
        request.setServletPath("/fhir");
        request.setPathInfo(pathInfo);
        request.setRequestURI("/fhir" + pathInfo);
        request.setContentType("application/fhir+json");
        request.addHeader("Accept", "application/fhir+json");
        return request;
    }

    @Test
    public void readPatient_shouldReturnPatientResource() throws Exception {
        Patient existingPatient = patientService.get("1");
        assertNotNull("Test patient with id=10 must exist", existingPatient);
        String patientUuid = existingPatient.getFhirUuidAsString();

        MockHttpServletRequest request = buildRequest("GET", "/Patient/" + patientUuid);
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());

        JsonNode jsonResponse = objectMapper.readTree(response.getContentAsString());
        assertEquals("Patient", jsonResponse.get("resourceType").asText());
        assertEquals(patientUuid, jsonResponse.get("id").asText());

        assertTrue("Response should contain name array", jsonResponse.has("name"));
        JsonNode nameArray = jsonResponse.get("name");
        assertTrue("Name array should not be empty", nameArray.size() > 0);
        assertEquals("Doe", nameArray.get(0).get("family").asText());

        assertEquals("male", jsonResponse.get("gender").asText());
    }

    @Test
    public void readPatient_withNonExistentId_shouldReturn404() throws Exception {
        String nonExistentUuid = "00000000-0000-0000-0000-000000000000";
        MockHttpServletRequest request = buildRequest("GET", "/Patient/" + nonExistentUuid);
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(404, response.getStatus());

        JsonNode jsonResponse = objectMapper.readTree(response.getContentAsString());
        assertEquals("OperationOutcome", jsonResponse.get("resourceType").asText());
    }

    // @Test
    // public void updatePatient_shouldUpdateNameAndTelecom() throws Exception {
    // Patient existingPatient = patientService.get("10");
    // assertNotNull("Test patient with id=10 must exist", existingPatient);
    // String patientUuid = existingPatient.getFhirUuidAsString();
    // assertNotNull("Test patient must have a fhirUuid", patientUuid);

    // MockHttpServletRequest request = buildRequest("PUT", "/Patient/" +
    // patientUuid);

    // String updateJson = """
    // {
    // "resourceType": "Patient",
    // "id": "%s",
    // "active": true,
    // "name": [
    // {
    // "use": "official",
    // "family": "UpdatedFamily",
    // "given": ["UpdatedGiven"]
    // }
    // ],
    // "telecom": [
    // {
    // "system": "email",
    // "value": "updated.email@example.com",
    // "use": "work"
    // }
    // ]
    // }
    // """.formatted(patientUuid);

    // request.setContent(updateJson.getBytes());
    // MockHttpServletResponse response = new MockHttpServletResponse();
    // fhirServlet.service(request, response);

    // // Verify HTTP response
    // assertEquals(200, response.getStatus());

    // // JsonNode jsonResponse =
    // objectMapper.readTree(response.getContentAsString());
    // // assertEquals("Patient", jsonResponse.get("resourceType").asText());
    // // assertEquals(patientUuid, jsonResponse.get("id").asText());

    // // // Verify name was returned in the response
    // // assertTrue("Response should contain name array",
    // jsonResponse.has("name"));
    // // JsonNode nameArray = jsonResponse.get("name");
    // // assertTrue("Name array should not be empty", nameArray.size() > 0);
    // // assertEquals("UpdatedFamily", nameArray.get(0).get("family").asText());

    // // // Verify the database was updated
    // // Patient updatedPatient = patientService.get("10");
    // // Person updatedPerson =
    // personService.get(updatedPatient.getPerson().getId());

    // // assertEquals("UpdatedFamily", updatedPerson.getLastName());
    // // assertEquals("UpdatedGiven", updatedPerson.getFirstName());
    // // assertEquals("updated.email@example.com", updatedPerson.getEmail());
    // }

    // @Test
    // public void updatePatient_withMultipleTelecom_shouldUpdateAllContactInfo()
    // throws Exception {
    // Patient existingPatient = patientService.get("10");
    // String patientUuid = existingPatient.getFhirUuidAsString();

    // MockHttpServletRequest request = buildRequest("PUT", "/Patient/" +
    // patientUuid);

    // String updateJson = """
    // {
    // "resourceType": "Patient",
    // "id": "%s",
    // "active": true,
    // "name": [
    // {
    // "use": "official",
    // "family": "Nakamura",
    // "given": ["Yuki"]
    // }
    // ],
    // "telecom": [
    // {
    // "system": "email",
    // "value": "new.email@example.com",
    // "use": "work"
    // },
    // {
    // "system": "phone",
    // "value": "555-0100",
    // "use": "mobile"
    // },
    // {
    // "system": "phone",
    // "value": "555-0200",
    // "use": "work"
    // }
    // ]
    // }
    // """.formatted(patientUuid);

    // request.setContent(updateJson.getBytes());
    // MockHttpServletResponse response = new MockHttpServletResponse();
    // fhirServlet.service(request, response);

    // assertEquals(200, response.getStatus());

    // // Verify database reflects all telecom updates
    // Patient updatedPatient = patientService.get("10");
    // Person updatedPerson = personService.get(updatedPatient.getPerson().getId());

    // assertEquals("new.email@example.com", updatedPerson.getEmail());
    // assertEquals("555-0100", updatedPerson.getCellPhone());
    // assertEquals("555-0200", updatedPerson.getWorkPhone());
    // }

    // @Test
    // public void updatePatient_withInvalidId_shouldReturn404() throws Exception {
    // String nonExistentUuid = "00000000-0000-0000-0000-000000000000";
    // MockHttpServletRequest request = buildRequest("PUT", "/Patient/" +
    // nonExistentUuid);

    // String updateJson = """
    // {
    // "resourceType": "Patient",
    // "id": "%s",
    // "active": true,
    // "name": [
    // {
    // "use": "official",
    // "family": "Test",
    // "given": ["User"]
    // }
    // ]
    // }
    // """.formatted(nonExistentUuid);

    // request.setContent(updateJson.getBytes());
    // MockHttpServletResponse response = new MockHttpServletResponse();
    // fhirServlet.service(request, response);

    // // HAPI FHIR returns 404 for ResourceNotFoundException
    // assertEquals(404, response.getStatus());

    // JsonNode jsonResponse = objectMapper.readTree(response.getContentAsString());
    // assertEquals("OperationOutcome", jsonResponse.get("resourceType").asText());
    // }

    // @Test
    // public void deletePatient_shouldReturnSuccessAndMarkInactive() throws
    // ServletException, IOException {
    // Patient existingPatient = patientService.get("11");
    // assertNotNull("Test patient with id=11 must exist", existingPatient);
    // String patientUuid = existingPatient.getFhirUuidAsString();

    // MockHttpServletRequest request = buildRequest("DELETE", "/Patient/" +
    // patientUuid);
    // MockHttpServletResponse response = new MockHttpServletResponse();

    // fhirServlet.service(request, response);

    // assertEquals(204, response.getStatus());

    // // Verify the person data is still intact (soft-delete preserves data)
    // Patient deletedPatient = patientService.get("11");
    // assertNotNull("Patient should still exist after soft-delete",
    // deletedPatient);
    // Person deletedPerson = personService.get(deletedPatient.getPerson().getId());
    // assertEquals("grace.ochieng@example.com", deletedPerson.getEmail());
    // assertEquals("Ochieng", deletedPerson.getLastName());
    // }

    // @Test
    // public void deletePatient_withNonExistentId_shouldReturn404() throws
    // ServletException, IOException {
    // String nonExistentUuid = "00000000-0000-0000-0000-000000000000";
    // MockHttpServletRequest request = buildRequest("DELETE", "/Patient/" +
    // nonExistentUuid);
    // MockHttpServletResponse response = new MockHttpServletResponse();

    // fhirServlet.service(request, response);

    // assertEquals(404, response.getStatus());

    // JsonNode jsonResponse = objectMapper.readTree(response.getContentAsString());
    // assertEquals("OperationOutcome", jsonResponse.get("resourceType").asText());
    // }
}
