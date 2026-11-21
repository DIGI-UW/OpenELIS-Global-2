package org.openelisglobal.reports.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

public class ReportControllerTest {

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ReportController()).build();
    }

    @Test
    public void testLegacyWhonetExportIsUnavailableViaApi() throws Exception {
        // The legacy WHONET export route was removed in OGC-782 M4 follow-up.
        // ReportImplementationFactory.getReportCreator("ExportWHONETReportByDate")
        // returns null, and ReportController.printReport throws
        // ResponseStatusException(HttpStatus.NOT_FOUND), returning 404 Not Found.
        mockMvc.perform(get("/ReportPrint").param("type", "patient").param("report", "ExportWHONETReportByDate"))
                .andExpect(status().isNotFound());
    }
}
