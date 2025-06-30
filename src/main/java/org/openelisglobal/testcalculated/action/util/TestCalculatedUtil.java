package org.openelisglobal.testcalculated.action.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import org.apache.commons.lang3.StringUtils;
import org.jfree.util.Log;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.note.service.NoteServiceImpl.NoteType;
import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.result.action.util.ResultSet;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.resultlimit.service.ResultLimitService;
import org.openelisglobal.resultlimits.valueholder.ResultLimit;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testcalculated.service.ResultCalculationService;
import org.openelisglobal.testcalculated.service.TestCalculationService;
import org.openelisglobal.testcalculated.valueholder.Calculation;
import org.openelisglobal.testcalculated.valueholder.Operation;
import org.openelisglobal.testcalculated.valueholder.ResultCalculation;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

@Service
@DependsOn({ "springContext" })
public class TestCalculatedUtil {

    private TestResultService testResultService = SpringContext.getBean(TestResultService.class);

    private ResultCalculationService resultcalculationService = SpringContext.getBean(ResultCalculationService.class);

    private TestCalculationService calculationService = SpringContext.getBean(TestCalculationService.class);

    private TestService testService = SpringContext.getBean(TestService.class);

    private ResultService resultService = SpringContext.getBean(ResultService.class);

    private AnalysisService analysisService = SpringContext.getBean(AnalysisService.class);

    private NoteService noteService = SpringContext.getBean(NoteService.class);

    private ResultLimitService resultLimitService = SpringContext.getBean(ResultLimitService.class);

    private String CALCULATION_SUBJECT = "Calculated Result Note";

