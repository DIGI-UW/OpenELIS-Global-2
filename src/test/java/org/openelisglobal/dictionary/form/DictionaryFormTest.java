package org.openelisglobal.dictionary.form;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.springframework.beans.factory.annotation.Autowired;

public class DictionaryFormTest extends BaseWebContextSensitiveTest {

    @Autowired
    private DictionaryService dictionaryService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/dictionary.xml");
    }

    @Test
    public void GetDictionariesByCategoryId_shouldReturnDictionaries() {
        String categoryId = "1";

        List<Dictionary> dictionaries = dictionaryService.getDictionaryEntriesByCategoryId(categoryId);

        assertNotNull("Dictionaries list should not be null", dictionaries);
        assertFalse("Expected at least one dictionary", dictionaries.isEmpty());

        Dictionary first = dictionaries.get(0);
        assertEquals("Expected dictionary entry", "Dictionary Entry 1", first.getDictEntry());
        assertEquals("Expected local abbreviation", "DE1", first.getLocalAbbreviation());
        assertEquals("Expected category ID", categoryId, first.getDictionaryCategory().getId());
    }

    @Test
    public void UpdateDictionary_shouldUpdateSuccessfully() {
        DictionaryForm form = new DictionaryForm();
        form.setId("1");
        form.setSelectedDictionaryCategoryId("1");
        form.setIsActive("Y");
        form.setDictEntry("Updated Entry");
        form.setLocalAbbreviation("UPD");

        Dictionary dictionary = dictionaryService.getDictionaryById(form.getId());
        dictionary.setDictEntry(form.getDictEntry());
        dictionary.setLocalAbbreviation(form.getLocalAbbreviation());

        dictionaryService.update(dictionary);

        Dictionary updatedDictionary = dictionaryService.getDictionaryById(form.getId());
        assertNotNull("Updated dictionary should not be null", updatedDictionary);
        assertEquals("Updated Entry", updatedDictionary.getDictEntry());
        assertEquals("UPD", updatedDictionary.getLocalAbbreviation());
    }

    @Test
    public void DeleteDictionary_shouldMarkAsInactive() {
        DictionaryForm form = new DictionaryForm();
        form.setId("3");

        Dictionary dictionary = dictionaryService.getDictionaryById(form.getId());
        assertNotNull("Dictionary should exist before delete", dictionary);

        dictionary.setSysUserId("test_user");
        dictionaryService.delete(dictionary);

        Dictionary deletedDictionary = dictionaryService.getDictionaryById(form.getId());
        assertNotNull("Dictionary should still exist after soft delete", deletedDictionary);
        assertEquals("Dictionary should be marked inactive", "N", deletedDictionary.getIsActive());
    }
}
