package org.openelisglobal.patient.controller.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.patient.dao.PatientDAO;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.Rollback;

/**
 * Integration tests for {@link PatientStudyViewRestController}.
 *
 * <p>
 * Endpoint under test: GET /rest/patient-study-view
 *
 * <p>
 * Uses the real Spring context and an in-memory test database via
 * {@link BaseWebContextSensitiveTest}. All data modifications are rolled back
 * after each test run.
 */
@Rollback
public class PatientStudyViewRestControllerTest extends BaseWebContextSensitiveTest {

    @Autowired
    private PatientDAO patientDAO;

    @Autowired
    private PersonService personService;

    private MockHttpSession mockSession;
    private Patient testPatient;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        executeDataSetWithStateManagement("testdata/system-user.xml");

        UserSessionData sessionData = new UserSessionData();
        sessionData.setSytemUserId(1);
        mockSession = new MockHttpSession();
        mockSession.setAttribute(IActionConstants.USER_SESSION_DATA, sessionData);

        Person person = new Person();
        person.setFirstName("Marie");
        person.setLastName("Dupont");
        String personId = personService.insert(person);
        person.setId(personId);

        testPatient = new Patient();
        testPatient.setPerson(person);
        testPatient.setNationalId("PSV-TEST-" + System.currentTimeMillis());
        testPatient.setGender("F");
        String patientId = patientDAO.insert(testPatient);
        testPatient.setId(patientId);
    }

    // ── GET /rest/patient-study-view (no patientID) ───────────────────────────

    /**
     * When no patientID is supplied the endpoint must return 200 with the reference
     * lists and an empty formData block — allowing the UI to render dropdowns
     * before any patient is selected.
     */
    @Test
    public void getPatientStudyView_NoPatientId_Returns200WithReferenceLists() throws Exception {
        mockMvc.perform(get("/rest/patient-study-view").session(mockSession).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$.referenceLists").exists())
                .andExpect(jsonPath("$.formData").exists());
    }

    /**
     * The reference lists payload must contain the key dropdown lists that the
     * React form uses to populate its select fields.
     */
    @Test
    public void getPatientStudyView_NoPatientId_ReferenceListsContainExpectedKeys() throws Exception {
        mockMvc.perform(get("/rest/patient-study-view").session(mockSession).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$.referenceLists.hivStatuses").exists())
                .andExpect(jsonPath("$.referenceLists.yesNo").exists())
                .andExpect(jsonPath("$.referenceLists.educationLevels").exists())
                .andExpect(jsonPath("$.referenceLists.maritalStatuses").exists())
                .andExpect(jsonPath("$.referenceLists.nationalities").exists());
    }

    // ── GET /rest/patient-study-view?patientID={id} ───────────────────────────

    /**
     * A valid patientID must return 200 with formData populated from the patient
     * record.
     */
    @Test
    public void getPatientStudyView_ValidPatientId_Returns200WithFormData() throws Exception {
        mockMvc.perform(get("/rest/patient-study-view").param("patientID", testPatient.getId()).session(mockSession)
                .accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
                .andExpect(jsonPath("$.formData").exists()).andExpect(jsonPath("$.referenceLists").exists());
    }

    /**
     * The formData block must reflect the patient demographics stored in the DB.
     */
    @Test
    public void getPatientStudyView_ValidPatientId_FormDataContainsPatientDemographics() throws Exception {
        mockMvc.perform(get("/rest/patient-study-view").param("patientID", testPatient.getId()).session(mockSession)
                .accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
                .andExpect(jsonPath("$.formData.lastName").value("Dupont"))
                .andExpect(jsonPath("$.formData.firstName").value("Marie"))
                .andExpect(jsonPath("$.formData.gender").value("F"));
    }

    /**
     * The formData block must include the patientPK so the React form can use it
     * for subsequent requests.
     */
    @Test
    public void getPatientStudyView_ValidPatientId_FormDataContainsPatientPK() throws Exception {
        mockMvc.perform(get("/rest/patient-study-view").param("patientID", testPatient.getId()).session(mockSession)
                .accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
                .andExpect(jsonPath("$.formData.patientPK").value(testPatient.getId()));
    }

    /**
     * A non-existent patientID must still return 200 (not 404) with an empty
     * formData and an error hint — matching the controller's defensive design that
     * logs a warning and returns gracefully.
     */
    @Test
    public void getPatientStudyView_NonExistentPatientId_Returns200WithError() throws Exception {
        mockMvc.perform(get("/rest/patient-study-view").param("patientID", "999999999").session(mockSession)
                .accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.formData").exists());
    }

    /**
     * A blank patientID string must be treated the same as an absent parameter —
     * returning reference lists with an empty formData block.
     */
    @Test
    public void getPatientStudyView_BlankPatientId_Returns200WithEmptyFormData() throws Exception {
        mockMvc.perform(get("/rest/patient-study-view").param("patientID", "").session(mockSession)
                .accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
                .andExpect(jsonPath("$.referenceLists").exists()).andExpect(jsonPath("$.formData").exists());
    }

    /**
     * The endpoint must be reachable without a Content-Type header (it is a GET
     * request) and must still return JSON.
     */
    @Test
    public void getPatientStudyView_NoContentTypeHeader_ReturnsJson() throws Exception {
        mockMvc.perform(get("/rest/patient-study-view").param("patientID", testPatient.getId()).session(mockSession))
                .andExpect(status().isOk()).andExpect(jsonPath("$.formData").exists());
    }

    /**
     * The availableStudyTypes list must be present in the response so the React
     * study-type selector knows which tabs to show.
     */
    @Test
    public void getPatientStudyView_ValidPatientId_ResponseContainsAvailableStudyForms() throws Exception {
        mockMvc.perform(get("/rest/patient-study-view").param("patientID", testPatient.getId()).session(mockSession)
                .accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
                .andExpect(jsonPath("$.formData.availableStudyTypes").exists());
    }
}
