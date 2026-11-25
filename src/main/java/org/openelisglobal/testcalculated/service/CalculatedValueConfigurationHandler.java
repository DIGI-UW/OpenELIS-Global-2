package org.openelisglobal.testcalculated.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openelisglobal.configuration.service.AbstractCatalogCsvHandler;
import org.openelisglobal.configuration.service.CatalogReferenceResolver;
import org.openelisglobal.configuration.service.CsvLoadSummary;
import org.openelisglobal.configuration.service.CsvRow;
import org.openelisglobal.configuration.service.LoadedRow;
import org.openelisglobal.configuration.service.RowTransactionRunner;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testcalculated.valueholder.Calculation;
import org.openelisglobal.testcalculated.valueholder.Operation;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.testresultcomponent.valueholder.TestResultComponent;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The {@code calculated-values} domain, one rule per row:
 * {@code name,testName,sampleType,componentCode,result,expression,note,isActive}.
 * In {@code expression}, {@code [CODE]} or {@code [Test name:CODE]} reads a
 * component; {@code [CODE] == 'Answer'} compares it with an answer.
 */
@Component
public class CalculatedValueConfigurationHandler extends AbstractCatalogCsvHandler {

    private static final Pattern TOKEN = Pattern.compile(
            "\\s*(?:\\[([^\\]]+)\\]|'([^']*)'|(\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?)|(==|!=|>=|<=|&&|\\|\\||[-+*/()]))");
    private static final Set<String> COMPARISONS = Set.of("==", "!=");

    @Autowired
    private TestCalculationService calculationService;

    @Autowired
    private TestResultService testResultService;

    @Autowired
    private TestService testService;

    @Autowired
    private DictionaryService dictionaryService;

    @Autowired
    private CatalogReferenceResolver resolver;

    @Override
    public String getDomainName() {
        return "calculated-values";
    }

    @Override
    public int getLoadOrder() {
        return 340;
    }

    @Override
    protected String[] requiredColumns() {
        return new String[] { "name", "testName", "expression" };
    }

    @Override
    protected void load(List<CsvRow> rows, RowTransactionRunner transaction, CsvLoadSummary summary, String fileName) {
        for (CsvRow row : rows) {
            LoadedRow<String> outcome;
            try {
                outcome = transaction.run(() -> loadRule(row, fileName));
            } catch (Exception e) {
                outcome = LoadedRow.skipped(CsvLoadSummary.reason(e));
            }
            summary.record(outcome, getClass().getSimpleName(), row.lineNumber());
            flushUnresolvedReferences(fileName, row.lineNumber());
        }
    }

    private LoadedRow<String> loadRule(CsvRow row, String fileName) {
        String name = row.get("name");
        if (name.isEmpty()) {
            return LoadedRow.skipped("missing name");
        }
        String context = fileName + " line " + row.lineNumber() + " (calculated value " + name + ")";
        Test test = resolver.resolveTest(row.get("testName"), row.get("sampleType"), context);
        if (test == null) {
            throw new IllegalArgumentException("test '" + row.get("testName") + "' not found");
        }
        TestResultComponent destination = component(test, row.get("componentCode"), context);
        Calculation calculation = calculationService.getAll().stream()
                .filter(existing -> name.equalsIgnoreCase(existing.getName())).findFirst().orElse(null);
        boolean created = calculation == null;
        if (created) {
            calculation = new Calculation();
        }
        calculation.setName(name);
        calculation.setTestId(Integer.valueOf(test.getId()));
        TypeOfSample specimen = specimen(test, row.get("sampleType"), context);
        calculation.setSampleId(Integer.valueOf(specimen.getId()));
        calculation.setComponentId(row.isBlank("componentCode") ? null : destination.getId());
        calculation.setResult(row.isBlank("result") ? null : answer(destination.getId(), row.get("result")));
        calculation.setNote(row.isBlank("note") ? null : row.get("note"));
        boolean active = row.flag("isActive", true);
        calculation.setActive(active);
        calculation.setToggled(active);
        List<Operation> operations = operations(row.get("expression"), test, specimen, context);
        if (created || !sameOperations(calculation.getOperations(), operations)) {
            calculation.setOperations(operations);
        }
        calculationService.validateRule(calculation, active);
        if (created) {
            calculationService.save(calculation);
        } else {
            calculationService.update(calculation);
        }
        String id = calculation.getId() == null ? name : String.valueOf(calculation.getId());
        return created ? LoadedRow.created(id) : LoadedRow.updated(id);
    }

