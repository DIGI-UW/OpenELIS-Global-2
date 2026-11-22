package org.openelisglobal.common.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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

    @Test
    public void anImpossibleOrMissingDateIsLeftForValidationToReject() {
        assertEquals("2026-02-30", IsoDateNormalizer.toDisplayFormat("2026-02-30", "dd/MM/yyyy"));
        assertEquals("", IsoDateNormalizer.toDisplayFormat("", "dd/MM/yyyy"));
        assertNull(IsoDateNormalizer.toDisplayFormat(null, "dd/MM/yyyy"));
    }
}
