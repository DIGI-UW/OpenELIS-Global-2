package org.openelisglobal.patient.controller;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MvcResult;

@Rollback
public class PatientManagementControllerTest extends BaseWebContextSensitiveTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/patient-management.xml");
    }

    @Test
    public void showPatientManagement_shouldReturnPageSuccessfully() throws Exception {
        MvcResult result = super.mockMvc
                .perform(get("/PatientManagement").accept(MediaType.TEXT_HTML_VALUE))
                .andReturn();

        int status = result.getResponse().getStatus();
        assertEquals(200, status);
    }

    @Test
    public void showPatientManagementUpdate_shouldAddNewPatient() throws Exception {
        MvcResult result = super.mockMvc.perform(post("/PatientManagement")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("patientProperties.firstName", "TestInteg")
                .param("patientProperties.lastName", "PatientNew")
                .param("patientProperties.gender", "M")
                .param("patientProperties.birthDateForDisplay", "01/01/2000")
                .param("patientProperties.patientUpdateStatus", "ADD")
                .param("patientProperties.patientContact.person.firstName", "ContactFirst")
                .param("patientProperties.patientContact.person.lastName", "ContactLast"))
                .andReturn();

        int status = result.getResponse().getStatus();
        // Successful add redirects (302) back to /PatientManagement
        assertEquals(302, status);

        String redirectUrl = result.getResponse().getRedirectedUrl();
        assertEquals("/PatientManagement", redirectUrl);
    }

    @Test
    public void showPatientManagementUpdate_shouldReturnBadRequestForInvalidData() throws Exception {
        MvcResult result = super.mockMvc.perform(post("/PatientManagement")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("patientProperties.firstName", "")
                .param("patientProperties.patientContact.person.firstName", "Contact")
                .param("patientProperties.patientContact.person.lastName", "NextOfKin"))
                .andReturn();

        int status = result.getResponse().getStatus();
        assertEquals(400, status);
    }
}
