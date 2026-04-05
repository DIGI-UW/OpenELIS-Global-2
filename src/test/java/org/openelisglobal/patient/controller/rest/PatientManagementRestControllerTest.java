package org.openelisglobal.patient.controller.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.patient.service.PatientPhotoService;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patientidentity.service.PatientIdentityService;
import org.openelisglobal.search.service.SearchResultsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PatientManagementRestController.class)
public class PatientManagementRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SearchResultsService searchService;

    @MockBean
    private PatientIdentityService patientIdentityService;

    @MockBean
    private PatientService patientService;

    @MockBean
    private FhirTransformService fhirTransformService;

    @MockBean
    private PatientPhotoService photoService;

    @Test
    @WithMockUser(roles = "PATIENT_VIEW")
    public void getPhoto_withValidRole_returnsPhoto() throws Exception {
        when(photoService.getPhotoByPatientId("1", false)).thenReturn("base64data");

        mockMvc.perform(get("/rest/patient-photos/1/false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("base64data"));
    }

    @Test
    @WithAnonymousUser
    public void getPhoto_withoutAuth_returns403() throws Exception {
        mockMvc.perform(get("/rest/patient-photos/1/false"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PATIENT_VIEW")
    public void getPhoto_withNonNumericId_returns400() throws Exception {
        mockMvc.perform(get("/rest/patient-photos/abc/false"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "PATIENT_VIEW")
    public void getPhoto_whenPhotoIsNull_returns404() throws Exception {
        when(photoService.getPhotoByPatientId("999", false)).thenReturn(null);

        mockMvc.perform(get("/rest/patient-photos/999/false"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    public void savepatient_whenBindingErrors_doesNotPersist() throws Exception {
        mockMvc.perform(post("/rest/PatientManagement")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"));

        verify(patientService, never()).persistPatientData(any(), any(), any());
    }
}
