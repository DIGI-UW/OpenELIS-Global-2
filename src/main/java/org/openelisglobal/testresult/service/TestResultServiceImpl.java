package org.openelisglobal.testresult.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testanalyte.valueholder.TestAnalyte;
import org.openelisglobal.testresult.dao.TestResultDAO;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TestResultServiceImpl extends AuditableBaseObjectServiceImpl<TestResult, String>
        implements TestResultService {
    @Autowired
    protected TestResultDAO baseObjectDAO;

    @Autowired
    private DictionaryService dictionaryService;

    TestResultServiceImpl() {
        super(TestResult.class);
    }

    @Override
    protected TestResultDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestResult> getAllActiveTestResultsPerTest(Test test) {
        Map<String, Object> propertyValues = new HashMap<>();
        List<String> orderProperties = new ArrayList<>();
        propertyValues.put("test.id", test.getId());
        propertyValues.put("isActive", true);
        orderProperties.add("resultGroup");
        orderProperties.add("id");
        return baseObjectDAO.getAllMatchingOrdered(propertyValues, orderProperties, true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestResult> getActiveTestResultsByTest(String testId) {
        Map<String, Object> propertyValues = new HashMap<>();
        propertyValues.put("test.id", testId);
        propertyValues.put("isActive", true);
        return baseObjectDAO.getAllMatching(propertyValues);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestResult> getActiveOptionsByComponentId(String componentId) {
        Map<String, Object> propertyValues = new HashMap<>();
        propertyValues.put("componentId", componentId);
        propertyValues.put("isActive", true);
        List<TestResult> options = baseObjectDAO.getAllMatching(propertyValues);

        options.removeIf(o -> !TypeOfTestResultServiceImpl.ResultType.isDictionaryVariant(o.getTestResultType()));
        // SORT_ORDER is a numeric column mapped as String; sort numerically, nulls
        // last.
        options.sort(Comparator.comparingInt(o -> parseSortOrder(o.getSortOrder())));
        return options;
    }

    private static int parseSortOrder(String s) {
        if (s == null || s.isBlank()) {
            return Integer.MAX_VALUE;
        }
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    @Override
    @Transactional
    public List<TestResult> saveOptionsForComponent(Test test, String componentId, List<TestResult> desired,
            String sysUserId) {
        List<TestResult> existing = getActiveOptionsByComponentId(componentId);
        Map<String, TestResult> existingById = new HashMap<>();
        for (TestResult e : existing) {
            existingById.put(e.getId(), e);
        }
        Set<String> keptIds = new HashSet<>();
        for (TestResult d : desired) {
            resolveDictionaryValue(d, sysUserId);
            TestResult match = d.getId() == null ? null : existingById.get(d.getId());
            if (match != null) {
                match.setValue(d.getValue());
                match.setSortOrder(d.getSortOrder());
                match.setIsNormal(d.getIsNormal());
                match.setTestResultType(d.getTestResultType());
                match.setSysUserId(sysUserId);
                update(match);
                keptIds.add(match.getId());
            } else {
                // New option: id is sequence-assigned on insert; FK to the (persistent) test.
                d.setTest(test);
                d.setComponentId(componentId);
                d.setIsActive(true);
                d.setSysUserId(sysUserId);
                insert(d);
            }
        }
        for (TestResult e : existing) {
            if (!keptIds.contains(e.getId())) {
                e.setIsActive(false);
                e.setSysUserId(sysUserId);
                update(e);
            }
        }
        return getActiveOptionsByComponentId(componentId);
    }

    /**
     * Dictionary-variant option rows must hold a numeric dictionary id in VALUE —
     * every consumer resolves it via {@code getDictionaryById}. A free-text option
     * typed in the Test Catalog Editor (FR-83) arrives here as raw text, so it is
     * materialized into the dictionary master list (reusing an active entry with
     * the same name if one exists) and the row repointed at the entry's id. A
     * numeric value that already resolves to an entry is kept as-is; a numeric
     * value that resolves to nothing is treated as free text too.
     */
    private void resolveDictionaryValue(TestResult option, String sysUserId) {
        if (!TypeOfTestResultServiceImpl.ResultType.isDictionaryVariant(option.getTestResultType())) {
            return;
        }
        String value = option.getValue() == null ? "" : option.getValue().trim();
        if (value.isEmpty()) {
            return;
        }
        if (StringUtils.isNumeric(value) && dictionaryService.getDictionaryById(value) != null) {
            return;
        }
        Map<String, Object> properties = new HashMap<>();
        properties.put("dictEntry", value);
        properties.put("isActive", IActionConstants.YES);
        List<Dictionary> matches = dictionaryService.getAllMatching(properties);
        if (!matches.isEmpty()) {
            matches.sort(Comparator.comparingInt(d -> Integer.parseInt(d.getId())));
            option.setValue(matches.get(0).getId());
            return;
        }
        Dictionary dictionary = new Dictionary();
        dictionary.setDictEntry(value);
        dictionary.setIsActive(IActionConstants.YES);
        dictionary.setSysUserId(sysUserId);
        option.setValue(dictionaryService.insert(dictionary));
    }

    @Override
    @Transactional(readOnly = true)
    public TestResult getTestResultsByTestAndDictonaryResult(String id, String value) {
        return baseObjectDAO.getTestResultsByTestAndDictonaryResult(id, value);
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(TestResult testResult) {
        getBaseObjectDAO().getData(testResult);
    }

    @Override
    @Transactional(readOnly = true)
    public TestResult getTestResultById(TestResult testResult) {
        return getBaseObjectDAO().getTestResultById(testResult);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestResult> getPageOfTestResults(int startingRecNo) {
        return getBaseObjectDAO().getPageOfTestResults(startingRecNo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestResult> getAllTestResults() {
        return getBaseObjectDAO().getAllTestResults();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestResult> getTestResultsByTestAndResultGroup(TestAnalyte testAnalyte) {
        return getBaseObjectDAO().getTestResultsByTestAndResultGroup(testAnalyte);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestResult> getAllSortedTestResults() {
        List<TestResult> testResults = getBaseObjectDAO().getAllTestResults();
        Collections.sort(testResults, new Comparator<TestResult>() {
            @Override
            public int compare(TestResult o1, TestResult o2) {
                int result = o1.getTest().getId().compareTo(o2.getTest().getId());

                if (result != 0) {
                    return result;
                }

                String so1 = o1.getSortOrder();
                String so2 = o2.getSortOrder();

                if (so1 == so2) {
                    return 0;
                } else if (so1 == null) {
                    return -1;
                } else if (so2 == null) {
                    return 1;
                } else {
                    return Integer.parseInt(o1.getSortOrder()) - Integer.parseInt(o2.getSortOrder());
                }
            }
        });
        return testResults;
    }

}
