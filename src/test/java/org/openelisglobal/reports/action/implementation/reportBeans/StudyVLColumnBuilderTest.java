package org.openelisglobal.reports.action.implementation.reportBeans;

import static org.junit.Assert.assertEquals;
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

    private Report.DateRange range() {
        Report.DateRange range = new ExportStudyProjectByDate().new DateRange("01/10/2026", "04/11/2026");
        assertTrue(range.validateHighLowDate("report.error.message.date.received.missing"));
        return range;
    }

    @Test
    public void filtersTheTreatmentCrosstabOnTheSamplesReleaseDate() {
        StudyVLColumnBuilder builder = new StudyVLColumnBuilder(range(), "28", DateType.RESULT_DATE);

        assertEquals(CSVColumnBuilder.LAST_RELEASE_DATE, builder.treatmentCrosstabDate("a.released_date "));
    }

    @Test
    public void keepsTheChosenDateForTheOtherDateTypes() {
        StudyVLColumnBuilder builder = new StudyVLColumnBuilder(range(), "28", DateType.ORDER_DATE);

        assertEquals("s.entered_date ", builder.treatmentCrosstabDate("s.entered_date "));
    }
}
