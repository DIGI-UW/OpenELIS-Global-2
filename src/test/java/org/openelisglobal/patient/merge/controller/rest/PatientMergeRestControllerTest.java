package org.openelisglobal.patient.merge.controller.rest;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.patient.dao.PatientDAO;
import org.openelisglobal.patient.merge.dto.PatientMergeRequestDTO;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Controller tests for PatientMergeRestController. TDD Phase: RED - Tests
 * written BEFORE controller implementation. Tests the REST API endpoints for
 * patient merge functionality.
 *
 * Endpoints: - GET /rest/patient/merge/details/{patientId} - POST
 * /rest/patient/merge/validate - POST /rest/patient/merge/execute
 */
@Rollback
public class PatientMergeRestControllerTest extends BaseWebContextSensitiveTest {

    @Autowired
    private PatientDAO patientDAO;

    @Autowired
    private PersonService personService;

    private ObjectMapper objectMapper;
    private Patient patient1;
    private Patient patient2;
    private MockHttpSession mockSession;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        objectMapper = new ObjectMapper();

        // Load system user dataset for audit
        executeDataSetWithStateManagement("testdata/system-user.xml");

        // Set up authenticated session with Global Admin user (system_user id=1)
        UserSessionData userSessionData = new UserSessionData();
        userSessionData.setSytemUserId(1); // Admin user from system-user.xml (note: typo in API)
        mockSession = new MockHttpSession();
        mockSession.setAttribute(IActionConstants.USER_SESSION_DATA, userSessionData);

        // Create test patients
        Person person1 = new Person();
        person1.setFirstName("John");
        person1.setLastName("Doe");
        String person1Id = personService.insert(person1);
        person1.setId(person1Id);

        Person person2 = new Person();
        person2.setFirstName("Jon");
        person2.setLastName("Doe");
        String person2Id = personService.insert(person2);
        person2.setId(person2Id);

        patient1 = new Patient();
        patient1.setPerson(person1);
        patient1.setNationalId("CTRL-TEST-P1-" + System.currentTimeMillis());
        patient1.setIsMerged(false);
        String patient1Id = patientDAO.insert(patient1);
        patient1.setId(patient1Id);

