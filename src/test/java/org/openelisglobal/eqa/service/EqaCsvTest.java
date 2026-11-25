package org.openelisglobal.eqa.service;

import static org.junit.Assert.assertEquals;

import java.util.List;
import org.junit.Test;

/**
 * The EQA exchange files are written with the spreadsheet formula guard, which
 * puts an apostrophe before a value starting with = + - or @. A reader takes
 * the value back without it, or a qualitative "+" arrives as "'+" (OGC-1665
 * review).
 */
public class EqaCsvTest {

    @Test
    public void aGuardedValueIsReadBackAsItWasWritten() {
        assertEquals("+", EqaCsv.cell(List.of("'+"), 0));
        assertEquals("-", EqaCsv.cell(List.of(" '- "), 0));
        assertEquals("=1+1", EqaCsv.cell(List.of("'=1+1"), 0));
        assertEquals("@home", EqaCsv.cell(List.of("'@home"), 0));
    }

    @Test
    public void anApostropheThatGuardsNothingIsKept() {
        assertEquals("'Reactive'", EqaCsv.cell(List.of("'Reactive'"), 0));
        assertEquals("'", EqaCsv.cell(List.of("'"), 0));
        assertEquals("Reactive", EqaCsv.cell(List.of("Reactive"), 0));
        assertEquals("", EqaCsv.cell(List.of("x"), 3));
    }
}
