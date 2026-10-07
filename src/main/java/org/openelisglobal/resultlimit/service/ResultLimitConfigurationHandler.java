package org.openelisglobal.resultlimit.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openelisglobal.configuration.service.AbstractCatalogCsvHandler;
import org.openelisglobal.configuration.service.CatalogReferenceResolver;
import org.openelisglobal.configuration.service.CsvLoadSummary;
import org.openelisglobal.configuration.service.CsvRow;
import org.openelisglobal.configuration.service.LoadedRow;
import org.openelisglobal.configuration.service.RowTransactionRunner;
import org.openelisglobal.resultlimits.valueholder.ResultLimit;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testresultcomponent.valueholder.TestResultComponent;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Loads the {@code result-limits} catalog domain from CSV: the numeric
 * reference ranges the Test Catalog editor's Ranges section manages.
 * <p>
 * Columns:
 * {@code testName,sampleType,componentCode,gender,minAge,maxAge,lowNormal,highNormal,lowCritical,highCritical,lowValid,highValid}.
 * {@code testName} is required. {@code sampleType} scopes the range to one of
 * the test's specimens (and picks a legacy {@code Name(Specimen)} record);
 * {@code componentCode} scopes it to one of the test's result components;
 * {@code gender} is M or F; ages are in years, blank meaning 0 and no upper
 * bound; a blank bound is open. The rows of one test are that test's complete
 * numeric ranges and replace what it had, written through
 * {@link ResultLimitService#saveRangesForTest}; dictionary limits and reporting
 * ranges are left alone, as the editor leaves them.
 */
@Component
public class ResultLimitConfigurationHandler extends AbstractCatalogCsvHandler {

    private static final Set<String> GENDERS = Set.of("M", "F");

    @Autowired
    private ResultLimitService resultLimitService;

    @Autowired
    private TestService testService;

    @Autowired
    private CatalogReferenceResolver resolver;

    @Override
    public String getDomainName() {
        return "result-limits";
    }

    @Override
    public int getLoadOrder() {
        return 320;
    }

    @Override
    protected String[] requiredColumns() {
        return new String[] { "testName" };
    }

    @Override
    protected void load(List<CsvRow> rows, RowTransactionRunner transaction, CsvLoadSummary summary, String fileName) {
        Map<String, List<CsvRow>> byTest = new LinkedHashMap<>();
        for (CsvRow row : rows) {
            byTest.computeIfAbsent(row.get("testName"), k -> new ArrayList<>()).add(row);
        }
        for (List<CsvRow> group : byTest.values()) {
            LoadedRow<String> outcome;
            try {
                outcome = transaction.run(() -> loadTest(group, fileName));
            } catch (Exception e) {
                outcome = LoadedRow.skipped(CsvLoadSummary.reason(e));
            }
            for (CsvRow row : group) {
                summary.record(outcome, getClass().getSimpleName(), row.lineNumber());
            }
            flushUnresolvedReferences(fileName, group.get(0).lineNumber());
        }
    }

    private LoadedRow<String> loadTest(List<CsvRow> group, String fileName) {
        CsvRow first = group.get(0);
        String testName = first.get("testName");
        if (testName.isEmpty()) {
            return LoadedRow.skipped("missing testName");
        }
        Test test = resolver.resolveTest(testName, first.get("sampleType"),
                fileName + " line " + first.lineNumber() + " (result limits)");
        if (test == null) {
            return LoadedRow.skipped("test '" + testName + "' not found");
        }
        Set<String> specimenIds = new HashSet<>();
        for (TypeOfSample type : testService.getTypeOfSamples(test)) {
            specimenIds.add(type.getId());
        }
        boolean hadRanges = !resultLimitService.getAllResultLimitsForTest(test.getId()).isEmpty();
        List<ResultLimit> desired = new ArrayList<>();
        for (CsvRow row : group) {
            desired.add(toLimit(row, test, specimenIds, fileName));
        }
        resultLimitService.saveRangesForTest(test.getId(), desired, SYS_USER_ID);
        return hadRanges ? LoadedRow.updated(test.getId()) : LoadedRow.created(test.getId());
    }

    private ResultLimit toLimit(CsvRow row, Test test, Set<String> specimenIds, String fileName) {
        ResultLimit limit = new ResultLimit();
        limit.setTestId(test.getId());
        String context = fileName + " line " + row.lineNumber();
        if (!row.isBlank("sampleType")) {
            TypeOfSample specimen = resolver.resolveSampleType(row.get("sampleType"), context);
            if (specimen == null) {
                throw new IllegalArgumentException("sample type '" + row.get("sampleType") + "' not found");
            }
            if (!specimenIds.contains(specimen.getId())) {
                throw new IllegalArgumentException(
                        "'" + test.getDescription() + "' is not linked to sample type '" + row.get("sampleType") + "'");
            }
            limit.setSampleTypeId(specimen.getId());
        }
        if (!row.isBlank("componentCode")) {
            TestResultComponent component = resolver.resolveComponent(test.getId(), row.get("componentCode"), context);
            if (component == null) {
                throw new IllegalArgumentException(
                        "component '" + row.get("componentCode") + "' is not on '" + test.getDescription() + "'");
            }
            limit.setComponentId(component.getId());
        }
        String gender = row.get("gender").toUpperCase();
        if (!gender.isEmpty()) {
            if (!GENDERS.contains(gender)) {
                throw new IllegalArgumentException("gender must be M or F");
            }
            limit.setGender(gender);
        }
        double minAge = orDefault(row.decimal("minAge"), 0d);
        double maxAge = orDefault(row.decimal("maxAge"), Double.POSITIVE_INFINITY);
        if (minAge < 0d || maxAge <= minAge) {
            throw new IllegalArgumentException("age range must be 0 <= minAge < maxAge");
        }
        limit.setMinAge(minAge);
        limit.setMaxAge(maxAge);
        limit.setLowNormal(orDefault(row.decimal("lowNormal"), Double.NEGATIVE_INFINITY));
        limit.setHighNormal(orDefault(row.decimal("highNormal"), Double.POSITIVE_INFINITY));
        limit.setLowCritical(orDefault(row.decimal("lowCritical"), Double.POSITIVE_INFINITY));
        limit.setHighCritical(orDefault(row.decimal("highCritical"), Double.POSITIVE_INFINITY));
        limit.setLowValid(orDefault(row.decimal("lowValid"), Double.NEGATIVE_INFINITY));
        limit.setHighValid(orDefault(row.decimal("highValid"), Double.POSITIVE_INFINITY));
        return limit;
    }

    private static double orDefault(Double value, double defaultValue) {
        return value == null ? defaultValue : value;
    }
}
