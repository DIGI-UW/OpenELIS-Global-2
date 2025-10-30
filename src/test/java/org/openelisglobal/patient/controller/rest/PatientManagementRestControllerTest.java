package org.openelisglobal.patient.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.patient.dao.PatientDAO;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.person.dao.PersonDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@Rollback
public class PatientManagementRestControllerTest extends BaseWebContextSensitiveTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private PatientDAO patientDAO;

    @Autowired
    private org.openelisglobal.patient.dao.PatientContactDAO patientContactDAO;

    @Autowired
    private PersonDAO personDAO;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/patient_management_rest_test.xml");

        Patient testPatient = patientDAO.getPatientByNationalId("999999");
        assertNotNull("Test data not loaded - patient with nationalId 999999", testPatient);
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    public void savePatient_shouldReturn200EvenWithEmptyRequiredFields() throws Exception {
        Patient patientFromDb = patientDAO.getPatientByNationalId("999999");
        assertNotNull("Test data not loaded - patient with nationalId 999999", patientFromDb);

        Map<String, Object> invalidPayload = new HashMap<>();
        invalidPayload.put("patientPK", patientFromDb.getId());
        invalidPayload.put("gender", "");

        Map<String, Object> patientContact = new HashMap<>();
        Map<String, Object> personMap = new HashMap<>();
        personMap.put("firstName", "");
        personMap.put("lastName", "");
        personMap.put("email", "");
        personMap.put("primaryPhone", "");
        patientContact.put("person", personMap);
        invalidPayload.put("patientContact", patientContact);

        String json = objectMapper.writeValueAsString(invalidPayload);

        MvcResult result = mockMvc
                .perform(post("/rest/PatientManagement").contentType(MediaType.APPLICATION_JSON).content(json))
                .andReturn();
        assertEquals(200, result.getResponse().getStatus());
    }

    @Test
    public void savePatient_shouldReturn200WithValidData() throws Exception {
        Patient patientFromDb = patientDAO.getPatientByNationalId("999999");
        assertNotNull("Test data not loaded - patient with nationalId 999999", patientFromDb);

        org.openelisglobal.patient.valueholder.PatientContact contact = patientContactDAO.get("2000").orElse(null);
        assertNotNull("PatientContact (id=2000) must be present in test dataset", contact);
        assertEquals("1000", contact.getPatientId());
        assertNotNull("Contact person must be present", contact.getPerson());
        assertEquals("1000", contact.getPerson().getId());

        Map<String, Object> payload = new HashMap<>();
        payload.put("patientPK", patientFromDb.getId());
        Map<String, Object> patientContact = new HashMap<>();
        patientContact.put("id", contact.getId());
        Map<String, Object> personMap = new HashMap<>();
        personMap.put("id", contact.getPerson().getId());
        patientContact.put("person", personMap);
        payload.put("patientContact", patientContact);

        String json = objectMapper.writeValueAsString(payload);

        MvcResult result = mockMvc
                .perform(post("/rest/PatientManagement").contentType(MediaType.APPLICATION_JSON).content(json))
                .andReturn();
        assertEquals(200, result.getResponse().getStatus());
    }

    @Test
    public void savePatient_shouldReturnOkEvenWithMissingGender() throws Exception {
        Patient patientFromDb = patientDAO.getPatientByNationalId("999999");
        assertNotNull("Test data not loaded - patient with nationalId 999999", patientFromDb);
        Map<String, Object> payload = new HashMap<>();
        payload.put("patientPK", patientFromDb.getId());
        Map<String, Object> patientContact = new HashMap<>();
        Map<String, Object> personMap = new HashMap<>();
        personMap.put("id", "1000");
        patientContact.put("person", personMap);
        payload.put("patientContact", patientContact);
        String json = objectMapper.writeValueAsString(payload);
        MvcResult result = mockMvc
                .perform(post("/rest/PatientManagement").contentType(MediaType.APPLICATION_JSON).content(json))
                .andReturn();
        assertEquals(200, result.getResponse().getStatus());
    }

    @Test
    public void updatePatientWithMismatchedPersonId_shouldReturnOk() throws Exception {
        Patient patientFromDb = patientDAO.getPatientByNationalId("999999");
        assertNotNull(patientFromDb);
        Map<String, Object> payload = new HashMap<>();
        payload.put("patientPK", patientFromDb.getId());
        Map<String, Object> patientContact = new HashMap<>();
        Map<String, Object> personMap = new HashMap<>();
        personMap.put("id", "3000");
        patientContact.put("person", personMap);
        payload.put("patientContact", patientContact);
        String json = objectMapper.writeValueAsString(payload);
        MvcResult result = mockMvc
                .perform(post("/rest/PatientManagement").contentType(MediaType.APPLICATION_JSON).content(json))
                .andReturn();
        assertEquals(200, result.getResponse().getStatus());
    }
}