package org.openelisglobal.common.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.time.LocalDate;
import org.junit.Test;

/**
 * OGC-1135: API callers can send ISO-8601 dates to the order fields that are
 * validated and stored in the site's display format.
 */
public class IsoDateNormalizerTest {

    @Test
    public void anIsoDateIsRewrittenInTheDisplayFormat() {
        assertEquals("26/09/2026", IsoDateNormalizer.toDisplayFormat("2026-09-26", "dd/MM/yyyy"));
        assertEquals("09/26/2026", IsoDateNormalizer.toDisplayFormat("2026-09-26", "MM/dd/yyyy"));
    }

    @Test
    public void anIsoDateTimeKeepsOnlyItsDate() {
        assertEquals("26/09/2026", IsoDateNormalizer.toDisplayFormat("2026-09-26T10:15:00Z", "dd/MM/yyyy"));
        assertEquals("26/09/2026", IsoDateNormalizer.toDisplayFormat("2026-09-26T10:15:00.123+10:00", "dd/MM/yyyy"));
        assertEquals("26/09/2026", IsoDateNormalizer.toDisplayFormat("2026-09-26 10:15", "dd/MM/yyyy"));
    }

    @Test
    public void aDisplayFormatDateIsLeftUnchanged() {
        assertEquals("26/09/2026", IsoDateNormalizer.toDisplayFormat("26/09/2026", "MM/dd/yyyy"));
        assertEquals("xx/09/2026", IsoDateNormalizer.toDisplayFormat("xx/09/2026", "dd/MM/yyyy"));
    }

    /**
     * OGC-1669: a date kept in the display format of the day it was saved could not
     * be read once the site changed its format.
     */
    @Test
    public void aDisplayDateIsStoredAsIsoReadInTheSitesOwnFormatOnly() {
        assertEquals("2026-11-12", IsoDateNormalizer.toIso("11/12/2026", "MM/dd/yyyy"));
        assertEquals("2026-12-11", IsoDateNormalizer.toIso("11/12/2026", "dd/MM/yyyy"));
        assertEquals("2026-09-26", IsoDateNormalizer.toIso("2026-09-26T10:15:00Z", "dd/MM/yyyy"));
        assertEquals("13/02/2026", IsoDateNormalizer.toIso("13/02/2026", "MM/dd/yyyy"));
        assertEquals("31/02/2026", IsoDateNormalizer.toIso("31/02/2026", "dd/MM/yyyy"));
        assertEquals("", IsoDateNormalizer.toIso("", "dd/MM/yyyy"));
        assertNull(IsoDateNormalizer.toIso(null, "dd/MM/yyyy"));
    }

    /**
     * The date validator accepts a day or month written with one digit, so a value
     * saved that way must be stored as ISO too, or a report later reads the
     * received date in its place. The strict read still refuses a swapped or
     * impossible date.
     */
    @Test
    public void aDayOrMonthWrittenWithOneDigitIsStoredAsIso() {
        assertEquals("2026-05-01", IsoDateNormalizer.toIso("1/5/2026", "dd/MM/yyyy"));
        assertEquals("2026-01-05", IsoDateNormalizer.toIso("1/5/2026", "MM/dd/yyyy"));
        assertEquals("2026-05-01", IsoDateNormalizer.toIso("01/5/2026", "dd/MM/yyyy"));
        assertEquals("11/20/2026", IsoDateNormalizer.toIso("11/20/2026", "dd/MM/yyyy"));
        assertEquals("31/2/2026", IsoDateNormalizer.toIso("31/2/2026", "dd/MM/yyyy"));
        assertEquals("1/5/26", IsoDateNormalizer.toIso("1/5/26", "dd/MM/yyyy"));
    }

    @Test
    public void aStoredDateIsReadFromIsoOrStrictlyFromTheSitesFormat() {
        assertEquals(LocalDate.of(2026, 11, 12), IsoDateNormalizer.toLocalDate("2026-11-12", "dd/MM/yyyy"));
        assertEquals(LocalDate.of(2026, 12, 11), IsoDateNormalizer.toLocalDate("11/12/2026", "dd/MM/yyyy"));
        assertNull("no silent day-month swap", IsoDateNormalizer.toLocalDate("13/02/2026", "MM/dd/yyyy"));
        assertNull(IsoDateNormalizer.toLocalDate("", "dd/MM/yyyy"));
        assertNull(IsoDateNormalizer.toLocalDate(null, "dd/MM/yyyy"));
    }

    @Test
    public void anImpossibleOrMissingDateIsLeftForValidationToReject() {
        assertEquals("2026-02-30", IsoDateNormalizer.toDisplayFormat("2026-02-30", "dd/MM/yyyy"));
        assertEquals("", IsoDateNormalizer.toDisplayFormat("", "dd/MM/yyyy"));
        assertNull(IsoDateNormalizer.toDisplayFormat(null, "dd/MM/yyyy"));
    }
}
