package org.openelisglobal.compliance.controller.rest;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * OGC-1657: the environmental report printed its collection and signature times
 * as yyyy-MM-dd HH:mm whatever the site's date format.
 */
public class EnvironmentalReportPdfDateTest {

    private static final String DAY_FIRST = "dd/MM/yyyy HH:mm";
    private static final String MONTH_FIRST = "MM/dd/yyyy HH:mm";

    @Test
    public void aStoredTimeIsPrintedDayFirstOnADayFirstSite() {
        assertEquals("05/03/2026 10:00", EnvironmentalReportPdf.shownDateTime("2026-03-05 10:00", DAY_FIRST));
    }

    @Test
    public void aStoredTimeIsPrintedMonthFirstOnAMonthFirstSite() {
        assertEquals("03/05/2026 10:00", EnvironmentalReportPdf.shownDateTime("2026-03-05 10:00", MONTH_FIRST));
    }

    @Test
    public void aMissingTimeIsPrintedAsADash() {
        assertEquals("–", EnvironmentalReportPdf.shownDateTime(null, DAY_FIRST));
        assertEquals("–", EnvironmentalReportPdf.shownDateTime(" ", DAY_FIRST));
    }

    @Test
    public void textThatIsNoStoredTimeIsPrintedAsItCame() {
        assertEquals("05/03/2026", EnvironmentalReportPdf.shownDateTime("05/03/2026", DAY_FIRST));
    }
}
