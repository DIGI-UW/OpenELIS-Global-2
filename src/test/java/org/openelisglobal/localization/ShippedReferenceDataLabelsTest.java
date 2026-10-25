package org.openelisglobal.localization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.qaevent.service.NceDisplayKeyLinker;
import org.openelisglobal.sampleacceptance.service.SampleAcceptanceChecklistService;
import org.openelisglobal.testconfiguration.form.ResultSelectListRenameForm;
import org.openelisglobal.testconfiguration.service.ResultSelectListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.ResultActions;

/**
 * OGC-1507 end to end on the database: existing NCE rows are linked to their
 * shipped keys by name on upgrade, the NCE lists show each value in the
 * language of the request, an administrator's override wins for its language
 * only and can be returned to the shipped translation, and a shipped dictionary
 * entry reads the same way. The rows here are this test's own and are removed
 * afterwards; the keys of the base data's NCE rows are put back as they were.
 */
public class ShippedReferenceDataLabelsTest extends BaseWebContextSensitiveTest {

    private static final int CATEGORY = 98701;
    private static final int LINKED_BY_NAME = 98701;
    private static final int SITE_OWN = 98702;
    private static final int DICTIONARY = 98701;

    @Autowired
    private NceDisplayKeyLinker linker;
    @Autowired
    private DictionaryService dictionaryService;
    @Autowired
    private SampleAcceptanceChecklistService checklistService;
    @Autowired
    private ResultSelectListService resultSelectListService;

    private final ObjectMapper json = new ObjectMapper();
    private List<Map<String, Object>> baseTypeKeys;
    private List<Map<String, Object>> baseCategoryKeys;

    @Before
    public void insertRows() {
        baseTypeKeys = jdbcTemplate.queryForList("SELECT id, display_key FROM clinlims.nce_type WHERE id < 98700");
        baseCategoryKeys = jdbcTemplate
                .queryForList("SELECT id, display_key FROM clinlims.nce_category WHERE id < 98700");
        localization(98701, "NCE Category: Post analytical", Map.of("en", "Post analytical"), "SYSTEM");
        localization(98702, "NCE Type: Sample collection error",
                Map.of("en", "Sample collection error", "fr", "Erreur de prélèvement (fichier)"), "SYSTEM");
        localization(98703, "NCE Type: Ward courier lost it", Map.of("en", "Ward courier lost it"), "SYSTEM");
        localization(98704, "dictionary entry: Positive", Map.of("en", "Positive", "fr", "Positif"), "SYSTEM");
        jdbcTemplate.update("INSERT INTO clinlims.nce_category (id, name, display_key, active, last_updated,"
                + " name_localization_id) VALUES (?, 'Post analytical', 'nce.category.postanalytical', true, now(),"
                + " 98701)", CATEGORY);
        jdbcTemplate.update("INSERT INTO clinlims.nce_type (id, name, display_key, category_id, active, last_updated,"
                + " name_localization_id) VALUES (?, 'Sample collection error', 'nce.type.sampleCollectionError', ?,"
                + " true, now(), 98702)", LINKED_BY_NAME, CATEGORY);
        jdbcTemplate.update(
                "INSERT INTO clinlims.nce_type (id, name, display_key, category_id, active, last_updated,"
                        + " name_localization_id) VALUES (?, 'Ward courier lost it', NULL, ?, true, now(), 98703)",
                SITE_OWN, CATEGORY);
        jdbcTemplate.update("INSERT INTO clinlims.dictionary_category (id, description, lastupdated, local_abbrev,"
                + " name) VALUES (98701, 'Test Results', now(), 'TR987', 'Test Results 98701')");
        jdbcTemplate.update("INSERT INTO clinlims.dictionary (id, is_active, dict_entry, lastupdated, local_abbrev,"
                + " dictionary_category_id, display_key, sort_order, name_localization_id) VALUES (?, 'Y', 'Positive',"
                + " now(), 'POS987', 98701, 'dictionary.testResults.positive', 1, 98704)", DICTIONARY);
    }

