package org.openelisglobal.localization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Locale;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.service.LocalizationServiceImpl;
import org.openelisglobal.localization.valueholder.Localization;
import org.springframework.beans.factory.annotation.Autowired;

public class LocalizationServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private LocalizationService localizationService;

    @Before
    public void setup() throws Exception {
        executeDataSetWithStateManagement("testdata/localization.xml");
    }

    @Test
    public void getLocalizationById_shouldReturnCorrectLocalization() {
        Localization localization = localizationService.get("1");
        assertNotNull("Localization should exist in dataset", localization);
        assertEquals("1", localization.getId());
    }

    @Test
    public void getLocalizedValue_shouldReturnCorrectTranslation() {
        Localization localization = localizationService.get("1");
        assertNotNull("Localization should exist", localization);

        String englishValue = localization.getLocalizedValue(Locale.ENGLISH);
        assertNotNull("English value should exist", englishValue);

        String frenchValue = localization.getLocalizedValue(Locale.FRENCH);
        assertNotNull("French value should exist", frenchValue);
    }

    @Test
    public void languageChanged_shouldReturnTrueWhenLanguagesDiffer() {
        Localization localizationOld = new Localization();
        localizationOld.setEnglish("Hello");
        localizationOld.setFrench("Bonjour");

        Localization localizationNew = new Localization();
        localizationNew.setEnglish("Hi");
        localizationNew.setFrench("Salut");

        boolean result = localizationService.languageChanged(localizationOld, localizationNew);

        assertTrue("Language should have changed", result);
    }

    @Test
    public void languageChanged_shouldReturnFalseWhenLanguagesAreSame() {
        Localization localizationOld = new Localization();
        localizationOld.setEnglish("Hello");
        localizationOld.setFrench("Bonjour");

        Localization localizationNew = new Localization();
        localizationNew.setEnglish("Hello");
        localizationNew.setFrench("Bonjour");

        boolean result = localizationService.languageChanged(localizationOld, localizationNew);

        assertFalse("Language should not have changed", result);
    }

    @Test
    public void getLocalizedValueById_shouldReturnCorrectLocalizedValue() {
        String localizedValue = localizationService.getLocalizedValueById("1");
        assertNotNull("Localized value should not be null", localizedValue);
        assertEquals("Localized value should match", "Hello", localizedValue);
    }

    @Test
    public void getAllActiveLocales_shouldReturnExpectedLocales() {
        List<Locale> locales = localizationService.getAllActiveLocales();
        assertNotNull("Locales should not be null", locales);
        assertEquals("There should be 2 active locales", 2, locales.size());
        assertTrue("Locales should contain English", locales.contains(Locale.ENGLISH));
        assertTrue("Locales should contain French", locales.contains(Locale.FRENCH));
    }

    @Test
    public void getCurrentLocale_shouldReturnCorrectLocale() {
        Locale.setDefault(Locale.US);
        Locale currentLocale = localizationService.getCurrentLocale();
        assertNotNull("Current locale should not be null", currentLocale);
        assertEquals("Locale should be en_US", Locale.US, currentLocale);
    }

    @Test
    public void insert_shouldInsertNewLocalization() {
        Localization localization = new Localization();
        localization.setEnglish("New English");
        localization.setFrench("Nouveau Français");

        String id = localizationService.insert(localization);

        assertNotNull("ID should not be null", id);
        Localization insertedLocalization = localizationService.get(id);
        assertEquals("New English", insertedLocalization.getEnglish());
        assertEquals("Nouveau Français", insertedLocalization.getFrench());
    }

    @Test
    public void getDBDescription_shouldReturnCorrectDescription() {
        assertEquals("test name", LocalizationServiceImpl.LocalizationType.TEST_NAME.getDBDescription());
        assertEquals("test report name",
                LocalizationServiceImpl.LocalizationType.REPORTING_TEST_NAME.getDBDescription());
        assertEquals("Site information banner test",
                LocalizationServiceImpl.LocalizationType.BANNER_LABEL.getDBDescription());

    }

    @Test
    public void getFrench_shouldReturnValueForFranceLocale() {
        Localization loc = new Localization();
        loc.setLocalizedValue(Locale.FRANCE, "Bonjour FRANCE");
        assertEquals("Bonjour FRANCE", loc.getFrench());
    }

    @Test
    public void getFrench_shouldReturnValueForFrenchLocale() {
        Localization loc = new Localization();
        loc.setLocalizedValue(Locale.FRENCH, "Bonjour FRENCH");
        assertEquals("Bonjour FRENCH", loc.getFrench());
    }

    @Test
    public void getFrench_shouldReturnEmptyIfNoValue() {
        Localization loc = new Localization();
        assertEquals("", loc.getFrench());
    }

    @Test
    public void getLocalizedValue_shouldReturnSecondaryLocaleValueIfPrimaryMissing() {
        Localization loc = new Localization();
        loc.setLocalizedValue(Locale.ENGLISH, "Hello");
        assertEquals("Hello", loc.getLocalizedValue(Locale.UK));
    }

    @Test
    public void getFrench_shouldReturnFranceValueIfAvailable() {
        Localization loc = new Localization();
        loc.setLocalizedValue(Locale.FRANCE, "Bonjour FRANCE");
        assertEquals("Bonjour FRANCE", loc.getFrench());
    }

    @Test
    public void getFrench_shouldFallbackToFrench() {
        Localization loc = new Localization();
        loc.setLocalizedValue(Locale.FRENCH, "Bonjour FRENCH");
        assertEquals("Bonjour FRENCH", loc.getFrench());
    }

    @Test
    public void getLocalizedValue_shouldReturnPrimaryLocaleValue() {
        Localization loc = new Localization();
        loc.setLocalizedValue(Locale.CANADA_FRENCH, "Salut");
        assertEquals("Salut", loc.getLocalizedValue(Locale.CANADA_FRENCH));
    }

    @Test
    public void getLocalizedValue_shouldFallbackToLanguageOnlyLocale() {
        Localization loc = new Localization();
        loc.setLocalizedValue(Locale.FRENCH, "Bonjour");
        assertEquals("Bonjour", loc.getLocalizedValue(Locale.CANADA_FRENCH));
    }

    @Test
    public void getLocalizedValue_shouldReturnEmptyIfNoValue() {
        Localization loc = new Localization();
        assertEquals("", loc.getLocalizedValue(Locale.CANADA_FRENCH));
    }
}