    public List<Analysis> addNewTestsToDBForCalculatedTests(List<ResultSet> resultSetList, String sysUserId)
            throws IllegalStateException {
        List<Analysis> analyses = new ArrayList<>();
        for (ResultSet resultSet : resultSetList) {
            if (resultSet.result == null) {
                continue;
            }
            if (resultSet.result.getTestResult() == null) {
                continue;
            }
            List<Calculation> calculations = calculationService.getAll();
            for (Calculation calculation : calculations) {
                if (!calculation.getActive()) {
                    continue;
                }
                List<ResultCalculation> resultCalculations = resultcalculationService
                        .getResultCalculationByPatientAndCalculation(resultSet.patient, calculation);

                if (resultCalculations.isEmpty()) {
                    Boolean createResultCalculation = false;
                    for (Operation oper : calculation.getOperations()) {
                        if (oper.getType().equals(Operation.OperationType.TEST_RESULT)) {
                            if (Integer.valueOf(oper.getValue())
                                    .equals(Integer.valueOf(resultSet.result.getTestResult().getTest().getId()))) {
                                createResultCalculation = true;
                                break;
                            }
                        }
                    }
                    if (createResultCalculation) {
                        ResultCalculation calc = new ResultCalculation();
                        calc.setCalculation(calculation);
                        calc.setPatient(resultSet.patient);
                        Set<Test> tests = new HashSet<>();
                        calculation.getOperations().forEach(oper -> {
                            if (oper.getType().equals(Operation.OperationType.TEST_RESULT)) {
                                Test test = testService.getActiveTestById(Integer.valueOf(oper.getValue()));
                                tests.add(test);
                            }
                        });
                        calc.setTest(tests);
                        Map<Integer, Integer> map = new HashMap<>();
                        tests.forEach(test -> {
                            map.put(Integer.valueOf(test.getId()), null);
                        });
                        // insert innitial result value
                        if (resultSet.result.getTestResult().getTest().getId() != null
                                && resultSet.result.getId() != null) {
                            map.put(Integer.valueOf(resultSet.result.getTestResult().getTest().getId()),
                                    Integer.valueOf(resultSet.result.getId()));
                        }
                        calc.setTestResultMap(map);
                        resultcalculationService.insert(calc);
                    }

                } else {
                    for (ResultCalculation resultCalculation : resultCalculations) {
                        if (resultSet.result.getTestResult().getTest().getId() != null
                                && resultSet.result.getId() != null) {
                            resultCalculation.getTestResultMap().put(
                                    Integer.valueOf(resultSet.result.getTestResult().getTest().getId()),
                                    Integer.valueOf(resultSet.result.getId()));
                        }

                        resultcalculationService.update(resultCalculation);
                    }
                }
            }
        }

        for (ResultSet resultSet : resultSetList) {
            if (resultSet.result == null) {
                continue;
            }
            List<ResultCalculation> resultCalculations = new ArrayList<>();
            if (resultSet.result.getTestResult() == null) {
                continue;
            } else {
                resultCalculations = resultcalculationService.getResultCalculationByPatientAndTest(resultSet.patient,
                        resultSet.result.getTestResult().getTest());
            }

            if (!resultCalculations.isEmpty()) {
                for (ResultCalculation resultCalculation : resultCalculations) {
                    Boolean isMissingParams = false;
                    for (Map.Entry<Integer, Integer> entry : resultCalculation.getTestResultMap().entrySet()) {
                        if (entry.getValue() == null) {
                            isMissingParams = true;
                            break;
                        }
                    }
                    Calculation calculation = resultCalculation.getCalculation();
                    if (!isMissingParams) {
                        StringBuffer function = new StringBuffer();
                        calculation.getOperations().forEach(operation -> {
                            switch (operation.getType()) {
                            case TEST_RESULT:
                                addNumericOperation(operation, resultCalculation, function,
                                        Operation.OperationType.TEST_RESULT.toString());
                                break;
                            case INTEGER:
                                try {
                                    if (operation.getValue().contains(".")) {
                                        double val = Double.parseDouble(operation.getValue());
                                        function.append(val).append(" ");
                                    } else {
                                        int number = Integer.parseInt(operation.getValue());
                                        function.append(number).append(" ");
                                    }
                                } catch (NumberFormatException e) {

                                }
                                break;
                            case MATH_FUNCTION:
                                if (operation.getValue().equals(Operation.IN_NORMAL_RANGE)) {
                                    int order = operation.getOrder();
                                    Operation prevOperation = calculation.getOperations().get(order - 1);
                                    addNumericOperation(prevOperation, resultCalculation, function,
                                            Operation.IN_NORMAL_RANGE);

                                } else if (operation.getValue().equals(Operation.OUTSIDE_NORMAL_RANGE)) {
                                    int order = operation.getOrder();
                                    Operation prevOperation = calculation.getOperations().get(order - 1);
                                    addNumericOperation(prevOperation, resultCalculation, function,
                                            Operation.OUTSIDE_NORMAL_RANGE);
                                } else {
                                    function.append(operation.getValue()).append(" ");
                                }
                                break;
                            case PATIENT_ATTRIBUTE:
                                if (operation.getValue().equals(Operation.PatientAttribute.AGE.toString())) {
                                    int age = DateUtil.getAgeInYears(
                                            new Date(resultSet.patient.getBirthDate().getTime()), new Date());
                                    function.append(age);
                                }
                                break;
                            }
                        });
                        ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
                        ScriptEngine scriptEngine = scriptEngineManager.getEngineByName("JavaScript");
                        String value = null;
                        try {
                            Log.debug("Caliculation Rule: " + calculation.getName() + " Function : "
                                    + function.toString());
                            value = scriptEngine.eval(function.toString()).toString();
                            Log.debug("Caliculation Rule: " + calculation.getName() + " Value  : " + value);
                        } catch (ScriptException e) {
                            Log.error("Invalid Caliculation Rule: " + calculation.getName(), e);
                        }
                        Analysis analysis = createCalculatedResult(resultCalculation, resultSet, calculation, value,
                                sysUserId);
                        if (analysis != null) {
                            analyses.add(analysis);
                        }

                    } else {
                        Analysis analysis = createCalculatedResult(resultCalculation, resultSet, calculation, null,
                                sysUserId);
                        if (analysis != null) {
                            analyses.add(analysis);
                        }
                    }
                }
            }
        }
        return analyses;
    }

