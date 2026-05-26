package org.openelisglobal.fhir;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.RestfulServer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
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
    private org.openelisglobal.fhir.providers.PatientProvider patientProvider;
    @Autowired
    private MockServletContext servletContext;

    private RestfulServer fhirServlet;
    private ObjectMapper objectMapper;

    @Before
    public void setUp() throws Exception {
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
        MockHttpServletRequest request = buildFhirRequest(method, pathInfo);

        UserSessionData data = new UserSessionData();
        data.setSytemUserId(Integer.parseInt(TEST_SYS_USER_ID));
        request.getSession().setAttribute(IActionConstants.USER_SESSION_DATA, data);

        return request;
    }

    @Test
    public void readPatient_shouldReturnPatientResource() throws Exception {
        Patient patient = patientService.get("1");
        String uuid = patient.getFhirUuidAsString();

        MockHttpServletRequest request = buildRequest("GET", "/Patient/" + uuid);
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());
        JsonNode json = objectMapper.readTree(response.getContentAsString());
        assertEquals("Patient", json.get("resourceType").asText());
        assertEquals(uuid, json.get("id").asText());
    }

    @Test
    public void readPatient_femalePatient_shouldReturnFemaleGender() throws Exception {
        Patient patient = patientService.get("2");
        String uuid = patient.getFhirUuidAsString();

        MockHttpServletRequest request = buildRequest("GET", "/Patient/" + uuid);
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());
        JsonNode json = objectMapper.readTree(response.getContentAsString());
        assertEquals("female", json.get("gender").asText());
    }

    private void prepareCleanSlate() throws Exception {
        cleanRowsInCurrentConnection(
                new String[] { "patient_contact", "patient_identity", "person_address", "patient", "person" });
    }

    @Test
    public void createPatient_withTelecom_shouldPersistEmail() throws Exception {
        prepareCleanSlate();
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
        prepareCleanSlate();
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
    public void updatePatient_shouldPreserveGender() throws Exception {
        Patient patient = patientService.get("1");
        String uuid = patient.getFhirUuidAsString();

        MockHttpServletRequest request = buildRequest("PUT", "/Patient/" + uuid);
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
                """.formatted(uuid);
        request.setContent(updateJson.getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());
        Patient updatedPatient = patientService.get("1");
        assertEquals("M", updatedPatient.getGender());
    }

    @Test
    public void updatePatient_withInvalidId_shouldReturn404() throws Exception {
        String nonExistentUuid = UUID.randomUUID().toString();

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
        Patient patient = patientService.get("1");
        String uuid = patient.getFhirUuidAsString();

        MockHttpServletRequest request = buildRequest("DELETE", "/Patient/" + uuid);
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(204, response.getStatus());
    }

    @Test
    public void deletePatient_thenPatientShouldStillExistInDatabase() throws Exception {
        Patient patient = patientService.get("1");
        String uuid = patient.getFhirUuidAsString();

        MockHttpServletRequest deleteRequest = buildRequest("DELETE", "/Patient/" + uuid);
        MockHttpServletResponse deleteResponse = new MockHttpServletResponse();
        fhirServlet.service(deleteRequest, deleteResponse);

        assertEquals(204, deleteResponse.getStatus());
        Patient afterDelete = patientService.get("1");
        assertNotNull(afterDelete);
    }

    @Test
    public void createPatient_withInvalidGender_shouldReturn400() throws Exception {
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
        Patient patient = patientService.get("1");
        String uuid = patient.getFhirUuidAsString();

        MockHttpServletRequest request = buildRequest("PUT", "/Patient/" + uuid);
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
                """.formatted(uuid);
        request.setContent(updateJson.getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());

        Patient updatedPatient = patientService.get("1");
        assertNotNull(updatedPatient.getBirthDate());
        assertTrue(updatedPatient.getBirthDateForDisplay().contains("1985"));
    }

    @Test
    public void deletePatient_withNonExistentId_shouldReturn404() throws Exception {
        String nonExistentUuid = UUID.randomUUID().toString();
        MockHttpServletRequest request = buildRequest("DELETE", "/Patient/" + nonExistentUuid);
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(404, response.getStatus());
    }

    @Test
    public void readPatient_withNonExistentId_shouldReturn404() throws Exception {
        String nonExistentUuid = UUID.randomUUID().toString();

        MockHttpServletRequest request = buildRequest("GET", "/Patient/" + nonExistentUuid);
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(404, response.getStatus());
        JsonNode json = objectMapper.readTree(response.getContentAsString());
        assertEquals("OperationOutcome", json.get("resourceType").asText());
    }
}
