package org.openelisglobal.localization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Locale;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openelisglobal.internationalization.UiMessagesWithoutOverrides;
import org.openelisglobal.localization.ShippedLabels.Origin;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.localization.valueholder.LocalizationValue;

/**
 * OGC-1507 BR-1: a value OpenELIS ships shows the lab's own label for the
 * language, else its key's translation, else the translation its configuration
 * file carried, else the key's English; a site's own value keeps its stored
 * labels.
 */
public class ShippedLabelsTest {

    /** Translated in fr.json ("Échec du contrôle qualité"). */
    private static final String TRANSLATED_KEY = "nce.type.qc_failure";
    /** Added for OGC-1507; Transifex has no French for it yet. */
    private static final String UNTRANSLATED_KEY = "nce.type.barcodeIssue";

    /** The catalogue without translation overrides, whatever context ran before. */
    @BeforeClass
    public static void catalogueWithoutOverrides() throws Exception {
        UiMessagesWithoutOverrides.install();
    }

    @AfterClass
    public static void restoreSpringContext() throws Exception {
        UiMessagesWithoutOverrides.restore();
    }

    @Test
    public void aShippedValueShowsItsKeysTranslationInTheUsersLanguage() {
        assertEquals("Échec du contrôle qualité",
                ShippedLabels.label(TRANSLATED_KEY, null, "QC failure", Locale.FRENCH));
        assertEquals("QC failure", ShippedLabels.label(TRANSLATED_KEY, null, "QC failure", Locale.ENGLISH));
    }

    @Test
    public void theKeysTranslationWinsOverTheConfigurationFilesAndTheBackfill() {
        Localization stored = stored(system("en", "QC failure"), system("fr", "Échec du CQ (fichier)"));

        assertEquals("Échec du contrôle qualité",
                ShippedLabels.label(TRANSLATED_KEY, stored, "QC failure", Locale.FRENCH));
    }

    @Test
    public void aSiteOverrideWinsForItsLanguageOnly() {
        Localization stored = stored(system("en", "QC failure"), site("fr", "Contrôle qualité non conforme"));

        assertEquals("Contrôle qualité non conforme",
                ShippedLabels.label(TRANSLATED_KEY, stored, "QC failure", Locale.FRENCH));
        assertEquals("English users still see the shipped English", "QC failure",
                ShippedLabels.label(TRANSLATED_KEY, stored, "QC failure", Locale.ENGLISH));
        assertEquals(Origin.SITE_OVERRIDE, ShippedLabels.origin(TRANSLATED_KEY, stored, "fr"));
        assertEquals(Origin.SHIPPED, ShippedLabels.origin(TRANSLATED_KEY, stored, "en"));
    }

    @Test
    public void aRegionalLabelWinsForItsRegionThenTheLanguagesOwn() {
        Localization stored = stored(system("en", "QC failure"), site("fr", "Contrôle qualité non conforme"),
                site("fr-HT", "Kontwòl kalite pa bon"));
        Locale haiti = Locale.forLanguageTag("fr-HT");

        assertEquals("Kontwòl kalite pa bon", ShippedLabels.label(TRANSLATED_KEY, stored, "QC failure", haiti));
        assertEquals("Contrôle qualité non conforme",
                ShippedLabels.label(TRANSLATED_KEY, stored, "QC failure", Locale.CANADA_FRENCH));
        assertEquals(Origin.SITE_OVERRIDE, ShippedLabels.origin(TRANSLATED_KEY, stored, "fr-HT"));
        assertEquals(Origin.SHIPPED, ShippedLabels.origin(TRANSLATED_KEY, stored(), "fr-HT"));
    }

    @Test
    public void withoutATranslationTheLabsEnglishShowsBeforeTheKeysEnglish() {
        Localization stored = stored(site("en", "Barcode unreadable"));

        assertEquals("Barcode unreadable",
                ShippedLabels.label(UNTRANSLATED_KEY, stored, "Barcode issue", Locale.FRENCH));
        assertEquals("a translation still wins", "Échec du contrôle qualité",
                ShippedLabels.label(TRANSLATED_KEY, stored, "QC failure", Locale.FRENCH));
        assertEquals("the shipped translation is still the key's", "Barcode Issue",
                ShippedLabels.shippedLabel(UNTRANSLATED_KEY, stored, Locale.FRENCH));
    }

    @Test
    public void aBlankSiteLabelIsNoOverride() {
        Localization stored = stored(site("fr", "  "));

        assertEquals("Échec du contrôle qualité",
                ShippedLabels.label(TRANSLATED_KEY, stored, "QC failure", Locale.FRENCH));
        assertEquals(Origin.SHIPPED, ShippedLabels.origin(TRANSLATED_KEY, stored, "fr"));
    }

    @Test
    public void withoutATranslationTheConfigurationFilesTranslationShowsThenEnglish() {
        Localization fromFile = stored(system("en", "Barcode Issue"), system("fr", "Problème de code-barres"));

        assertEquals("Problème de code-barres",
                ShippedLabels.label(UNTRANSLATED_KEY, fromFile, "Barcode Issue", Locale.FRENCH));
        assertEquals("Barcode Issue", ShippedLabels.label(UNTRANSLATED_KEY, null, "Barcode Issue", Locale.FRENCH));
        assertEquals("Barcode Issue", ShippedLabels.shippedLabel(UNTRANSLATED_KEY, stored(), Locale.FRENCH));
    }

    @Test
    public void theEnglishOfAShippedValueIsItsKeysEnglishNotItsStoredName() {
        Localization backfill = stored(system("en", "Barcode issue (old)"));

        assertEquals("Barcode Issue",
                ShippedLabels.label(UNTRANSLATED_KEY, backfill, "Barcode issue (old)", Locale.ENGLISH));
    }

    @Test
    public void aSiteValueKeepsItsStoredLabelsWhateverItsKeySays() {
        Localization stored = stored(site("en", "Lost in transit"), site("fr", "Perdu en route"));

        assertFalse(ShippedLabels.isShipped("nce.type.labOwnValue"));
        assertEquals("Perdu en route", ShippedLabels.label("nce.type.labOwnValue", stored, "Lost", Locale.FRENCH));
        assertEquals("falls back to English", "Lost in transit",
                ShippedLabels.label(null, stored, "Lost", Locale.of("es")));
        assertEquals("then to the name", "Lost", ShippedLabels.label(null, null, "Lost", Locale.FRENCH));
        assertEquals(Origin.SITE_VALUE, ShippedLabels.origin(null, stored, "fr"));
    }

    @Test
    public void aKeyIsShippedOnlyWhenTheEnglishCatalogueHoldsIt() {
        assertTrue(ShippedLabels.isShipped(TRANSLATED_KEY));
        assertTrue(ShippedLabels.isShipped(" " + TRANSLATED_KEY + " "));
        assertFalse(ShippedLabels.isShipped(""));
        assertFalse(ShippedLabels.isShipped(null));
    }

    private static Localization stored(LocalizationValue... values) {
        Localization localization = new Localization();
        for (LocalizationValue value : values) {
            value.setLocalization(localization);
            localization.getValues().put(value.getLocale(), value);
        }
        return localization;
    }

    private static LocalizationValue site(String locale, String value) {
        LocalizationValue lv = new LocalizationValue(locale, value);
        lv.setOrigin(LocalizationValue.ORIGIN_SITE);
        return lv;
    }

    private static LocalizationValue system(String locale, String value) {
        LocalizationValue lv = new LocalizationValue(locale, value);
        lv.setOrigin(LocalizationValue.ORIGIN_SYSTEM);
        return lv;
    }
}
