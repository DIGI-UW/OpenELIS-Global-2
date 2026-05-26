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
        ensureReferenceTables("PATIENT", "PERSON", "PATIENT_IDENTITY");
        executeDataSetWithStateManagement("testdata/system-user.xml");
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

        // Fixture-loaded entities have null sys_user_id; the audit pipeline
        // rejects deletes without one. Stamp before the deleteAll fan-out.
        identities.forEach(e -> e.setSysUserId("1"));
        contacts.forEach(e -> e.setSysUserId("1"));
        personAddresses.forEach(e -> e.setSysUserId("1"));
        patients.forEach(e -> e.setSysUserId("1"));
        persons.forEach(e -> e.setSysUserId("1"));

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

    @Test
    public void readPatient_withNonExistentId_shouldReturn404() throws Exception {
        String nonExistentUuid = "00000000-0000-0000-0000-000000000000";

        MockHttpServletRequest request = buildRequest("GET", "/Patient/" + nonExistentUuid);
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(404, response.getStatus());
        JsonNode json = objectMapper.readTree(response.getContentAsString());
        assertEquals("OperationOutcome", json.get("resourceType").asText());
    }

    @Test
    public void readPatient_shouldReturnCorrectResourceType() throws Exception {
        Patient existingPatient = patientService.get("1");
        String patientUuid = existingPatient.getFhirUuidAsString();

        MockHttpServletRequest request = buildRequest("GET", "/Patient/" + patientUuid);
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());
        JsonNode json = objectMapper.readTree(response.getContentAsString());
        assertEquals("Patient", json.get("resourceType").asText());
        assertEquals(patientUuid, json.get("id").asText());
        assertTrue("Response should contain a name", json.has("name"));
        assertTrue("Response should contain gender", json.has("gender"));
    }

    @Test
    public void readPatient_femalePatient_shouldReturnFemaleGender() throws Exception {
        // fixture patient id=2 has gender=F
        Patient existingPatient = patientService.get("2");
        String patientUuid = existingPatient.getFhirUuidAsString();

        MockHttpServletRequest request = buildRequest("GET", "/Patient/" + patientUuid);
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());
        JsonNode json = objectMapper.readTree(response.getContentAsString());
        assertEquals("female", json.get("gender").asText());
    }

    @Test
    public void createPatient_withTelecom_shouldPersistEmail() throws Exception {
        clearDataBase();

        MockHttpServletRequest request = buildRequest("POST", "/Patient");
        String createJson = """
                {
                  "resourceType": "Patient",
                  "name": [{
                    "use": "official",
                    "family": "Nakato",
                    "given": ["Aisha"]
                  }],
                  "gender": "female",
                  "birthDate": "1995-05-20",
                  "telecom": [{
                    "system": "email",
                    "value": "aisha.nakato@example.com",
                    "use": "work"
                  }]
                }
                """;

        request.setContent(createJson.getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);

        assertEquals(201, response.getStatus());
        JsonNode json = objectMapper.readTree(response.getContentAsString());
        String createdId = json.get("id").asText();

        Patient savedPatient = patientService.getAllMatching("fhirUuid", UUID.fromString(createdId)).get(0);
        Person savedPerson = personService.get(savedPatient.getPerson().getId());
        assertEquals("aisha.nakato@example.com", savedPerson.getEmail());
    }

    @Test
    public void createPatient_withFemaleGender_shouldPersistGenderCorrectly() throws Exception {
        clearDataBase();

        MockHttpServletRequest request = buildRequest("POST", "/Patient");
        String createJson = """
                {
                  "resourceType": "Patient",
                  "name": [{
                    "use": "official",
                    "family": "Akello",
                    "given": ["Grace"]
                  }],
                  "gender": "female",
                  "birthDate": "1990-03-15"
                }
                """;

        request.setContent(createJson.getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);

        assertEquals(201, response.getStatus());
        JsonNode json = objectMapper.readTree(response.getContentAsString());
        String createdId = json.get("id").asText();

        Patient savedPatient = patientService.getAllMatching("fhirUuid", UUID.fromString(createdId)).get(0);
        assertEquals("F", savedPatient.getGender());
    }

    @Test
    public void createPatient_shouldBeReadableAfterCreation() throws Exception {
        clearDataBase();

        MockHttpServletRequest createRequest = buildRequest("POST", "/Patient");
        String createJson = """
                {
                  "resourceType": "Patient",
                  "name": [{
                    "use": "official",
                    "family": "Ssebunya",
                    "given": ["Ronald"]
                  }],
                  "gender": "male",
                  "birthDate": "1988-07-04"
                }
                """;
        createRequest.setContent(createJson.getBytes());
        MockHttpServletResponse createResponse = new MockHttpServletResponse();
        fhirServlet.service(createRequest, createResponse);

        assertEquals(201, createResponse.getStatus());
        JsonNode created = objectMapper.readTree(createResponse.getContentAsString());
        String newUuid = created.get("id").asText();
        assertNotNull(newUuid);

        MockHttpServletRequest readRequest = buildRequest("GET", "/Patient/" + newUuid);
        MockHttpServletResponse readResponse = new MockHttpServletResponse();
        fhirServlet.service(readRequest, readResponse);

        assertEquals(200, readResponse.getStatus());
        JsonNode readJson = objectMapper.readTree(readResponse.getContentAsString());
        assertEquals("Patient", readJson.get("resourceType").asText());
        assertEquals(newUuid, readJson.get("id").asText());
        assertEquals("male", readJson.get("gender").asText());
    }

    @Test
    public void updatePatient_shouldPreserveGender() throws Exception {
        Patient existingPatient = patientService.get("1");
        String patientUuid = existingPatient.getFhirUuidAsString();

        MockHttpServletRequest request = buildRequest("PUT", "/Patient/" + patientUuid);
        String updateJson = """
                {
                  "resourceType": "Patient",
                  "id": "%s",
                  "name": [{
                    "use": "official",
                    "family": "Namukasa",
                    "given": ["Esther"]
                  }],
                  "gender": "male"
                }
                """.formatted(patientUuid);
        request.setContent(updateJson.getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());
        Patient updatedPatient = patientService.get("1");
        assertEquals("M", updatedPatient.getGender());
    }

    @Test
    public void updatePatient_shouldReturnUpdatedResourceInResponse() throws Exception {
        Patient existingPatient = patientService.get("1");
        String patientUuid = existingPatient.getFhirUuidAsString();

        MockHttpServletRequest request = buildRequest("PUT", "/Patient/" + patientUuid);
        String updateJson = """
                {
                  "resourceType": "Patient",
                  "id": "%s",
                  "name": [{
                    "use": "official",
                    "family": "ResponseCheck",
                    "given": ["Test"]
                  }],
                  "gender": "male"
                }
                """.formatted(patientUuid);
        request.setContent(updateJson.getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());
        JsonNode json = objectMapper.readTree(response.getContentAsString());
        assertEquals("Patient", json.get("resourceType").asText());
        assertEquals(patientUuid, json.get("id").asText());
    }

    @Test
    public void deletePatient_thenPatientShouldStillExistInDatabase() throws Exception {
        Patient existingPatient = patientService.get("1");
        String patientUuid = existingPatient.getFhirUuidAsString();

        MockHttpServletRequest deleteRequest = buildRequest("DELETE", "/Patient/" + patientUuid);
        MockHttpServletResponse deleteResponse = new MockHttpServletResponse();
        fhirServlet.service(deleteRequest, deleteResponse);

        assertEquals(204, deleteResponse.getStatus());
        Patient afterDelete = patientService.get("1");
        assertNotNull("Patient record should still exist in the database after delete", afterDelete);
    }

    @Test
    public void createPatient_withInvalidGender_shouldReturn400() throws Exception {
        clearDataBase();

        MockHttpServletRequest request = buildRequest("POST", "/Patient");
        String invalidJson = """
                {
                  "resourceType": "Patient",
                  "name": [{
                    "use": "official",
                    "family": "Test",
                    "given": ["User"]
                  }],
                  "gender": "invalidGender"
                }
                """;
        request.setContent(invalidJson.getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);

        assertEquals(400, response.getStatus());
    }

    @Test
    public void updatePatient_withNewBirthDate_shouldPersistInDatabase() throws Exception {
        Patient existingPatient = patientService.get("1");
        String patientUuid = existingPatient.getFhirUuidAsString();

        MockHttpServletRequest request = buildRequest("PUT", "/Patient/" + patientUuid);
        String updateJson = """
                {
                  "resourceType": "Patient",
                  "id": "%s",
                  "name": [{
                    "use": "official",
                    "family": "Doe",
                    "given": ["John"]
                  }],
                  "gender": "male",
                  "birthDate": "1985-06-15"
                }
                """.formatted(patientUuid);
        request.setContent(updateJson.getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());

        Patient updatedPatient = patientService.get("1");
        assertNotNull("birthDate should not be null after update", updatedPatient.getBirthDate());
        assertTrue("birthDate should reflect 1985", updatedPatient.getBirthDateForDisplay().contains("1985"));
    }

    @Test
    public void deletePatient_twice_shouldReturn204BothTimes() throws Exception {
        Patient existingPatient = patientService.get("1");
        String patientUuid = existingPatient.getFhirUuidAsString();

        MockHttpServletRequest firstDelete = buildRequest("DELETE", "/Patient/" + patientUuid);
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        fhirServlet.service(firstDelete, firstResponse);
        assertEquals(204, firstResponse.getStatus());

        MockHttpServletRequest secondDelete = buildRequest("DELETE", "/Patient/" + patientUuid);
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        fhirServlet.service(secondDelete, secondResponse);
        assertEquals(204, secondResponse.getStatus());
    }
}
