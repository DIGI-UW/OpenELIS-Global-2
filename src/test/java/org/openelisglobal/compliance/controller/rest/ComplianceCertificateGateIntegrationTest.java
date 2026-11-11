package org.openelisglobal.compliance.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.After;
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
        jdbcTemplate.update("DELETE FROM clinlims.compliance_report_archive WHERE sample_id = 9101");
        jdbcTemplate.update("UPDATE clinlims.sample SET amendment_number = NULL, amends_lhu_number = NULL,"
                + " amendment_reason = NULL WHERE id = 9101");
        jdbcTemplate.update("INSERT INTO clinlims.observation_history_type (id, type_name, description, lastupdated)"
                + " SELECT 9201, 'envWorkflowType', 'Workflow type', now() WHERE NOT EXISTS"
                + " (SELECT 1 FROM clinlims.observation_history_type WHERE type_name = 'envWorkflowType')");
        jdbcTemplate.update("DELETE FROM clinlims.observation_history WHERE id = 9201");
        jdbcTemplate.update("INSERT INTO clinlims.observation_history (id, sample_id, observation_history_type_id,"
                + " value_type, value, lastupdated) SELECT 9201, 9101, id, 'L', 'environmental', now()"
                + " FROM clinlims.observation_history_type WHERE type_name = 'envWorkflowType'");
        statusService.refreshCache();
    }

    @After
    public void removeSignaturesAndSecondStandard() {
        jdbcTemplate.execute("TRUNCATE TABLE clinlims.electronic_signature");
        jdbcTemplate.update("DELETE FROM clinlims.sample_compliance_standards WHERE id = 9102");
        jdbcTemplate.update("DELETE FROM clinlims.compliance_threshold WHERE id = 9102");
        jdbcTemplate.update("DELETE FROM clinlims.parameter_group WHERE id = 9102");
        jdbcTemplate.update("DELETE FROM clinlims.compliance_standard WHERE id = 9102");
    }

    @Test
    public void aReleasedOrderIsGeneratedBeforeItCanBeAmended() throws Exception {
        setStatus("9101, 9102, 9103, 9104, 9109", AnalysisStatus.Finalized);
        jdbcTemplate.update("INSERT INTO clinlims.electronic_signature (id, signer_id, signer_name_printed,"
                + " signature_meaning, signed_at, record_type, record_id, session_signing_sequence, auth_method)"
                + " VALUES (nextval('clinlims.electronic_signature_seq'), 1, 'John Doe',"
                + " 'VALIDATED_AND_RELEASED', NOW(), 'VALIDATION_BATCH', 9101, 1, 'LOCAL')");

        assertFalse("validated and released, but no report issued yet", reportIssued());
        assertEquals("nothing to amend before the report is issued", 409, reissue().getResponse().getStatus());

        assertEquals(200, exportPdf().getResponse().getStatus());

        assertTrue(reportIssued());
        MvcResult amended = reissue();
        assertEquals(200, amended.getResponse().getStatus());
        assertTrue(pdfText(amended).contains("CD-9101/Am.1"));
    }

    @Test
    public void theResultsReportFilterListsOrdersWithoutAStandard() throws Exception {
        setStatus("9101, 9102, 9103, 9104, 9109", AnalysisStatus.Finalized);
        assertEquals("an order with a standard is not a results report", List.of(),
                labNumbersWithStatus(ComplianceReportOrderDTO.KIND_RESULTS));

        jdbcTemplate.update("DELETE FROM clinlims.sample_compliance_standards WHERE sample_id = 9101");

        assertEquals(List.of("CD-9101"), labNumbersWithStatus(ComplianceReportOrderDTO.KIND_RESULTS));
        assertEquals(List.of(), labNumbersWithStatus("COMPLIANT"));
    }

    @Test
    public void aResultJudgedAgainstTwoStandardsIsOneParameterAssessed() throws Exception {
        setStatus("9101, 9102, 9103, 9104, 9109", AnalysisStatus.Finalized);
        jdbcTemplate.update("INSERT INTO clinlims.compliance_standard (id, fhir_uuid, name, version, description,"
                + " issuing_body, regulation_number, country_region, status, effective_date, sys_user_id,"
                + " last_updated) VALUES (9102, 'cd000001-0000-0000-0000-000000000012', 'Second Water Standard',"
                + " '2022', 'Second', 'Government', 'CD 2/2022', 'Indonesia', 'ACTIVE', '2024-01-01', 1, now())");
        jdbcTemplate.update("INSERT INTO clinlims.parameter_group (id, fhir_uuid, standard_id, name, description,"
                + " sort_order, is_mandatory, sys_user_id, last_updated) VALUES (9102,"
                + " 'cd000001-0000-0000-0000-000000000013', 9102, 'Physical', 'Physical', 1, true, 1, now())");
        jdbcTemplate.update("INSERT INTO clinlims.compliance_threshold (id, fhir_uuid, group_id, test_id,"
                + " parameter_code, display_name, threshold_type, min_value, max_value, is_mandatory, is_active,"
                + " archived, sort_order, sys_user_id, last_updated) VALUES (9102,"
                + " 'cd000001-0000-0000-0000-000000000014', 9102, 9101, 'pH', 'pH', 'RANGE', 6.5, 8.5, true, true,"
                + " false, 1, 1, now())");
        jdbcTemplate.update("INSERT INTO clinlims.sample_compliance_standards (id, sample_id, compliance_standard_id,"
                + " priority, created_date, last_updated) VALUES (9102, 9101, 9102, 1, now(), now())");

        String text = pdfText(exportPdf());

        assertTrue("four pH results, each judged against both standards: " + text,
                text.contains("for the 4 parameters assessed"));
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

    private MvcResult reissue() throws Exception {
        return mockMvc.perform(post("/rest/complianceReport/reissue").contentType(MediaType.APPLICATION_JSON)
                .content("{\"sampleId\":9101,\"reason\":\"Typo in the site name\"}")).andReturn();
    }

    private boolean reportIssued() throws Exception {
        String body = mockMvc.perform(get("/rest/complianceReport").param("labNumber", "CD-9101")).andReturn()
                .getResponse().getContentAsString();
        List<Boolean> issued = JsonPath.read(body, "$.orders[*].reportIssued");
        assertEquals(1, issued.size());
        return issued.get(0);
    }

    private List<String> labNumbersWithStatus(String complianceStatus) throws Exception {
        String body = mockMvc.perform(
                get("/rest/complianceReport").param("labNumber", "CD-9101").param("complianceStatus", complianceStatus))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.orders[*].labNumber");
    }

    private void setStatus(String analysisIds, AnalysisStatus status) {
        jdbcTemplate.update(
                "UPDATE clinlims.analysis SET status_id = CAST(? AS numeric) WHERE id IN (" + analysisIds + ")",
                statusService.getStatusID(status));
    }
}
