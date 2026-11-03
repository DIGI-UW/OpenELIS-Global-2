package org.openelisglobal.reports.action.implementation.reportBeans;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.reports.action.implementation.ExportStudyProjectByDate;
import org.openelisglobal.reports.action.implementation.Report;
import org.openelisglobal.reports.form.ReportForm.DateType;

/**
 * The Routine HIV Testing and Indeterminate Results exports (RTN builder) were
 * built without the chosen date type, so building their query threw a
 * NullPointerException and the export answered "Internal error while generating
 * report". The builder now takes the date type and falls back to the order
 * date, and its question lists hold only real questions.
 */
public class RTNColumnBuilderTest extends BaseWebContextSensitiveTest {

    private Report.DateRange range() {
        Report.DateRange range = new ExportStudyProjectByDate().new DateRange("01/10/2026", "04/11/2026");
        assertTrue(range.validateHighLowDate("report.error.message.date.received.missing"));
        return range;
    }

    @Test
    public void buildsItsQueryOnTheChosenDate() {
        RTNColumnBuilder builder = new RTNColumnBuilder(range(), "26", DateType.RESULT_DATE);

        builder.makeSQL();

        assertTrue(builder.query.toString().contains("a.released_date"));
    }

    @Test
    public void buildsItsQueryOnTheOrderDateWhenNoDateTypeIsGiven() {
        RTNColumnBuilder builder = new RTNColumnBuilder(range(), "26");

        builder.makeSQL();

        assertTrue(builder.query.toString().contains("s.entered_date"));
    }

    @Test
    public void listsOnlyRealQuestions() {
        RTNColumnBuilder builder = new RTNColumnBuilder(range(), "26", DateType.ORDER_DATE);

        assertFalse(Arrays.asList(builder.getRTNDiseaseQuestionNames()).contains(null));
        assertFalse(Arrays.asList(builder.getRTNExamQuestionNames()).contains(null));
    }
}
