package org.openelisglobal.dictionary.service;

import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.dictionarycategory.service.DictionaryCategoryService;
import org.openelisglobal.dictionarycategory.valueholder.DictionaryCategory;
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

    // @Test
    public void verifyTestData() {
        List<DictionaryCategory> categories = dictionaryCategoryService.getAll();
        System.out.println("Dictionary Categories: " + categories.size());
        categories.forEach(cat -> System.out
                .println(cat.getCategoryName() + " - " + cat.getLocalAbbreviation() + " - " + cat.getDescription()));

        List<Dictionary> dictionaries = dictionaryService.getAll();
        System.out.println("Dictionaries: " + dictionaries.size());
        dictionaries.forEach(dict -> System.out.println(dict.getDictEntry() + " - " + dict.getIsActive()));
    }

    @Test
    public void delete_shouldDeleteDictionary() {
        Dictionary dictionaryToDelete = dictionaryService.get("1");
        dictionaryToDelete.setSysUserId("admin");

        Assert.assertNotNull(dictionaryToDelete);

        dictionaryService.delete(dictionaryToDelete);
        Assert.assertEquals("N", dictionaryService.get("1").getIsActive());
    }

    @Test
    public void getDictionaryEntriesByCategoryId_shouldReturnaListOfDictionaryEntriesByCategoryId() {
        List<Dictionary> dictionaries = dictionaryService.getDictionaryEntriesByCategoryId("1");
        Assert.assertNotEquals(0, dictionaries.size());

        Assert.assertEquals("Dictionary Entry 1", dictionaries.get(0).getDictEntry());
        Assert.assertEquals("Y", dictionaries.get(0).getIsActive());
        Assert.assertEquals("DE1", dictionaries.get(0).getLocalAbbreviation());
    }

    @Test
    public void getDictionaryByLocalAbbrev_shouldReturnDictionaryWhenGivenLocalAbbreviation() {
        Dictionary dictionaryToGetByLocalAbbrev = dictionaryService.get("1");

        Dictionary dictionary = dictionaryService.getDictionaryByLocalAbbrev(dictionaryToGetByLocalAbbrev);

        Assert.assertNotNull(dictionary);
        Assert.assertEquals("Dictionary Entry 1", dictionary.getDictEntry());
        Assert.assertEquals("Y", dictionary.getIsActive());
        Assert.assertEquals("DE1", dictionary.getLocalAbbreviation());
    }

    @Test
    public void getDictionaryByDictEntry_shouldReturnDictionaryWhenGivenDictEntry() {
        Dictionary dictionary = dictionaryService.getDictionaryByDictEntry("Dictionary Entry 2");

        Assert.assertNotNull(dictionary);
        Assert.assertEquals("Y", dictionary.getIsActive());
        Assert.assertEquals("DE2", dictionary.getLocalAbbreviation());
        Assert.assertEquals("2", dictionary.getId());
    }

    @Test
    public void getDictionaryById_shouldReturnDictionaryWhenGivenDictionaryId() {
        Dictionary dictionary = dictionaryService.getDictionaryById("2");

        Assert.assertNotNull(dictionary);
        Assert.assertEquals("Dictionary Entry 2", dictionary.getDictEntry());
        Assert.assertEquals("DE2", dictionary.getLocalAbbreviation());
        Assert.assertEquals("2", dictionary.getId());
        Assert.assertEquals("Y", dictionary.getIsActive());
    }

    @Test
    public void getDictionaryEntrysByNameAndCategoryDescription_shouldGetDictionaryEntrysByNameAndCategoryDescription() {
        Dictionary dictionary = dictionaryService.getDictionaryEntrysByNameAndCategoryDescription("Dictionary Entry 1",
                "Category Description 1");
        Assert.assertNotNull(dictionary);

        Assert.assertEquals("Dictionary Entry 1", dictionary.getDictEntry());
        Assert.assertEquals("Y", dictionary.getIsActive());
        Assert.assertEquals("DE1", dictionary.getLocalAbbreviation());
    }

    @Test
    public void getDictionaryEntrysByCategoryNameLocalizedSort_shouldGetDictionaryEntrysByCategoryNameLocalizedSort() {
        List<Dictionary> dictionaries = dictionaryService
                .getDictionaryEntrysByCategoryNameLocalizedSort("Category Name 1");
        Assert.assertNotEquals(0, dictionaries.size());

        Assert.assertEquals("Dictionary Entry 1", dictionaries.get(0).getDictEntry());
        Assert.assertEquals("Y", dictionaries.get(0).getIsActive());
        Assert.assertEquals("DE1", dictionaries.get(0).getLocalAbbreviation());
    }

    @Test
    public void getDataForId_shouldReturnDictionaryDataForTheProvidedDictionaryId() {
        Dictionary dictionary = dictionaryService.getDataForId("1");

        Assert.assertNotNull(dictionary);
        Assert.assertEquals("Dictionary Entry 1", dictionary.getDictEntry());
        Assert.assertEquals("Y", dictionary.getIsActive());
        Assert.assertEquals("DE1", dictionary.getLocalAbbreviation());
    }

    @Test
    public void getData_shouldReturnDictionaryDataForTheProvidedDictionaryId() {
        Dictionary dictionaryToGet = dictionaryService.get("1");

        dictionaryService.getData(dictionaryToGet);

        Assert.assertNotNull(dictionaryToGet);
        Assert.assertEquals("Dictionary Entry 1", dictionaryToGet.getDictEntry());
        Assert.assertEquals("Y", dictionaryToGet.getIsActive());
        Assert.assertEquals("DE1", dictionaryToGet.getLocalAbbreviation());
    }

    @Test
    public void getPagesOfSearchedDictionaries_shouldGetPagesOfSearchedDictionaries() {
        List<Dictionary> dictionaries = dictionaryService.getPagesOfSearchedDictionaries(1, "Dictionary Entry 1");

        Assert.assertNotEquals(0, dictionaries.size());

        Assert.assertEquals("Dictionary Entry 1", dictionaries.get(0).getDictEntry());
        Assert.assertEquals("Y", dictionaries.get(0).getIsActive());
        Assert.assertEquals("DE1", dictionaries.get(0).getLocalAbbreviation());
    }

    @Test
    public void update_shouldUpdateDictionary() {
        Dictionary dictionaryToUpdate = dictionaryService.get("1");
        dictionaryToUpdate.setDictEntry("INFLUENZA VIRUS A RNA DETECTEDetest");

        Dictionary updatedDictionary = dictionaryService.update(dictionaryToUpdate);
        Assert.assertNotNull(updatedDictionary);

        Assert.assertEquals("Y", dictionaryService.get("1").getIsActive());
        Assert.assertEquals("INFLUENZA VIRUS A RNA DETECTEDetest", dictionaryService.get("1").getDictEntry());
    }

    @Test
    public void update_shouldUpdateDictionaryWhenDictionaryFrozenCheckIsRequired() {
        Dictionary dictionaryToUpdate = dictionaryService.get("1");
        dictionaryToUpdate.setDictEntry("INFLUENZA VIRUS A RNA DETECTEDetest");

        dictionaryService.update(dictionaryToUpdate, true);

        Assert.assertEquals("Y", dictionaryService.get("1").getIsActive());
        Assert.assertEquals("INFLUENZA VIRUS A RNA DETECTEDetest", dictionaryService.get("1").getDictEntry());
    }

    @Test
    public void update_shouldUpdateDictionaryWhenDictionaryFrozenCheckIsNotRequired() {
        Dictionary dictionaryToUpdate = dictionaryService.get("1");
        dictionaryToUpdate.setDictEntry("INFLUENZA VIRUS A RNA DETECTEDetest");

        dictionaryService.update(dictionaryToUpdate, false);

        Assert.assertEquals("Y", dictionaryService.get("1").getIsActive());
        Assert.assertEquals("INFLUENZA VIRUS A RNA DETECTEDetest", dictionaryService.get("1").getDictEntry());
    }

    @Test
    public void createDictionary_shouldCreateNewDictionary() throws Exception {
        Dictionary dict = createDictionaryObject();

        String inserted = dictionaryService.insert(dict);
        Dictionary dictionary = dictionaryService.get(inserted);

        Assert.assertEquals("Dictionary Entry 4", dictionary.getDictEntry());
        Assert.assertEquals("Y", dictionary.getIsActive());
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
