package org.openelisglobal.microbiology.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.microbiology.controller.rest.MicroWhonetExportRestController;
import org.openelisglobal.microbiology.form.MicroWhonetExportQueryForm;
import org.openelisglobal.microbiology.form.MicroWhonetFilterOptionsForm;
import org.openelisglobal.microbiology.form.MicroWhonetPreviewForm;
import org.openelisglobal.reports.service.MicroWhonetExportResult;
import org.openelisglobal.reports.service.WHONetReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

public class MicroWhonetExportRestControllerTest {

    @Test
    public void previewDelegatesCanonicalQuery() {
        WHONetReportService service = org.mockito.Mockito.mock(WHONetReportService.class);
        MicroWhonetExportQueryForm query = query();
        MicroWhonetPreviewForm preview = new MicroWhonetPreviewForm();
        preview.canGenerate = true;
        when(service.previewMicrobiologyExport(query)).thenReturn(preview);

        ResponseEntity<MicroWhonetPreviewForm> response = new MicroWhonetExportRestController(service).preview(query);

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().canGenerate);
        verify(service).previewMicrobiologyExport(query);
    }

    @Test
    public void filterOptionsDelegateOnlyTheReportingPeriod() {
        WHONetReportService service = org.mockito.Mockito.mock(WHONetReportService.class);
        MicroWhonetExportQueryForm query = query();
        MicroWhonetFilterOptionsForm options = new MicroWhonetFilterOptionsForm();
        when(service.getMicrobiologyExportFilterOptions(query)).thenReturn(options);

        ResponseEntity<MicroWhonetFilterOptionsForm> response = new MicroWhonetExportRestController(service)
                .filterOptions(query);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(options, response.getBody());
        verify(service).getMicrobiologyExportFilterOptions(query);
    }

    @Test
    public void generationUsesAuthenticatedActorAndReturnsAttachmentMetadata() {
        WHONetReportService service = org.mockito.Mockito.mock(WHONetReportService.class);
        MicroWhonetExportQueryForm query = query();
        MicroWhonetExportResult result = new MicroWhonetExportResult("WHONET_period.csv",
                "csv-content".getBytes(StandardCharsets.UTF_8));
        when(service.generateMicrobiologyExport(query, "42")).thenReturn(result);

        ResponseEntity<byte[]> response = new MicroWhonetExportRestController(service).generate(query,
                requestFor("42"));

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("attachment; filename=\"WHONET_period.csv\"",
                response.getHeaders().getFirst("Content-Disposition"));
        assertEquals("csv-content", new String(response.getBody(), StandardCharsets.UTF_8));
        verify(service).generateMicrobiologyExport(query, "42");
    }

    @Test
    public void previewBindsCanonicalQueryAndReturnsStructuredInvalidRequest() throws Exception {
        WHONetReportService service = org.mockito.Mockito.mock(WHONetReportService.class);
        when(service.previewMicrobiologyExport(any(MicroWhonetExportQueryForm.class)))
                .thenThrow(new IllegalArgumentException("to must be on or after from"));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new MicroWhonetExportRestController(service)).build();

        mvc.perform(get("/rest/microbiology/whonet/preview").param("from", "2026-07-31").param("to", "2026-07-01")
                .param("specimen", "sample-type-1", "sample-type-2").param("organism", "organism-1")
                .param("origin", "INPATIENT").param("significance", "CLINICALLY_SIGNIFICANT", "NORMAL_FLORA")
                .param("dedup", "NONE").param("page", "2").param("pageSize", "50")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MICROBIOLOGY_REFERENCE_INVALID"))
                .andExpect(jsonPath("$.message").value("to must be on or after from"));

        ArgumentCaptor<MicroWhonetExportQueryForm> query = ArgumentCaptor.forClass(MicroWhonetExportQueryForm.class);
        verify(service).previewMicrobiologyExport(query.capture());
        assertEquals("2026-07-31", query.getValue().from);
        assertEquals("2026-07-01", query.getValue().to);
        assertEquals(List.of("sample-type-1", "sample-type-2"), query.getValue().specimen);
        assertEquals(List.of("organism-1"), query.getValue().organism);
        assertEquals(List.of("INPATIENT"), query.getValue().origin);
        assertEquals(List.of("CLINICALLY_SIGNIFICANT", "NORMAL_FLORA"), query.getValue().significance);
        assertEquals("NONE", query.getValue().dedup);
        assertEquals(2, query.getValue().page);
        assertEquals(50, query.getValue().pageSize);
    }

    private MicroWhonetExportQueryForm query() {
        MicroWhonetExportQueryForm query = new MicroWhonetExportQueryForm();
        query.from = "2026-07-01";
        query.to = "2026-07-31";
        return query;
    }

    private MockHttpServletRequest requestFor(String userId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        UserSessionData sessionData = new UserSessionData();
        sessionData.setSytemUserId(Integer.parseInt(userId));
        request.getSession().setAttribute(IActionConstants.USER_SESSION_DATA, sessionData);
        return request;
    }
}
