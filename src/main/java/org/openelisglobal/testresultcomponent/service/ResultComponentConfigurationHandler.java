package org.openelisglobal.testresultcomponent.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.configuration.service.AbstractCatalogCsvHandler;
import org.openelisglobal.configuration.service.CatalogReferenceResolver;
import org.openelisglobal.configuration.service.CsvLoadSummary;
import org.openelisglobal.configuration.service.CsvRow;
import org.openelisglobal.configuration.service.LoadedRow;
import org.openelisglobal.configuration.service.RowTransactionRunner;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testresultcomponent.valueholder.TestResultComponent;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl;
import org.openelisglobal.unitofmeasure.valueholder.UnitOfMeasure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Loads the {@code result-components} catalog domain from CSV: the result
 * components of a test as the Test Catalog editor's Sample &amp; Results
 * section defines them.
 * <p>
 * Columns:
 * {@code testName,sampleType,code,label,resultType,unitOfMeasure,significantDigits,displayOrder,isPrimary,showOnReport,defaultResult,allowMultipleReadings}.
 * {@code testName} and {@code code} are required; {@code sampleType} only
 * disambiguates a legacy {@code Name(Specimen)} record. The rows of one test
 * are one unit of work, written through
 * {@link TestResultComponentService#saveSampleResults}: a code the test already
 * has is updated (blank cells leave the value as it is), a new code is added,
 * and components the file does not mention are kept. Select-list options and
 * interpretations are not touched here; options come from the
 * {@code test-results} domain. {@code resultType} is one of the result type
 * letters (N numeric, D dictionary, A alpha, R remark, T titer, M/C
 * multi-select).
 */
@Component
public class ResultComponentConfigurationHandler extends AbstractCatalogCsvHandler {

    @Autowired
    private TestResultComponentService componentService;

    @Autowired
    private CatalogReferenceResolver resolver;

    @Override
    public String getDomainName() {
        return "result-components";
    }

    @Override
    public int getLoadOrder() {
        return 210;
    }

    @Override
    protected String[] requiredColumns() {
        return new String[] { "testName", "code" };
    }

    @Override
    protected void load(List<CsvRow> rows, RowTransactionRunner transaction, CsvLoadSummary summary, String fileName) {
        Map<String, List<CsvRow>> byTest = new LinkedHashMap<>();
        for (CsvRow row : rows) {
            byTest.computeIfAbsent(row.get("testName") + "|" + row.get("sampleType"), k -> new ArrayList<>()).add(row);
        }
        for (List<CsvRow> group : byTest.values()) {
            List<LoadedRow<String>> outcomes;
            try {
                outcomes = transaction.run(() -> loadTest(group, fileName));
            } catch (Exception e) {
                String reason = CsvLoadSummary.reason(e);
                outcomes = new ArrayList<>();
                for (int i = 0; i < group.size(); i++) {
                    outcomes.add(LoadedRow.skipped(reason));
                }
            }
            for (int i = 0; i < group.size(); i++) {
                summary.record(outcomes.get(i), getClass().getSimpleName(), group.get(i).lineNumber());
            }
            flushUnresolvedReferences(fileName, group.get(0).lineNumber());
        }
    }

    private List<LoadedRow<String>> loadTest(List<CsvRow> group, String fileName) {
        CsvRow first = group.get(0);
        String testName = first.get("testName");
        List<LoadedRow<String>> outcomes = new ArrayList<>();
        if (testName.isEmpty()) {
            group.forEach(r -> outcomes.add(LoadedRow.skipped("missing testName")));
            return outcomes;
        }
        Test test = resolver.resolveTest(testName, first.get("sampleType"),
                fileName + " line " + first.lineNumber() + " (result components)");
        if (test == null) {
            group.forEach(r -> outcomes.add(LoadedRow.skipped("test '" + testName + "' not found")));
            return outcomes;
        }

        Map<String, TestResultComponent> byCode = new LinkedHashMap<>();
        for (TestResultComponent existing : componentService.getActiveComponentsByTestId(test.getId())) {
            byCode.put(existing.getCode().toUpperCase(), existing);
        }
        List<CsvLoadSummary.Outcome> rowOutcomes = new ArrayList<>();
        for (CsvRow row : group) {
            String code = row.get("code");
            if (code.isEmpty()) {
                throw new IllegalArgumentException("line " + row.lineNumber() + " has no code");
            }
            TestResultComponent component = byCode.get(code.toUpperCase());
            boolean created = component == null;
            if (created) {
                component = new TestResultComponent();
                component.setTestId(test.getId());
                component.setCode(code);
                component.setLabel(code);
                component.setDisplayOrder(byCode.size());
                component.setShowOnReport(true);
                component.setIsActive("Y");
                byCode.put(code.toUpperCase(), component);
            }
            applyRow(row, component, fileName);
            rowOutcomes.add(created ? CsvLoadSummary.Outcome.CREATED : CsvLoadSummary.Outcome.UPDATED);
        }
        componentService.saveSampleResults(test.getId(), new ArrayList<>(byCode.values()), null, null, SYS_USER_ID);
        for (CsvLoadSummary.Outcome outcome : rowOutcomes) {
            outcomes.add(outcome == CsvLoadSummary.Outcome.CREATED ? LoadedRow.created(test.getId())
                    : LoadedRow.updated(test.getId()));
        }
        return outcomes;
    }

    private void applyRow(CsvRow row, TestResultComponent component, String fileName) {
        if (!row.isBlank("label")) {
            component.setLabel(row.get("label"));
        }
        if (!row.isBlank("resultType")) {
            component.setResultType(resultTypeCode(row.get("resultType")));
        }
        if (!row.isBlank("unitOfMeasure")) {
            UnitOfMeasure unit = resolver.resolveUnitOfMeasure(row.get("unitOfMeasure"),
                    fileName + " line " + row.lineNumber());
            if (unit == null) {
                throw new IllegalArgumentException("unit of measure '" + row.get("unitOfMeasure") + "' not found");
            }
            component.setUomId(unit.getId());
        }
        if (!row.isBlank("significantDigits")) {
            component.setSignificantDigits(row.integer("significantDigits"));
        }
        if (!row.isBlank("displayOrder")) {
            component.setDisplayOrder(row.integer("displayOrder"));
        }
        if (!row.isBlank("isPrimary")) {
            component.setIsPrimary(row.flag("isPrimary", false));
        }
        if (!row.isBlank("showOnReport")) {
            component.setShowOnReport(row.flag("showOnReport", true));
        }
        if (!row.isBlank("defaultResult")) {
            component.setDefaultResult(row.get("defaultResult"));
        }
        if (!row.isBlank("allowMultipleReadings")) {
            component.setAllowMultipleReadings(row.flag("allowMultipleReadings", false));
        }
    }

    private static String resultTypeCode(String value) {
        for (TypeOfTestResultServiceImpl.ResultType type : TypeOfTestResultServiceImpl.ResultType.values()) {
            if (type.getCharacterValue().equalsIgnoreCase(value) || type.name().equalsIgnoreCase(value)) {
                return type.getCharacterValue();
            }
        }
        throw new IllegalArgumentException("unknown resultType '" + value + "'");
    }
}
