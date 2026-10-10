package org.openelisglobal.reports.dataexport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.Test;
import org.openelisglobal.reports.dataexport.form.ExportField;
import org.openelisglobal.reports.dataexport.form.ExportRecord;
import org.openelisglobal.reports.dataexport.service.ReportingCsvWriter;

public class ReportingCsvWriterTest {
    private final ExportField accession = new ExportField("accession", "Accession", false);
    private final ExportField hb = new ExportField("component:hb", "Hemoglobin", true);
    private final ExportField wbc = new ExportField("component:wbc", "White Cell Count", true);

    private ExportRecord record(String id, String specimen, String event, String field, String value) {
        return new ExportRecord(id, specimen, event, Map.of("accession", "A-1", "resultValue", value),
                Map.of(field, value));
    }

    private String export(ReportingCsvWriter.Layout layout, List<ExportField> columns, ExportRecord... records)
            throws IOException {
        StringWriter output = new StringWriter();
        new ReportingCsvWriter().write(output, layout, columns, List.of(records).iterator());
        return output.toString();
    }

    /**
     * A result value or free-text comment that starts like a formula must not
     * execute when reporting staff open the export. CSV quoting is not a defence:
     * the quotes are consumed by the parser and the cell is still evaluated.
     */
    @Test
    public void spreadsheetFormulaLeadingValuesAreNeutralizedNotJustQuoted() throws Exception {
        for (String payload : List.of("=1+1", "+1", "@SUM(A1)", "=cmd|'/c calc'!A1", "\t=1+1", " =1+1")) {
            String csv = export(ReportingCsvWriter.Layout.SPREADSHEET, List.of(hb),
                    record("1", "s1", null, hb.id(), payload));
            String cell = csv.split("\r\n")[1];
            assertTrue("formula-leading value must be prefixed, got: " + cell,
                    cell.startsWith("'") || cell.startsWith("\"'"));
        }
    }

    /**
     * The guard must not corrupt data. Negative numbers lead with '-' and are
     * routine here (deltas, reference ranges), so they stay verbatim.
     */
    @Test
    public void negativeAndPlainNumbersSurviveTheFormulaGuardUnchanged() throws Exception {
        assertEquals("﻿Hemoglobin\r\n-5.5\r\n",
                export(ReportingCsvWriter.Layout.SPREADSHEET, List.of(hb), record("1", "s1", null, hb.id(), "-5.5")));
        assertEquals("﻿Hemoglobin\r\n12\r\n",
                export(ReportingCsvWriter.Layout.SPREADSHEET, List.of(hb), record("1", "s1", null, hb.id(), "12")));
    }

    @Test
    public void routineSpreadsheetCombinesSingleReadingsAndKeepsSelectedOrder() throws Exception {
        assertEquals("\uFEFFWhite Cell Count,Accession,Hemoglobin\r\n7,A-1,12\r\n",
                export(ReportingCsvWriter.Layout.SPREADSHEET, List.of(wbc, accession, hb),
                        record("1", "s1", null, hb.id(), "12"), record("2", "s1", null, wbc.id(), "7")));
    }

    @Test
    public void independentRepeatSetsNeverBecomeInventedPairsOrCrossProducts() throws Exception {
        assertEquals("\uFEFFAccession,Hemoglobin,White Cell Count\r\nA-1,12,\r\nA-1,11,\r\n" + "A-1,,7\r\nA-1,,8\r\n",
                export(ReportingCsvWriter.Layout.SPREADSHEET, List.of(accession, hb, wbc),
                        record("1", "s1", null, hb.id(), "12"), record("2", "s1", null, wbc.id(), "7"),
                        record("3", "s1", null, hb.id(), "11"), record("4", "s1", null, wbc.id(), "8")));
    }

    @Test
    public void distinctRepeatedReadingsWithEqualValuesAreRetained() throws Exception {
        assertEquals("\uFEFFAccession,Hemoglobin\r\nA-1,12\r\nA-1,12\r\n",
                export(ReportingCsvWriter.Layout.SPREADSHEET, List.of(accession, hb),
                        record("1", "s1", null, hb.id(), "12"), record("2", "s1", null, hb.id(), "12")));
    }

