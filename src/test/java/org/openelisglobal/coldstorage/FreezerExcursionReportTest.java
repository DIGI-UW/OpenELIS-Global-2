package org.openelisglobal.coldstorage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.coldstorage.service.FreezerReadingService;
import org.openelisglobal.coldstorage.service.FreezerService;
import org.openelisglobal.coldstorage.service.dto.FreezerExcursionData;
import org.openelisglobal.coldstorage.valueholder.Freezer;
import org.openelisglobal.coldstorage.valueholder.FreezerReading;
import org.openelisglobal.reports.action.implementation.FreezerExcursionReport;
import org.openelisglobal.reports.action.implementation.reportBeans.FreezerExcursionReportData;
import org.openelisglobal.reports.form.ReportForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

public class FreezerExcursionReportTest extends BaseWebContextSensitiveTest {

    @Autowired
    private FreezerService freezerService;

    @Autowired
    private FreezerReadingService freezerReadingService;

    private Freezer freezer;
    private OffsetDateTime windowStart;
    private OffsetDateTime windowEnd;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/freezer.xml");
        freezer = freezerService.findById(100L).orElse(null);
        assertNotNull("Test freezer should exist", freezer);
        windowStart = OffsetDateTime.now().minusHours(2);
        windowEnd = OffsetDateTime.now().plusHours(1);
    }

    /**
     * A failed poll is persisted as CRITICAL with no temperature, so without the
     * transmission guard an outage reports as a blank-bounded excursion.
     */
    @Test
    public void findExcursions_shouldNotReportACommsOutageAsATemperatureExcursion() {
        saveReading(60, new BigDecimal("-80.0"), FreezerReading.Status.NORMAL, true);
        saveReading(50, null, FreezerReading.Status.CRITICAL, false);
        saveReading(40, null, FreezerReading.Status.CRITICAL, false);
        saveReading(30, new BigDecimal("-79.0"), FreezerReading.Status.NORMAL, true);

        List<FreezerExcursionData> excursions = freezerReadingService.findExcursions(freezer, windowStart, windowEnd);

        assertTrue("An outage is not a temperature excursion: " + describe(excursions), excursions.isEmpty());
    }

    @Test
    public void findExcursions_shouldSummariseAContiguousBreachWithItsMinAndMax() {
        saveReading(60, new BigDecimal("-80.0"), FreezerReading.Status.NORMAL, true);
        saveReading(50, new BigDecimal("-18.0"), FreezerReading.Status.CRITICAL, true);
        saveReading(40, new BigDecimal("-15.0"), FreezerReading.Status.CRITICAL, true);
        saveReading(30, new BigDecimal("-80.0"), FreezerReading.Status.NORMAL, true);

        List<FreezerExcursionData> excursions = freezerReadingService.findExcursions(freezer, windowStart, windowEnd);

        assertEquals("The two breaching readings are one excursion: " + describe(excursions), 1, excursions.size());
        FreezerExcursionData excursion = excursions.get(0);
        assertEquals("CRITICAL", excursion.getSeverity());
        assertEquals(0, new BigDecimal("-18.0").compareTo(excursion.getMinTemperature()));
        assertEquals(0, new BigDecimal("-15.0").compareTo(excursion.getMaxTemperature()));
        assertEquals("Duration spans the first to the last breaching reading", Long.valueOf(600),
                excursion.getDurationSeconds());
        assertEquals(freezer.getName(), excursion.getFreezerName());
    }

    @Test
    public void findExcursions_shouldSplitABreachInterruptedByAnOutage() {
        saveReading(60, new BigDecimal("-18.0"), FreezerReading.Status.CRITICAL, true);
        saveReading(50, null, FreezerReading.Status.CRITICAL, false);
        saveReading(40, new BigDecimal("-17.0"), FreezerReading.Status.CRITICAL, true);

        List<FreezerExcursionData> excursions = freezerReadingService.findExcursions(freezer, windowStart, windowEnd);

        assertEquals("Two excursions, one either side of the outage: " + describe(excursions), 2, excursions.size());
        assertEquals(0, new BigDecimal("-18.0").compareTo(excursions.get(0).getMinTemperature()));
        assertEquals(0, new BigDecimal("-17.0").compareTo(excursions.get(1).getMinTemperature()));
    }

    /**
     * The PDF and the on-screen preview are two renderings of one grouping, so a
     * comms outage must be absent from both.
     */
    @Test
    public void pdfReport_shouldNotReportACommsOutageAsATemperatureExcursion() {
        saveReading(60, new BigDecimal("-80.0"), FreezerReading.Status.NORMAL, true);
        saveReading(50, null, FreezerReading.Status.CRITICAL, false);
        saveReading(40, null, FreezerReading.Status.CRITICAL, false);
        saveReading(30, new BigDecimal("-79.0"), FreezerReading.Status.NORMAL, true);

        List<FreezerExcursionReportData> rows = generatePdfReportRows();

        assertTrue("An outage is not a temperature excursion: " + describeRows(rows), rows.isEmpty());
    }

    @Test
    public void pdfReport_shouldRenderAContiguousBreachAsOneRow() {
        saveReading(60, new BigDecimal("-80.0"), FreezerReading.Status.NORMAL, true);
        saveReading(50, new BigDecimal("-18.0"), FreezerReading.Status.CRITICAL, true);
        saveReading(40, new BigDecimal("-15.0"), FreezerReading.Status.CRITICAL, true);
        saveReading(30, new BigDecimal("-80.0"), FreezerReading.Status.NORMAL, true);

        List<FreezerExcursionReportData> rows = generatePdfReportRows();

        assertEquals("The two breaching readings are one row: " + describeRows(rows), 1, rows.size());
        FreezerExcursionReportData row = rows.get(0);
        assertEquals("CRITICAL", row.getSeverity());
        assertEquals(0, new BigDecimal("-18.0").compareTo(row.getMinTemperature()));
        assertEquals(0, new BigDecimal("-15.0").compareTo(row.getMaxTemperature()));
        assertEquals("10 minutes", row.getDuration());
        assertEquals(freezer.getName(), row.getFreezerName());
        assertEquals(String.valueOf(freezer.getId()), row.getFreezerId());
        assertTrue("Excursion id names the freezer: " + row.getExcursionId(),
                row.getExcursionId().startsWith("EXC-" + freezer.getId() + "-"));
        assertTrue("Start time is rendered as a local timestamp: " + row.getStartTime(),
                row.getStartTime().matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"));
    }

    @SuppressWarnings("unchecked")
    private List<FreezerExcursionReportData> generatePdfReportRows() {
        ReportForm form = new ReportForm();
        form.setLowerDateRange(LocalDate.now().minusDays(1).toString());
        form.setUpperDateRange(LocalDate.now().toString());
        form.setProjectCode(String.valueOf(freezer.getId()));

        FreezerExcursionReport report = new FreezerExcursionReport();
        report.initializeReport(form);

        return (List<FreezerExcursionReportData>) ReflectionTestUtils.getField(report, "reportItems");
    }

    private String describeRows(List<FreezerExcursionReportData> rows) {
        return rows.stream().map(r -> r.getSeverity() + " " + r.getTemperatureRange()).toList().toString();
    }

    private void saveReading(int minutesAgo, BigDecimal temperature, FreezerReading.Status status,
            boolean transmissionOk) {
        freezerReadingService.saveReading(freezer, OffsetDateTime.now().minusMinutes(minutesAgo), temperature, null,
                null, status, transmissionOk, transmissionOk ? null : "timeout");
    }

    private String describe(List<FreezerExcursionData> excursions) {
        return excursions.stream()
                .map(e -> e.getSeverity() + " " + e.getMinTemperature() + ".." + e.getMaxTemperature()).toList()
                .toString();
    }
}
