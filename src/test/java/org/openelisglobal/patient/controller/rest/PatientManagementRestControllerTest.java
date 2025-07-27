package org.openelisglobal.patient.controller.rest;

import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.patient.dao.PatientDAO;
import org.openelisglobal.patient.valueholder.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@Rollback
public class PatientManagementRestControllerTest extends BaseWebContextSensitiveTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private PatientDAO patientDAO;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/patient_management_rest_test.xml");
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    public void savePatient_shouldReturn200EvenWithEmptyRequiredFields() throws Exception {
        Patient existingPatient = patientDAO.getPatientByNationalId("999999");
        assertNotNull("Test requires patient with nationalId 999999 to exist", existingPatient);

        Map<String, Object> invalidPayload = new HashMap<>();
        invalidPayload.put("patientPK", existingPatient.getId());
        invalidPayload.put("firstName", "");
        invalidPayload.put("lastName", "");
        invalidPayload.put("gender", "");

        Map<String, Object> patientContact = new HashMap<>();
        Map<String, Object> person = new HashMap<>();
        person.put("firstName", "");
        person.put("lastName", "");
        person.put("email", "");
        person.put("primaryPhone", "");
        patientContact.put("person", person);
        invalidPayload.put("patientContact", patientContact);

        String json = objectMapper.writeValueAsString(invalidPayload);

        mockMvc.perform(post("/rest/PatientManagement").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk());
    }

    @Test
    public void savePatient_shouldReturn200WithValidData() throws Exception {
        Patient existingPatient = patientDAO.getPatientByNationalId("999999");
        assertNotNull("Test requires patient with nationalId 999999 to exist", existingPatient);

        Map<String, Object> payload = new HashMap<>();
        payload.put("patientPK", existingPatient.getId());
        payload.put("firstName", "Test");
        payload.put("lastName", "Patient");
        payload.put("gender", "M");
        payload.put("dob", "2000-01-01");

        Map<String, Object> patientContact = new HashMap<>();
        Map<String, Object> person = new HashMap<>();
        person.put("firstName", "Contact");
        person.put("lastName", "Person");
        person.put("email", "contact@test.com");
        person.put("primaryPhone", "1234567890");
        patientContact.put("person", person);
        patientContact.put("sysUserId", "1");
        payload.put("patientContact", patientContact);

        String json = objectMapper.writeValueAsString(payload);

        mockMvc.perform(post("/rest/PatientManagement").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk());
    }
}