package org.openelisglobal.testcatalog.service;

import java.util.List;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testcalculated.service.TestCalculationService;
import org.openelisglobal.testcalculated.valueholder.Calculation;
import org.openelisglobal.testcalculated.valueholder.Operation;
import org.openelisglobal.testreflex.service.TestReflexService;
import org.openelisglobal.testreflex.valueholder.TestReflex;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReflexCalcViewServiceImpl implements ReflexCalcViewService {

    @Autowired
    private TestReflexService testReflexService;

    @Autowired
    private TestCalculationService testCalculationService;

    @Autowired
    private TestService testService;

    @Override
    @Transactional(readOnly = true)
    public ReflexCalcView getForTest(String testId) {
        ReflexCalcView view = new ReflexCalcView();

        for (TestReflex reflex : testReflexService.getTestReflexsByTestId(testId)) {
            ReflexRow row = new ReflexRow();
            row.id = reflex.getId();
            Test added = reflex.getAddedTest();
            row.reflexTests = added != null ? added.getLocalizedName() : null;
            row.triggerCondition = describeTrigger(reflex);
            row.ruleName = reflex.getInternalNote() != null && !reflex.getInternalNote().isBlank()
                    ? reflex.getInternalNote()
                    : row.reflexTests;
            view.reflexRules.add(row);
        }

        Integer tid = parseIntOrNull(testId);
        for (Calculation calc : testCalculationService.getAll()) {
            if (Boolean.FALSE.equals(calc.getActive())) {
                continue;
            }
            if (tid != null && tid.equals(calc.getTestId())) {
                view.calculatedBy.add(toCalcRow(calc));
            } else if (operationsReference(calc, testId)) {
                view.feedsInto.add(toCalcRow(calc));
            }
        }
        return view;
    }

    private String describeTrigger(TestReflex reflex) {
        TestResult testResult = reflex.getTestResult();
        String value = testResult != null && testResult.getValue() != null ? testResult.getValue()
                : reflex.getNonDictionaryValue();
        String relation = reflex.getRelation() != null ? reflex.getRelation().toString() + " " : "";
        if (value == null || value.isBlank()) {
            return relation.isBlank() ? "Any result" : relation.trim();
        }
        return (relation + value).trim();
    }

    private CalcRow toCalcRow(Calculation calc) {
        CalcRow row = new CalcRow();
        row.id = calc.getId();
        row.name = calc.getName();
        row.formula = buildFormula(calc);
        if (calc.getTestId() != null) {
            Test output = testService.getTestById(String.valueOf(calc.getTestId()));
            row.outputTest = output != null ? output.getLocalizedName() : null;
        }
        return row;
    }

    private String buildFormula(Calculation calc) {
        List<Operation> operations = calc.getOperations();
        if (operations == null || operations.isEmpty()) {
            return calc.getResult();
        }
        StringBuilder sb = new StringBuilder();
        operations.stream().sorted().forEach(op -> {
            String token = op.getValue() != null && !op.getValue().isBlank() ? op.getValue()
                    : (op.getType() != null ? op.getType().toString() : "");
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(token);
        });
        return sb.toString();
    }

    private boolean operationsReference(Calculation calc, String testId) {
        List<Operation> operations = calc.getOperations();
        if (operations == null) {
            return false;
        }
        return operations.stream().anyMatch(op -> testId.equals(op.getValue()));
    }

    private Integer parseIntOrNull(String value) {
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
