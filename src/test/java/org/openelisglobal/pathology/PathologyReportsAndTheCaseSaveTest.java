package org.openelisglobal.pathology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.program.controller.pathology.PathologySampleForm;
import org.openelisglobal.program.controller.pathology.PathologySampleForm.PathologyReportForm;
import org.openelisglobal.program.service.PathologyCaseRuleException;
import org.openelisglobal.program.service.PathologyReportService;
import org.openelisglobal.program.valueholder.pathology.PathologySample;
import org.openelisglobal.program.valueholder.pathology.ReportView;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Report rows have left the case: a case save never writes them, and a save
 * that still carries a new or changed one is refused rather than dropping it
 * (design D1, D27; L12, R15, R16).
 */
public class PathologyReportsAndTheCaseSaveTest extends PathologyCaseTestSupport {

    /** The fixture's report row: on file from before versions, no file. */
    private static final int LEGACY_ROW_ID = 601;

    @Autowired
    private PathologyReportService pathologyReportService;

    private StubReportRenderer renderer;

    @Before
    public void stubTheTemplate() {
        clearReportRows();
        renderer = StubReportRenderer.installIn(pathologyReportService);
    }

    @After
    public void restoreTheTemplate() {
        renderer.restore();
        clearReportRows();
    }

    // L12, R15
    @Test
    public void caseSaves_neverTouchReportRows() {
        ReportView generated = pathologyReportService.generate(CASE_ID, TECHNICIAN_ID, StubReportRenderer.PATHS);
        ReportView uploaded = pathologyReportService.upload(CASE_ID,
                PathologyReportServiceTest.encoded("data:image/png", PathologyReportServiceTest.png()), TECHNICIAN_ID);
        List<Map<String, Object>> before = reportRows();

        PathologySampleForm none = caseViewForm();
        none.setReports(null);
        none.setGrossExam("A save without reports");
        save(none);
        PathologySampleForm empty = caseViewForm();
        empty.setGrossExam("A save with an empty list");
        save(empty);
        PathologySampleForm asRead = caseViewForm();
        asRead.setReports(postedAsStored(generated.id(), uploaded.id(), LEGACY_ROW_ID));
        asRead.setGrossExam("A save carrying the rows as read");
        save(asRead);

        assertEquals("each save was taken", "A save carrying the rows as read", jdbcTemplate.queryForObject(
                "SELECT gross_exam FROM clinlims.pathology_sample WHERE id = ?", String.class, CASE_ID));
        assertEquals("and no report row changed: ids, files, versions, flags and row versions alike", before,
                reportRows());
    }

    // R16
    @Test
    public void aNewOrChangedReportInTheSave_isRefused_withNothingWritten() {
        ReportView uploaded = pathologyReportService.upload(CASE_ID,
                PathologyReportServiceTest.encoded("data:image/png", PathologyReportServiceTest.png()), TECHNICIAN_ID);
        List<Map<String, Object>> before = reportRows();
        String grossBefore = pathologySampleService.get(CASE_ID).getGrossExam();
        int diffsBefore = caseSaveDiffs().size();

        PathologySampleForm newFile = caseViewForm();
        newFile.setGrossExam("must not be saved");
        newFile.setReports(
                List.of(PathologyReportServiceTest.encoded("data:application/pdf", PathologyReportServiceTest.pdf())));
        PathologySampleForm changedBytes = caseViewForm();
        changedBytes.setGrossExam("must not be saved");
        List<PathologyReportForm> changed = postedAsStored(uploaded.id());
        changed.get(0).setImage(PathologyReportServiceTest.jpeg());
        changedBytes.setReports(changed);

        for (PathologySampleForm refused : List.of(newFile, changedBytes)) {
            assertThrows("the save is refused with the reload message", PathologyCaseRuleException.class,
                    () -> save(refused));
        }

        assertEquals("no report row changed", before, reportRows());
        assertEquals("nor the case", grossBefore, pathologySampleService.get(CASE_ID).getGrossExam());
        assertEquals("and no history row was written", diffsBefore, caseSaveDiffs().size());
    }

    @Test
    public void theCaseNoLongerLoadsItsReports() {
        assertTrue("the case holds no report collection", Arrays.stream(PathologySample.class.getDeclaredFields())
                .noneMatch(field -> field.getName().equals("reports")));
        assertTrue("nor reads one for its copy or its audit snapshot",
                Arrays.stream(PathologySample.class.getMethods())
                        .noneMatch(method -> method.getName().equals("getReports")
                                || method.getName().equals("getReports_Audit")));

        pathologyReportService.upload(CASE_ID,
                PathologyReportServiceTest.encoded("data:image/png", PathologyReportServiceTest.png()), TECHNICIAN_ID);
        PathologySampleForm form = caseViewForm();
        form.setGrossExam("A change beside a stored report");
        save(form);

        assertFalse("and the save's history row names no reports",
                caseSaveDiffs().stream().anyMatch(history -> changesOf(history).contains("<reports>")));
    }

    // helpers

    /** The rows as a page that read them posts them back: id, file and type. */
    private List<PathologyReportForm> postedAsStored(Integer... reportIds) {
        List<PathologyReportForm> posted = new ArrayList<>();
        for (Integer reportId : reportIds) {
            Map<String, Object> row = jdbcTemplate
                    .queryForMap("SELECT id, image, file_type FROM clinlims.pathology_report WHERE id = ?", reportId);
            PathologyReportForm form = new PathologyReportForm();
            form.setId(reportId);
            form.setImage((byte[]) row.get("image"));
            form.setFileType((String) row.get("file_type"));
            posted.add(form);
        }
        return posted;
    }

    private List<Map<String, Object>> reportRows() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map<String, Object> row : jdbcTemplate.queryForList("SELECT id, encode(image, 'hex') AS image,"
                + " file_type, report_type, version_number, voided, generated_at, generated_by, last_updated FROM"
                + " clinlims.pathology_report WHERE pathology_sample_id = ? ORDER BY id", CASE_ID)) {
            rows.add(row);
        }
        return rows;
    }

    private void clearReportRows() {
        jdbcTemplate.update("DELETE FROM clinlims.history WHERE reference_table ="
                + " (SELECT id FROM clinlims.reference_tables WHERE name = 'PATHOLOGY_REPORT')");
        jdbcTemplate.update("DELETE FROM clinlims.pathology_report WHERE pathology_sample_id = ? AND id <> ?", CASE_ID,
                LEGACY_ROW_ID);
    }
}
