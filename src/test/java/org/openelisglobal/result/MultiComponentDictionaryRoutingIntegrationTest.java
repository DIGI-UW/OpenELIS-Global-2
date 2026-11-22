package org.openelisglobal.result;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.ResultSaveService;
import org.openelisglobal.common.services.serviceBeans.ResultSaveBean;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.dictionarycategory.service.DictionaryCategoryService;
import org.openelisglobal.dictionarycategory.valueholder.DictionaryCategory;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.testresultcomponent.service.TestResultComponentService;
import org.openelisglobal.testresultcomponent.valueholder.TestResultComponent;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * OGC-1186 — on a test with several result components, a dictionary or
 * multi-select value has to be filed against the component it was entered on.
 * Two components can offer the same dictionary entry, so a lookup by test and
 * value alone picked whichever component's option row came first and the value
 * landed on a sibling, coercing that sibling's type or adding a phantom row.
 */
public class MultiComponentDictionaryRoutingIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String TEST_ID = "1";
    private static final String ANALYSIS_ID = "1";
    private static final String USER_ID = "1";
    private static final String NUMERIC = "RESULT_N";
    private static final String DICTIONARY = "RESULT_D";
    private static final String MULTI_SELECT = "RESULT_M";

    @Autowired
    private TestResultComponentService componentService;

    @Autowired
    private TestResultService testResultService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private DictionaryService dictionaryService;

    @Autowired
    private DictionaryCategoryService dictionaryCategoryService;

    private static final String CATEGORY_NAME = "OGC-1186 shared result options";
    private static final String ENTRY_NAME = "Detected (OGC-1186)";

    /** One dictionary entry that both selectable components offer. */
    private String sharedEntryId;

    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/analysis.xml");
        sharedEntryId = ensureSharedEntry();
    }

    private String ensureSharedEntry() {
        DictionaryCategory category = dictionaryCategoryService.getDictionaryCategoryByName(CATEGORY_NAME);
        if (category == null) {
            category = new DictionaryCategory();
            category.setCategoryName(CATEGORY_NAME);
            category.setDescription(CATEGORY_NAME);
            category.setLocalAbbreviation("OGC1186");
            category.setSysUserId(USER_ID);
            category = dictionaryCategoryService.get(dictionaryCategoryService.insert(category));
        }
        Map<String, Object> match = new HashMap<>();
        match.put("dictEntry", ENTRY_NAME);
        match.put("dictionaryCategory.categoryName", CATEGORY_NAME);
        List<Dictionary> existing = dictionaryService.getAllMatching(match);
        if (!existing.isEmpty()) {
            return existing.get(0).getId();
        }
        Dictionary entry = new Dictionary();
        entry.setDictionaryCategory(category);
        entry.setDictEntry(ENTRY_NAME);
        entry.setLocalAbbreviation("DET");
        entry.setIsActive("Y");
        entry.setSortOrder(1);
        entry.setSysUserId(USER_ID);
        return dictionaryService.insert(entry);
    }

    private TestResultComponent component(String code, int order, String type, boolean primary) {
        TestResultComponent component = new TestResultComponent();
        component.setCode(code);
        component.setLabel(code);
        component.setDisplayOrder(order);
        component.setResultType(type);
        component.setIsPrimary(primary);
        return component;
    }

    private TestResult option(String type) {
        TestResult option = new TestResult();
        option.setValue(sharedEntryId);
        option.setTestResultType(type);
        option.setSortOrder("1");
        option.setIsNormal(false);
        return option;
    }

    /**
     * Configures test 1 with a numeric primary plus a dictionary and a multi-select
     * component that both offer the same dictionary entry, in the given order, so
     * the earlier component's option row gets the lower id.
     */
    private Map<String, TestResultComponent> configure(String... codesInOrder) {
        List<TestResultComponent> components = new ArrayList<>();
        Map<String, List<TestResult>> optionsByCode = new HashMap<>();
        int order = 0;
        for (String code : codesInOrder) {
            if (NUMERIC.equals(code)) {
                components.add(component(code, order++, "N", true));
            } else if (DICTIONARY.equals(code)) {
                components.add(component(code, order++, "D", false));
                optionsByCode.put(code, List.of(option("D")));
            } else {
                components.add(component(code, order++, "M", false));
                optionsByCode.put(code, List.of(option("M")));
            }
        }
        Map<String, TestResultComponent> byCode = new HashMap<>();
        for (TestResultComponent saved : componentService.saveSampleResults(TEST_ID, components, null, optionsByCode,
                USER_ID)) {
            byCode.put(saved.getCode(), saved);
        }
        for (String code : new String[] { DICTIONARY, MULTI_SELECT }) {
            List<TestResult> options = testResultService.getActiveOptionsByComponentId(byCode.get(code).getId());
            assertEquals("each component offers the shared entry", 1, options.size());
            assertEquals(sharedEntryId, options.get(0).getValue());
        }
        return byCode;
    }

    private ResultSaveBean bean(String type, String componentId) {
        ResultSaveBean bean = new ResultSaveBean();
        bean.setResultType(type);
        bean.setTestId(TEST_ID);
        bean.setTestResultComponentId(componentId);
        bean.setReportable("Y");
        bean.setHasQualifiedResult(false);
        return bean;
    }

    private List<Result> save(ResultSaveBean bean) {
        Analysis analysis = analysisService.get(ANALYSIS_ID);
        ResultSaveService saveService = SpringContext.getBean(ResultSaveService.class);
        saveService.setAnalysis(analysis);
        saveService.setCurrentUserId(USER_ID);
        return saveService.createResultsFromTestResultItem(bean, new ArrayList<>());
    }

    @Test
    public void multiSelectValue_isFiledOnItsOwnComponent_whenASiblingOffersTheSameEntry() {
        Map<String, TestResultComponent> byCode = configure(NUMERIC, DICTIONARY, MULTI_SELECT);
        ResultSaveBean bean = bean("M", byCode.get(MULTI_SELECT).getId());
        bean.setMultiSelectResultValues("{\"0\":\"" + sharedEntryId + "\"}");

        List<Result> results = save(bean);

        assertEquals(1, results.size());
        TestResult bound = results.get(0).getTestResult();
        assertNotNull("the selection binds to an option row", bound);
        assertEquals("the value belongs to the multi-select component, not the dictionary sibling",
                byCode.get(MULTI_SELECT).getId(), bound.getComponentId());
        assertEquals("M", bound.getTestResultType());
        assertEquals(sharedEntryId, results.get(0).getValue());
    }

    @Test
    public void dictionaryValue_isFiledOnItsOwnComponent_whenTheMultiSelectRowsComeFirst() {
        Map<String, TestResultComponent> byCode = configure(NUMERIC, MULTI_SELECT, DICTIONARY);
        ResultSaveBean bean = bean("D", byCode.get(DICTIONARY).getId());
        bean.setResultValue(sharedEntryId);

        List<Result> results = save(bean);

        assertEquals(1, results.size());
        TestResult bound = results.get(0).getTestResult();
        assertNotNull("the value binds to an option row", bound);
        assertEquals("the value belongs to the dictionary component, not the multi-select sibling",
                byCode.get(DICTIONARY).getId(), bound.getComponentId());
        assertEquals("D", bound.getTestResultType());
    }

    @Test
    public void componentAwareLookup_returnsEachComponentsOwnRow() {
        Map<String, TestResultComponent> byCode = configure(NUMERIC, DICTIONARY, MULTI_SELECT);

        for (String code : new String[] { DICTIONARY, MULTI_SELECT }) {
            String componentId = byCode.get(code).getId();
            TestResult row = testResultService.getTestResultsByTestAndDictonaryResult(TEST_ID, sharedEntryId,
                    componentId);
            assertNotNull(code + " has an option row for the shared entry", row);
            assertEquals(code + " gets its own row, not a sibling's", componentId, row.getComponentId());
        }
        assertNotNull("an unknown component falls back to the test-wide match",
                testResultService.getTestResultsByTestAndDictonaryResult(TEST_ID, sharedEntryId, "no-such-component"));
        assertNotNull("a blank component keeps the test-wide match",
                testResultService.getTestResultsByTestAndDictonaryResult(TEST_ID, sharedEntryId, null));
    }

    @Test
    public void valueWithoutAComponent_keepsTheLegacyFirstMatch() {
        Map<String, TestResultComponent> byCode = configure(NUMERIC, DICTIONARY, MULTI_SELECT);
        ResultSaveBean bean = bean("D", null);
        bean.setResultValue(sharedEntryId);

        List<Result> results = save(bean);

        assertEquals(1, results.size());
        TestResult bound = results.get(0).getTestResult();
        assertNotNull("a save that names no component still binds to an option row", bound);
        assertTrue("and that row is one of this test's option rows",
                byCode.get(DICTIONARY).getId().equals(bound.getComponentId())
                        || byCode.get(MULTI_SELECT).getId().equals(bound.getComponentId()));
    }
}
