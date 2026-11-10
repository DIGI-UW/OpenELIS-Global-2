package org.openelisglobal.configuration.service;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.Test;

public class AbstractCatalogCsvHandlerTest {
    private static class RecordingHandler extends AbstractCatalogCsvHandler {
        private List<CsvRow> loaded;

        public String getDomainName() {
            return "test-records";
        }

        protected String[] requiredColumns() {
            return new String[] { "code", "notes" };
        }

        protected void load(List<CsvRow> rows, RowTransactionRunner transaction, CsvLoadSummary summary,
                String fileName) {
            loaded = rows;
        }
    }

    @Test
    public void multilineNotesPreserveEscapedQuotesCommentsAndPhysicalRowNumbers() throws Exception {
        var handler = new RecordingHandler();
        String csv = "code,notes\r\n  # ignored comment with a quote \"\r\n\r\n"
                + "A,\"First, \"\"quoted\"\" line\r\n# this is part of the note\r\n\r\nLast line\"\r\nB,plain\r\n";
        handler.processConfiguration(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), "notes.csv");
        assertEquals(2, handler.loaded.size());
        assertEquals("First, \"quoted\" line\n# this is part of the note\n\nLast line",
                handler.loaded.get(0).get("notes"));
        assertEquals(4, handler.loaded.get(0).lineNumber());
        assertEquals("B", handler.loaded.get(1).get("code"));
        assertEquals(8, handler.loaded.get(1).lineNumber());
    }

    @Test(expected = java.io.IOException.class)
    public void unterminatedQuotedRecordFailsBeforeAnyRowsAreLoaded() throws Exception {
        var handler = new RecordingHandler();
        handler.processConfiguration(
                new ByteArrayInputStream("code,notes\nA,\"unfinished\nB,other".getBytes(StandardCharsets.UTF_8)),
                "notes.csv");
    }
}