    @Test
    public void actualEventRelationshipsMayAlignReadingsButRepeatsRemainSeparate() throws Exception {
        assertEquals("\uFEFFAccession,Hemoglobin,White Cell Count\r\nA-1,12,7\r\nA-1,11,8\r\n",
                export(ReportingCsvWriter.Layout.SPREADSHEET, List.of(accession, hb, wbc),
                        record("1", "s1", "e1", hb.id(), "12"), record("2", "s1", "e1", wbc.id(), "7"),
                        record("3", "s1", "e2", hb.id(), "11"), record("4", "s1", "e2", wbc.id(), "8")));
    }

    @Test
    public void separateSpecimensUnderAnAccessionDoNotCollapse() throws Exception {
        assertEquals("\uFEFFAccession,Hemoglobin\r\nA-1,12\r\nA-1,11\r\n",
                export(ReportingCsvWriter.Layout.SPREADSHEET, List.of(accession, hb),
                        record("1", "s1", null, hb.id(), "12"), record("2", "s2", null, hb.id(), "11")));
    }

    @Test
    public void detailedLayoutRetainsEachResultEvenWhenOnlyMetadataIsSelected() throws Exception {
        ExportRecord first = record("1", "s1", null, hb.id(), "12");
        ExportRecord second = record("2", "s1", null, hb.id(), "11");
        assertEquals("\uFEFFAccession\r\nA-1\r\nA-1\r\n",
                export(ReportingCsvWriter.Layout.RESULT_LIST, List.of(accession), first, second));
        assertEquals("\uFEFFAccession\r\nA-1\r\n",
                export(ReportingCsvWriter.Layout.SPREADSHEET, List.of(accession), first, second));
    }

    @Test
    public void textEscapingAndNullsPreserveOriginalValues() throws Exception {
        ExportField value = new ExportField("resultValue", "Result, \"text\"", false);
        ExportField missing = new ExportField("missing", "Absent", false);
        assertEquals("\uFEFF\"Result, \"\"text\"\"\",Absent\r\n\"Échantillon, \"\"a\"\"\nβ\",\r\n",
                export(ReportingCsvWriter.Layout.RESULT_LIST, List.of(value, missing),
                        record("1", "s1", null, hb.id(), "Échantillon, \"a\"\nβ")));
    }

    @Test
    public void emptyResultProducesOnlyHeadersAndReportsZeroRows() throws Exception {
        StringWriter output = new StringWriter();
        long rows = new ReportingCsvWriter().write(output, ReportingCsvWriter.Layout.SPREADSHEET,
                List.of(accession, hb), List.<ExportRecord>of().iterator());
        assertEquals(0, rows);
        assertEquals("\uFEFFAccession,Hemoglobin\r\n", output.toString());
    }

    @Test
    public void largeRepeatSetIsConsumedAndWrittenIncrementally() throws Exception {
        AtomicInteger consumed = new AtomicInteger();
        AtomicInteger outputLines = new AtomicInteger();
        Writer output = new Writer() {
            @Override
            public void write(char[] chars, int offset, int length) {
                for (int i = offset; i < offset + length; i++) {
                    if (chars[i] == '\n') {
                        outputLines.incrementAndGet();
                    }
                }
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        Iterator<ExportRecord> records = IntStream.range(0, 50_000).mapToObj(i -> {
            if (i > 10) {
                assertTrue("writer must not buffer a whole specimen's repeats", outputLines.get() >= i - 1);
            }
            consumed.incrementAndGet();
            return record(Integer.toString(i), "s1", null, hb.id(), Integer.toString(i));
        }).iterator();
        long rows = new ReportingCsvWriter().write(output, ReportingCsvWriter.Layout.SPREADSHEET,
                List.of(accession, hb), records);
        assertEquals(50_000, rows);
        assertEquals(50_000, consumed.get());
        assertEquals(50_001, outputLines.get());
    }
}
