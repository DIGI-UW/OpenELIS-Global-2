package org.openelisglobal.reports.action.implementation;

import static org.junit.Assert.assertEquals;

import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.pathology.PathologyCaseTestSupport;
import org.openelisglobal.pathology.PathologyFindingsDictionary;
import org.openelisglobal.reports.action.implementation.reportBeans.ProgramSampleReportData;
import org.openelisglobal.reports.form.ReportForm;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The pathology report prints the case's coded conclusions by name, in the
 * order they are stored.
 */
public class PatientPathologyReportTest extends PathologyCaseTestSupport {

    private static final int[] CONCLUSION_ROWS = { 9761, 9762, 9763 };

    private PathologyFindingsDictionary dictionary;

    @Before
    public void seedTheDictionary() {
        dictionary = PathologyFindingsDictionary.seed(jdbcTemplate);
    }

    @After
    public void removeTheRows() {
        for (int id : CONCLUSION_ROWS) {
            jdbcTemplate.update("DELETE FROM clinlims.pathology_conclusion WHERE id = ?", id);
        }
        dictionary.close();
    }

    @Test
    public void theReportNamesCodedConclusionsInStoredOrder_andSkipsAnEntryThatNoLongerExists() {
        storeConclusion(9761, PathologyFindingsDictionary.CHRONIC_GASTRITIS);
        storeConclusion(9762, PathologyFindingsDictionary.NO_ENTRY);
        storeConclusion(9763, PathologyFindingsDictionary.ADENOCARCINOMA);

        ProgramSampleReportData data = new TransactionTemplate(transactionManager).execute(status -> {
            PatientPathologyReport report = new PatientPathologyReport();
            ReportForm form = new ReportForm();
            form.setProgramSampleId(String.valueOf(CASE_ID));
            report.innitializeSample(form);
            ProgramSampleReportData reportData = new ProgramSampleReportData();
            report.data = reportData;
            report.setAdditionalReportItems();
            return reportData;
        });

        assertEquals("the entries that exist, by name, in the stored order",
                List.of("Chronic gastritis", "Adenocarcinoma"), data.getCodedConclusion());
    }

    private void storeConclusion(int id, String code) {
        jdbcTemplate.update("INSERT INTO clinlims.pathology_conclusion (id, pathology_sample_id, type, value,"
                + " last_updated) VALUES (?, ?, 'DICTIONARY', ?, now())", id, CASE_ID, code);
    }
}
