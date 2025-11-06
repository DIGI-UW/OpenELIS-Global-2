package org.openelisglobal.testconfiguration.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.dictionarycategory.service.DictionaryCategoryService;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testconfiguration.form.ResultSelectListForm;
import org.openelisglobal.testconfiguration.form.ResultSelectListRenameForm;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ResultSelectListServiceImpl implements ResultSelectListService {

    @Autowired
    private DictionaryService dictionaryService;
    @Autowired
    private DictionaryCategoryService dictionaryCategoryService;
    @Autowired
    private TestService testService;
    @Autowired
    private TestResultService resultService;
    @Autowired
    private LocalizationService localizationService;

    @Override
    public Map<String, List<IdValuePair>> getTestSelectDictionary() {
        List<TestResult> testResults = resultService.getAllSortedTestResults();

        Map<String, List<IdValuePair>> testDictionary = new HashMap<>();
        String currentTestId = null;
        String dictionaryIdGroup = null;
        for (TestResult testResult : testResults) {
            if (TypeOfTestResultServiceImpl.ResultType.isDictionaryVariant(testResult.getTestResultType())) {
                if (testResult.getTest().getId().equals(currentTestId)) {
                    dictionaryIdGroup += "," + testResult.getValue();
                } else {
                    String previousTestId = currentTestId;
                    currentTestId = testResult.getTest().getId();
                    if (dictionaryIdGroup != null) {
                        List<IdValuePair> pairs = new ArrayList<>();
                        String[] dictionaryIds = dictionaryIdGroup.split(",");
                        for (String id : dictionaryIds) {
                            Dictionary dictionary = dictionaryService.getDictionaryById(id);
                            if (dictionary != null) {
                                pairs.add(new IdValuePair(id, dictionary.getLocalizedName()));
                            }
                        }
                        testDictionary.put(previousTestId, pairs);
                    }
                    dictionaryIdGroup = testResult.getValue();
                }
            }
        }
        return testDictionary;
    }

    @Override
    public List<Dictionary> getAllSelectListOptions() {
        List<Dictionary> dictionaries = new ArrayList<>();
        List<TestResult> testResults = resultService.getAllSortedTestResults();
        List<String> ids = new ArrayList<>();

        for (TestResult testResult : testResults) {
            if (TypeOfTestResultServiceImpl.ResultType.isDictionaryVariant(testResult.getTestResultType())) {
                if (!ids.contains(testResult.getValue())) {
                    ids.add(testResult.getValue());
                }
            }
        }
        for (String id : ids) {
            Dictionary dictionary = dictionaryService.getDictionaryById(id);
            dictionaries.add(dictionary);
        }
        return dictionaries;
    }

    @Override
    public boolean addResultSelectList(ResultSelectListForm form, String currentUserId) {

        Dictionary dictionary = new Dictionary();
        dictionary.setSortOrder(1);
        dictionary.setIsActive("Y");
        dictionary.setDictEntry(form.getNameEnglish());
        dictionary.setLocalAbbreviation(form.getNameEnglish());
        dictionary.setSysUserId(currentUserId);
        dictionary.setLoincCode(form.getLoincCode() != null ? form.getLoincCode() : "");
        dictionary.setDictionaryCategory(dictionaryCategoryService.getDictionaryCategoryByName("Test Result"));

        Localization localization = new Localization();
        localization.setEnglish(form.getNameEnglish());
        localization.setFrench(form.getNameFrench());
        localization.setSysUserId(currentUserId);
        localization = localizationService.save(localization);

        dictionary.setLocalizedDictionaryName(localization);
        dictionary = dictionaryService.save(dictionary);

        String s = form.getTestSelectListJson();

        JSONParser parser = new JSONParser();
        try {
            Object parsed = parser.parse(s);
            JSONArray tests;
            if (parsed instanceof JSONArray) {
                tests = (JSONArray) parsed;
            } else if (parsed instanceof JSONObject) {
                JSONObject obj = (JSONObject) parsed;
                String testsStr = (String) obj.get("tests");
                tests = (JSONArray) parser.parse(testsStr);
            } else {
                throw new IllegalArgumentException("Invalid testSelectListJson format");
            }
            for (int j = 0; j < tests.size(); j++) {
                JSONObject testObject = (JSONObject) tests.get(j);

                String testId = (String) testObject.get("id");
                Test test = testService.getTestById(testId);
                JSONArray items = (JSONArray) testObject.get("items");

                for (int i = 0; i < items.size(); i++) {
                    JSONObject object = (JSONObject) items.get(i);

                    if (object.containsKey("id")) {
                        Map<String, Object> filter = new HashMap<>();
                        filter.put("test.id", testId);
                        filter.put("value", object.get("id"));
                        Optional<TestResult> testResult = resultService.getMatch(filter); // get((String)
                                                                                          // object.get("id"));
                        long order = (Long) object.get("order");
                        if (testResult.isPresent()) {
                            testResult.get().setSortOrder(String.valueOf(10 * order));
                            testResult.get().setSysUserId(currentUserId);
                            resultService.save(testResult.get());
                        }

                    } else {
                        TestResult testResult = new TestResult();
                        testResult.setIsQuantifiable((Boolean) object.get("qualifiable"));
                        testResult.setIsNormal((Boolean) object.get("normal"));
                        testResult.setValue(dictionary.getId());
                        long order = (Long) object.get("order");
                        testResult.setSortOrder(String.valueOf(order * 10));
                        testResult.setTest(test);
                        testResult.setTestResultType("D");
                        testResult.setResultGroup("");
                        testResult.setSysUserId(currentUserId);
                        resultService.save(testResult);
                    }
                }
            }
            return true;
        } catch (ParseException e) {
            LogEvent.logError(e);
        }
        return false;
    }

    @Override
    public boolean renameOption(ResultSelectListRenameForm form, String currentUserId) {
        try {
            Dictionary dictionary = dictionaryService.getDictionaryById(form.getResultSelectOptionId());

            Localization localization = dictionary.getLocalizedDictionaryName();
            if (localization == null) {
                localization = new Localization();
            }
            localization.setEnglish(form.getNameEnglish());
            localization.setFrench(form.getNameFrench());
            localization.setSysUserId(currentUserId);
            localizationService.save(localization);

            dictionary.setDictEntry(form.getNameEnglish());
            dictionary.setLocalAbbreviation(form.getNameEnglish());
            dictionary.setSysUserId(currentUserId);
            dictionaryService.save(dictionary);
            return true;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
        }
        return false;
    }
}
