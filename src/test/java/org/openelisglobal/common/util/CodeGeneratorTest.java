package org.openelisglobal.common.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Set;
import org.junit.Test;
import org.openelisglobal.common.exception.LocalizedValidationException;

public class CodeGeneratorTest {

    @Test
    public void prefixFor_takesThreeLettersThenUpToTwoDigitTokens() {
        assertEquals("PAR-500MG", CodeGenerator.prefixFor("Paracetamol 500mg Tablets"));
        assertEquals("SOD-09-500ML", CodeGenerator.prefixFor("Sodium Chloride 0.9% 500mL"));
        assertEquals("HIV-12", CodeGenerator.prefixFor("HIV 1/2 Rapid Test Kit"));
    }

    @Test
    public void prefixFor_usesFirstTokenWithALetter_evenWhenADigitTokenComesFirst() {
        assertEquals("INC-12", CodeGenerator.prefixFor("1/2 inch tubing"));
    }

    @Test
    public void prefixFor_doesNotReuseTheLetterSourceWordAsADigitWord() {
        assertEquals("VR-500MG", CodeGenerator.prefixFor("VR3 Paracetamol 500mg Tablets"));
        assertEquals("ML", CodeGenerator.prefixFor("2mL Tube"));
    }

    @Test
    public void prefixFor_fallsBackToItem_whenNoTokenHasALetter() {
        assertEquals("ITEM", CodeGenerator.prefixFor("   "));
        assertEquals("ITEM", CodeGenerator.prefixFor(null));
        assertEquals("ITEM-12345", CodeGenerator.prefixFor("12345"));
    }

    @Test
    public void prefixFor_stopsAfterTwoDigitTokens() {
        assertEquals("TUB-1-2", CodeGenerator.prefixFor("Tube 1 2 3"));
    }

    @Test
    public void prefixFor_capsEachDigitTokenAtEightCharacters() {
        assertEquals("KIT-12345678", CodeGenerator.prefixFor("Kit 123456789ABC"));
    }

    @Test
    public void normalize_joinsWithHyphens_andStripsThemFromTheEnds() {
        assertEquals("MY-REAGENT-1", CodeGenerator.normalize(" my reagent 1 ", 64));
        assertEquals("MY-CODE", CodeGenerator.normalize("_my-code!_", 64));
    }

    @Test
    public void normalize_truncatesWithoutLeavingATrailingHyphen() {
        assertEquals("AB-CD", CodeGenerator.normalize("ab-cd-ef", 6));
    }

    @Test
    public void normalize_rejectsAValueWithNoLetterOrDigit() {
        try {
            CodeGenerator.normalize("___", 64);
            fail("Expected a LocalizedValidationException");
        } catch (LocalizedValidationException e) {
            assertEquals("common.codeGenerator.error.invalidCode", e.getErrorCode());
        }
    }

    @Test
    public void generateFromName_suffixesCollisionsWithAHyphen() {
        Set<String> taken = Set.of("TAQ-DNA-20260918", "TAQ-DNA-20260918-2");

        assertEquals("TAQ-DNA-20260918-3",
                CodeGenerator.generateFromName("TAQ-DNA-20260918", 64, "LOT", taken::contains));
    }
}