    private Analysis createCalculatedResult(ResultCalculation resultCalculation, ResultSet resultSet,
            Calculation calculation, String value, String systemUserId) {
        Test test = testService.get(calculation.getTestId().toString());
        String resultType = testService.getResultType(test);
        Analysis analysis = null;
        if (test != null) {
            if (resultCalculation.getTestResultMap().containsKey(Integer.valueOf(test.getId()))) {
                if (Boolean.valueOf(value)) {
                    if (StringUtils.isNotBlank(calculation.getNote())) {
                        Note note = noteService.createSavableNote(resultSet.result.getAnalysis(), NoteType.EXTERNAL,
                                calculation.getNote(), CALCULATION_SUBJECT, systemUserId);
                        if (!noteService.duplicateNoteExists(note)) {
                            noteService.save(note);
                        }
                    }
                }
                return analysis;
            }
            TestResult testResult = getTestResultForCalculation(calculation);
            Result result = null;
            if (resultCalculation.getResult() != null) {
                result = resultCalculation.getResult();
            } else {
                result = new Result();
            }
            result.setTestResult(testResult);
            ResultLimit resultLimit = resultLimitService.getResultLimitForTestAndPatient(test.getId(),
                    resultCalculation.getPatient());
            if (resultLimit != null) {
                result.setMaxNormal(resultLimit.getHighNormal());
                result.setMinNormal(resultLimit.getLowNormal());
            }
            if (testResult.getSignificantDigits() != null) {
                result.setSignificantDigits(Integer.valueOf(testResult.getSignificantDigits()));
            }
            result.setResultType(testService.getResultType(test));
            result.setSysUserId(systemUserId);
            Boolean resultCalculated = false;
            if (value != null) {
                if ("D".equals(resultType)) {
                    if (Boolean.valueOf(value)) {
                        result.setValue(calculation.getResult());
                        resultCalculated = true;
                    } else {
                        result.setValue("");
                    }
                } else if ("R".equals(resultType) || "A".equals(resultType)) {
                    if (Boolean.valueOf(value)) {
                        result.setValue(calculation.getResult());
                        resultCalculated = true;
                    } else {
                        result.setValue("");
                    }
                } else if ("N".equals(resultType)) {
                    if (testResult.getSignificantDigits() != null) {
                        double factor = Math.pow(10, Double.valueOf(testResult.getSignificantDigits()));
                        value = String.valueOf(Math.round(Double.valueOf(value) * factor) / factor);
                    }
                    result.setValue(value);
                    resultCalculated = true;
                }
            } else {
                result.setValue("");
            }
            if (resultCalculation.getResult() != null) {
                analysis = createCalculatedAnalysis(resultCalculation.getResult().getAnalysis(), test, resultSet.result,
                        value, calculation.getName(), systemUserId, resultCalculated, calculation.getNote());
                result.setAnalysis(analysis);
                resultService.update(result);
            } else {
                analysis = createCalculatedAnalysis(null, test, resultSet.result, value, calculation.getName(),
                        systemUserId, resultCalculated, calculation.getNote());
                result.setAnalysis(analysis);
                resultService.insert(result);
            }
            resultCalculation.setResult(result);
            resultcalculationService.update(resultCalculation);
        }
        return analysis;
    }

    private void createInternalNote(Analysis newAnalysis, Analysis currentAnalysis, String calculatioName,
            String systemUserId, String externalNote) {
        List<Note> notes = new ArrayList<>();
        Note note = noteService.createSavableNote(newAnalysis, NoteType.INTERNAL,
                "Result Succesfully Calculated From Calculation Rule :" + calculatioName, CALCULATION_SUBJECT,
                systemUserId);
        if (!noteService.duplicateNoteExists(note)) {
            notes.add(note);
        }

        Note note2 = noteService.createSavableNote(newAnalysis, NoteType.INTERNAL,
                "Calculation Parameters include Result of Test "
                        + currentAnalysis.getTest().getLocalizedReportingName().getLocalizedValue(),
                CALCULATION_SUBJECT, systemUserId);
        if (!noteService.duplicateNoteExists(note2)) {
            notes.add(note2);
        }

        if (StringUtils.isNotBlank(externalNote)) {
            Note note3 = noteService.createSavableNote(newAnalysis, NoteType.EXTERNAL, externalNote,
                    CALCULATION_SUBJECT, systemUserId);
            if (!noteService.duplicateNoteExists(note3)) {
                notes.add(note3);
            }
        }

        noteService.saveAll(notes);
    }

    private void createMissingValueInternalNote(Analysis newAnalysis, Analysis currentAnalysis, String calculatioName,
            String systemUserId) {
        List<Note> notes = new ArrayList<>();
        Note note = noteService.createSavableNote(newAnalysis, NoteType.INTERNAL,
                "Result Missing Calculation Parameters From Calculation Rule : " + calculatioName, CALCULATION_SUBJECT,
                systemUserId);
        if (!noteService.duplicateNoteExists(note)) {
            notes.add(note);
        }
        Note note2 = noteService.createSavableNote(newAnalysis, NoteType.INTERNAL,
                "Calculation Parameters include Result of Test : "
                        + currentAnalysis.getTest().getLocalizedReportingName().getLocalizedValue(),
                CALCULATION_SUBJECT, systemUserId);
        if (!noteService.duplicateNoteExists(note2)) {
            notes.add(note2);
        }
        noteService.saveAll(notes);
    }