    private List<Operation> operations(String expression, Test test, TypeOfSample specimen, String context) {
        List<Operation> operations = new ArrayList<>();
        Matcher matcher = TOKEN.matcher(expression);
        int position = 0;
        while (position < expression.length() && !expression.substring(position).isBlank()) {
            if (!matcher.find(position) || matcher.start() != position) {
                throw new IllegalArgumentException(
                        "cannot read the expression from '" + expression.substring(position) + "'");
            }
            Operation operation = new Operation();
            if (matcher.group(1) != null) {
                String reference = matcher.group(1);
                int separator = reference.lastIndexOf(':');
                Test operandTest = separator < 0 ? test
                        : resolver.resolveTest(reference.substring(0, separator).trim(), "", context);
                if (operandTest == null) {
                    throw new IllegalArgumentException("test '" + reference.substring(0, separator) + "' not found");
                }
                operation.setType(Operation.OperationType.TEST_RESULT);
                operation.setValue(operandTest.getId());
                operation.setSampleId(
                        Integer.valueOf((separator < 0 ? specimen : specimen(operandTest, "", context)).getId()));
                operation.setComponentId(
                        component(operandTest, reference.substring(separator + 1).trim(), context).getId());
            } else if (matcher.group(2) != null) {
                operation.setType(Operation.OperationType.ANSWER);
                operation.setValue(answer(comparedComponentId(operations, matcher.group(2)), matcher.group(2)));
            } else if (matcher.group(3) != null) {
                operation.setType(Operation.OperationType.INTEGER);
                operation.setValue(matcher.group(3));
            } else {
                operation.setType(Operation.OperationType.MATH_FUNCTION);
                operation.setValue(matcher.group(4));
            }
            operation.setOrder(operations.size());
            operations.add(operation);
            position = matcher.end();
        }
        if (operations.isEmpty()) {
            throw new IllegalArgumentException("the expression is empty");
        }
        return operations;
    }

    // Orders in progress bind their recorded results to operation ids, so an
    // unchanged expression keeps its operations.
    private static boolean sameOperations(List<Operation> existing, List<Operation> loaded) {
        if (existing == null || existing.size() != loaded.size()) {
            return false;
        }
        List<Operation> ordered = new ArrayList<>(existing);
        ordered.sort(Comparator.comparing(Operation::getOrder));
        for (int i = 0; i < loaded.size(); i++) {
            Operation a = ordered.get(i), b = loaded.get(i);
            if (a.getType() != b.getType() || !Objects.equals(a.getValue(), b.getValue())
                    || !Objects.equals(a.getComponentId(), b.getComponentId())
                    || !Objects.equals(a.getSampleId(), b.getSampleId())) {
                return false;
            }
        }
        return true;
    }

    private String comparedComponentId(List<Operation> operations, String answer) {
        int size = operations.size();
        if (size < 2 || operations.get(size - 1).getType() != Operation.OperationType.MATH_FUNCTION
                || !COMPARISONS.contains(operations.get(size - 1).getValue())
                || operations.get(size - 2).getType() != Operation.OperationType.TEST_RESULT) {
            throw new IllegalArgumentException(
                    "'" + answer + "' must follow the result it is compared with, as [CODE] == '" + answer + "'");
        }
        return operations.get(size - 2).getComponentId();
    }

    private TypeOfSample specimen(Test test, String sampleTypeName, String context) {
        List<TypeOfSample> linked = testService.getTypeOfSamples(test);
        if (sampleTypeName.isEmpty()) {
            if (linked.size() == 1) {
                return linked.get(0);
            }
            throw new IllegalArgumentException(
                    "'" + test.getLocalizedName() + "' runs on " + linked.size() + " specimens, name one");
        }
        TypeOfSample specimen = resolver.resolveSampleType(sampleTypeName, context);
        if (specimen == null || linked.stream().noneMatch(type -> type.getId().equals(specimen.getId()))) {
            throw new IllegalArgumentException(
                    "'" + test.getLocalizedName() + "' is not linked to sample type '" + sampleTypeName + "'");
        }
        return specimen;
    }

    private TestResultComponent component(Test test, String code, String context) {
        TestResultComponent component = resolver.resolveComponent(test.getId(), code, context);
        if (component == null) {
            throw new IllegalArgumentException("component '" + code + "' is not on '" + test.getLocalizedName() + "'");
        }
        return component;
    }

    private String answer(String componentId, String name) {
        for (TestResult option : testResultService.getActiveOptionsByComponentId(componentId)) {
            if (!"D".equals(option.getTestResultType())) {
                continue;
            }
            Dictionary entry = dictionaryService.getDictionaryById(option.getValue());
            if (entry != null && name.trim().equalsIgnoreCase(entry.getDictEntry())) {
                return option.getValue();
            }
        }
        throw new IllegalArgumentException("'" + name + "' is not an answer of the result it is compared with");
    }
}
