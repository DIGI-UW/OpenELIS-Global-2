package org.openelisglobal.result.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import org.junit.Test;
import org.openelisglobal.reports.dataexport.dao.SampleTestingExportDAO;
import org.openelisglobal.reports.dataexport.service.ReportingResultValues;
import org.openelisglobal.result.valueholder.Result;

public class ReportingPresentationBoundaryTest {
    @Test
    public void displayChangesButClinicalExportAndStoredRepresentationDoNot() {
        ReportingResultValues export = new ReportingResultValues(mock(SampleTestingExportDAO.class));
        Result result = new Result();
        result.setResultType("N");
        result.setValue("1.1760");
        int[] places = { -1, 0, 1, 2 };
        String[] display = { "1.1760", "1", "1.2", "1.18" };
        for (int i = 0; i < places.length; i++) {
            result.setReportingDecimalPlaces(places[i]);
            assertEquals(display[i], result.getDisplayValue());
            assertEquals("1.1760", result.getValue());
            assertEquals("1.1760", export.format(result, new HashMap<>()));
        }
    }

    @Test
    public void formattingNeverMovesAQualifiedBoundOrReinterpretsScientificNotation() {
        Result result = new Result();
        result.setResultType("N");
        result.setReportingDecimalPlaces(0);
        for (String value : new String[] { "<1.176", ">1.176", "<=1.176", ">=1.176", "1.176e-3", "1.176×10⁻³" }) {
            result.setValue(value);
            assertEquals(value, result.getDisplayValue());
            assertEquals(value, result.getEnteredValue());
        }
    }
}
