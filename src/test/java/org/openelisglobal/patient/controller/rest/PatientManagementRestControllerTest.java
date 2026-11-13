package org.openelisglobal.patient.controller.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.services.StaleSaveGuard;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.patient.service.PatientIdDocumentService;
import org.openelisglobal.patient.service.PatientPhotoService;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.PatientIdDocument;
import org.openelisglobal.patientidentity.service.PatientIdentityService;
import org.openelisglobal.search.service.SearchResultsService;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Standalone MockMvc REST controller unit tests for
 * {@link PatientManagementRestController}.
 *
 * Pattern 1 test: No database or Spring Web Context initialization required.
 * Runs in <100ms.
 */
@RunWith(MockitoJUnitRunner.class)
public class PatientManagementRestControllerTest {

    @Mock
    private SearchResultsService searchService;

    @Mock
    private PatientIdentityService patientIdentityService;

    @Mock
    private PatientService patientService;

    @Mock
    private FhirTransformService fhirTransformService;

    @Mock
    private PatientPhotoService photoService;

    @Mock
    private PatientIdDocumentService idDocumentService;

    @Mock
    private StaleSaveGuard staleSaveGuard;

    @InjectMocks
    private PatientManagementRestController controller;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void updateIdDocument_whenDocumentExists_returns200OkWithSuccessStatus() throws Exception {
        PatientIdDocument doc = new PatientIdDocument();
        doc.setId(10);
        when(idDocumentService.updateDocument(eq(10), eq("base64data"), eq("NATIONAL_ID"), eq("National Identity Card"),
                any())).thenReturn(doc);

        String payload = "{\"data\":\"base64data\",\"category\":\"NATIONAL_ID\",\"description\":\"National Identity Card\"}";

        mockMvc.perform(
                put("/rest/patient-id-documents/10").contentType(MediaType.APPLICATION_JSON_VALUE).content(payload))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    public void updateIdDocument_whenDocumentNotFound_returns404NotFoundWithNotFoundStatus() throws Exception {
        when(idDocumentService.updateDocument(eq(999), any(), any(), any(), any())).thenReturn(null);

        String payload = "{\"data\":\"base64data\",\"category\":\"NATIONAL_ID\",\"description\":\"Non-existent\"}";

        mockMvc.perform(put("/rest/patient-id-documents/999").contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(payload)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("not_found"));
    }

    @Test
    public void getIdDocuments_whenDocumentsExist_returns200OkWithDocumentList() throws Exception {
        PatientIdDocument doc = new PatientIdDocument();
        doc.setId(1);
        doc.setDocumentType("image/jpeg");
        doc.setThumbnailData("thumbBase64");
        doc.setDocumentCategory("PASSPORT");
        doc.setDescription("Passport scan");

        when(idDocumentService.getDocumentsByPatientId("100")).thenReturn(List.of(doc));

        mockMvc.perform(get("/rest/patient-id-documents/100")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].category").value("PASSPORT"))
                .andExpect(jsonPath("$[0].description").value("Passport scan"))
                .andExpect(jsonPath("$[0].thumbnail").value("data:image/jpeg;base64,thumbBase64"));
    }

    @Test
    public void deleteIdDocument_deletesDocumentAndReturns200OkWithSuccessStatus() throws Exception {
        mockMvc.perform(delete("/rest/patient-id-documents/5")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(idDocumentService).softDeleteDocument(eq(5), any());
    }
}
