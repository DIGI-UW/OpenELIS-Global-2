package org.openelisglobal.siteinformation.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;

/**
 * The General Configuration menus describe each setting through its instruction
 * key. A key with no message used to be shown raw ("siteInformation.instruction
 * .validate.all" for "validate all results") because the message source answers
 * a missing key with the key itself; such a key now falls through to the
 * description key, then the stored description.
 */
public class SiteInformationInstructionsTest extends BaseWebContextSensitiveTest {

    private SiteInformation setting(String instructionKey, String descriptionKey, String description) {
        SiteInformation siteInformation = new SiteInformation();
        siteInformation.setInstructionKey(instructionKey);
        siteInformation.setDescriptionKey(descriptionKey);
        siteInformation.setDescription(description);
        return siteInformation;
    }

    @Test
    public void getMessageIfPresent_returnsNullForAMissingOrBlankKey() {
        assertNull(MessageUtil.getMessageIfPresent("siteInformation.instruction.validate.all"));
        assertNull(MessageUtil.getMessageIfPresent(""));
        assertNull(MessageUtil.getMessageIfPresent(null));
    }

    @Test
    public void anInstructionKeyWithAMessageDescribesTheSetting() {
        String described = SiteInformationInstructions
                .describe(setting("instructions.results.technician", null, "stored description"));

        assertTrue(described, described.startsWith("If true then the techincians name"));
    }

    @Test
    public void aMissingInstructionKeyFallsBackToTheStoredDescription() {
        assertEquals("all results should be validated even if normal",
                SiteInformationInstructions.describe(setting("siteInformation.instruction.validate.all", null,
                        "all results should be validated even if normal")));
    }

    @Test
    public void aMissingInstructionKeyFallsBackToTheDescriptionKeyFirst() {
        String described = SiteInformationInstructions.describe(
                setting("no.such.instruction.key", "siteInformation.instruction.headerInfo", "stored description"));

        assertTrue(described, described.startsWith("This text will appear in the header"));
    }

    @Test
    public void noKeysAtAllUseTheStoredDescription() {
        assertEquals("stored description", SiteInformationInstructions.describe(setting("", "", "stored description")));
    }
}