    @After
    public void removeRows() {
        jdbcTemplate.update("DELETE FROM clinlims.supported_locale WHERE id = 98701");
        jdbcTemplate.update("DELETE FROM clinlims.dictionary WHERE id = ?", DICTIONARY);
        jdbcTemplate.update("DELETE FROM clinlims.dictionary_category WHERE id = 98701");
        jdbcTemplate.update("DELETE FROM clinlims.nce_type WHERE id IN (?, ?)", LINKED_BY_NAME, SITE_OWN);
        jdbcTemplate.update("DELETE FROM clinlims.nce_category WHERE id = ?", CATEGORY);
        jdbcTemplate.update("DELETE FROM clinlims.localization_value WHERE localization_id BETWEEN 98701 AND 98704");
        jdbcTemplate.update("DELETE FROM clinlims.localization WHERE id BETWEEN 98701 AND 98704");
        for (Map<String, Object> row : baseTypeKeys) {
            jdbcTemplate.update("UPDATE clinlims.nce_type SET display_key = ? WHERE id = ?", row.get("display_key"),
                    row.get("id"));
        }
        for (Map<String, Object> row : baseCategoryKeys) {
            jdbcTemplate.update("UPDATE clinlims.nce_category SET display_key = ? WHERE id = ?", row.get("display_key"),
                    row.get("id"));
        }
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    public void theUpgradeLinksRowsByNameAndLeavesTheSitesOwnValues() {
        Map<String, NceDisplayKeyLinker.Report> reports = linker.link();

        assertEquals("nce.type.sample_collection_error", key("nce_type", LINKED_BY_NAME));
        assertEquals("unmatched rows keep their key", null, key("nce_type", SITE_OWN));
        assertTrue(reports.get("NCE types").unmatched.contains("Ward courier lost it"));
        assertTrue(reports.get("NCE types").linkedByName >= 1);

        Map<String, NceDisplayKeyLinker.Report> again = linker.link();
        assertEquals("a second start links nothing", 0,
                again.get("NCE types").linkedByName + again.get("NCE types").linkedByAlias);
        assertEquals("nce.type.sample_collection_error", key("nce_type", LINKED_BY_NAME));
    }

    @Test
    public void theNceListsShowEachShippedValueInTheLanguageOfTheRequest() throws Exception {
        linker.link();

        JsonNode french = categories("fr-FR");
        assertEquals("Post-analytique", french.get("name").asText());
        assertEquals("nce.category.postanalytical", french.get("displayKey").asText());
        assertEquals("Transifex wins over the configuration file's French", "Erreur de prélèvement de l'échantillon",
                type(french, LINKED_BY_NAME).get("name").asText());
        assertEquals("the site's own value keeps its stored label", "Ward courier lost it",
                type(french, SITE_OWN).get("name").asText());

        JsonNode english = categories("en-US");
        assertEquals("Post-analytical", english.get("name").asText());
        assertEquals("Sample collection error", type(english, LINKED_BY_NAME).get("name").asText());
    }

    @Test
    public void anOverrideWinsForItsLanguageOnlyCannotBeBlankAndResetsToTheShippedTranslation() throws Exception {
        linker.link();
        JsonNode before = localization(98702);
        assertEquals("nce.type.sample_collection_error", before.get("displayKey").asText());
        assertEquals("SHIPPED", before.get("origins").get("fr").asText());
        assertEquals("Erreur de prélèvement de l'échantillon", before.get("shippedLabels").get("fr").asText());

        ok(mockMvc.perform(put("/rest/localizations/98702/translations").contentType(MediaType.APPLICATION_JSON)
                .content("{\"fr\":\"Prélèvement mal fait\"}")));
        assertEquals("SITE_OVERRIDE", localization(98702).get("origins").get("fr").asText());
        assertEquals("Prélèvement mal fait", type(categories("fr-FR"), LINKED_BY_NAME).get("name").asText());
        assertEquals("English users are not affected", "Sample collection error",
                type(categories("en-US"), LINKED_BY_NAME).get("name").asText());

        String refused = mockMvc
                .perform(put("/rest/localizations/98702/translations").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fr\":\"  \"}"))
                .andExpect(status().isBadRequest()).andReturn().getResponse().getContentAsString();
        assertTrue(refused, refused.contains("cannot be blank"));
        assertEquals("Prélèvement mal fait", type(categories("fr-FR"), LINKED_BY_NAME).get("name").asText());

        ok(mockMvc.perform(post("/rest/localizations/98702/translations/fr/reset")));
        assertEquals("SHIPPED", localization(98702).get("origins").get("fr").asText());
        assertEquals("Erreur de prélèvement de l'échantillon",
                type(categories("fr-FR"), LINKED_BY_NAME).get("name").asText());
        assertEquals("SYSTEM", jdbcTemplate.queryForObject(
                "SELECT origin FROM clinlims.localization_value" + " WHERE localization_id = 98702 AND locale = 'fr'",
                String.class));

        mockMvc.perform(post("/rest/localizations/98703/translations/fr/reset")).andExpect(status().isBadRequest());
        assertEquals("SITE_VALUE", localization(98703).get("origins").get("fr").asText());
    }

