package org.openelisglobal.fhir;

import static org.junit.Assert.*;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.RestfulServer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
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
    public void updatePatient_withoutEmergencyContact_succeeds() throws Exception {
        Patient patientWithoutContact = patientService.get("2");
        assertTrue(patientContactService.getForPatient("2").isEmpty());
        String patientUuid = patientWithoutContact.getFhirUuidAsString();

        MockHttpServletResponse response = put(patientUuid, """
                {
                  "resourceType": "Patient",
                  "id": "%s",
                  "name": [{ "family": "Mulizi", "given": ["Jamie"] }],
                  "gender": "female",
                  "birthDate": "1997-10-09"
                }
                """.formatted(patientUuid));

        assertEquals(response.getContentAsString(), 200, response.getStatus());
        assertEquals("Jamie", personService.get("2").getFirstName());
        assertTrue("an update must not invent an emergency contact",
                patientContactService.getForPatient("2").isEmpty());
    }

    @Test
    public void updatePatient_leavesEmergencyContactUntouched() throws Exception {
        Person contactPerson = new Person();
        contactPerson.setFirstName("Grace");
        contactPerson.setLastName("Contact");
        contactPerson.setPrimaryPhone("0700111222");
        contactPerson.setSysUserId("1");
        personService.insert(contactPerson);
        PatientContact contact = patientContactService.getForPatient("3").get(0);
        contact.setPerson(contactPerson);
        contact.setSysUserId("1");
        patientContactService.update(contact);
        String patientUuid = patientService.get("3").getFhirUuidAsString();

        MockHttpServletResponse response = put(patientUuid, """
                {
                  "resourceType": "Patient",
                  "id": "%s",
                  "name": [{ "family": "Kukki", "given": ["Faithful"] }],
                  "telecom": [{ "system": "phone", "value": "0788999000", "use": "mobile" }],
                  "gender": "male",
                  "birthDate": "1992-12-12"
                }
                """.formatted(patientUuid));

        assertEquals(response.getContentAsString(), 200, response.getStatus());
        Person storedContact = personService.get(contactPerson.getId());
        assertEquals("Grace", storedContact.getFirstName());
        assertEquals("Contact", storedContact.getLastName());
        assertEquals("0700111222", storedContact.getPrimaryPhone());
        assertEquals("Faithful", personService.get("3").getFirstName());
    }

    @Test
    public void updatePatient_keepsIdentitiesAFhirPatientCannotCarry() throws Exception {
        String patientUuid = patientService.get("1").getFhirUuidAsString();

        MockHttpServletResponse response = put(patientUuid, """
                {
                  "resourceType": "Patient",
                  "id": "%s",
                  "name": [{ "family": "Doe", "given": ["Johnny"] }],
                  "gender": "male",
                  "birthDate": "1992-12-12"
                }
                """.formatted(patientUuid));

        assertEquals(response.getContentAsString(), 200, response.getStatus());
        assertEquals("Ugandan",
                patientIdentityService.getPatitentIdentityForPatientAndType("1", "17").getIdentityData());
        assertEquals("USA", patientIdentityService.getPatitentIdentityForPatientAndType("1", "18").getIdentityData());
    }

    @Test
    public void updatePatient_withTheResourceAsRead_changesNothingItPublishes() throws Exception {
        Person person = personService.get("1");
        person.setFax("");
        person.setSysUserId("1");
        personService.update(person);
        String patientUuid = patientService.get("1").getFhirUuidAsString();
        JsonNode asRead = read(patientUuid);

        MockHttpServletResponse response = put(patientUuid, asRead.toString());

        assertEquals(response.getContentAsString(), 200, response.getStatus());
        JsonNode afterwards = read(patientUuid);
        for (String element : new String[] { "identifier", "name", "telecom", "gender", "birthDate", "address" }) {
            assertEquals(element, asRead.get(element), afterwards.get(element));
        }
        assertEquals(null, personService.get("1").getStreetAddress());
    }

    @Test
    public void updatePatient_withTheResourceAsRead_writesNothingToThePerson() throws Exception {
        Person before = personService.get("1");
        String zipCodeBefore = before.getZipCode();
        String cellPhoneBefore = before.getCellPhone();
        Timestamp lastUpdatedBefore = before.getLastupdated();
        String patientUuid = patientService.get("1").getFhirUuidAsString();

        MockHttpServletResponse response = put(patientUuid, read(patientUuid).toString());

        assertEquals(response.getContentAsString(), 200, response.getStatus());
        Person after = personService.get("1");
        assertEquals("09785432", after.getCellPhone());
        assertEquals(cellPhoneBefore, after.getCellPhone());
        assertEquals(zipCodeBefore, after.getZipCode());
        assertEquals(lastUpdatedBefore, after.getLastupdated());
    }

    @Test
    public void createPatient_doesNotRecordThePatientAsItsOwnEmergencyContact() throws Exception {
        MockHttpServletRequest request = buildRequest("POST", "/Patient");
        request.setContent("""
                {
                  "resourceType": "Patient",
                  "name": [{ "family": "Nakato", "given": ["Ruth"] }],
                  "gender": "female",
                  "birthDate": "1991-03-04",
                  "address": [{ "line": ["Plot 4"], "city": "Gulu", "postalCode": "256", "country": "Uganda" }]
                }
                """.getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);

        assertEquals(response.getContentAsString(), 201, response.getStatus());
        String createdId = objectMapper.readTree(response.getContentAsString()).get("id").asText();
        Patient created = patientService.getAllMatching("fhirUuid", UUID.fromString(createdId)).get(0);
        assertTrue(patientContactService.getForPatient(created.getId()).isEmpty());
        Person person = personService.get(created.getPerson().getId());
        assertEquals("Uganda", person.getCountry());
        assertEquals("256", person.getZipCode().trim());
    }

    private MockHttpServletResponse put(String patientUuid, String body) throws Exception {
        MockHttpServletRequest request = buildRequest("PUT", "/Patient/" + patientUuid);
        request.setContent(body.getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);
        return response;
    }

    private JsonNode read(String patientUuid) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(buildRequest("GET", "/Patient/" + patientUuid), response);
        assertEquals(200, response.getStatus());
        return objectMapper.readTree(response.getContentAsString());
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

    /**
     * A create the database cannot store answers 422 naming the reason, rather than
     * a bare 500.
     *
     * <p>
     * This asserts the data-error mapping only. The facade does not apply the
     * {@code @ValidName} character-set rule: that constraint is declared on the
     * form classes the controllers bind, and the FHIR providers persist the entity
     * directly, so no bean validation runs on this path. An earlier version of this
     * test posted the family name "Probe123" and expected the charset rule to
     * reject it; it passed only because a stale {@code patient_contact} sequence
     * made the insert collide on its primary key, so the 422 came from the
     * duplicate key and any name would have satisfied it.
     */
    @Test
    public void createPatient_withUnstorableName_returns422() throws Exception {
        String tooLongForTheColumn = "A".repeat(300);
        MockHttpServletRequest request = buildRequest("POST", "/Patient");
        request.setContent(("{\"resourceType\": \"Patient\", \"name\": [{\"family\": \"" + tooLongForTheColumn
                + "\", \"given\": [\"Live\"]}], \"gender\": \"female\", \"birthDate\": \"1990-05-05\"}").getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);

        assertEquals(422, response.getStatus());
        JsonNode outcome = objectMapper.readTree(response.getContentAsString());
        assertEquals("OperationOutcome", outcome.get("resourceType").asText());
        assertTrue("the outcome should name the column that refused the value",
                outcome.get("issue").get(0).get("diagnostics").asText().contains("character varying(255)"));
    }
}
