package org.openelisglobal.reports.action.implementation;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.reports.action.implementation.reportBeans.CSVColumnBuilder;

/**
 * The viral load trends export answered HTTP 500 when no sample matched its
 * range: writing the CSV read the last row of an empty result. An empty result
 * now writes nothing.
 */
public class ExportTrendsByDateTest extends BaseWebContextSensitiveTest {

    @Test
    public void anEmptyResultWritesNothing() throws Exception {
        CSVColumnBuilder noRows = mock(CSVColumnBuilder.class);
        when(noRows.next()).thenReturn(false);
        ExportTrendsByDate report = new ExportTrendsByDate();
        report.csvColumnBuilder = noRows;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        report.writeResultsToBuffer(buffer);

        assertEquals(0, buffer.size());
    }
}
