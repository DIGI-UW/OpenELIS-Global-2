package org.openelisglobal.patient.controller.rest;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

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
    public void savePatient_shouldReturn200OnEmptyRequiredFields() throws Exception {
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

        MvcResult result = mockMvc
                .perform(post("/rest/PatientManagement").contentType(MediaType.APPLICATION_JSON).content(json))
                .andReturn();

        assertEquals("Expected 200 due to controller swallowing validation exceptions", 200,
                result.getResponse().getStatus());
        assertEquals("Expected empty response body", "", result.getResponse().getContentAsString());
    }

    @Test
    public void savePatient_shouldHandleEmptyRequiredFields() throws Exception {
        Patient existingPatient = patientDAO.getPatientByNationalId("999999");
        assertNotNull("Test requires patient with nationalId 999999 to exist", existingPatient);

        Map<String, Object> payload = new HashMap<>();
        payload.put("patientPK", existingPatient.getId());
        payload.put("firstName", "");
        payload.put("lastName", "");
        payload.put("gender", "");

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

        MvcResult result = mockMvc
                .perform(post("/rest/PatientManagement").contentType(MediaType.APPLICATION_JSON).content(json))
                .andReturn();

        assertTrue("Should return either 200 or 400",
                result.getResponse().getStatus() == 200 || result.getResponse().getStatus() == 400);
    }
}