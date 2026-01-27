package org.openelisglobal.testconfiguration;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.testconfiguration.form.ResultSelectListForm;
import org.openelisglobal.testconfiguration.form.ResultSelectListRenameForm;
import org.openelisglobal.testconfiguration.service.ResultSelectListService;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.springframework.beans.factory.annotation.Autowired;

public class ResultSelectListServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ResultSelectListService resultSelectListService;
    @Autowired
    private TestService testService;
    @Autowired
    private TestResultService testResultService;
    @Autowired
    private DictionaryService dictionaryService;
    @Autowired
    private LocalizationService localizationService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/result-select-list.xml");
    }

    @Test
    public void addResultSelectList_ShouldInsertANewResultSelectionList() {
        String jsonString = "{" + "  \"tests\": \"[" + "    {" + "      \\\"id\\\": \\\"1\\\","
                + "      \\\"name\\\": \\\"Test A\\\"," + "      \\\"items\\\": ["
                + "        { \\\"id\\\": \\\"201\\\", \\\"name\\\": \\\"item A\\\", \\\"order\\\": 3 }" + "      ]"
                + "    }," + "    {" + "      \\\"id\\\": \\\"2\\\"," + "      \\\"name\\\": \\\"Test B\\\","
                + "      \\\"items\\\": ["
                + "        { \\\"id\\\": \\\"202\\\", \\\"name\\\": \\\"item C\\\", \\\"order\\\": 5 }" + "      ]"
                + "    }" + "  ]\"" + "}";

        List<org.openelisglobal.test.valueholder.Test> testList = testService.getAll();
        ResultSelectListForm form = new ResultSelectListForm();
        form.setNameEnglish("Tomorrow");
        form.setNameFrench("Demain");
        form.setLoincCode("677832");
        form.setTestSelectListJson(jsonString);
        form.setTests(testList);

        List<Dictionary> initialDictionaries = dictionaryService.getAll();
        int initialDictionaryCount = initialDictionaries.size();
        assertEquals(3, initialDictionaryCount);
        assertFalse(initialDictionaries.stream().anyMatch(dict -> "Tomorrow".equals(dict.getDictEntry())));

        TestResult initialTestResult = testResultService.get("1");
        assertEquals("1", initialTestResult.getSortOrder());

        boolean result = resultSelectListService.addResultSelectList(form, "6702");
        assertTrue(result);
        List<Dictionary> newDictionaries = dictionaryService.getAll();
        assertEquals((initialDictionaryCount + 1), newDictionaries.size());
        assertTrue(newDictionaries.stream().anyMatch(dict -> "Tomorrow".equals(dict.getDictEntry())));

        TestResult newTestResult = testResultService.get("1");
        assertEquals("30", newTestResult.getSortOrder());
    }

    @Test
    public void getAllSelectListOptions() {
        List<Dictionary> dictionaryList = dictionaryService.getAll();
        assertEquals(3, dictionaryList.size());

        List<Dictionary> selectedDictionaries = resultSelectListService.getAllSelectListOptions();
        assertEquals(2, selectedDictionaries.size());
        assertEquals("Dictionary Entry 1", selectedDictionaries.get(0).getDictEntry());
        assertEquals("Dictionary Entry 2", selectedDictionaries.get(1).getDictEntry());

    }

    @Test
    public void getTestSelectDictionary_ShouldReturnATestDictionary() {
        Map<String, List<IdValuePair>> selectDictionaries = resultSelectListService.getTestSelectDictionary();
        assertEquals(1, selectDictionaries.size());
        assertEquals("201", selectDictionaries.get("1").get(0).getId());
        assertEquals("Dictionary Entry 1", selectDictionaries.get("1").get(0).getValue());
    }

    @Test
    public void renameOption() {
        ResultSelectListRenameForm form = new ResultSelectListRenameForm();
        form.setNameEnglish("Tomorrow");
        form.setNameFrench("Demain");
        form.setResultSelectOptionId("203");

        List<Localization> initialLocalizations = localizationService.getAll();
        int initialLocalizationCount = initialLocalizations.size();
        assertFalse(initialLocalizations.stream().anyMatch(loc -> "Tomorrow".equals(loc.getEnglish())));

        List<Dictionary> initialDictionaries = dictionaryService.getAll();
        assertFalse(initialDictionaries.stream().anyMatch(dict -> "Tomorrow".equals(dict.getDictEntry())));

        boolean isOptionRenamed = resultSelectListService.renameOption(form, "6901");
        assertTrue(isOptionRenamed);

        List<Localization> newLocalizations = localizationService.getAll();
        assertEquals((initialLocalizationCount + 1), newLocalizations.size());
        assertTrue(newLocalizations.stream().anyMatch(loc -> "Tomorrow".equals(loc.getEnglish())));

        List<Dictionary> newDictionaries = dictionaryService.getAll();
        assertTrue(newDictionaries.stream().anyMatch(dict -> "Tomorrow".equals(dict.getDictEntry())));
    }
}
