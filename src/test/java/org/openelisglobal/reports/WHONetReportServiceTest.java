package org.openelisglobal.reports;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Field;
import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.reports.action.implementation.reportBeans.WHONETCSVRoutineColumnBuilder.WHONetRow;
import org.openelisglobal.reports.service.WHONetReportService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.springframework.beans.factory.annotation.Autowired;

public class WHONetReportServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private WHONetReportService whoNetReportService;

    private Date lowDate;
    private Date highDate;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/whonet-test-data.xml");

        lowDate = new Date(LocalDate.of(2020, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
        highDate = new Date(LocalDate.of(2025, 1, 31).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
    }

    private String getPrivateField(WHONetRow row, String fieldName) throws Exception {
        Field field = WHONetRow.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (String) field.get(row);
    }

    @Test
    public void getAntimicrobialEntries_ShouldReturnSampleItems() {
        List<SampleItem> result = whoNetReportService.getAntimicrobialEntries(lowDate, highDate);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals("1", result.get(0).getId());
    }

    @Test
    public void getWHONetRows_ShouldReturnCorrectRows() throws Exception {
        List<WHONetRow> rows = whoNetReportService.getWHONetRows(lowDate, highDate);

        assertNotNull(rows);
        assertFalse(rows.isEmpty());
        assertEquals(1, rows.size());

        WHONetRow row = rows.get(0);

        // Verify patient information
        assertEquals("PAT-2024001", getPrivateField(row, "nationalId"));
        assertEquals("John", getPrivateField(row, "firstName"));
        assertEquals("Doe", getPrivateField(row, "lastName"));
        assertEquals("M", getPrivateField(row, "sex"));
        assertEquals("1985-05-12", getPrivateField(row, "birthdate"));

        // Verify sample information
        assertEquals("12345", getPrivateField(row, "labNo"));
        assertEquals("Whole Blood Sample", getPrivateField(row, "sampleType"));
        assertEquals("01/03/2024", getPrivateField(row, "collectionDate"));

        // Verify test/result information
        assertEquals("Complete Blood Count", getPrivateField(row, "antibiotic"));
        assertEquals("EQUALS", getPrivateField(row, "organism"));
        assertEquals("EQUALS", getPrivateField(row, "result"));
        assertEquals("", getPrivateField(row, "method"));
    }
}