package org.openelisglobal.testconfiguration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.testconfiguration.form.ResultSelectListRenameForm;
import org.openelisglobal.testconfiguration.service.ResultSelectListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Renaming a result list option in both languages.
 *
 * <p>
 * The option list carries one name per option — whichever locale it was read in
 * — so the rename screen had no way to know what the other languages said. It
 * sent the value it was displaying as every language, which meant renaming in
 * English quietly replaced the French translation with the English text.
 *
 * <p>
 * Reading the stored translations back is what makes sending both honest, so
 * both directions are covered here.
 */
public class ResultSelectListRenameTest extends BaseWebContextSensitiveTest {

    private static final long LOCALIZED = 96611L;
    private static final long UNLOCALIZED = 96612L;
    private static final long LOCALIZATION = 96613L;
    private static final String ABSENT = "9661499";

    @Autowired
    private ResultSelectListService resultSelectListService;

    @Autowired
    private DictionaryService dictionaryService;

    @Autowired
    private javax.sql.DataSource dataSource;

    private JdbcTemplate jdbc;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        cleanup();

        Long categoryId = jdbc.queryForObject("SELECT min(id) FROM clinlims.dictionary_category", Long.class);

        jdbc.update("INSERT INTO clinlims.localization (id, description, lastupdated) VALUES (?, ?, NOW())",
                LOCALIZATION, "dictionary name");
        for (String[] value : new String[][] { { "en", "Reactive" }, { "fr", "Reactif" } }) {
            jdbc.update(
                    "INSERT INTO clinlims.localization_value (id, localization_id, locale, value, last_updated)"
                            + " VALUES (nextval('clinlims.localization_value_seq'), ?, ?, ?, NOW())",
                    LOCALIZATION, value[0], value[1]);
        }

        jdbc.update(
                "INSERT INTO clinlims.dictionary (id, is_active, dict_entry, local_abbrev, dictionary_category_id,"
                        + " sort_order, name_localization_id, lastupdated) VALUES (?, 'Y', ?, ?, ?, 1, ?, NOW())",
                LOCALIZED, "RenameTestReactive", "RenameTestReactive", categoryId, LOCALIZATION);
        jdbc.update(
                "INSERT INTO clinlims.dictionary (id, is_active, dict_entry, local_abbrev,"
                        + " dictionary_category_id, sort_order, lastupdated) VALUES (?, 'Y', ?, ?, ?, 2, NOW())",
                UNLOCALIZED, "RenameTestNonReactive", "RenameTestNonReactive", categoryId);
    }

    @After
    public void tearDown() {
        cleanup();
    }

    // ── reading what is stored, so the screen can prefill both fields ──────────

    @Test
    public void bothStoredTranslationsCanBeRead() {
        Localization localization = resultSelectListService
                .getLocalizationForResultSelectOption(String.valueOf(LOCALIZED));

        assertNotNull(localization);
        assertEquals("Reactive", localization.getLocalizedValue("en"));
        assertEquals("Reactif", localization.getLocalizedValue("fr"));
    }

    @Test
    public void anOptionWithNothingStoredReadsAsNone() {
        assertNull(resultSelectListService.getLocalizationForResultSelectOption(String.valueOf(UNLOCALIZED)));
        assertNull(resultSelectListService.getLocalizationForResultSelectOption(ABSENT));
        assertNull(resultSelectListService.getLocalizationForResultSelectOption(""));
    }

    // ── writing both back ─────────────────────────────────────────────────────

    @Test
    public void renamingStoresBothLanguages() {
        assertTrue(resultSelectListService.renameOption(form(LOCALIZED, "Detected", "Detecte"), "1"));

        Localization after = resultSelectListService.getLocalizationForResultSelectOption(String.valueOf(LOCALIZED));
        assertEquals("Detected", after.getLocalizedValue("en"));
        assertEquals("Detecte", after.getLocalizedValue("fr"));
    }

    @Test
    public void renamingOnlyTheFrenchLeavesTheEnglishAlone() {
        assertTrue(resultSelectListService.renameOption(form(LOCALIZED, "", "Detecte"), "1"));

        Localization after = resultSelectListService.getLocalizationForResultSelectOption(String.valueOf(LOCALIZED));
        assertEquals("the English translation survives", "Reactive", after.getLocalizedValue("en"));
        assertEquals("Detecte", after.getLocalizedValue("fr"));
    }

    @Test
    public void renamingOnlyTheEnglishLeavesTheFrenchAlone() {
        // The case that used to lose data: the screen sent the English text as the
        // French name too.
        assertTrue(resultSelectListService.renameOption(form(LOCALIZED, "Detected", ""), "1"));

        Localization after = resultSelectListService.getLocalizationForResultSelectOption(String.valueOf(LOCALIZED));
        assertEquals("Detected", after.getLocalizedValue("en"));
        assertEquals("the French translation survives", "Reactif", after.getLocalizedValue("fr"));
    }

    @Test
    public void renamingAnOptionThatHadNoTranslationsStoresThem() {
        assertTrue(resultSelectListService.renameOption(form(UNLOCALIZED, "Not Detected", "Non Detecte"), "1"));

        Localization after = resultSelectListService.getLocalizationForResultSelectOption(String.valueOf(UNLOCALIZED));
        assertNotNull("an unattached localization leaves the option renamed in name only", after);
        assertEquals("Not Detected", after.getLocalizedValue("en"));
        assertEquals("Non Detecte", after.getLocalizedValue("fr"));
    }

    @Test
    public void renamingAlsoUpdatesTheDictionaryEntry() {
        resultSelectListService.renameOption(form(LOCALIZED, "Detected", "Detecte"), "1");

        assertEquals("Detected", dictionaryService.getDictionaryById(String.valueOf(LOCALIZED)).getDictEntry());
    }

    private ResultSelectListRenameForm form(long dictionaryId, String english, String french) {
        ResultSelectListRenameForm form = new ResultSelectListRenameForm();
        form.setResultSelectOptionId(String.valueOf(dictionaryId));
        form.setNameEnglish(english);
        form.setNameFrench(french);
        return form;
    }

    private void cleanup() {
        // Collected before the dictionary rows go, so a localization a rename
        // attached to the untranslated option is removed with the fixture's own.
        List<Long> localizationIds = jdbc.queryForList(
                "SELECT name_localization_id FROM clinlims.dictionary"
                        + " WHERE id IN (?, ?) AND name_localization_id IS NOT NULL",
                Long.class, LOCALIZED, UNLOCALIZED);
        jdbc.update("DELETE FROM clinlims.dictionary WHERE id IN (?, ?)", LOCALIZED, UNLOCALIZED);
        for (Long id : localizationIds) {
            jdbc.update("DELETE FROM clinlims.localization WHERE id = ?", id);
        }
        jdbc.update("DELETE FROM clinlims.localization WHERE id = ?", LOCALIZATION);
    }
}
