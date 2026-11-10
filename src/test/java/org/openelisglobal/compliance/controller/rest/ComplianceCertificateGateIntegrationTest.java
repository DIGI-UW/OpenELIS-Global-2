package org.openelisglobal.compliance.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * OGC-1620: the report is generated only when every test of the order that was
 * not cancelled is validated and released (OGC-552). OGC-1617: an order with a
 * standard prints the certificate, one without prints a results report. The
 * order is sample 9101 of compliance-dashboard.xml: five analyses under a
 * linked standard.
 */
public class ComplianceCertificateGateIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private IStatusService statusService;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/compliance-dashboard.xml");
        jdbcTemplate.update("UPDATE clinlims.analysis SET revision = 0 WHERE id IN (9101, 9102, 9103, 9104, 9109)");
        jdbcTemplate.update("DELETE FROM clinlims.compliance_report_generation WHERE sample_id = 9101");
        statusService.refreshCache();
    }

    @Test
    public void aCertificateIsRefusedWhileATestAwaitsValidation() throws Exception {
        setStatus("9101, 9102, 9103, 9104", AnalysisStatus.Finalized);
        setStatus("9109", AnalysisStatus.TechnicalAcceptance);

        MvcResult refused = exportPdf();

        assertEquals(409, refused.getResponse().getStatus());
        assertEquals("Every test must be validated before the report is generated",
                refused.getResponse().getErrorMessage());
    }

    @Test
    public void aCertificateIsGeneratedOnceEveryTestIsValidatedOrCancelled() throws Exception {
        setStatus("9101, 9102, 9103, 9104", AnalysisStatus.Finalized);
        setStatus("9109", AnalysisStatus.Canceled);

        MvcResult generated = exportPdf();

        assertEquals(200, generated.getResponse().getStatus());
        assertEquals("application/pdf", generated.getResponse().getContentType().split(";")[0]);
    }

    @Test
    public void anOrderWithoutAStandardPrintsAResultsReport() throws Exception {
        setStatus("9101, 9102, 9103, 9104, 9109", AnalysisStatus.Finalized);
        jdbcTemplate.update("DELETE FROM clinlims.sample_compliance_standards WHERE sample_id = 9101");

        MvcResult generated = exportPdf();

        assertEquals(200, generated.getResponse().getStatus());
        String text = pdfText(generated);
        assertTrue(text, text.contains("TEST RESULTS REPORT"));
        assertTrue(text, text.contains("not a statement of conformity"));
        assertTrue("every validated result is listed", text.contains("7.6"));
        assertFalse(text, text.contains("COMPLIANCE SUMMARY"));
        assertFalse(text, text.contains("Conclusion"));
    }

    @Test
    public void aTestRemovedAndAddedBackHoldsTheCertificateUntilValidated() throws Exception {
        setStatus("9101, 9102, 9103, 9104", AnalysisStatus.Finalized);
        setStatus("9109", AnalysisStatus.NotStarted);
        jdbcTemplate.update(
                "INSERT INTO clinlims.analysis (id, sampitem_id, test_id, analysis_type, revision,"
                        + " status_id) VALUES (9120, 9101, 9102, 'MANUAL', 1, CAST(? AS numeric))",
                statusService.getStatusID(AnalysisStatus.Canceled));

        MvcResult refused = exportPdf();

        assertEquals("the removed copy is cancelled at revision 1; the one added back has no result", 409,
                refused.getResponse().getStatus());
    }

    @Test
    public void anOrderWithAStandardPrintsTheCertificateWithEveryResult() throws Exception {
        setStatus("9101, 9102, 9103, 9104, 9109", AnalysisStatus.Finalized);

        String text = pdfText(exportPdf());

        assertTrue(text, text.contains("CERTIFICATE OF TEST RESULTS"));
        assertTrue(text, text.contains("COMPLIANCE SUMMARY"));
        assertTrue(text, text.contains("Conclusion"));
        assertTrue("four pH results have a limit: " + text, text.contains("for the 4 parameters assessed"));
        assertTrue(text, text.contains("Decision rule"));
    }

    private static String pdfText(MvcResult result) throws Exception {
        PdfReader reader = new PdfReader(result.getResponse().getContentAsByteArray());
        PdfTextExtractor extractor = new PdfTextExtractor(reader);
        StringBuilder text = new StringBuilder();
        for (int page = 1; page <= reader.getNumberOfPages(); page++) {
            text.append(extractor.getTextFromPage(page)).append('\n');
        }
        reader.close();
        return text.toString().replaceAll("\\s+", " ");
    }

    private MvcResult exportPdf() throws Exception {
        return mockMvc.perform(post("/rest/complianceReport/exportPdf").contentType(MediaType.APPLICATION_JSON)
                .content("{\"sampleId\":9101}")).andReturn();
    }

    private void setStatus(String analysisIds, AnalysisStatus status) {
        jdbcTemplate.update(
                "UPDATE clinlims.analysis SET status_id = CAST(? AS numeric) WHERE id IN (" + analysisIds + ")",
                statusService.getStatusID(status));
    }
}
