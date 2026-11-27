package org.openelisglobal.program.service;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.program.controller.pathology.PathologySampleForm;
import org.openelisglobal.program.controller.pathology.PathologySampleForm.PathologyReportForm;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A pathology report file is uploaded through its own call and validated there;
 * the case save carries reports only as a page read them.
 */
public class ClinicalAttachmentPersistenceTest extends BaseWebContextSensitiveTest {

    /** The fixture's report row on case 1, from before versions. */
    private static final int LEGACY_ROW_ID = 601;

    @Autowired
    private PathologySampleService service;

    @Autowired
    private PathologyReportService reportService;

    @Before
    public void fixture() throws Exception {
        executeDataSetWithStateManagement("testdata/pathology-sample-with-patient.xml");
        authenticateAs("technician1");
        clearUploads();
    }

    @After
    public void clearUploadsAfterwards() {
        clearUploads();
    }

    @Test
    public void validPdfSurvivesUploadAndReadback() throws Exception {
        byte[] bytes = ClinicalAttachmentServiceTest.fixture("application/pdf");
        Integer id = reportService.upload(1, upload("data:application/pdf", bytes), "1001").id();
        assertArrayEquals(bytes, storedBytes(id));
        assertEquals("data:application/pdf", jdbcTemplate
                .queryForObject("SELECT file_type FROM clinlims.pathology_report WHERE id = ?", String.class, id));
    }

    @Test
    public void rejectedUploadDoesNotReplaceTheExistingReport() throws Exception {
        byte[] original = ClinicalAttachmentServiceTest.fixture("image/png");
        Integer id = reportService.upload(1, upload("data:image/png", original), "1001").id();
        assertThrows(InvalidClinicalAttachmentException.class,
                () -> reportService.upload(1, upload("data:text/html", original), "1001"));
        assertArrayEquals(original, storedBytes(id));
        assertEquals("the refused file stored no row", Integer.valueOf(1), jdbcTemplate.queryForObject(
                "SELECT count(*) FROM clinlims.pathology_report WHERE pathology_sample_id = 1 AND image IS NOT NULL",
                Integer.class));
    }

    @Test
    public void unchangedUnsupportedReportDoesNotBlockAnUnrelatedCaseEdit() throws Exception {
        byte[] old = "historical document".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        jdbcTemplate.update(
                "UPDATE clinlims.pathology_report SET image = ?, file_type = 'data:application/msword' WHERE id = ?",
                old, LEGACY_ROW_ID);
        PathologySampleForm form = caseForm();
        PathologyReportForm submitted = new PathologyReportForm();
        submitted.setId(LEGACY_ROW_ID);
        submitted.setFileType("data:application/msword");
        submitted.setImage(old.clone());
        form.setReports(List.of(submitted));
        form.setGrossExam("An unrelated case edit");
        service.updateWithFormValues(1, form);
        assertArrayEquals(old, storedBytes(LEGACY_ROW_ID));
        assertEquals("An unrelated case edit", service.get(1).getGrossExam());
    }

    private byte[] storedBytes(Integer id) {
        return jdbcTemplate.queryForObject("SELECT image FROM clinlims.pathology_report WHERE id = ?", byte[].class,
                id);
    }

    private PathologySampleForm caseForm() {
        PathologySampleForm form = new PathologySampleForm();
        form.setStatus(service.get(1).getStatus());
        form.setSystemUserId("1001");
        form.setBlocks(new ArrayList<>());
        form.setSlides(new ArrayList<>());
        return form;
    }

    private static PathologyReportForm upload(String type, byte[] bytes) {
        PathologyReportForm report = new PathologyReportForm();
        report.setBase64Image(type + ";base64," + Base64.getEncoder().encodeToString(bytes));
        return report;
    }

    private void clearUploads() {
        jdbcTemplate.update("DELETE FROM clinlims.history WHERE reference_table ="
                + " (SELECT id FROM clinlims.reference_tables WHERE name = 'PATHOLOGY_REPORT')");
        jdbcTemplate.update("DELETE FROM clinlims.pathology_report WHERE pathology_sample_id = 1 AND id <> ?",
                LEGACY_ROW_ID);
    }
}
