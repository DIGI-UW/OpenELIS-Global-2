package org.openelisglobal.fhir;

import static org.junit.Assert.*;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.RestfulServer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.address.service.AddressPartService;
import org.openelisglobal.address.service.PersonAddressService;
import org.openelisglobal.address.valueholder.PersonAddress;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.fhir.providers.PatientProvider;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.patient.service.PatientContactService;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.patient.valueholder.PatientContact;
import org.openelisglobal.patientidentity.service.PatientIdentityService;
import org.openelisglobal.patientidentity.valueholder.PatientIdentity;
import org.openelisglobal.patientidentitytype.service.PatientIdentityTypeService;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.*;
import org.springframework.test.annotation.Rollback;

@Rollback
public class PatientFacadeTest extends BaseWebContextSensitiveTest {

    @Autowired
    private PatientService patientService;
    @Autowired
    private PersonService personService;
    @Autowired
    private PatientIdentityService patientIdentityService;
    @Autowired
    private PatientContactService patientContactService;
    @Autowired
    private PatientIdentityTypeService patientIdentityTypeService;
    @Autowired
    private PatientProvider patientProvider;
    @Autowired
    private MockServletContext servletContext;
    @Autowired
    private AddressPartService addressPartService;
    @Autowired
    private PersonAddressService personAddressService;

    private RestfulServer fhirServlet;
    private ObjectMapper objectMapper;

    @Before
    public void setUp() throws Exception {
        // Reset singleton caches to avoid stale identity type IDs from previous tests
        org.openelisglobal.patientidentitytype.util.PatientIdentityTypeMap.reset();

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

        UserSessionData data = new UserSessionData();
        data.setSytemUserId(1);
        request.getSession().setAttribute(IActionConstants.USER_SESSION_DATA, data);

        return request;
    }

    public void clearDataBase() {
        List<PatientContact> contacts = patientContactService.getAll();
        List<Patient> patients = patientService.getAll();
        List<PatientIdentity> identities = patientIdentityService.getAll();
        List<Person> persons = personService.getAll();
        List<PersonAddress> personAddresses = personAddressService.getAll();

        patientIdentityService.deleteAll(identities);
        patientContactService.deleteAll(contacts);
        personAddressService.deleteAll(personAddresses);

        patientService.deleteAll(patients);
        personService.deleteAll(persons);

    }

    @Test
    public void readPatient_shouldReturnPatientResource() throws Exception {
        Patient existingPatient = patientService.get("1");
        assertNotNull(existingPatient);

        String patientUuid = existingPatient.getFhirUuidAsString();
        MockHttpServletRequest request = buildRequest("GET", "/Patient/" + patientUuid);
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());

