package org.openelisglobal.testdictionary;

import java.sql.Timestamp;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.dictionarycategory.service.DictionaryCategoryService;
import org.openelisglobal.dictionarycategory.valueholder.DictionaryCategory;
import org.openelisglobal.testdictionary.service.TestDictionaryService;
import org.openelisglobal.testdictionary.valueholder.TestDictionary;
import org.springframework.beans.factory.annotation.Autowired;

public class TestDictionaryServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    TestDictionaryService testDictionaryService;

    @Autowired
    DictionaryCategoryService dictionaryCategoryService;

    private static final String TEST_ID = "103";
    private static final String TEST_CONTEXT = "New Test Context";
    private static final String DICTIONARY_CATEGORY_ID = "2";

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/test-dictionary.xml");
    }

    public void verifyTestData() {
        List<TestDictionary> testDictionaryList = testDictionaryService.getAll();

        Assert.assertFalse("The test_dictionary table should not be empty!", testDictionaryList.isEmpty());

        for (TestDictionary testDictionary : testDictionaryList) {
            Assert.assertNotNull("Test ID should not be null", testDictionary.getTestId());

            if (testDictionary.getDictionaryCategory() != null) {
                Assert.assertNotNull("Dictionary category ID should not be null",
                        testDictionary.getDictionaryCategory().getId());
            }
        }
    }

    @Test
    public void createTestDictionary_shouldCreateNewTestDictionary() throws Exception {
        cleanRowsInCurrentConnection(new String[] { "test_dictionary" });
        TestDictionary testDictionary = createTestDictionary();
        Assert.assertEquals(0, testDictionaryService.getAll().size());
        testDictionaryService.insert(testDictionary);
        Assert.assertEquals(1, testDictionaryService.getAll().size());

        TestDictionary insertedDictionary = testDictionaryService.getTestDictionaryForTestId(TEST_ID);
        Assert.assertNotNull("TestDictionary should not be null", insertedDictionary);
        Assert.assertEquals(TEST_CONTEXT, insertedDictionary.getContext());
        Assert.assertNotNull(insertedDictionary.getDictionaryCategory());
        Assert.assertEquals(DICTIONARY_CATEGORY_ID, insertedDictionary.getDictionaryCategory().getId());

        testDictionaryService.delete(testDictionary);
    }

    @Test
    public void updateTestDictionary_shouldUpdateTestDictionary() throws Exception {
        cleanRowsInCurrentConnection(new String[] { "test_dictionary" });
        TestDictionary testDictionary = createTestDictionary();
        testDictionaryService.insert(testDictionary);

        TestDictionary insertedDictionary = testDictionaryService.getTestDictionaryForTestId(TEST_ID);

        insertedDictionary.setContext("Updated Test Context");
        insertedDictionary.setDictionaryCategory(dictionaryCategoryService.get("1"));

        testDictionaryService.update(insertedDictionary);

        TestDictionary updatedDictionary = testDictionaryService.getTestDictionaryForTestId(TEST_ID);

        Assert.assertNotNull("TestDictionary should not be null", updatedDictionary);
        Assert.assertEquals("Updated Test Context", updatedDictionary.getContext());
        Assert.assertNotNull(updatedDictionary.getDictionaryCategory());
        Assert.assertEquals("1", updatedDictionary.getDictionaryCategory().getId());

        testDictionaryService.delete(testDictionary);
    }

    @Test
    public void deleteTestDictionary_shouldDeleteTestDictionary() throws Exception {
        cleanRowsInCurrentConnection(new String[] { "test_dictionary" });
        TestDictionary testDictionary = createTestDictionary();
        Assert.assertEquals(0, testDictionaryService.getAll().size());
        testDictionaryService.insert(testDictionary);
        Assert.assertEquals(1, testDictionaryService.getAll().size());

        TestDictionary insertedDictionary = testDictionaryService.getTestDictionaryForTestId(TEST_ID);
        Assert.assertNotNull("TestDictionary should not be null", insertedDictionary);
        Assert.assertEquals(TEST_CONTEXT, insertedDictionary.getContext());
        Assert.assertNotNull(insertedDictionary.getDictionaryCategory());
        Assert.assertEquals(DICTIONARY_CATEGORY_ID, insertedDictionary.getDictionaryCategory().getId());

        testDictionaryService.delete(testDictionary);
        Assert.assertEquals(0, testDictionaryService.getAll().size());
    }

    @Test
    public void getTestDictionaryForTestId_shouldReturnCorrectTestDictionary() {
        TestDictionary testDictionary = testDictionaryService.getTestDictionaryForTestId("101");
        Assert.assertNotNull("TestDictionary should not be null", testDictionary);
        Assert.assertEquals("1", testDictionary.getId());
        Assert.assertEquals("Blood Test Context", testDictionary.getContext());
        Assert.assertNotNull(testDictionary.getDictionaryCategory());
        Assert.assertEquals("2", testDictionary.getDictionaryCategory().getId()); // Blood category

        TestDictionary glucoseTestDictionary = testDictionaryService.getTestDictionaryForTestId("103");
        Assert.assertNotNull("Glucose TestDictionary should not be null", glucoseTestDictionary);
        Assert.assertEquals("3", glucoseTestDictionary.getId());
        Assert.assertEquals("Glucose Test Context", glucoseTestDictionary.getContext());
        Assert.assertNotNull(glucoseTestDictionary.getDictionaryCategory());
        Assert.assertEquals("2", glucoseTestDictionary.getDictionaryCategory().getId()); // Blood category

        TestDictionary nonExistentTestDictionary = testDictionaryService.getTestDictionaryForTestId("999");
        Assert.assertNull("TestDictionary should be null for non-existent test ID", nonExistentTestDictionary);
    }

    private TestDictionary createTestDictionary() {
        TestDictionary testDictionary = new TestDictionary();
        testDictionary.setTestId(TEST_ID);
        testDictionary.setContext(TEST_CONTEXT);
        testDictionary.setQualifiableDictionaryId("2");

        DictionaryCategory bloodCategory = dictionaryCategoryService.get(DICTIONARY_CATEGORY_ID);
        testDictionary.setDictionaryCategory(bloodCategory);

        testDictionary.setLastupdated(new Timestamp(System.currentTimeMillis()));
        return testDictionary;
    }
}