    @Test
    public void aShippedDictionaryEntryReadsThroughItsKey() {
        Dictionary positive = dictionaryService.get(String.valueOf(DICTIONARY));

        LocaleContextHolder.setLocale(Locale.FRENCH);
        assertEquals("the file's French while Transifex has none", "Positif", positive.getLocalizedName());
        assertEquals("SHIPPED", positive.getLabelOrigin());
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        assertEquals("Positive", positive.getLocalizedName());

        jdbcTemplate.update("UPDATE clinlims.localization_value SET value = 'Positive (old list)'"
                + " WHERE localization_id = 98704 AND locale = 'en'");
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        assertEquals("the key's English, not the stored backfill", "Positive",
                dictionaryService.get(String.valueOf(DICTIONARY)).getLocalizedName());

        jdbcTemplate.update("UPDATE clinlims.localization_value SET value = 'Positif (+)', origin = 'SITE'"
                + " WHERE localization_id = 98704 AND locale = 'fr'");
        Dictionary overridden = dictionaryService.get(String.valueOf(DICTIONARY));
        LocaleContextHolder.setLocale(Locale.FRENCH);
        assertEquals("Positif (+)", overridden.getLocalizedName());
        assertEquals("Positif (+)", overridden.getDisplayValue());
        assertEquals("SITE_OVERRIDE", overridden.getLabelOrigin());
    }

    @Test
    public void aRenameInTheChecklistEditorShowsAndOnlyForItsLanguage() {
        checklistService.updateItem(String.valueOf(DICTIONARY), "Positive, confirmed", true, "1");

        Dictionary renamed = dictionaryService.get(String.valueOf(DICTIONARY));
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        assertEquals("Positive, confirmed", renamed.getLocalizedName());
        assertEquals("SITE_OVERRIDE", renamed.getLabelOrigin());
        LocaleContextHolder.setLocale(Locale.FRENCH);
        assertEquals("Positif", renamed.getLocalizedName());
        assertEquals("SHIPPED", renamed.getLabelOrigin());
    }

    @Test
    public void aRenameInTestManagementShowsAndAnUnchangedLanguageStaysShipped() throws Exception {
        ResultSelectListRenameForm form = new ResultSelectListRenameForm();
        form.setResultSelectOptionId(String.valueOf(DICTIONARY));
        form.setNameEnglish("Positive (reactive)");
        form.setNameFrench("Positif");
        assertTrue(resultSelectListService.renameOption(form, "1"));

        Dictionary renamed = dictionaryService.get(String.valueOf(DICTIONARY));
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        assertEquals("Positive (reactive)", renamed.getLocalizedName());
        assertEquals("SYSTEM", jdbcTemplate.queryForObject(
                "SELECT origin FROM clinlims.localization_value WHERE localization_id = 98704 AND locale = 'fr'",
                String.class));
        JsonNode editor = localization(98704);
        assertEquals("SITE_OVERRIDE", editor.get("origins").get("en").asText());
        assertEquals("Positive (reactive)", editor.get("labels").get("en").asText());
        assertEquals("Positif", editor.get("labels").get("fr").asText());
    }

