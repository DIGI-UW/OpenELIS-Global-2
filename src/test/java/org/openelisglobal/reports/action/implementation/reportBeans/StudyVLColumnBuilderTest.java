package org.openelisglobal.reports.action.implementation.reportBeans;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.reports.action.implementation.ExportStudyProjectByDate;
import org.openelisglobal.reports.action.implementation.Report;
import org.openelisglobal.reports.form.ReportForm.DateType;

/**
 * The viral load study export with a result-date range named the analysis alias
 * inside the ARV treatment crosstab, which has only the sample, and answered
 * "Internal error while generating report". That crosstab now filters on the
 * sample's last release date.
 */
public class StudyVLColumnBuilderTest extends BaseWebContextSensitiveTest {

    @Test
    public void filtersTheTreatmentCrosstabOnTheSamplesReleaseDate() {
        Report.DateRange range = new ExportStudyProjectByDate().new DateRange("01/10/2026", "04/11/2026");
        assertTrue(range.validateHighLowDate("report.error.message.date.received.missing"));
        StudyVLColumnBuilder builder = new StudyVLColumnBuilder(range, "28", DateType.RESULT_DATE);

        builder.makeSQL();

        String sql = builder.query.toString();
        assertTrue(sql.contains("rsi.samp_id = s.id"));
    }
}
