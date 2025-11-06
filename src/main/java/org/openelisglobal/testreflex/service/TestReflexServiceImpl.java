package org.openelisglobal.testreflex.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyte.service.AnalyteService;
import org.openelisglobal.analyte.valueholder.Analyte;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.exception.LIMSDuplicateRecordException;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.service.TestServiceImpl;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testanalyte.service.TestAnalyteService;
import org.openelisglobal.testanalyte.valueholder.TestAnalyte;
import org.openelisglobal.testreflex.action.bean.ReflexRule;
import org.openelisglobal.testreflex.action.bean.ReflexRuleAction;
import org.openelisglobal.testreflex.action.bean.ReflexRuleCondition;
import org.openelisglobal.testreflex.action.bean.ReflexRuleOptions.NumericRelationOptions;
import org.openelisglobal.testreflex.dao.ReflexRuleDAO;
import org.openelisglobal.testreflex.dao.TestReflexDAO;
import org.openelisglobal.testreflex.valueholder.TestReflex;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TestReflexServiceImpl extends AuditableBaseObjectServiceImpl<TestReflex, String>
        implements TestReflexService {
    @Autowired
    protected TestReflexDAO baseObjectDAO;
    @Autowired
    protected ReflexRuleDAO reflexRuleDAO;
    @Autowired
    TestReflexService reflexService;
    @Autowired
    private TestService testService;
    @Autowired
    private TestResultService testResultService;
    @Autowired
    DictionaryService dictionaryService;
    @Autowired
    TypeOfSampleService typeOfSampleService;
    @Autowired
    AnalyteService analyteService;
    @Autowired
    TestAnalyteService testAnalyteService;

    static final String REFLEX_RESULT_GROUP = "30";
    static final String REFLEX_RESULT_TYPE = "R";

    TestReflexServiceImpl() {
        super(TestReflex.class);
    }

    @Override
    protected TestReflexDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(TestReflex testReflex) {
        getBaseObjectDAO().getData(testReflex);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestReflex> getPageOfTestReflexs(int startingRecNo) {
        return getBaseObjectDAO().getPageOfTestReflexs(startingRecNo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestReflex> getTestReflexesByTestResult(TestResult testResult) {
        return getBaseObjectDAO().getTestReflexesByTestResult(testResult);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestReflex> getTestReflexsByTestAndFlag(String testId, String flag) {
        return getBaseObjectDAO().getTestReflexsByTestAndFlag(testId, flag);
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getTotalTestReflexCount() {
        return getBaseObjectDAO().getTotalTestReflexCount();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestReflex> getAllTestReflexs() {
        return getBaseObjectDAO().getAllTestReflexs();
    }

    @Override
    public boolean isReflexedTest(Analysis analysis) {
        return getBaseObjectDAO().isReflexedTest(analysis);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestReflex> getFlaggedTestReflexesByTestResult(TestResult testResult, String flag) {
        return getBaseObjectDAO().getFlaggedTestReflexesByTestResult(testResult, flag);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestReflex> getTestReflexesByTestResultAndTestAnalyte(TestResult testResult, TestAnalyte testAnalyte) {
        return getBaseObjectDAO().getTestReflexesByTestResultAndTestAnalyte(testResult, testAnalyte);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestReflex> getTestReflexsByTestResultAnalyteTest(String testResultId, String analyteId,
            String testId) {
        return getBaseObjectDAO().getTestReflexsByTestResultAnalyteTest(testResultId, analyteId, testId);
    }

    @Override
    public String insert(TestReflex testReflex) {
        if (duplicateTestReflexExists(testReflex)) {
            throw new LIMSDuplicateRecordException(
                    "Duplicate record exists for " + TestServiceImpl.getUserLocalizedTestName(testReflex.getTest())
                            + IActionConstants.BLANK + testReflex.getTestAnalyte().getAnalyte().getAnalyteName()
                            + IActionConstants.BLANK + testReflex.getTestResult().getValue() + IActionConstants.BLANK
                            + TestServiceImpl.getUserLocalizedTestName(testReflex.getAddedTest()));
        }
        return super.insert(testReflex);
    }

    @Override
    public TestReflex save(TestReflex testReflex) {
        if (duplicateTestReflexExists(testReflex)) {
            throw new LIMSDuplicateRecordException(
                    "Duplicate record exists for " + TestServiceImpl.getUserLocalizedTestName(testReflex.getTest())
                            + IActionConstants.BLANK + testReflex.getTestAnalyte().getAnalyte().getAnalyteName()
                            + IActionConstants.BLANK + testReflex.getTestResult().getValue() + IActionConstants.BLANK
                            + TestServiceImpl.getUserLocalizedTestName(testReflex.getAddedTest()));
        }
        return super.save(testReflex);
    }

    @Override
    public TestReflex update(TestReflex testReflex) {
        if (duplicateTestReflexExists(testReflex)) {
            throw new LIMSDuplicateRecordException(
                    "Duplicate record exists for " + TestServiceImpl.getUserLocalizedTestName(testReflex.getTest())
                            + IActionConstants.BLANK + testReflex.getTestAnalyte().getAnalyte().getAnalyteName()
                            + IActionConstants.BLANK + testReflex.getTestResult().getValue() + IActionConstants.BLANK
                            + TestServiceImpl.getUserLocalizedTestName(testReflex.getAddedTest()));
        }
        return super.update(testReflex);
    }

    private boolean duplicateTestReflexExists(TestReflex testReflex) {
        return baseObjectDAO.duplicateTestReflexExists(testReflex);
    }

    @Override
    public List<TestReflex> getTestReflexsByTestAnalyteId(String testAnalyteId) {
        return baseObjectDAO.getTestReflexsByTestAnalyteId(testAnalyteId);
    }

    @Override
    @Transactional()
    public void saveOrUpdateReflexRule(ReflexRule reflexRule) {
        if (reflexRule.getId() == null) {
            processReflexRule(reflexRule);
            reflexRuleDAO.insert(reflexRule);
        } else {
            processReflexRule(reflexRule);
            reflexRuleDAO.update(reflexRule);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReflexRule> getAllReflexRules() {
        return reflexRuleDAO.getAll();
    }

    @Override
    @Transactional()
    public void deactivateReflexRule(String id) {
        Optional<ReflexRule> rule = reflexRuleDAO.get(Integer.valueOf(id));
        if (rule.isPresent()) {
            // clear all the existing reflex tests
            for (ReflexRuleCondition condition : rule.get().getConditions()) {
                if (condition.getId() != null && condition.getTestAnalyteId() != null) {
                    List<TestReflex> reflexes = baseObjectDAO
                            .getTestReflexsByTestAnalyteId(condition.getTestAnalyteId().toString());
                    reflexes.forEach(r -> baseObjectDAO.delete(r));
                }
            }
            rule.get().setActive(false);
            reflexRuleDAO.update(rule.get());
        }
    }

    private void processReflexRule(ReflexRule rule) {
        Analyte analyte = null;
        if (rule.getId() != null && rule.getAnalyteId() != null) {
            analyte = analyteService.get(rule.getAnalyteId().toString());
            analyte.setAnalyteName(rule.getRuleName());
            analyte = analyteService.update(analyte);
        } else {
            analyte = new Analyte();
            analyte.setAnalyteName(rule.getRuleName());
            analyte.setIsActive(IActionConstants.YES);
            analyte = analyteService.save(analyte);
            rule.setAnalyteId(Integer.valueOf(analyte.getId()));
        }

        // clear all the existing reflex tests
        for (ReflexRuleCondition condition : rule.getConditions()) {
            if (condition.getId() != null && condition.getTestAnalyteId() != null) {
                List<TestReflex> reflexes = baseObjectDAO
                        .getTestReflexsByTestAnalyteId(condition.getTestAnalyteId().toString());
                reflexes.forEach(r -> baseObjectDAO.delete(r));
            }
        }

        for (ReflexRuleCondition condition : rule.getConditions()) {
            if (testAndSampleMatches(condition.getTestId(), condition.getSampleId())) {
                TestAnalyte testAnalyte = null;
                Test triggerTest = null;
                if (condition.getId() != null && condition.getTestAnalyteId() != null) {
                    testAnalyte = testAnalyteService.get(condition.getTestAnalyteId().toString());
                    triggerTest = testService.getTestById(condition.getTestId());
                    testAnalyte.setTest(triggerTest);
                    testAnalyte = testAnalyteService.update(testAnalyte);
                } else {
                    testAnalyte = new TestAnalyte();
                    testAnalyte.setAnalyte(analyte);
                    triggerTest = testService.getTestById(condition.getTestId());
                    testAnalyte.setTest(triggerTest);
                    testAnalyte.setResultGroup(REFLEX_RESULT_GROUP);
                    testAnalyte.setTestAnalyteType(REFLEX_RESULT_TYPE);
                    testAnalyte = testAnalyteService.save(testAnalyte);
                    condition.setTestAnalyteId(Integer.valueOf(testAnalyte.getId()));
                }
                for (ReflexRuleAction action : rule.getActions()) {
                    TestReflex reflex = new TestReflex();
                    setTestReflexTest(triggerTest, condition, action, reflex, testAnalyte);
                    reflexService.save(reflex);
                    action.setTestReflexId(Integer.valueOf(reflex.getId()));
                }
            }
        }
    }

    private void setTestReflexTest(Test triggerTest, ReflexRuleCondition condition, ReflexRuleAction action,
            TestReflex reflex, TestAnalyte testAnalyte) {
        List<TestResult> results = testResultService.getActiveTestResultsByTest(triggerTest.getId());
        if (testService.getResultType(triggerTest).equals("D")) {
            Optional<TestResult> result = results.stream().filter(res -> res.getValue().equals(condition.getValue()))
                    .findFirst();
            if (result.isPresent()) {
                reflex.setTestResult(result.get());
            } else {
                reflex.setTestResult(results.get(0));
            }
        } else {
            reflex.setTestResult(results.get(0));
            if (testService.getResultType(triggerTest).equals("N")) {
                Double value = Double.parseDouble(condition.getValue());
                Double value2 = Double.parseDouble(condition.getValue2());
                if (condition.getRelation().equals(NumericRelationOptions.BETWEEN)) {
                    reflex.setNonDictionaryValue(value.toString() + "-" + value2.toString());
                } else {
                    reflex.setNonDictionaryValue(value.toString());
                }
            } else {
                reflex.setNonDictionaryValue(condition.getValue());
            }
        }
        reflex.setRelation(condition.getRelation());
        reflex.setTestAnalyte(testAnalyte);
        reflex.setTest(triggerTest);
        reflex.setInternalNote(action.getInternalNote());
        reflex.setExternalNote(action.getExternalNote());
        if (testAndSampleMatches(action.getReflexTestId(), action.getSampleId())) {
            Test reflexTest = testService.getTestById(action.getReflexTestId());
            reflex.setAddedTest(reflexTest);
        }
    }

    private Boolean testAndSampleMatches(String testId, String sampleTypeId) {
        List<Test> testList = typeOfSampleService.getActiveTestsBySampleTypeId(sampleTypeId, false);
        List<String> testIdList = new ArrayList<>();
        testList.forEach(test -> {
            testIdList.add(test.getId());
        });
        return testIdList.contains(testId);
    }

    @Override
    public List<TestReflex> getTestReflexsByAnalyteAndTest(String analyteId, String testId) {
        return getBaseObjectDAO().getTestReflexsByAnalyteAndTest(analyteId, testId);
    }

    @Override
    public ReflexRule getReflexRuleByAnalyteId(String analyteId) {
        return reflexRuleDAO.getReflexRuleByAnalyteId(analyteId);
    }
}