    @Test
    public void aRegionalLanguageHasItsOwnOriginShippedLabelAndOverride() throws Exception {
        jdbcTemplate.update("INSERT INTO clinlims.supported_locale (id, locale_code, display_name, is_active,"
                + " is_fallback, sort_order, last_updated) VALUES (98701, 'fr-HT', 'Français (Haïti)', true, false,"
                + " 98, now())");
        linker.link();
        JsonNode before = localization(98702);
        assertEquals("SHIPPED", before.get("origins").get("fr-HT").asText());
        assertEquals("Erreur de prélèvement de l'échantillon", before.get("shippedLabels").get("fr-HT").asText());

        ok(mockMvc.perform(put("/rest/localizations/98702/translations").contentType(MediaType.APPLICATION_JSON)
                .content("{\"fr-HT\":\"Prélèvement raté\"}")));
        JsonNode after = localization(98702);
        assertEquals("SITE_OVERRIDE", after.get("origins").get("fr-HT").asText());
        assertEquals("SHIPPED", after.get("origins").get("fr").asText());
        assertEquals("Prélèvement raté", type(categories("fr-HT"), LINKED_BY_NAME).get("name").asText());
        assertEquals("Erreur de prélèvement de l'échantillon",
                type(categories("fr-FR"), LINKED_BY_NAME).get("name").asText());
    }

    @Test
    public void theShippedTextIsNoOverrideAndAnUnknownLocaleIsRefused() throws Exception {
        linker.link();
        ok(mockMvc.perform(put("/rest/localizations/98702/translations").contentType(MediaType.APPLICATION_JSON)
                .content("{\"fr\":\"Erreur de prélèvement de l'échantillon\"}")));
        assertEquals("SHIPPED", localization(98702).get("origins").get("fr").asText());

        mockMvc.perform(post("/rest/localizations/98702/translations/xx/reset")).andExpect(status().isBadRequest());
        mockMvc.perform(put("/rest/localizations/98702/translations").contentType(MediaType.APPLICATION_JSON)
                .content("{\"xx\":\"Junk\"}")).andExpect(status().isBadRequest());
        assertEquals(Integer.valueOf(0), jdbcTemplate.queryForObject(
                "SELECT count(*) FROM clinlims.localization_value WHERE localization_id = 98702 AND locale = 'xx'",
                Integer.class));

        jdbcTemplate.update("INSERT INTO clinlims.localization_value (id, localization_id, locale, value, last_updated)"
                + " VALUES (98798, 98703, 'es', 'Perdido', now())");
        ok(mockMvc.perform(put("/rest/localizations/98703/translations").contentType(MediaType.APPLICATION_JSON)
                .content("{\"es\":\"Perdido por el mensajero\"}")));
    }

    @Test
    public void anImportFollowsTheEditorsRules() throws Exception {
        linker.link();
        JsonNode refused = json
                .readTree(ok(mockMvc.perform(post("/rest/localizations/import").contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"id\":\"98702\",\"translations\":{\"fr\":\" \"}}]"))));
        assertEquals(1, refused.get("failed").asInt());
        assertEquals("SHIPPED", localization(98702).get("origins").get("fr").asText());

