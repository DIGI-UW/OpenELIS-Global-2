package org.openelisglobal.reports.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;

public class ReportControllerTest extends BaseWebContextSensitiveTest {

    @Test
    public void testLegacyWhonetExportIsUnavailableViaApi() throws Exception {
        mockMvc.perform(get("/ReportPrint").param("type", "patient").param("report", "ExportWHONETReportByDate"))
                .andExpect(status().isNotFound());
    }
}
