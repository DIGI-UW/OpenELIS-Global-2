package org.openelisglobal.dictionarycategory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.exception.LIMSDuplicateRecordException;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.dictionarycategory.service.DictionaryCategoryService;
import org.openelisglobal.dictionarycategory.valueholder.DictionaryCategory;
import org.springframework.beans.factory.annotation.Autowired;

public class DictionaryCategoryServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private DictionaryCategoryService dictionaryCategoryService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/dictionary-category.xml");
    }

    // @Test
    public void getDictionaryCategoryfromDataBase() {
        List<DictionaryCategory> dictionaryCategoryList = dictionaryCategoryService.getAll();
        dictionaryCategoryList.forEach(category -> {
            System.out.print(category.getCategoryName() + " ");
        });
    }

    @Test
    public void getDictionaryCategoryByName_shouldReturnDictionaryCategoryByName() {
        DictionaryCategory dictionaryCategory = dictionaryCategoryService
                .getDictionaryCategoryByName("TEST_CATEGORY_ONE");
        assertEquals("TEST_CATEGORY_ONE", dictionaryCategory.getCategoryName());
        assertEquals("TEST1", dictionaryCategory.getLocalAbbreviation());
    }

    @Test
    public void insert_shouldInsertDictionaryCategory() {
        DictionaryCategory dictionaryCategory = new DictionaryCategory();
        dictionaryCategory.setCategoryName("NEW_CATEGORY");
        dictionaryCategory.setDescription("New Test Category");
        dictionaryCategory.setLocalAbbreviation("NEW");

        assertEquals(3, dictionaryCategoryService.getAll().size());

        String inserted = dictionaryCategoryService.insert(dictionaryCategory);
        DictionaryCategory insertedCategory = dictionaryCategoryService.get(inserted);

        assertEquals("NEW", insertedCategory.getLocalAbbreviation());
        assertEquals("New Test Category", insertedCategory.getDescription());
        assertEquals("NEW_CATEGORY", insertedCategory.getCategoryName());
        assertEquals(4, dictionaryCategoryService.getAll().size());
    }

    @Test
    public void save_shouldSaveDictionaryCategory() {
        DictionaryCategory dictionaryCategory = new DictionaryCategory();
        dictionaryCategory.setCategoryName("SAVED_CATEGORY");
        dictionaryCategory.setDescription("Saved Test Category");
        dictionaryCategory.setLocalAbbreviation("SAVED");

        DictionaryCategory saved = dictionaryCategoryService.save(dictionaryCategory);

        assertEquals("SAVED", saved.getLocalAbbreviation());
        assertEquals("Saved Test Category", saved.getDescription());
        assertEquals("SAVED_CATEGORY", saved.getCategoryName());
    }

    @Test
    public void update_shouldUpdateDictionaryCategory() {
        DictionaryCategory dictionaryCategory = dictionaryCategoryService.get("1");
        dictionaryCategory.setCategoryName("UPDATED_CATEGORY");
        dictionaryCategory.setDescription("Updated Test Category");
        dictionaryCategory.setLocalAbbreviation("UPD");

        DictionaryCategory updated = dictionaryCategoryService.update(dictionaryCategory);

        assertEquals("1", updated.getId());
        assertEquals("UPDATED_CATEGORY", updated.getCategoryName());
        assertEquals("Updated Test Category", updated.getDescription());
        assertEquals("UPD", updated.getLocalAbbreviation());
    }

    @Test
    public void delete_shouldDeleteDictionaryCategory() {
        DictionaryCategory dictionaryCategory = dictionaryCategoryService.get("1");
        assertNotNull(dictionaryCategory);

        dictionaryCategoryService.delete(dictionaryCategory);

    }

    @Test
    public void insert_shouldThrowExceptionForDuplicateRecord() {
        // Get an existing category from the test dataset
        DictionaryCategory existingCategory = dictionaryCategoryService
                .getDictionaryCategoryByName("TEST_CATEGORY_ONE");
        assertNotNull("Test setup issue: Could not find category 'TEST_CATEGORY_ONE'", existingCategory);

        // Create a new category with the same name
        DictionaryCategory duplicateCategory = new DictionaryCategory();
        duplicateCategory.setCategoryName("TEST_CATEGORY_ONE");
        duplicateCategory.setDescription("Duplicate Test Category");
        duplicateCategory.setLocalAbbreviation("DUP");

        try {
            dictionaryCategoryService.insert(duplicateCategory);
            fail("Expected an exception for duplicate record");
        } catch (LIMSDuplicateRecordException e) {

        } catch (LIMSRuntimeException e) {

        }
    }

    @Test
    public void save_shouldThrowExceptionForDuplicateRecord() {

        DictionaryCategory existingCategory = dictionaryCategoryService
                .getDictionaryCategoryByName("TEST_CATEGORY_ONE");
        assertNotNull("Test setup issue: Could not find category 'TEST_CATEGORY_ONE'", existingCategory);

        DictionaryCategory duplicateCategory = new DictionaryCategory();
        duplicateCategory.setCategoryName("TEST_CATEGORY_ONE");
        duplicateCategory.setDescription("Duplicate Test Category");
        duplicateCategory.setLocalAbbreviation("DUP");

        try {
            dictionaryCategoryService.save(duplicateCategory);
            fail("Expected an exception for duplicate record");
        } catch (LIMSDuplicateRecordException e) {

        } catch (LIMSRuntimeException e) {

        }
    }

    @Test
    public void update_shouldThrowExceptionForDuplicateRecord() {

        DictionaryCategory categoryToUpdate = dictionaryCategoryService
                .getDictionaryCategoryByName("TEST_CATEGORY_TWO");
        DictionaryCategory existingCategory = dictionaryCategoryService
                .getDictionaryCategoryByName("TEST_CATEGORY_ONE");

        assertNotNull("Test setup issue: Could not find category 'TEST_CATEGORY_TWO'", categoryToUpdate);
        assertNotNull("Test setup issue: Could not find category 'TEST_CATEGORY_ONE'", existingCategory);

        categoryToUpdate.setCategoryName("TEST_CATEGORY_ONE");
        categoryToUpdate.setDescription("Updated Test Category");
        categoryToUpdate.setLocalAbbreviation("UPD");

        try {
            dictionaryCategoryService.update(categoryToUpdate);
            fail("Expected an exception for duplicate record");
        } catch (LIMSDuplicateRecordException e) {

        } catch (LIMSRuntimeException e) {

        }
    }
}