        ok(mockMvc.perform(post("/rest/localizations/import").contentType(MediaType.APPLICATION_JSON)
                .content("[{\"id\":\"98702\",\"translations\":{\"fr\":\"Erreur de prélèvement de l'échantillon\","
                        + "\"en\":\"Collection error\"}}]")));
        JsonNode imported = localization(98702);
        assertEquals("the shipped text stays shipped", "SHIPPED", imported.get("origins").get("fr").asText());
        assertEquals("SITE_OVERRIDE", imported.get("origins").get("en").asText());
    }

    /**
     * Transifex has no French for this entry, so its shipped French is the file's:
     * an override keeps it, and Reset returns to it rather than to English.
     */
    @Test
    public void aResetAfterAnOverrideReturnsToTheFilesTranslation() throws Exception {
        ok(mockMvc.perform(put("/rest/localizations/98704/translations").contentType(MediaType.APPLICATION_JSON)
                .content("{\"fr\":\"Positif (+)\"}")));
        JsonNode overridden = localization(98704);
        assertEquals("SITE_OVERRIDE", overridden.get("origins").get("fr").asText());
        assertEquals("Positif", overridden.get("shippedLabels").get("fr").asText());

        ok(mockMvc.perform(post("/rest/localizations/98704/translations/fr/reset")));
        JsonNode reset = localization(98704);
        assertEquals("SHIPPED", reset.get("origins").get("fr").asText());
        assertEquals("Positif", reset.get("labels").get("fr").asText());
        assertEquals("Positif",
                jdbcTemplate.queryForObject(
                        "SELECT value FROM clinlims.localization_value WHERE localization_id = 98704 AND locale = 'fr'",
                        String.class));

        ok(mockMvc.perform(put("/rest/localizations/98704/translations").contentType(MediaType.APPLICATION_JSON)
                .content("{\"fr\":\"Positif (+)\"}")));
        ok(mockMvc.perform(post("/rest/localizations/import").contentType(MediaType.APPLICATION_JSON)
                .content("[{\"id\":\"98704\",\"translations\":{\"fr\":\"Positif\"}}]")));
        assertEquals("importing the shipped text drops the override", "SHIPPED",
                localization(98704).get("origins").get("fr").asText());
    }

    @Test
    public void aLabelWrittenWithoutAnOriginIsTheLabsOwn() {
        jdbcTemplate.update("INSERT INTO clinlims.localization_value (id, localization_id, locale, value, last_updated)"
                + " VALUES (98799, 98703, 'es', 'Perdido', now())");
        assertEquals("SITE", jdbcTemplate
                .queryForObject("SELECT origin FROM clinlims.localization_value WHERE id = 98799", String.class));
    }

    private static String ok(ResultActions call) throws Exception {
        MockHttpServletResponse response = call.andReturn().getResponse();
        assertEquals(response.getContentAsString(), 200, response.getStatus());
        return response.getContentAsString();
    }

    private JsonNode categories(String language) throws Exception {
        String body = ok(mockMvc.perform(get("/rest/nce/categories").header("Accept-Language", language)));
        for (JsonNode category : json.readTree(body)) {
            if (category.get("id").asText().equals(String.valueOf(CATEGORY))) {
                return category;
            }
        }
        throw new AssertionError("category " + CATEGORY + " not listed: " + body);
    }

    private static JsonNode type(JsonNode category, int id) {
        for (JsonNode type : category.get("types")) {
            if (type.get("id").asText().equals(String.valueOf(id))) {
                return type;
            }
        }
        throw new AssertionError("type " + id + " not under " + category);
    }

    private JsonNode localization(int id) throws Exception {
        return json.readTree(ok(mockMvc.perform(get("/rest/localizations/" + id))));
    }

    private String key(String table, int id) {
        return jdbcTemplate.queryForObject("SELECT display_key FROM clinlims." + table + " WHERE id = ?", String.class,
                id);
    }

    private void localization(int id, String description, Map<String, String> values, String origin) {
        jdbcTemplate.update("INSERT INTO clinlims.localization (id, description, lastupdated) VALUES (?, ?, now())", id,
                description);
        int valueId = id * 10;
        for (Map.Entry<String, String> value : values.entrySet()) {
            jdbcTemplate.update(
                    "INSERT INTO clinlims.localization_value (id, localization_id, locale, value, origin,"
                            + " last_updated) VALUES (?, ?, ?, ?, ?, now())",
                    valueId++, id, value.getKey(), value.getValue(), origin);
        }
    }
}
