package org.openelisglobal.reports.action.implementation;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;

public class HaitiLNSPExportResultTest {
    @Test
    public void numericExportPreservesClinicalValuesRegardlessOfDisplayPolicy() {
        ResultService service = mock(ResultService.class);
        Result result = new Result();
        result.setResultType("N");
        for (String value : new String[] { "1.1760", "<=1.176", "1.176e-3" }) {
            result.setValue(value);
            for (int places : new int[] { 0, 1, 2 }) {
                result.setReportingDecimalPlaces(places);
                assertEquals(value, HaitiLNSPExportReport.exportResultValue(result, service));
            }
        }
        verifyZeroInteractions(service);
    }

    @Test
    public void categoricalExportStillResolvesTheAnswerLabel() {
        ResultService service = mock(ResultService.class);
        Result result = new Result();
        result.setResultType("D");
        result.setValue("42");
        when(service.formatResultForDisplay(result, true)).thenReturn("Detected");
        assertEquals("Detected", HaitiLNSPExportReport.exportResultValue(result, service));
    }
}
