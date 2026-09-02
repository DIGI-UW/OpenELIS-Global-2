package org.openelisglobal.fhir;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
import org.openelisglobal.fhir.providers.PatientProvider;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.service.PatientServiceImpl;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.util.AopTestUtils;

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
        org.openelisglobal.patientidentitytype.util.PatientIdentityTypeMap.reset();
        executeDataSetWithStateManagement("testdata/facade-patient.xml");
        ensureReferenceTables("PATIENT", "PERSON", "PATIENT_IDENTITY", "PATIENT_IDENTITY_TYPE");
        executeDataSetWithStateManagement("testdata/system-user.xml");

        PatientServiceImpl target = AopTestUtils.getUltimateTargetObject(patientService);
        target.initializeGlobalVariables();

        fhirServlet = new RestfulServer(FhirContext.forR4());
        fhirServlet.setResourceProviders(Arrays.asList(patientProvider));
        MockServletConfig servletConfig = new MockServletConfig(servletContext);
        servletConfig.addInitParameter("name", "FhirServlet");
        fhirServlet.init(servletConfig);

        objectMapper = new ObjectMapper();
    }

    @Test
    public void readPatient_shouldReturn200() throws Exception {
        Patient patient = patientService.get("2");
        String uuid = patient.getFhirUuidAsString();

        MockHttpServletRequest request = buildFhirRequest("GET", "/Patient/" + uuid);
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());
    }

    @Test
    public void readPatient_shouldMapResourceTypeAndId() throws Exception {
        Patient patient = patientService.get("2");
        String uuid = patient.getFhirUuidAsString();

        MockHttpServletRequest request = buildFhirRequest("GET", "/Patient/" + uuid);
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        JsonNode json = objectMapper.readTree(response.getContentAsString());
        assertEquals("Patient", json.get("resourceType").asText());
        assertEquals(uuid, json.get("id").asText());
    }

    @Test
    public void readPatient_shouldMapFemaleGender() throws Exception {
        Patient patient = patientService.get("2");
        String uuid = patient.getFhirUuidAsString();

        MockHttpServletRequest request = buildFhirRequest("GET", "/Patient/" + uuid);
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        JsonNode json = objectMapper.readTree(response.getContentAsString());
        assertEquals("female", json.get("gender").asText());
    }

    @Test
    public void readPatient_shouldMapNameField() throws Exception {
        Patient patient = patientService.get("1");
        String uuid = patient.getFhirUuidAsString();

        MockHttpServletRequest request = buildFhirRequest("GET", "/Patient/" + uuid);
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        JsonNode json = objectMapper.readTree(response.getContentAsString());
        assertNotNull(json.get("name"));
        assertTrue(json.get("name").size() > 0);
    }

    @Test
    public void readPatient_shouldMapGenderField() throws Exception {
        Patient patient = patientService.get("1");
        String uuid = patient.getFhirUuidAsString();

        MockHttpServletRequest request = buildFhirRequest("GET", "/Patient/" + uuid);
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        JsonNode json = objectMapper.readTree(response.getContentAsString());
        assertNotNull(json.get("gender"));
        assertFalse(json.get("gender").asText().isEmpty());
    }

    @Test
    public void readPatient_shouldMapBirthDateField() throws Exception {
        Patient patient = patientService.get("1");
        String uuid = patient.getFhirUuidAsString();

        MockHttpServletRequest request = buildFhirRequest("GET", "/Patient/" + uuid);
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        JsonNode json = objectMapper.readTree(response.getContentAsString());
        assertNotNull(json.get("birthDate"));
        assertFalse(json.get("birthDate").asText().isEmpty());
    }

    @Test
    public void readPatient_shouldMapAddressField() throws Exception {
        Patient patient = patientService.get("1");
        String uuid = patient.getFhirUuidAsString();

        MockHttpServletRequest request = buildFhirRequest("GET", "/Patient/" + uuid);
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        JsonNode json = objectMapper.readTree(response.getContentAsString());
        JsonNode address = json.get("address");
        assertNotNull(address);
        assertTrue(address.size() > 0);
        assertEquals("Kampala", address.get(0).get("city").asText());
        assertEquals("Uganda", address.get(0).get("country").asText());
    }

    @Test
    public void readPatient_shouldMapAtLeastFiveIdentifiers() throws Exception {
        Patient patient = patientService.get("1");
        String uuid = patient.getFhirUuidAsString();

        MockHttpServletRequest request = buildFhirRequest("GET", "/Patient/" + uuid);
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        JsonNode json = objectMapper.readTree(response.getContentAsString());
        JsonNode identifiers = json.get("identifier");
        assertNotNull(identifiers);
        assertTrue(identifiers.size() >= 5);
    }

    @Test
    public void readPatient_shouldMapNationalIdIdentifier() throws Exception {
        Patient patient = patientService.get("1");
        String uuid = patient.getFhirUuidAsString();

        MockHttpServletRequest request = buildFhirRequest("GET", "/Patient/" + uuid);
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        JsonNode json = objectMapper.readTree(response.getContentAsString());
        JsonNode identifiers = json.get("identifier");
        boolean hasNationalId = false;
        for (JsonNode identifier : identifiers) {
            if (identifier.path("system").asText().endsWith("/pat_nationalId")) {
                hasNationalId = true;
                break;
            }
        }
        assertTrue(hasNationalId);
    }

    @Test
    public void readPatient_shouldMapSubjectNumberIdentifier() throws Exception {
        Patient patient = patientService.get("1");
        String uuid = patient.getFhirUuidAsString();

        MockHttpServletRequest request = buildFhirRequest("GET", "/Patient/" + uuid);
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        JsonNode json = objectMapper.readTree(response.getContentAsString());
        JsonNode identifiers = json.get("identifier");
        boolean hasSubjectNumber = false;
        for (JsonNode identifier : identifiers) {
            if (identifier.path("system").asText().endsWith("/pat_subjectNumber")) {
                hasSubjectNumber = true;
                break;
            }
        }
        assertTrue(hasSubjectNumber);
    }

    @Test
    public void readPatient_shouldMapStNumberIdentifier() throws Exception {
        Patient patient = patientService.get("1");
        String uuid = patient.getFhirUuidAsString();

        MockHttpServletRequest request = buildFhirRequest("GET", "/Patient/" + uuid);
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        JsonNode json = objectMapper.readTree(response.getContentAsString());
        JsonNode identifiers = json.get("identifier");
        boolean hasStNumber = false;
        for (JsonNode identifier : identifiers) {
            if (identifier.path("system").asText().endsWith("/pat_stNumber")) {
                hasStNumber = true;
                break;
            }
        }
        assertTrue(hasStNumber);
    }

    @Test
    public void readPatient_shouldMapGuidIdentifier() throws Exception {
        Patient patient = patientService.get("1");
        String uuid = patient.getFhirUuidAsString();

        MockHttpServletRequest request = buildFhirRequest("GET", "/Patient/" + uuid);
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        JsonNode json = objectMapper.readTree(response.getContentAsString());
        JsonNode identifiers = json.get("identifier");
        boolean hasGuid = false;
        for (JsonNode identifier : identifiers) {
            if (identifier.path("system").asText().endsWith("/pat_guid")) {
                hasGuid = true;
                break;
            }
        }
        assertTrue(hasGuid);
    }

    @Test
    public void readPatient_shouldMapUuidIdentifier() throws Exception {
        Patient patient = patientService.get("1");
        String uuid = patient.getFhirUuidAsString();

        MockHttpServletRequest request = buildFhirRequest("GET", "/Patient/" + uuid);
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        JsonNode json = objectMapper.readTree(response.getContentAsString());
        JsonNode identifiers = json.get("identifier");
        boolean hasUuid = false;
        for (JsonNode identifier : identifiers) {
            if (identifier.path("system").asText().endsWith("/pat_uuid")) {
                hasUuid = true;
                break;
            }
        }
        assertTrue(hasUuid);
    }

    @Test
    public void readPatient_withNonExistentId_shouldReturn404() throws Exception {
        String nonExistentUuid = UUID.randomUUID().toString();

        MockHttpServletRequest request = buildFhirRequest("GET", "/Patient/" + nonExistentUuid);
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(404, response.getStatus());
    }

    @Test
    public void readPatient_withNonExistentId_shouldReturnOperationOutcome() throws Exception {
        String nonExistentUuid = UUID.randomUUID().toString();

        MockHttpServletRequest request = buildFhirRequest("GET", "/Patient/" + nonExistentUuid);
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        JsonNode json = objectMapper.readTree(response.getContentAsString());
        assertEquals("OperationOutcome", json.get("resourceType").asText());
    }

    @Test
    public void createPatient_withAllIdentifiers_shouldReturn201CreatedStatus() throws Exception {
        MockHttpServletRequest request = buildFhirRequest("POST", "/Patient");
        String createJson = """
                {
                  "resourceType": "Patient",
                  "identifier": [
                    { "system": "http://openelis-global.org/pat_nationalId", "value": "NAT123" },
                    { "system": "http://openelis-global.org/pat_subjectNumber", "value": "SUB123" },
                    { "system": "http://openelis-global.org/pat_stNumber", "value": "ST123" },
                    { "system": "http://openelis-global.org/pat_guid", "value": "GUID123" }
                  ],
                  "name": [{
                    "use": "official",
                    "family": "Identifier",
                    "given": ["Test"]
                  }],
                  "gender": "male",
                  "birthDate": "1992-12-12",
                  "address": [{
                    "line": ["123 Test St"],
                    "city": "Kampala",
                    "country": "Uganda"
                  }]
                }
                """;
        request.setContent(createJson.getBytes());

        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(201, response.getStatus());
    }

    @Test
    public void createPatient_withBirthDate_shouldReturn201CreatedStatus() throws Exception {
        MockHttpServletRequest request = buildFhirRequest("POST", "/Patient");
        String createJson = """
                {
                  "resourceType": "Patient",
                  "name": [{
                    "use": "official",
                    "family": "Martin",
                    "given": ["Joe"]
                  }],
                  "gender": "male",
                  "birthDate": "1992-12-12",
                  "address": [{
                    "line": ["456 Birth St"],
                    "city": "Entebbe",
                    "country": "Uganda"
                  }]
                }
                """;
        request.setContent(createJson.getBytes());

        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(201, response.getStatus());
    }

    @Test
    public void createPatient_withTelecom_shouldReturn201Created() throws Exception {
        MockHttpServletRequest request = buildFhirRequest("POST", "/Patient");
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
                  }],
                  "address": [{
                    "line": ["789 Telecom Rd"],
                    "city": "Jinja",
                    "country": "Uganda"
                  }]
                }
                """;
        request.setContent(createJson.getBytes());

        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(201, response.getStatus());
    }

    @Test
    public void createPatient_withFemaleGender_shouldReturn201Created() throws Exception {
        MockHttpServletRequest request = buildFhirRequest("POST", "/Patient");
        String createJson = """
                {
                  "resourceType": "Patient",
                  "name": [{
                    "use": "official",
                    "family": "Akello",
                    "given": ["Grace"]
                  }],
                  "gender": "female",
                  "birthDate": "1990-03-15",
                  "address": [{
                    "line": ["101 Gender Ave"],
                    "city": "Gulu",
                    "country": "Uganda"
                  }]
                }
                """;
        request.setContent(createJson.getBytes());

        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(201, response.getStatus());
    }

    @Test
    public void createPatient_withInvalidGender_shouldReturn400() throws Exception {
        MockHttpServletRequest request = buildFhirRequest("POST", "/Patient");
        String invalidJson = """
                {
                  "resourceType": "Patient",
                  "name": [{
                    "use": "official",
                    "family": "Test",
                    "given": ["User"]
                  }],
                  "gender": "invalidGender",
                  "address": [{
                    "line": ["Invalid St"],
                    "city": "TestCity",
                    "country": "TestCountry"
                  }]
                }
                """;
        request.setContent(invalidJson.getBytes());

        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(400, response.getStatus());
    }

    @Test
    public void updatePatient_shouldReturn200() throws Exception {
        Patient patient = patientService.get("1");
        String uuid = patient.getFhirUuidAsString();
        MockHttpServletRequest request = buildFhirRequest("PUT", "/Patient/" + uuid);
        String updateJson = """
                {
                  "resourceType": "Patient",
                  "id": "%s",
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
                  "gender": "male",
                  "address": [{
                    "line": ["Updated Address"],
                    "city": "Updated City",
                    "country": "Uganda"
                  }]
                }
                """.formatted(uuid);
        request.setContent(updateJson.getBytes());

        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());
    }

    @Test
    public void updatePatient_shouldPersistUpdatedFieldsInDatabase() throws Exception {
        Patient patient = patientService.get("1");
        String uuid = patient.getFhirUuidAsString();
        MockHttpServletRequest request = buildFhirRequest("PUT", "/Patient/" + uuid);
        String updateJson = """
                {
                  "resourceType": "Patient",
                  "id": "%s",
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
                  "gender": "male",
                  "address": [{
                    "line": ["Updated Road"],
                    "city": "Updated City",
                    "country": "Uganda"
                  }]
                }
                """.formatted(uuid);
        request.setContent(updateJson.getBytes());

        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        Patient updatedPatient = patientService.get("1");
        Person updatedPerson = personService.get(updatedPatient.getPerson().getId());

        assertEquals("UpdatedFamily", updatedPerson.getLastName());
        assertEquals("UpdatedGiven", updatedPerson.getFirstName());
        assertEquals("updated.email@example.com", updatedPerson.getEmail());
        assertEquals("Updated City", updatedPerson.getCity());
    }

    @Test
    public void updatePatient_shouldReturn200WhenGenderProvided() throws Exception {
        Patient patient = patientService.get("1");
        String uuid = patient.getFhirUuidAsString();
        MockHttpServletRequest request = buildFhirRequest("PUT", "/Patient/" + uuid);
        String updateJson = """
                {
                  "resourceType": "Patient",
                  "id": "%s",
                  "name": [{
                    "use": "official",
                    "family": "Namukasa",
                    "given": ["Esther"]
                  }],
                  "gender": "male",
                  "address": [{
                    "line": ["Preserve St"],
                    "city": "Preserve City",
                    "country": "Uganda"
                  }]
                }
                """.formatted(uuid);
        request.setContent(updateJson.getBytes());

        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());
    }

    @Test
    public void updatePatient_shouldPreserveGenderInDatabase() throws Exception {
        Patient patient = patientService.get("1");
        String uuid = patient.getFhirUuidAsString();
        MockHttpServletRequest request = buildFhirRequest("PUT", "/Patient/" + uuid);
        String updateJson = """
                {
                  "resourceType": "Patient",
                  "id": "%s",
                  "name": [{
                    "use": "official",
                    "family": "Namukasa",
                    "given": ["Esther"]
                  }],
                  "gender": "male",
                  "address": [{
                    "line": ["Gender St"],
                    "city": "Gender City",
                    "country": "Uganda"
                  }]
                }
                """.formatted(uuid);
        request.setContent(updateJson.getBytes());

        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        Patient updatedPatient = patientService.get("1");

        assertEquals("M", updatedPatient.getGender());
    }

    @Test
    public void updatePatient_withInvalidId_shouldReturn404() throws Exception {
        String nonExistentUuid = UUID.randomUUID().toString();
        MockHttpServletRequest request = buildFhirRequest("PUT", "/Patient/" + nonExistentUuid);
        String updateJson = """
                {
                  "resourceType": "Patient",
                  "id": "%s",
                  "name": [{
                    "use": "official",
                    "family": "Test",
                    "given": ["User"]
                  }],
                  "address": [{
                    "line": ["Invalid ID St"],
                    "city": "Invalid City",
                    "country": "Uganda"
                  }]
                }
                """.formatted(nonExistentUuid);
        request.setContent(updateJson.getBytes());

        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(404, response.getStatus());
    }

    @Test
    public void updatePatient_withNewBirthDate_shouldReturn200() throws Exception {
        Patient patient = patientService.get("1");
        String uuid = patient.getFhirUuidAsString();
        MockHttpServletRequest request = buildFhirRequest("PUT", "/Patient/" + uuid);
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
                  "birthDate": "1985-06-15",
                  "address": [{
                    "line": ["Birthdate St"],
                    "city": "Birthdate City",
                    "country": "Uganda"
                  }]
                }
                """.formatted(uuid);
        request.setContent(updateJson.getBytes());

        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(200, response.getStatus());
    }

    @Test
    public void updatePatient_withNewBirthDate_shouldPersistInDatabase() throws Exception {
        Patient patient = patientService.get("1");
        String uuid = patient.getFhirUuidAsString();
        MockHttpServletRequest request = buildFhirRequest("PUT", "/Patient/" + uuid);
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
                  "birthDate": "1985-06-15",
                  "address": [{
                    "line": ["Birthdate Road"],
                    "city": "Birthdate City",
                    "country": "Uganda"
                  }]
                }
                """.formatted(uuid);
        request.setContent(updateJson.getBytes());

        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        Patient updatedPatient = patientService.get("1");

        assertNotNull(updatedPatient.getBirthDate());
        assertTrue(updatedPatient.getBirthDateForDisplay().contains("1985"));
    }

    @Test
    public void deletePatient_shouldReturn204() throws Exception {
        Patient patient = patientService.get("1");
        String uuid = patient.getFhirUuidAsString();

        MockHttpServletRequest request = buildFhirRequest("DELETE", "/Patient/" + uuid);
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(204, response.getStatus());
    }

    @Test
    public void deletePatient_withNonExistentId_shouldReturn404() throws Exception {
        String nonExistentUuid = UUID.randomUUID().toString();

        MockHttpServletRequest request = buildFhirRequest("DELETE", "/Patient/" + nonExistentUuid);
        MockHttpServletResponse response = new MockHttpServletResponse();

        fhirServlet.service(request, response);

        assertEquals(404, response.getStatus());
    }

    @Test
    public void deletePatient_thenPatientShouldStillExistInDatabase() throws Exception {
        Patient patient = patientService.get("1");
        String uuid = patient.getFhirUuidAsString();

        MockHttpServletRequest deleteRequest = buildFhirRequest("DELETE", "/Patient/" + uuid);
        MockHttpServletResponse deleteResponse = new MockHttpServletResponse();

        fhirServlet.service(deleteRequest, deleteResponse);

        Patient afterDelete = patientService.get("1");

        assertNotNull(afterDelete);
    }
}
