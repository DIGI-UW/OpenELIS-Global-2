package org.openelisglobal.program.service;

import static org.junit.Assert.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.program.controller.pathology.PathologySampleForm;
import org.openelisglobal.program.controller.pathology.PathologySampleForm.PathologyReportForm;
import org.openelisglobal.program.valueholder.pathology.PathologyReport.PathologyReportType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

public class ClinicalAttachmentPersistenceTest extends BaseWebContextSensitiveTest {
    @Autowired
    private PathologySampleService service;

    @PersistenceContext
    private EntityManager entityManager;

    @Before
    public void fixture() throws Exception {
        executeDataSetWithStateManagement("testdata/pathology-sample-with-patient.xml");
        authenticateAs("technician1");
    }

    @Test
    public void validPdfSurvivesSaveAndReadback() throws Exception {
        byte[] bytes = ClinicalAttachmentServiceTest.fixture("application/pdf");
        PathologySampleForm form = formWithReport("data:application/pdf", bytes);
        service.updateWithFormValues(1, form);
        assertArrayEquals(bytes, storedBytes());
        assertEquals("data:application/pdf", jdbcTemplate
                .queryForObject("SELECT file_type FROM pathology_report WHERE pathology_sample_id = 1", String.class));
    }

    @Test
    public void rejectedUploadDoesNotReplaceTheExistingReportOrCaseText() throws Exception {
        byte[] original = ClinicalAttachmentServiceTest.fixture("image/png");
        service.updateWithFormValues(1, formWithReport("data:image/png", original));
        String before = jdbcTemplate.queryForObject("SELECT gross_exam FROM pathology_sample WHERE id = 1",
                String.class);
        PathologySampleForm invalid = formWithReport("data:text/html", original);
        invalid.setGrossExam("must not be saved");
        assertThrows(InvalidClinicalAttachmentException.class, () -> service.updateWithFormValues(1, invalid));
        assertArrayEquals(original, storedBytes());
        assertEquals(before,
                jdbcTemplate.queryForObject("SELECT gross_exam FROM pathology_sample WHERE id = 1", String.class));
    }

    @Test
    @Transactional
    public void unchangedUnsupportedReportDoesNotBlockAnUnrelatedCaseEdit() throws Exception {
        byte[] old = "historical document".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        var existingCase = service.get(1);
        var existing = existingCase.getReports().get(0);
        existing.setImage(old);
        existing.setFileType("data:application/msword");
        service.update(existingCase);
        PathologySampleForm form = formWithReport("data:application/pdf",
                ClinicalAttachmentServiceTest.fixture("application/pdf"));
        PathologyReportForm submitted = form.getReports().get(0);
        submitted.setId(existing.getId());
        submitted.setBase64Image(null);
        submitted.setFileType(existing.getFileType());
        submitted.setImage(old.clone());
        form.setGrossExam("An unrelated case edit");
        service.updateWithFormValues(1, form);
        entityManager.flush();
        entityManager.clear();
        assertArrayEquals(old, storedBytes());
        assertEquals("An unrelated case edit", service.get(1).getGrossExam());
    }

    private byte[] storedBytes() {
        return jdbcTemplate.queryForObject("SELECT image FROM pathology_report WHERE pathology_sample_id = 1",
                byte[].class);
    }

    private PathologySampleForm formWithReport(String type, byte[] bytes) {
        PathologySampleForm form = new PathologySampleForm();
        form.setStatus(service.get(1).getStatus());
        form.setSystemUserId("1001");
        form.setBlocks(new ArrayList<>());
        form.setSlides(new ArrayList<>());
        PathologyReportForm report = new PathologyReportForm();
        report.setReportType(PathologyReportType.PATHOLOGY);
        report.setBase64Image(type + ";base64," + Base64.getEncoder().encodeToString(bytes));
        form.setReports(List.of(report));
        return form;
    }
}