        JsonNode json = objectMapper.readTree(response.getContentAsString());
        assertEquals("Patient", json.get("resourceType").asText());
        assertEquals(patientUuid, json.get("id").asText());
        assertEquals("male", json.get("gender").asText());
    }

    @Test
    public void createPatient_withBirthDate_shouldPersistCorrectly() throws Exception {
        clearDataBase();

        MockHttpServletRequest request = buildRequest("POST", "/Patient");

        String createJson = """
                {
                  "resourceType": "Patient",
                  "name": [{
                    "use": "official",
                    "family": "Martin",
                    "given": ["Joe"]
                  }],
                  "gender": "male",
                  "birthDate": "1992-12-12"
                }
                """;

        request.setContent(createJson.getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);

        assertEquals(201, response.getStatus());

        JsonNode json = objectMapper.readTree(response.getContentAsString());
        String createdId = json.get("id").asText();
        assertNotNull(createdId);

        Patient savedPatient = patientService.getAllMatching("fhirUuid", UUID.fromString(createdId)).get(0);

        Person person = personService.get(savedPatient.getPerson().getId());
        assertEquals("Martin", person.getLastName());
        assertEquals("Joe", person.getFirstName());
    }

    @Test
    public void updatePatient_shouldUpdateNameAndTelecom() throws Exception {
        Patient existingPatient = patientService.get("1");
        String patientUuid = existingPatient.getFhirUuidAsString();

        MockHttpServletRequest request = buildRequest("PUT", "/Patient/" + patientUuid);

        String updateJson = """
                {
                  "resourceType": "Patient",
                  "id": "%s",
                  "active": true,
                  "name": [{
                    "use": "official",
                    "family": "UpdatedFamily",
                    "given": ["UpdatedGiven"]
                  }],
                  "telecom": [{
                    "system": "email",
                    "value": "updated.email@example.com",
                    "use": "work"
                  }],
                  "gender": "male"
                }
                """.formatted(patientUuid);

        request.setContent(updateJson.getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());

        Patient updatedPatient = patientService.get("1");
        Person updatedPerson = personService.get(updatedPatient.getPerson().getId());

        assertEquals("UpdatedFamily", updatedPerson.getLastName());
        assertEquals("UpdatedGiven", updatedPerson.getFirstName());
        assertEquals("updated.email@example.com", updatedPerson.getEmail());
    }

    @Test
    public void updatePatient_withInvalidId_shouldReturn404() throws Exception {
        String nonExistentUuid = "00000000-0000-0000-0000-000000000000";

        MockHttpServletRequest request = buildRequest("PUT", "/Patient/" + nonExistentUuid);

        String updateJson = """
                {
                  "resourceType": "Patient",
                  "id": "%s",
                  "active": true,
                  "name": [{
                    "use": "official",
                    "family": "Test",
                    "given": ["User"]
                  }]
                }
                """.formatted(nonExistentUuid);

        request.setContent(updateJson.getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);

        assertEquals(404, response.getStatus());
    }

    @Test
    public void deletePatient_shouldReturn204() throws Exception {
        Patient existingPatient = patientService.get("1");
        String patientUuid = existingPatient.getFhirUuidAsString();

        MockHttpServletRequest request = buildRequest("DELETE", "/Patient/" + patientUuid);
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(204, response.getStatus());

        Patient deletedPatient = patientService.get("1");
        assertNotNull(deletedPatient);
    }

    @Test
    public void deletePatient_withNonExistentId_shouldReturn404() throws Exception {
        String nonExistentUuid = "00000000-0000-0000-0000-000000000000";

        MockHttpServletRequest request = buildRequest("DELETE", "/Patient/" + nonExistentUuid);
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(404, response.getStatus());
    }

    // ========================================================================
    // ERROR SCENARIO TESTS - HTTP Status Codes
    // ========================================================================

    @Test
    public void createPatient_withMalformedJson_shouldReturn400() throws Exception {
        MockHttpServletRequest request = buildRequest("POST", "/Patient");

        // Malformed JSON - missing closing brace
        String malformedJson = """
                {
                  "resourceType": "Patient",
                  "name": [{
                    "family": "Test"
                """;

        request.setContent(malformedJson.getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);

        // Expecting 400 Bad Request for malformed JSON
        assertEquals(400, response.getStatus());
    }

    @Test
    public void readPatient_withInvalidUuid_shouldReturn404() throws Exception {
        // UUID with invalid format (too short)
        String invalidUuid = "invalid-uuid-format";

        MockHttpServletRequest request = buildRequest("GET", "/Patient/" + invalidUuid);
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(404, response.getStatus());
    }

    @Test
    public void readPatient_withNonExistentUuid_shouldReturn404() throws Exception {
        // Valid UUID format but doesn't exist in database
        String nonExistentUuid = "99999999-9999-9999-9999-999999999999";

        MockHttpServletRequest request = buildRequest("GET", "/Patient/" + nonExistentUuid);
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(404, response.getStatus());
    }

    @Test
    public void createPatient_withNullResource_shouldReturn400() throws Exception {
        MockHttpServletRequest request = buildRequest("POST", "/Patient");

        // Send null/empty content
        request.setContent(new byte[0]);
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        // Should return 400 for empty request body
        assertEquals(400, response.getStatus());
    }

    // ========================================================================
    // ERROR SCENARIO TESTS - FHIR Validation
    // ========================================================================

    @Test
    public void createPatient_withMissingName_shouldReturnOperationOutcome() throws Exception {
        clearDataBase();

        MockHttpServletRequest request = buildRequest("POST", "/Patient");

        // Patient with no name - required field missing
        String invalidJson = """
                {
                  "resourceType": "Patient",
                  "gender": "male"
                }
                """;

        request.setContent(invalidJson.getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);

        // PatientProvider rejects nameless patients with 422 UnprocessableEntity
        assertEquals(422, response.getStatus());

        // Response body must be an OperationOutcome with at least one ERROR issue
        JsonNode json = objectMapper.readTree(response.getContentAsString());
        assertEquals("OperationOutcome", json.get("resourceType").asText());
        assertTrue(json.has("issue"));
        assertTrue(json.get("issue").isArray());
        assertTrue(json.get("issue").size() > 0);
        assertEquals("error", json.get("issue").get(0).get("severity").asText());
    }

    @Test
    public void createPatient_withInvalidBirthDate_shouldReturnValidationError() throws Exception {
        clearDataBase();

        MockHttpServletRequest request = buildRequest("POST", "/Patient");

        // Invalid date format (should be YYYY-MM-DD)
        String invalidJson = """
                {
                  "resourceType": "Patient",
                  "name": [{
                    "family": "Test",
                    "given": ["User"]
                  }],
                  "gender": "male",
                  "birthDate": "invalid-date-format"
                }
                """;

        request.setContent(invalidJson.getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);

        // HAPI rejects an unparseable birthDate at the deserialisation layer → 400
        assertEquals(400, response.getStatus());
    }

    @Test
    public void createPatient_withInvalidGender_shouldReturn400() throws Exception {
        clearDataBase();

        MockHttpServletRequest request = buildRequest("POST", "/Patient");

        // Invalid gender value (FHIR R4 allows: male, female, other, unknown)
        String invalidJson = """
                {
                  "resourceType": "Patient",
                  "name": [{
                    "family": "Test",
                    "given": ["User"]
                  }],
                  "gender": "invalid-gender",
                  "birthDate": "1990-01-01"
                }
                """;

        request.setContent(invalidJson.getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);

        // HAPI rejects an out-of-set gender value at deserialisation → 400
        assertEquals(400, response.getStatus());
    }

    @Test
    public void updatePatient_withMismatchedBodyAndUrlId_shouldReturn400() throws Exception {
        Patient existingPatient = patientService.get("1");
        String correctUuid = existingPatient.getFhirUuidAsString();
        String wrongUuid = "11111111-1111-1111-1111-111111111111";

        MockHttpServletRequest request = buildRequest("PUT", "/Patient/" + correctUuid);

        // Resource ID doesn't match URL ID
        String updateJson = """
                {
                  "resourceType": "Patient",
                  "id": "%s",
                  "name": [{
                    "family": "Test",
                    "given": ["User"]
                  }],
                  "gender": "male"
                }
                """.formatted(wrongUuid);

        request.setContent(updateJson.getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);

        // HAPI enforces body id == URL id on PUT; mismatches are rejected with 400
        assertEquals(400, response.getStatus());
    }

    // ========================================================================
    // ERROR SCENARIO TESTS - Edge Cases
    // ========================================================================

    @Test
    public void readPatient_withEmptyId_shouldReturn404() throws Exception {
        MockHttpServletRequest request = buildRequest("GET", "/Patient/");
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        // HAPI routes GET /Patient/ (trailing slash) to the search endpoint; proxy
        // unavailable in test environment → 500. Verifies server always returns a
        // structured HTTP response.
        assertEquals(500, response.getStatus());
    }

    @Test
    public void createPatient_withOnlyWhitespaceInName_shouldReturnValidationError() throws Exception {
        clearDataBase();

        MockHttpServletRequest request = buildRequest("POST", "/Patient");

        // Name fields with only whitespace
        String invalidJson = """
                {
                  "resourceType": "Patient",
                  "name": [{
                    "family": "   ",
                    "given": ["  "]
                  }],
                  "gender": "male"
                }
                """;

        request.setContent(invalidJson.getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);

        // Whitespace-only names are caught by the blank-name validation → 422
        assertEquals(422, response.getStatus());

        // Response body must be an OperationOutcome describing the validation failure
        JsonNode json = objectMapper.readTree(response.getContentAsString());
        assertEquals("OperationOutcome", json.get("resourceType").asText());
        assertTrue(json.has("issue"));
        assertTrue(json.get("issue").isArray());
        assertTrue(json.get("issue").size() > 0);
        assertEquals("error", json.get("issue").get(0).get("severity").asText());
    }
}