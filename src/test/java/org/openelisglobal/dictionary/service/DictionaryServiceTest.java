package org.openelisglobal.dictionary.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.dictionarycategory.service.DictionaryCategoryService;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.valueholder.Localization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.test.annotation.Rollback;

@Rollback
public class DictionaryServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    DictionaryService dictionaryService;

    @Autowired
    DictionaryCategoryService dictionaryCategoryService;

    @Autowired
    LocalizationService localizationService;

    @Before
    public void setup() throws Exception {
        executeDataSetWithStateManagement("testdata/dictionary.xml");
    }

    /** Shipped in the vector dictionaries; fr.json translates it as "Paludisme". */
    private static final String SHIPPED_KEY = "dictionary.vecPathogens.malaria";

    private final List<String> shippedEntryIds = new ArrayList<>();

    @After
    public void resetLocaleAndShippedRows() {
        LocaleContextHolder.resetLocaleContext();
        for (String id : shippedEntryIds) {
            Integer labelId = jdbcTemplate.queryForObject("SELECT name_localization_id FROM dictionary WHERE id = ?",
                    Integer.class, Integer.parseInt(id));
            jdbcTemplate.update("DELETE FROM dictionary WHERE id = ?", Integer.parseInt(id));
            if (labelId != null) {
                jdbcTemplate.update("DELETE FROM localization_value WHERE localization_id = ?", labelId);
                jdbcTemplate.update("DELETE FROM localization WHERE id = ?", labelId);
            }
        }
        shippedEntryIds.clear();
    }

    @Test
    public void delete_shouldDeleteDictionary() {
        Dictionary dictionaryToDelete = dictionaryService.get("1");
        dictionaryToDelete.setSysUserId("1");

        dictionaryService.delete(dictionaryToDelete);
        assertEquals("N", dictionaryService.get("1").getIsActive());
    }

    @Test
    public void getDictionaryEntriesByCategoryId_shouldReturnaListOfDictionaryEntriesByCategoryId() {
        List<Dictionary> dictionaries = dictionaryService.getDictionaryEntriesByCategoryId("1");
        assertEquals(4, dictionaries.size());

        assertEquals("Dictionary Entry 1", dictionaries.get(0).getDictEntry());
        assertEquals("Y", dictionaries.get(0).getIsActive());
        assertEquals("DE1", dictionaries.get(0).getLocalAbbreviation());
    }

    @Test
    public void getDictionaryByLocalAbbrev_shouldReturnDictionaryWhenGivenLocalAbbreviation() {
        Dictionary dictionaryToGetByLocalAbbrev = dictionaryService.get("1");

        Dictionary dictionary = dictionaryService.getDictionaryByLocalAbbrev(dictionaryToGetByLocalAbbrev);

        assertEquals("Dictionary Entry 1", dictionary.getDictEntry());
        assertEquals("Y", dictionary.getIsActive());
        assertEquals("DE1", dictionary.getLocalAbbreviation());
    }

    @Test
    public void getDictionaryByDictEntry_shouldReturnDictionaryWhenGivenDictEntry() {
        Dictionary dictionary = dictionaryService.getDictionaryByDictEntry("Dictionary Entry 2");

        assertEquals("Y", dictionary.getIsActive());
        assertEquals("DE2", dictionary.getLocalAbbreviation());
        assertEquals("2", dictionary.getId());
    }

    @Test
    public void getDictionaryById_shouldReturnDictionaryWhenGivenDictionaryId() {
        Dictionary dictionary = dictionaryService.getDictionaryById("2");

        assertEquals("Dictionary Entry 2", dictionary.getDictEntry());
        assertEquals("DE2", dictionary.getLocalAbbreviation());
        assertEquals("2", dictionary.getId());
        assertEquals("Y", dictionary.getIsActive());
    }

    /**
     * Regression for the 500 on /rest/test-display-beans-map: legacy TEST_RESULT
     * rows can carry raw text where a dictionary id belongs, and the lookup used to
     * propagate the id-parse failure as LIMSRuntimeException, taking down every
     * page that resolves result values. Non-numeric ids must read as not-found.
     */
    @Test
    public void getDictionaryById_shouldReturnNullForNonNumericId() {
        assertNull(dictionaryService.getDictionaryById("HIV1"));
        assertNull(dictionaryService.getDictionaryById("  "));
    }

    @Test
    public void getDictionaryEntrysByNameAndCategoryDescription_shouldGetDictionaryEntrysByNameAndCategoryDescription() {
        Dictionary dictionary = dictionaryService.getDictionaryEntrysByNameAndCategoryDescription("Dictionary Entry 1",
                "Category Description 1");

        assertEquals("Dictionary Entry 1", dictionary.getDictEntry());
        assertEquals("Y", dictionary.getIsActive());
        assertEquals("DE1", dictionary.getLocalAbbreviation());
    }

    @Test
    public void getDictionaryEntrysByCategoryNameLocalizedSort_shouldGetDictionaryEntrysByCategoryNameLocalizedSort() {
        List<Dictionary> dictionaries = dictionaryService
                .getDictionaryEntrysByCategoryNameLocalizedSort("Category Name 1");
        assertEquals(4, dictionaries.size());

        assertEquals("Dictionary Entry 1", dictionaries.get(0).getDictEntry());
        assertEquals("Y", dictionaries.get(0).getIsActive());
        assertEquals("DE1", dictionaries.get(0).getLocalAbbreviation());
    }

    @Test
    public void getDataForId_shouldReturnDictionaryDataForTheProvidedDictionaryId() {
        Dictionary dictionary = dictionaryService.getDataForId("1");

        assertEquals("Dictionary Entry 1", dictionary.getDictEntry());
        assertEquals("Y", dictionary.getIsActive());
        assertEquals("DE1", dictionary.getLocalAbbreviation());
    }

    @Test
    public void getData_shouldReturnDictionaryDataForTheProvidedDictionaryId() {
        Dictionary dictionaryToGet = dictionaryService.get("1");

        dictionaryService.getData(dictionaryToGet);

        assertEquals("Dictionary Entry 1", dictionaryToGet.getDictEntry());
        assertEquals("Y", dictionaryToGet.getIsActive());
        assertEquals("DE1", dictionaryToGet.getLocalAbbreviation());
    }

    @Test
    public void getPagesOfSearchedDictionaries_shouldGetPagesOfSearchedDictionaries() {
        List<Dictionary> dictionaries = dictionaryService.getPagesOfSearchedDictionaries(1, "Dictionary Entry 1");

        assertEquals(1, dictionaries.size());

        assertEquals("Dictionary Entry 1", dictionaries.get(0).getDictEntry());
        assertEquals("Y", dictionaries.get(0).getIsActive());
        assertEquals("DE1", dictionaries.get(0).getLocalAbbreviation());
    }

    @Test
    public void update_shouldUpdateDictionary() {
        Dictionary dictionaryToUpdate = dictionaryService.get("1");
        dictionaryToUpdate.setDictEntry("INFLUENZA VIRUS A RNA DETECTEDetest");
        dictionaryToUpdate.setSysUserId("1");

        dictionaryService.update(dictionaryToUpdate);

        assertEquals("Y", dictionaryService.get("1").getIsActive());
        assertEquals("INFLUENZA VIRUS A RNA DETECTEDetest", dictionaryService.get("1").getDictEntry());
    }

    @Test
    public void update_shouldUpdateDictionaryWhenDictionaryFrozenCheckIsRequired() {
        Dictionary dictionaryToUpdate = dictionaryService.get("1");
        dictionaryToUpdate.setDictEntry("INFLUENZA VIRUS A RNA DETECTEDetest");
        dictionaryToUpdate.setSysUserId("1");

        dictionaryService.update(dictionaryToUpdate, true);

        assertEquals("Y", dictionaryService.get("1").getIsActive());
        assertEquals("INFLUENZA VIRUS A RNA DETECTEDetest", dictionaryService.get("1").getDictEntry());
    }

    @Test
    public void update_shouldUpdateDictionaryWhenDictionaryFrozenCheckIsNotRequired() {
        Dictionary dictionaryToUpdate = dictionaryService.get("1");
        dictionaryToUpdate.setDictEntry("INFLUENZA VIRUS A RNA DETECTEDetest");
        dictionaryToUpdate.setSysUserId("1");

        dictionaryService.update(dictionaryToUpdate, false);

        assertEquals("Y", dictionaryService.get("1").getIsActive());
        assertEquals("INFLUENZA VIRUS A RNA DETECTEDetest", dictionaryService.get("1").getDictEntry());
    }

    @Test
    public void createDictionary_shouldCreateNewDictionary() throws Exception {
        Dictionary dict = createDictionaryObject();

        String inserted = dictionaryService.insert(dict);
        Dictionary dictionary = dictionaryService.get(inserted);

        assertEquals("Dictionary Entry 4", dictionary.getDictEntry());
        assertEquals("Y", dictionary.getIsActive());
    }

    @Test
    public void getDictionary_shouldReturnWithLoincCodeWhenExists() {

        Dictionary dictionary = dictionaryService.get("1");
        dictionary.setLoincCode("LA9663-1");
        dictionary.setSysUserId("1");
        dictionaryService.update(dictionary);
        Dictionary updated = dictionaryService.get("1");
        assertEquals("LA9663-1", updated.getLoincCode());
    }

    @Test
    public void getDictionary_shouldReturnNullLoincCodeWhenNotExists() {
        Dictionary dictionary = dictionaryService.get("6");
        assertNull(dictionary.getLoincCode());
    }

    @Test
    public void update_shouldPersistLoincCode() {
        Dictionary dictionary = dictionaryService.get("1");
        dictionary.setLoincCode("LA12345-6");
        dictionary.setSysUserId("1");

        dictionaryService.update(dictionary);

        Dictionary updated = dictionaryService.get("1");
        assertEquals("LA12345-6", updated.getLoincCode());
    }

    @Test
    public void createDictionary_shouldStoreLoincCode() throws Exception {
        Dictionary dict = createDictionaryObject();
        dict.setLoincCode("LA54321-0");

        String id = dictionaryService.insert(dict);
        Dictionary saved = dictionaryService.get(id);

        assertEquals("LA54321-0", saved.getLoincCode());
    }

    @Test
    public void getPagesOfSearchedDictionaries_shouldFindByDictionaryText() {
        List<Dictionary> results = dictionaryService.getPagesOfSearchedDictionaries(1, "Dictionary Entry 1");
        assertEquals(1, results.size());
        assertEquals("Dictionary Entry 1", results.get(0).getDictEntry());
    }

    private Dictionary createDictionaryObject() {
        Dictionary dictionary = new Dictionary();
        dictionary.setSortOrder(4);
        dictionary.setDictionaryCategory(dictionaryCategoryService.getDictionaryCategoryByName("CA3"));
        dictionary.setDictEntry("Dictionary Entry 4");
        dictionary.setIsActive("Y");
        dictionary.setLocalAbbreviation("DE4");
        dictionary.setSysUserId("1");
        return dictionary;
    }

    @Test
    public void update_renamedEntry_localizedNameIsTheNewName() {
        String id = insertLabelledEntry();
        Dictionary entry = dictionaryService.get(id);
        entry.setDictEntry("Renamed Entry");
        entry.setSysUserId("1");

        dictionaryService.update(entry, false);

        LocaleContextHolder.setLocale(Locale.ENGLISH);
        Dictionary stored = dictionaryService.get(id);
        assertEquals("Renamed Entry", stored.getLocalizedName());
        assertEquals("Renamed Entry", stored.getLocalizedDictionaryName().getLocalizedValue("en"));
    }

    @Test
    public void update_renamedEntry_keepsTheFrenchValue() {
        String id = insertLabelledEntry();
        Dictionary entry = dictionaryService.get(id);
        entry.setDictEntry("Renamed Entry");
        entry.setSysUserId("1");

        dictionaryService.update(entry, false);

        assertEquals("Nom localisé 7", dictionaryService.get(id).getLocalizedDictionaryName().getLocalizedValue("fr"));
    }

    @Test
    public void update_unchangedName_keepsAnEnglishLabelThatDiffersFromTheEntry() {
        String id = insertLabelledEntry();
        Dictionary entry = dictionaryService.get(id);
        entry.setLocalAbbreviation("DE7X");
        entry.setSysUserId("1");

        dictionaryService.update(entry, false);

        Dictionary stored = dictionaryService.get(id);
        assertEquals("DE7X", stored.getLocalAbbreviation());
        assertEquals("Localized Name 7", stored.getLocalizedDictionaryName().getLocalizedValue("en"));
    }

    @Test
    public void update_renamedEntryWithoutALocalization_createsAndLinksOne() {
        Dictionary entry = dictionaryService.get("2");
        assertNull(entry.getLocalizedDictionaryName());
        entry.setDictEntry("Renamed Entry 2");
        entry.setSysUserId("1");

        dictionaryService.update(entry, false);

        Localization label = dictionaryService.get("2").getLocalizedDictionaryName();
        assertEquals("Renamed Entry 2", label == null ? null : label.getLocalizedValue("en"));
    }

    @Test
    public void update_renamedShippedEntry_englishShowsTheNewNameAndFrenchKeepsTheCatalogueTranslation() {
        String id = insertShippedEntry();
        Dictionary entry = dictionaryService.get(id);
        entry.setDictEntry("Malaria (falciparum)");
        entry.setSysUserId("1");

        dictionaryService.update(entry, false);

        Dictionary stored = dictionaryService.get(id);
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        assertEquals("Malaria (falciparum)", stored.getLocalizedName());
        LocaleContextHolder.setLocale(Locale.FRENCH);
        assertEquals("Paludisme", stored.getLocalizedName());
    }

    /**
     * A shipped entry as the configuration loader leaves it: display key and a
     * system English value.
     */
    private String insertShippedEntry() {
        Dictionary dictionary = new Dictionary();
        dictionary.setSortOrder(8);
        dictionary.setDictionaryCategory(dictionaryCategoryService.getDictionaryCategoryByName("CA3"));
        dictionary.setDictEntry("Malaria");
        dictionary.setNameKey(SHIPPED_KEY);
        dictionary.setIsActive("Y");
        dictionary.setLocalAbbreviation("MAL");
        dictionary.setSysUserId("1");
        String id = dictionaryService.insert(dictionary);
        shippedEntryIds.add(id);

        Localization label = localizationService.get(dictionaryService.get(id).getLocalizedDictionaryName().getId());
        label.setSystemValue("en", "Malaria", true);
        label.setSysUserId("1");
        localizationService.update(label);
        return id;
    }

    /**
     * An entry made through the admin, so it has a localization row, with its
     * English label and a French value set afterwards.
     */
    private String insertLabelledEntry() {
        Dictionary dictionary = new Dictionary();
        dictionary.setSortOrder(7);
        dictionary.setDictionaryCategory(dictionaryCategoryService.getDictionaryCategoryByName("CA3"));
        dictionary.setDictEntry("Dictionary Entry 7");
        dictionary.setIsActive("Y");
        dictionary.setLocalAbbreviation("DE7");
        dictionary.setSysUserId("1");
        String id = dictionaryService.insert(dictionary);

        Localization label = localizationService.get(dictionaryService.get(id).getLocalizedDictionaryName().getId());
        label.setLocalizedValue("en", "Localized Name 7");
        label.setLocalizedValue("fr", "Nom localisé 7");
        label.setSysUserId("1");
        localizationService.update(label);
        return id;
    }
}