        patient2 = new Patient();
        patient2.setPerson(person2);
        patient2.setNationalId("CTRL-TEST-P2-" + System.currentTimeMillis());
        patient2.setIsMerged(false);
        String patient2Id = patientDAO.insert(patient2);
        patient2.setId(patient2Id);
    }

    // ========== GET /rest/patient/merge/details/{patientId} Tests ==========

    /**
     * Test: GET merge details for existing patient returns 200 OK. Business Rule:
     * Valid patient ID should return merge details.
     */
    @Test
    public void getMergeDetails_ValidPatientId_Returns200() throws Exception {
        MvcResult result = mockMvc
                .perform(get("/rest/patient/merge/details/" + patient1.getId()).session(mockSession)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$.patientId").exists())
                .andExpect(jsonPath("$.firstName").exists()).andExpect(jsonPath("$.lastName").exists()).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertNotNull("Response body should not be null", responseBody);
        assertTrue("Response should contain patient ID", responseBody.contains("patientId"));
    }

    /**
     * Test: GET merge details for non-existent patient returns 404. Business Rule:
     * Non-existent patient should return Not Found.
     */
    @Test
    public void getMergeDetails_NonExistentPatientId_Returns404() throws Exception {
        mockMvc.perform(
                get("/rest/patient/merge/details/999999").session(mockSession).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    // ========== POST /rest/patient/merge/validate Tests ==========

    /**
     * Test: POST validate with valid patients returns 200 OK. Business Rule: Valid
     * merge candidates should return validation result.
     */
    @Test
    public void validateMerge_ValidPatients_Returns200() throws Exception {
        PatientMergeRequestDTO request = new PatientMergeRequestDTO();
        request.setPatient1Id(patient1.getId());
        request.setPatient2Id(patient2.getId());
        request.setPrimaryPatientId(patient1.getId());
        request.setReason("Duplicate patient entry");
        request.setConfirmed(false);

        MvcResult result = mockMvc
                .perform(post("/rest/patient/merge/validate").session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.valid").exists()).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertTrue("Response should contain 'valid' field", responseBody.contains("valid"));
    }

    /**
     * Test: POST validate with same patient ID returns validation error. Business
     * Rule: Cannot merge patient with itself.
     */
    @Test
    public void validateMerge_SamePatientId_ReturnsValidationError() throws Exception {
        PatientMergeRequestDTO request = new PatientMergeRequestDTO();
        request.setPatient1Id(patient1.getId());
        request.setPatient2Id(patient1.getId()); // Same ID
        request.setPrimaryPatientId(patient1.getId());
        request.setReason("Test same patient");
        request.setConfirmed(false);

        MvcResult result = mockMvc
                .perform(post("/rest/patient/merge/validate").session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.errors").isArray()).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertTrue("Response should indicate invalid", responseBody.contains("\"valid\":false"));
    }

    /**
     * Test: POST validate with missing fields returns 400 Bad Request. Business
     * Rule: Request must have required fields.
     */
    @Test
    public void validateMerge_MissingFields_Returns400() throws Exception {
        PatientMergeRequestDTO request = new PatientMergeRequestDTO();
        // Missing patient1Id, patient2Id, etc.

        mockMvc.perform(post("/rest/patient/merge/validate").session(mockSession)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ========== POST /rest/patient/merge/execute Tests ==========

    /**
     * Test: POST execute with valid confirmed request returns 200 OK. Business
     * Rule: Confirmed merge should execute successfully.
     */
    @Test
    public void executeMerge_ValidConfirmedRequest_Returns200() throws Exception {
        PatientMergeRequestDTO request = new PatientMergeRequestDTO();
        request.setPatient1Id(patient1.getId());
        request.setPatient2Id(patient2.getId());
        request.setPrimaryPatientId(patient1.getId());
        request.setReason("Confirmed duplicate merge");
        request.setConfirmed(true);

        MvcResult result = mockMvc
                .perform(post("/rest/patient/merge/execute").session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.mergeAuditId").exists()).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertTrue("Response should indicate success", responseBody.contains("\"success\":true"));
    }

    /**
     * Test: POST execute without confirmation returns 400 Bad Request. Business
     * Rule: Merge requires explicit confirmation.
     */
    @Test
    public void executeMerge_NotConfirmed_Returns400() throws Exception {
        PatientMergeRequestDTO request = new PatientMergeRequestDTO();
        request.setPatient1Id(patient1.getId());
        request.setPatient2Id(patient2.getId());
        request.setPrimaryPatientId(patient1.getId());
        request.setReason("Unconfirmed merge");
        request.setConfirmed(false); // Not confirmed

        mockMvc.perform(post("/rest/patient/merge/execute").session(mockSession).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest());
    }

    /**
     * Test: POST execute with non-existent patient returns 404. Business Rule: Both
     * patients must exist.
     */
    @Test
    public void executeMerge_NonExistentPatient_Returns404() throws Exception {
        PatientMergeRequestDTO request = new PatientMergeRequestDTO();
        request.setPatient1Id(patient1.getId());
        request.setPatient2Id("999999"); // Non-existent
        request.setPrimaryPatientId(patient1.getId());
        request.setReason("Testing non-existent patient");
        request.setConfirmed(true);

        mockMvc.perform(post("/rest/patient/merge/execute").session(mockSession).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))).andExpect(status().isNotFound());
    }

    /**
     * Test: POST validate with already merged patient returns validation error.
     * Business Rule: Cannot merge an already merged patient.
     */
    @Test
    public void validateMerge_AlreadyMergedPatient_ReturnsValidationError() throws Exception {
        // First, merge patient2 into patient1
        PatientMergeRequestDTO firstMerge = new PatientMergeRequestDTO();
        firstMerge.setPatient1Id(patient1.getId());
        firstMerge.setPatient2Id(patient2.getId());
        firstMerge.setPrimaryPatientId(patient1.getId());
        firstMerge.setReason("First merge");
        firstMerge.setConfirmed(true);

        mockMvc.perform(post("/rest/patient/merge/execute").session(mockSession).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstMerge))).andExpect(status().isOk());

        // Now try to validate merging the already merged patient
        PatientMergeRequestDTO secondMerge = new PatientMergeRequestDTO();
        secondMerge.setPatient1Id(patient2.getId()); // This patient is already merged
        secondMerge.setPatient2Id(patient1.getId());
        secondMerge.setPrimaryPatientId(patient1.getId());
        secondMerge.setReason("Second merge attempt");
        secondMerge.setConfirmed(false);

        MvcResult result = mockMvc
                .perform(post("/rest/patient/merge/validate").session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(secondMerge)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.valid").value(false)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertTrue("Should indicate already merged", responseBody.contains("already merged"));
    }
}
