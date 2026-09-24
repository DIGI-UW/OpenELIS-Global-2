package org.openelisglobal.reports.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;

public class ReportControllerIntegrationTest extends BaseWebContextSensitiveTest {

    @Test
    public void testLegacyWhonetExportIsUnavailableViaApi() throws Exception {
        // The legacy WHONET export route was removed in OGC-782 M4 follow-up.
        // It relies on ReportImplementationFactory.getReportCreator, which will now
        // return null.
        // In ReportController, if reportCreator is null, the method simply finishes
        // without writing to the response body
        // (or redirecting), so we expect an empty response (200 OK but with no
        // content).

        mockMvc.perform(get("/Report").param("type", "patient").param("report", "ExportWHONETReportByDate"))
                .andExpect(status().isNotFound());
    }
}
