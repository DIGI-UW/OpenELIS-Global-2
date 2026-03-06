package org.openelisglobal.patient.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.Before;
import org.junit.Test;

public class PatientManagementControllerTest extends PatientControllerTestBase {

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void testShowPatientManagement() throws Exception {
        mockMvc.perform(get("/PatientManagement"))
                .andExpect(status().isOk())
                .andExpect(view().name("/pages/common/formTemplate.jsp"))
                .andExpect(model().attributeExists("form"));
    }

    @Test
    public void testShowPatientManagementUpdate_SuccessAdd() throws Exception {
        mockMvc.perform(post("/PatientManagement")
                .param("patientProperties.firstName", "Test")
                .param("patientProperties.lastName", "Patient")
                .param("patientProperties.gender", "M")
                .param("patientProperties.birthDateForDisplay", "01/01/2000")
                .param("patientProperties.patientUpdateStatus", "ADD")
                .param("patientProperties.patientContact.person.firstName", "Contact")
                .param("patientProperties.patientContact.person.lastName", "NextOfKin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/PatientManagement"))
                .andExpect(flash().attributeExists("success"));
    }

    @Test
    public void testShowPatientManagementUpdate_ValidationError() throws Exception {
        mockMvc.perform(post("/PatientManagement")
                .param("patientProperties.firstName", "")
                .param("patientProperties.patientContact.person.firstName", "Contact")
                .param("patientProperties.patientContact.person.lastName", "NextOfKin"))
                .andExpect(status().isBadRequest());
    }
}
