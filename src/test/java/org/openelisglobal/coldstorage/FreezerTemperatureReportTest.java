package org.openelisglobal.coldstorage;

import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.coldstorage.service.FreezerReadingService;
import org.openelisglobal.coldstorage.service.FreezerReportService;
import org.openelisglobal.coldstorage.service.FreezerService;
import org.openelisglobal.coldstorage.valueholder.Freezer;
import org.openelisglobal.coldstorage.valueholder.FreezerReading;
import org.openelisglobal.testsupport.PdfText;
import org.springframework.beans.factory.annotation.Autowired;

public class FreezerTemperatureReportTest extends BaseWebContextSensitiveTest {

    @Autowired
    private FreezerService freezerService;

    @Autowired
    private FreezerReadingService freezerReadingService;

    @Autowired
    private FreezerReportService freezerReportService;

    private final LocalDate today = LocalDate.now();

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/freezer.xml");
        Freezer freezer = freezerService.findById(100L).orElseThrow();
        saveReading(freezer, 8, "-80.0", "40.0", FreezerReading.Status.NORMAL);
        saveReading(freezer, 9, "-18.0", null, FreezerReading.Status.CRITICAL);
        saveReading(freezer, 10, "-60.0", "41.0", FreezerReading.Status.WARNING);
    }

    @Test
    public void dailyLog_listsEveryReadingWithItsStatus() throws Exception {
        List<String> lines = lines(freezerReportService.generatePdfReport("daily", 100L, today, today));

        assertLine(lines, "Freezer Temperature Monitoring Report");
        assertLine(lines, "DAILY LOG TEMPERATURE LOG");
        assertContains(lines, "Freezer Unit: Test Freezer 1");
        assertContains(lines, "Period: " + today + " to " + today);
        assertLine(lines, today.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)));
        assertLine(lines, today + " 08:00:00 -80.0 40.0 Normal —");
        assertLine(lines, today + " 09:00:00 -18.0 — CRITICAL —");
        assertLine(lines, today + " 10:00:00 -60.0 41.0 WARNING —");
        assertLine(lines, "Regulatory Compliance Statement");
    }

    @Test
    public void weeklyLog_summarisesTheWeeksReadings() throws Exception {
        List<String> lines = lines(freezerReportService.generatePdfReport("weekly", 100L, today, today));

        assertLine(lines, "WEEKLY LOG TEMPERATURE LOG");
        assertContains(lines, "3 -52.67 -80.00 -18.00 40.5 1 1 1 / 0");
    }

    @Test
    public void monthlyLog_summarisesTheMonthsReadings() throws Exception {
        List<String> lines = lines(freezerReportService.generatePdfReport("monthly", 100L, today, today));

        assertLine(lines, "MONTHLY LOG TEMPERATURE LOG");
        assertLine(lines, "Year " + today.getYear());
        assertLine(lines, today.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH))
                + " 1 3 -52.67 -80.00 -18.00 40.5 1 1 1/0");
    }

    private void saveReading(Freezer freezer, int hour, String temperature, String humidity,
            FreezerReading.Status status) {
        freezerReadingService.saveReading(freezer,
                today.atTime(hour, 0).atZone(ZoneId.systemDefault()).toOffsetDateTime(), new BigDecimal(temperature),
                humidity == null ? null : new BigDecimal(humidity), null, status, true, null);
    }

    private List<String> lines(byte[] pdf) throws Exception {
        return Arrays.asList(PdfText.of(pdf).split("\n"));
    }

    private void assertLine(List<String> lines, String expected) {
        assertTrue(expected + " in " + lines, lines.contains(expected));
    }

    private void assertContains(List<String> lines, String expected) {
        assertTrue(expected + " in " + lines, lines.stream().anyMatch(line -> line.contains(expected)));
    }
}
