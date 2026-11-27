package org.openelisglobal.program.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

/**
 * The critical starts an administrator types are read as upper-case starts and
 * match a code by its beginning, so a diagnosis is marked the same way whatever
 * the case or the spaces around the starts.
 */
public class DiagnosisCodeStartsTest {

    @Test
    public void starts_areTrimmedAndCaseBlind_andAnEmptySettingMatchesNothing() {
        // K1: a missing, an empty and a setting of empty items hold no start
        for (String setting : new String[] { null, "", " ", ", ," }) {
            List<String> none = DiagnosisCodeStarts.parse(setting);
            assertEquals("no start in [" + setting + "]", List.of(), none);
            assertFalse("so no code matches", DiagnosisCodeStarts.matches(none, "C80.1"));
        }

        // K3: spaces and case are ignored, and the order typed is kept
        List<String> starts = DiagnosisCodeStarts.parse(" c , d0 ");
        assertEquals(DiagnosisCodeStarts.parse("C,D0"), starts);
        assertEquals(List.of("C", "D0"), starts);

        // K2: a code matches by its beginning, typed in either case
        assertTrue(DiagnosisCodeStarts.matches(starts, "C80.1"));
        assertTrue("the code's case is ignored", DiagnosisCodeStarts.matches(starts, "c80.1"));
        assertTrue("so are spaces around the code", DiagnosisCodeStarts.matches(starts, "  d04.2 "));
        assertFalse("a start inside the code is not its beginning", DiagnosisCodeStarts.matches(starts, "K29.5"));
        assertFalse(DiagnosisCodeStarts.matches(starts, "K0D0"));

        // K4: an ICD-O code and a missing code are not caught by a letter start
        assertFalse(DiagnosisCodeStarts.matches(starts, "8500/3"));
        assertFalse(DiagnosisCodeStarts.matches(starts, null));
        assertFalse(DiagnosisCodeStarts.matches(starts, ""));
        assertFalse(DiagnosisCodeStarts.matches(starts, "   "));
        assertFalse("no starts, no match", DiagnosisCodeStarts.matches(List.of(), "C80.1"));
    }
}
