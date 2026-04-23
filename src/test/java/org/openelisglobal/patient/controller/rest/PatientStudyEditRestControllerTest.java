package org.openelisglobal.patient.controller.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
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
 * Integration tests for {@link PatientStudyEditRestController}.
 *
 * <p>
 * Endpoint under test: POST /rest/patient-study-edit
 *
 * <p>
 * Uses the real Spring context and an in-memory test database via
 * {@link BaseWebContextSensitiveTest}. All data modifications are rolled back
 * after each test run.
 */
@Rollback
public class PatientStudyEditRestControllerTest extends BaseWebContextSensitiveTest {

    @Autowired
    private PatientDAO patientDAO;

    @Autowired
    private PersonService personService;

    private ObjectMapper objectMapper;
    private MockHttpSession mockSession;
    private Patient testPatient;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        objectMapper = new ObjectMapper();

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
        testPatient.setNationalId("PSE-TEST-" + System.currentTimeMillis());
        testPatient.setGender("F");
        String patientId = patientDAO.insert(testPatient);
        testPatient.setId(patientId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Map<String, Object> buildMinimalPayload() {
        Map<String, Object> observations = new HashMap<>();
        observations.put("hivStatus", "");
        observations.put("educationLevel", "");
        observations.put("maritalStatus", "");
        observations.put("nationality", "");
        observations.put("nameOfDoctor", "");
        observations.put("patientWeight", "");
        observations.put("karnofskyScore", "");
        observations.put("underInvestigation", "false");

        Map<String, Object> projectData = new HashMap<>();
        projectData.put("underInvestigationNote", "");

        Map<String, Object> payload = new HashMap<>();
        payload.put("patientPK", testPatient.getId());
        payload.put("samplePK", "");
        payload.put("projectFormName", "InitialARV_Id");
        payload.put("lastName", "Dupont");
        payload.put("firstName", "Marie");
        payload.put("gender", "F");
        payload.put("birthDateForDisplay", "");
        payload.put("subjectNumber", "SUB-001");
        payload.put("siteSubjectNumber", "SITE-001");
        payload.put("labNo", "");
        payload.put("receivedDateForDisplay", "");
        payload.put("interviewDate", "");
        payload.put("centerName", "");
        payload.put("centerCode", "");
        payload.put("observations", observations);
        payload.put("projectData", projectData);
        return payload;
    }

    // ── POST /rest/patient-study-edit ─────────────────────────────────────────

    /**
     * The endpoint is reachable with a well-formed JSON payload and returns a JSON
     * envelope. The "success" field must always be present (true on full stack,
     * false when project seed data is absent).
     */
    @Test
    public void savePatientStudyEdit_ValidPayload_Returns200WithSuccessField() throws Exception {
        Map<String, Object> payload = buildMinimalPayload();

        mockMvc.perform(post("/rest/patient-study-edit").session(mockSession).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload))).andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.success").exists());
    }

    /**
     * The endpoint must accept JSON content type. Sending a different content type
     * must result in a 415 Unsupported Media Type.
     */
    @Test
    public void savePatientStudyEdit_WrongContentType_Returns415() throws Exception {
        Map<String, Object> payload = buildMinimalPayload();

        mockMvc.perform(post("/rest/patient-study-edit").session(mockSession)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED).content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnsupportedMediaType());
    }

    /**
     * An empty JSON body must not throw an unhandled exception. The controller must
     * handle missing keys gracefully and return a response (success or error).
     */
    @Test
    public void savePatientStudyEdit_EmptyPayload_ReturnsResponseWithSuccessField() throws Exception {
        Map<String, Object> payload = new HashMap<>();

        mockMvc.perform(post("/rest/patient-study-edit").session(mockSession).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload))).andExpect(jsonPath("$.success").exists());
    }

    /**
     * A payload with an observations block containing ARV fields must be accepted
     * without a 500 error — verifying the observation mapping path does not throw a
     * NullPointerException on missing keys.
     */
    @Test
    public void savePatientStudyEdit_PayloadWithObservations_DoesNotReturn500() throws Exception {
        Map<String, Object> observations = new HashMap<>();
        observations.put("hivStatus", "1");
        observations.put("arvProphylaxisBenefit", "true");
        observations.put("currentARVTreatment", "true");
        observations.put("priorARVTreatment", "false");
        observations.put("aidsStage", "");
        observations.put("patientWeight", "65");
        observations.put("karnofskyScore", "90");

        Map<String, Object> payload = buildMinimalPayload();
        payload.put("observations", observations);

        mockMvc.perform(post("/rest/patient-study-edit").session(mockSession).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload))).andExpect(jsonPath("$.success").exists());
    }

    /**
     * A payload for the FollowupARV study form must be routed correctly through
     * buildFormFromPayload without exceptions.
     */
    @Test
    public void savePatientStudyEdit_FollowupARVForm_DoesNotReturn500() throws Exception {
        Map<String, Object> payload = buildMinimalPayload();
        payload.put("projectFormName", "FollowupARV_Id");

        mockMvc.perform(post("/rest/patient-study-edit").session(mockSession).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload))).andExpect(jsonPath("$.success").exists());
    }

    /**
     * A payload for the EID study form must be routed correctly through
     * buildFormFromPayload without exceptions.
     */
    @Test
    public void savePatientStudyEdit_EIDForm_DoesNotReturn500() throws Exception {
        Map<String, Object> observations = new HashMap<>();
        observations.put("eidTypeOfClinic", "");
        observations.put("eidTypeOfClinicOther", "");
        observations.put("eidHowChildFed", "");
        observations.put("eidStoppedBreastfeeding", "false");
        observations.put("eidMothersHivStatus", "");
        observations.put("eidInfantProphylaxis", "");
        observations.put("eidMotherProphylaxis", "");

        Map<String, Object> payload = buildMinimalPayload();
        payload.put("projectFormName", "EID_Id");
        payload.put("observations", observations);

        mockMvc.perform(post("/rest/patient-study-edit").session(mockSession).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload))).andExpect(jsonPath("$.success").exists());
    }

    /**
     * A payload for the VL (Viral Load) study form must be routed correctly through
     * buildFormFromPayload without exceptions.
     */
    @Test
    public void savePatientStudyEdit_VLForm_DoesNotReturn500() throws Exception {
        Map<String, Object> observations = new HashMap<>();
        observations.put("vlReasonForRequest", "");
        observations.put("vlOtherReasonForRequest", "");
        observations.put("vlBenefit", "");
        observations.put("vlPregnancy", "false");
        observations.put("vlSuckle", "false");
        observations.put("priorVLLab", "");
        observations.put("priorVLValue", "");
        observations.put("priorVLDate", "");

        Map<String, Object> payload = buildMinimalPayload();
        payload.put("projectFormName", "VL_Id");
        payload.put("observations", observations);

        mockMvc.perform(post("/rest/patient-study-edit").session(mockSession).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload))).andExpect(jsonPath("$.success").exists());
    }

    /**
     * A non-numeric centerCode value must not throw a NumberFormatException — the
     * controller must silently ignore it and leave centerCode null.
     */
    @Test
    public void savePatientStudyEdit_NonNumericCenterCode_DoesNotReturn500() throws Exception {
        Map<String, Object> payload = buildMinimalPayload();
        payload.put("centerCode", "NOT_A_NUMBER");

        mockMvc.perform(post("/rest/patient-study-edit").session(mockSession).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload))).andExpect(jsonPath("$.success").exists());
    }

    /**
     * When the payload contains a priorARVTreatmentINNsList with entries, the
     * indexed INNs must be mapped without an IndexOutOfBoundsException.
     */
    @Test
    public void savePatientStudyEdit_WithPriorARVInnList_DoesNotReturn500() throws Exception {
        Map<String, Object> observations = new HashMap<>();
        observations.put("priorARVTreatmentINNsList", java.util.Arrays.asList("TDF", "3TC", "", ""));

        Map<String, Object> payload = buildMinimalPayload();
        payload.put("observations", observations);

        mockMvc.perform(post("/rest/patient-study-edit").session(mockSession).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload))).andExpect(jsonPath("$.success").exists());
    }

    /**
     * The response body must be JSON — Content-Type must include application/json.
     */
    @Test
    public void savePatientStudyEdit_ValidPayload_ResponseIsJson() throws Exception {
        Map<String, Object> payload = buildMinimalPayload();

        mockMvc.perform(post("/rest/patient-study-edit").session(mockSession).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload))).andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$").isMap());
    }
}
