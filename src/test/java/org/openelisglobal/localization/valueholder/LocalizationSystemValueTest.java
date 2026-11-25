package org.openelisglobal.localization.valueholder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * OGC-1507: a label OpenELIS writes from a configuration file never replaces a
 * lab's own label on a shipped value, and is recorded as OpenELIS's; a label an
 * editor changes is the lab's own.
 */
public class LocalizationSystemValueTest {

    @Test
    public void aNewLabelIsRecordedAsWrittenByOpenElis() {
        Localization localization = new Localization();
        localization.setSystemValue("fr", "Problème de transport", true);

        assertEquals(LocalizationValue.ORIGIN_SYSTEM, localization.getValues().get("fr").getOrigin());
        assertEquals("Problème de transport", localization.getSystemValue("fr"));
        assertNull(localization.getSiteValue("fr"));
    }

    @Test
    public void aShippedValueKeepsTheLabsOwnLabel() {
        Localization localization = new Localization();
        localization.setLocalizedValue("fr", "Souci de transport");

        localization.setSystemValue("fr", "Problème de transport", true);

        assertEquals("Souci de transport", localization.getSiteValue("fr"));
        assertEquals(LocalizationValue.ORIGIN_SITE, localization.getValues().get("fr").getOrigin());
    }

    @Test
    public void aLabEditThatReadsTheSameAsTheFileBecomesOpenElissOwn() {
        Localization localization = new Localization();
        localization.setLocalizedValue("fr", "Problème de transport");

        localization.setSystemValue("fr", "Problème de transport", true);

        assertEquals(LocalizationValue.ORIGIN_SYSTEM, localization.getValues().get("fr").getOrigin());
    }

    @Test
    public void aSiteValueIsRewrittenFromItsFileAsBefore() {
        Localization localization = new Localization();
        localization.setLocalizedValue("fr", "Ancien libellé");

        localization.setSystemValue("fr", "Nouveau libellé", false);

        assertEquals("Nouveau libellé", localization.getValues().get("fr").getValue());
        assertEquals(LocalizationValue.ORIGIN_SYSTEM, localization.getValues().get("fr").getOrigin());
    }

    @Test
    public void anEditorsRenameOfAShippedLabelBecomesTheLabsOwn() {
        Localization localization = new Localization();
        localization.setSystemValue("en", "Container intact / undamaged", true);

        localization.setLocalizedValue("en", "Container intact");

        assertEquals("Container intact", localization.getSiteValue("en"));
        assertEquals(LocalizationValue.ORIGIN_SITE, localization.getValues().get("en").getOrigin());
    }

    @Test
    public void anEditorSavingTheSameTextLeavesTheShippedLabelAlone() {
        Localization localization = new Localization();
        localization.setSystemValue("fr", "Récipient intact", true);

        localization.setLocalizedValue("fr", "Récipient intact");

        assertEquals(LocalizationValue.ORIGIN_SYSTEM, localization.getValues().get("fr").getOrigin());
        assertNull(localization.getSiteValue("fr"));
    }

    @Test
    public void aLabLabelKeepsTheFilesLabelForAReset() {
        Localization localization = new Localization();
        localization.setSystemValue("fr", "Demande", true);

        localization.setLocalizedValue("fr", "Ordre labo");

        assertEquals("Ordre labo", localization.getSiteValue("fr"));
        assertEquals("Demande", localization.getSystemValue("fr"));
    }

    @Test
    public void aLoaderUpdatesTheKeptLabelUnderTheLabsOwn() {
        Localization localization = new Localization();
        localization.setSystemValue("fr", "Commande", true);
        localization.setLocalizedValue("fr", "Ordre labo");

        localization.setSystemValue("fr", "Demande", true);

        assertEquals("Ordre labo", localization.getSiteValue("fr"));
        assertEquals("Demande", localization.getSystemValue("fr"));
    }

    @Test
    public void aResetDropsTheKeptCopy() {
        Localization localization = new Localization();
        localization.setSystemValue("fr", "Demande", true);
        localization.setLocalizedValue("fr", "Ordre labo");

        localization.setSystemValue("fr", "Demande", false);

        assertNull(localization.getSiteValue("fr"));
        assertEquals("Demande", localization.getSystemValue("fr"));
        assertNull(localization.getValues().get("fr").getSystemValue());
    }
}
