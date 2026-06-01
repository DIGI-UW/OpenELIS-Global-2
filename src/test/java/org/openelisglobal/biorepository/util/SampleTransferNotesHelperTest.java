package org.openelisglobal.biorepository.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.openelisglobal.biorepository.util.SampleTransferNotesHelper.ParsedTransferNotes;

public class SampleTransferNotesHelperTest {

    @Test
    public void formatAndParseStructuredNotes_roundTrip() {
        String formatted = SampleTransferNotesHelper.formatStructuredNotes("COVID Study", "Long-term storage");
        ParsedTransferNotes parsed = SampleTransferNotesHelper.parseStructuredNotes(formatted);

        assertEquals("COVID Study", parsed.getProjectName());
        assertEquals("Long-term storage", parsed.getTransferReason());
    }

    @Test
    public void parseStructuredNotes_fallsBackToLegacyNotes() {
        ParsedTransferNotes parsed = SampleTransferNotesHelper.parseStructuredNotes("Legacy transfer note");

        assertNull(parsed.getProjectName());
        assertEquals("Legacy transfer note", parsed.getTransferReason());
    }
}
