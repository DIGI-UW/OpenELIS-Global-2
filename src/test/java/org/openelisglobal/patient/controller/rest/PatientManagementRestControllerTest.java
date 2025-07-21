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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

public class PatientManagementRestControllerTest extends BaseWebContextSensitiveTest {

    @Autowired
    private PatientDAO patientDAO;

    private ObjectMapper objectMapper = new ObjectMapper();

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
}
