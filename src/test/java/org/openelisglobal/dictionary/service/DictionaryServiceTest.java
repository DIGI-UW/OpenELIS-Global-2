package org.openelisglobal.dictionary.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.dictionarycategory.service.DictionaryCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;

@Rollback
public class DictionaryServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    DictionaryService dictionaryService;

    @Autowired
    DictionaryCategoryService dictionaryCategoryService;

    @Before
    public void setup() throws Exception {
        executeDataSetWithStateManagement("testdata/dictionary.xml");
    }

    @Test
    public void delete_shouldDeleteDictionary() {
        Dictionary dictionaryToDelete = dictionaryService.get("1");
        dictionaryToDelete.setSysUserId("admin");

        assertNotNull(dictionaryToDelete);

        dictionaryService.delete(dictionaryToDelete);
        assertEquals("N", dictionaryService.get("1").getIsActive());
    }

    @Test
    public void getDictionaryEntriesByCategoryId_shouldReturnaListOfDictionaryEntriesByCategoryId() {
        List<Dictionary> dictionaries = dictionaryService.getDictionaryEntriesByCategoryId("1");
        Assert.assertNotEquals(0, dictionaries.size());

        assertEquals("Dictionary Entry 1", dictionaries.get(0).getDictEntry());
        assertEquals("Y", dictionaries.get(0).getIsActive());
        assertEquals("DE1", dictionaries.get(0).getLocalAbbreviation());
    }

    @Test
    public void getDictionaryByLocalAbbrev_shouldReturnDictionaryWhenGivenLocalAbbreviation() {
        Dictionary dictionaryToGetByLocalAbbrev = dictionaryService.get("1");

        Dictionary dictionary = dictionaryService.getDictionaryByLocalAbbrev(dictionaryToGetByLocalAbbrev);

        assertNotNull(dictionary);
        assertEquals("Dictionary Entry 1", dictionary.getDictEntry());
        assertEquals("Y", dictionary.getIsActive());
        assertEquals("DE1", dictionary.getLocalAbbreviation());
    }

    @Test
    public void getDictionaryByDictEntry_shouldReturnDictionaryWhenGivenDictEntry() {
        Dictionary dictionary = dictionaryService.getDictionaryByDictEntry("Dictionary Entry 2");

        assertNotNull(dictionary);
        assertEquals("Y", dictionary.getIsActive());
        assertEquals("DE2", dictionary.getLocalAbbreviation());
        assertEquals("2", dictionary.getId());
    }

    @Test
    public void getDictionaryById_shouldReturnDictionaryWhenGivenDictionaryId() {
        Dictionary dictionary = dictionaryService.getDictionaryById("2");

        assertNotNull(dictionary);
        assertEquals("Dictionary Entry 2", dictionary.getDictEntry());
        assertEquals("DE2", dictionary.getLocalAbbreviation());
        assertEquals("2", dictionary.getId());
        assertEquals("Y", dictionary.getIsActive());
    }

    @Test
    public void getDictionaryEntrysByNameAndCategoryDescription_shouldGetDictionaryEntrysByNameAndCategoryDescription() {
        Dictionary dictionary = dictionaryService.getDictionaryEntrysByNameAndCategoryDescription("Dictionary Entry 1",
                "Category Description 1");
        assertNotNull(dictionary);

        assertEquals("Dictionary Entry 1", dictionary.getDictEntry());
        assertEquals("Y", dictionary.getIsActive());
        assertEquals("DE1", dictionary.getLocalAbbreviation());
    }

    @Test
    public void getDictionaryEntrysByCategoryNameLocalizedSort_shouldGetDictionaryEntrysByCategoryNameLocalizedSort() {
        List<Dictionary> dictionaries = dictionaryService
                .getDictionaryEntrysByCategoryNameLocalizedSort("Category Name 1");
        Assert.assertNotEquals(0, dictionaries.size());

        assertEquals("Dictionary Entry 1", dictionaries.get(0).getDictEntry());
        assertEquals("Y", dictionaries.get(0).getIsActive());
        assertEquals("DE1", dictionaries.get(0).getLocalAbbreviation());
    }

    @Test
    public void getDataForId_shouldReturnDictionaryDataForTheProvidedDictionaryId() {
        Dictionary dictionary = dictionaryService.getDataForId("1");

        assertNotNull(dictionary);
        assertEquals("Dictionary Entry 1", dictionary.getDictEntry());
        assertEquals("Y", dictionary.getIsActive());
        assertEquals("DE1", dictionary.getLocalAbbreviation());
    }

    @Test
    public void getData_shouldReturnDictionaryDataForTheProvidedDictionaryId() {
        Dictionary dictionaryToGet = dictionaryService.get("1");

        dictionaryService.getData(dictionaryToGet);

        assertNotNull(dictionaryToGet);
        assertEquals("Dictionary Entry 1", dictionaryToGet.getDictEntry());
        assertEquals("Y", dictionaryToGet.getIsActive());
        assertEquals("DE1", dictionaryToGet.getLocalAbbreviation());
    }

    @Test
    public void getPagesOfSearchedDictionaries_shouldGetPagesOfSearchedDictionaries() {
        List<Dictionary> dictionaries = dictionaryService.getPagesOfSearchedDictionaries(1, "Dictionary Entry 1");

        Assert.assertNotEquals(0, dictionaries.size());

        assertEquals("Dictionary Entry 1", dictionaries.get(0).getDictEntry());
        assertEquals("Y", dictionaries.get(0).getIsActive());
        assertEquals("DE1", dictionaries.get(0).getLocalAbbreviation());
    }

    @Test
    public void update_shouldUpdateDictionary() {
        Dictionary dictionaryToUpdate = dictionaryService.get("1");
        dictionaryToUpdate.setDictEntry("INFLUENZA VIRUS A RNA DETECTEDetest");

        Dictionary updatedDictionary = dictionaryService.update(dictionaryToUpdate);
        assertNotNull(updatedDictionary);

        assertEquals("Y", dictionaryService.get("1").getIsActive());
        assertEquals("INFLUENZA VIRUS A RNA DETECTEDetest", dictionaryService.get("1").getDictEntry());
    }

    @Test
    public void update_shouldUpdateDictionaryWhenDictionaryFrozenCheckIsRequired() {
        Dictionary dictionaryToUpdate = dictionaryService.get("1");
        dictionaryToUpdate.setDictEntry("INFLUENZA VIRUS A RNA DETECTEDetest");

        dictionaryService.update(dictionaryToUpdate, true);

        assertEquals("Y", dictionaryService.get("1").getIsActive());
        assertEquals("INFLUENZA VIRUS A RNA DETECTEDetest", dictionaryService.get("1").getDictEntry());
    }

    @Test
    public void update_shouldUpdateDictionaryWhenDictionaryFrozenCheckIsNotRequired() {
        Dictionary dictionaryToUpdate = dictionaryService.get("1");
        dictionaryToUpdate.setDictEntry("INFLUENZA VIRUS A RNA DETECTEDetest");

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
        dictionaryService.update(dictionary);
        Dictionary updated = dictionaryService.get("1");
        assertNotNull(updated);
        assertEquals("LA9663-1", updated.getLoincCode());
    }

    @Test
    public void getDictionary_shouldReturnNullLoincCodeWhenNotExists() {
        Dictionary dictionary = dictionaryService.get("6");
        assertNotNull(dictionary);
        assertNull(dictionary.getLoincCode());
    }

    @Test
    public void update_shouldPersistLoincCode() {
        Dictionary dictionary = dictionaryService.get("1");
        dictionary.setLoincCode("LA12345-6");

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
        return dictionary;
    }
}