    private TestResult getTestResultForCalculation(Calculation calculation) {
        Test test = testService.get(calculation.getTestId().toString());
        String resultType = testService.getResultType(test);
        if ("D".equals(resultType)) {
            TestResult testResult;
            testResult = testResultService.getTestResultsByTestAndDictonaryResult(test.getId(),
                    calculation.getResult());
            return testResult;
        } else {
            List<TestResult> testResultList = testResultService.getActiveTestResultsByTest(test.getId());
            // we are assuming there is only one testResult for a numeric
            // type result
            if (!testResultList.isEmpty()) {
                // get the latest modified test result
                return testResultList.get(testResultList.size() - 1);
            }
        }

        return null;
    }

    private void addNumericOperation(Operation operation, ResultCalculation resultCalculation, StringBuffer function,
            String inputType) {
        Test test = testService.getActiveTestById(Integer.valueOf(operation.getValue()));
        if (test != null) {
            Integer resultId = resultCalculation.getTestResultMap().get(Integer.valueOf(test.getId()));
            Result result = null;
            if (resultId != null) {
                result = resultService.get(resultId.toString());
            }

            if (result != null) {
                if (testService.getResultType(result.getTestResult().getTest()).equals("N")) {
                    switch (inputType) {
                    case Operation.TEST_RESULT:
                        function.append(result.getValue()).append(" ");
                        break;
                    case Operation.IN_NORMAL_RANGE:
                        function.append(" >= ")
                                .append(result.getMinNormal() != null ? result.getMinNormal()
                                        : Double.NEGATIVE_INFINITY)
                                .append(" && ").append(result.getValue()).append(" <= ")
                                .append(result.getMaxNormal() != null ? result.getMaxNormal()
                                        : Double.POSITIVE_INFINITY)
                                .append(" ");
                        break;
                    case Operation.OUTSIDE_NORMAL_RANGE:
                        function.append(" <= ")
                                .append(result.getMinNormal() != null ? result.getMinNormal()
                                        : Double.NEGATIVE_INFINITY)
                                .append(" || ").append(result.getValue()).append(" >= ")
                                .append(result.getMaxNormal() != null ? result.getMaxNormal()
                                        : Double.POSITIVE_INFINITY)
                                .append(" ");
                        break;
                    }
                }
            }
        }
    }

    private Analysis createCalculatedAnalysis(Analysis existingAnalysis, Test test, Result result, String value,
            String calculationName, String systemUserId, Boolean resultCalculated, String externalNote) {
        Analysis currentAnalysis = result.getAnalysis();
        Analysis generatedAnalysis = null;
        if (existingAnalysis != null) {
            generatedAnalysis = analysisService.get(existingAnalysis.getId());
        } else {
            generatedAnalysis = new Analysis();
        }
        generatedAnalysis.setTest(test);
        generatedAnalysis.setIsReportable(currentAnalysis.getIsReportable());
        generatedAnalysis.setAnalysisType(currentAnalysis.getAnalysisType());
        generatedAnalysis.setRevision(currentAnalysis.getRevision());
        generatedAnalysis.setStartedDate(DateUtil.getNowAsSqlDate());
        if (resultCalculated) {
            generatedAnalysis.setStatusId(
                    SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.TechnicalAcceptance));
        } else {
            generatedAnalysis
                    .setStatusId(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.NotStarted));
        }
        generatedAnalysis.setParentAnalysis(currentAnalysis);
        generatedAnalysis.setParentResult(result);
        generatedAnalysis.setSampleItem(currentAnalysis.getSampleItem());
        generatedAnalysis.setTestSection(currentAnalysis.getTestSection());
        generatedAnalysis.setSampleTypeName(currentAnalysis.getSampleTypeName());
        generatedAnalysis.setSysUserId(systemUserId);
        generatedAnalysis.setResultCalculated(resultCalculated);
        if (existingAnalysis != null) {
            try {
                analysisService.update(generatedAnalysis);
            } catch (Exception e) {
                return null;
            }

        } else {
            try {
                analysisService.insert(generatedAnalysis);
            } catch (Exception e) {
                return null;
            }
        }
        if (resultCalculated) {
            createInternalNote(generatedAnalysis, currentAnalysis, calculationName, systemUserId, externalNote);
        } else {
            createMissingValueInternalNote(generatedAnalysis, currentAnalysis, calculationName, systemUserId);
        }
        return generatedAnalysis;
    }
}
