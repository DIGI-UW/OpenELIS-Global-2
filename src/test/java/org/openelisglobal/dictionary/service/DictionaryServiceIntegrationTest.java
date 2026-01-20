package org.openelisglobal.dictionary.service;

import static org.junit.Assert.*;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.dictionarycategory.service.DictionaryCategoryService;
import org.openelisglobal.dictionarycategory.valueholder.DictionaryCategory;
import org.springframework.beans.factory.annotation.Autowired;

public class DictionaryServiceIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private DictionaryService dictionaryService;

    @Autowired
    private DictionaryCategoryService dictionaryCategoryService;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/dictionary.xml");
    }

    @Test
    public void testGetById_WithValidId_ReturnsDictionary() {
        Dictionary retrieved = dictionaryService.get("1");

        assertNotNull("Dictionary should be found", retrieved);
        assertEquals("ID should match", "1", retrieved.getId());
        assertEquals("Entry name should match", "Dictionary Entry 1", retrieved.getDictEntry());
        assertEquals("Abbreviation should match", "DE1", retrieved.getLocalAbbreviation());
    }

    @Test
    public void testGetById_WithInvalidId_ReturnsNull() {
        try {
            Dictionary retrieved = dictionaryService.get("99999999");
            assertNull("Non-existent ID should return null", retrieved);
        } catch (Exception e) {
            assertTrue("Should throw ObjectNotFoundException",
                    e.getMessage().contains("No row with the given identifier"));
        }
    }

    @Test
    public void testGetDictionaryByDictEntry_WithExistingEntry_ReturnsDictionary() {
        Dictionary retrieved = dictionaryService.getDictionaryByDictEntry("Positive");

        assertNotNull("Dictionary should be found by entry name", retrieved);
        assertEquals("Entry name should match", "Positive", retrieved.getDictEntry());
        assertEquals("ID should match", "4", retrieved.getId());
    }

    @Test
    public void testGetDictionaryEntriesByCategoryId_WithValidCategory_ReturnsList() {
        List<Dictionary> entries = dictionaryService.getDictionaryEntriesByCategoryId("1");

        assertNotNull("Should return list", entries);
        assertTrue("Should contain multiple entries", entries.size() >= 4);

        boolean foundPositive = entries.stream().anyMatch(d -> "Positive".equals(d.getDictEntry()));
        assertTrue("Should contain 'Positive' entry", foundPositive);
    }

    @Test
    public void testInsert_WithValidData_PersistsToDatabase() {
        Dictionary dictionary = new Dictionary();
        dictionary.setDictEntry("IntegrationTest_" + System.currentTimeMillis());
        dictionary.setLocalAbbreviation("IT");
        dictionary.setIsActive("Y");

        DictionaryCategory category = dictionaryCategoryService.get("1");
        dictionary.setDictionaryCategory(category);

        String insertedId = dictionaryService.insert(dictionary);

        assertNotNull("Insert should return ID", insertedId);
        Dictionary retrieved = dictionaryService.get(insertedId);
        assertNotNull("Dictionary should be persisted", retrieved);
        assertEquals("Entry name should match", dictionary.getDictEntry(), retrieved.getDictEntry());
    }

    @Test
    public void testUpdate_WithValidData_UpdatesDatabase() {
        Dictionary toUpdate = dictionaryService.get("1");
        assertNotNull("Test data should exist", toUpdate);

        String originalEntry = toUpdate.getDictEntry();
        toUpdate.setLocalAbbreviation("UPDATED");
        dictionaryService.update(toUpdate);

        Dictionary updated = dictionaryService.get("1");
        assertEquals("Abbreviation should be updated", "UPDATED", updated.getLocalAbbreviation());
        assertEquals("Entry should not change", originalEntry, updated.getDictEntry());
    }

    @Test(expected = LIMSRuntimeException.class)
    public void testInsert_WithDuplicateEntry_ThrowsException() {
        Dictionary dictionary = new Dictionary();
        dictionary.setDictEntry("Positive");
        dictionary.setLocalAbbreviation("DUP");
        dictionary.setIsActive("Y");

        DictionaryCategory category = dictionaryCategoryService.get("1");
        dictionary.setDictionaryCategory(category);

        dictionaryService.insert(dictionary);
    }

    @Test
    public void testDuplicateDictionaryExists_WithExistingEntry_ReturnsTrue() {
        Dictionary dictionary = new Dictionary();
        dictionary.setDictEntry("Positive");
        dictionary.setLocalAbbreviation("POS");

        DictionaryCategory category = dictionaryCategoryService.get("1");
        dictionary.setDictionaryCategory(category);

        boolean exists = dictionaryService.duplicateDictionaryExists(dictionary);
        assertTrue("Existing entry should return true", exists);
    }

    @Test
    public void testDuplicateDictionaryExists_WithUniqueEntry_ReturnsFalse() {
        Dictionary dictionary = new Dictionary();
        dictionary.setDictEntry("IntegrationTest_Unique_" + System.currentTimeMillis());
        dictionary.setLocalAbbreviation("UNIQ");

        DictionaryCategory category = dictionaryCategoryService.get("1");
        dictionary.setDictionaryCategory(category);

        boolean exists = dictionaryService.duplicateDictionaryExists(dictionary);
        assertFalse("Unique entry should return false", exists);
    }
}
