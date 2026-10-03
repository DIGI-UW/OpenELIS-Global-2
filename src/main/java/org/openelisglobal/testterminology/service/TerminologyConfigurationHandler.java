package org.openelisglobal.testterminology.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.openelisglobal.configuration.service.AbstractCatalogCsvHandler;
import org.openelisglobal.configuration.service.CatalogReferenceResolver;
import org.openelisglobal.configuration.service.CsvLoadSummary;
import org.openelisglobal.configuration.service.CsvRow;
import org.openelisglobal.configuration.service.LoadedRow;
import org.openelisglobal.configuration.service.RowTransactionRunner;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testresultcomponent.valueholder.TestResultComponent;
import org.openelisglobal.testterminology.valueholder.TestTerminologyMapping;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Loads the {@code terminology} catalog domain from CSV: the standard codes
 * (LOINC, SNOMED, CIEL, OCL) the Test Catalog editor's Terminology section
 * keeps for a test.
 * <p>
 * Columns:
 * {@code testName,sampleType,componentCode,source,code,relationship,displayName}.
 * {@code testName}, {@code source} and {@code code} are required;
 * {@code sampleType} and {@code componentCode} scope a mapping to one of the
 * test's specimens or components; {@code relationship} is SAME_AS (default),
 * BROADER_THAN or NARROWER_THAN. Rows are merged into the test's mappings by
 * (component, specimen, source, code): a mapping the test already has takes the
 * row's relationship and display name, a new one is added, the others are kept.
 * Written through {@link TestTerminologyMappingService#saveMappingsForTest}.
 */
@Component
public class TerminologyConfigurationHandler extends AbstractCatalogCsvHandler {

    private static final Set<String> SOURCES = Set.of("LOINC", "SNOMED", "CIEL", "OCL");
    private static final Set<String> RELATIONSHIPS = Set.of("SAME_AS", "BROADER_THAN", "NARROWER_THAN");

    @Autowired
    private TestTerminologyMappingService terminologyService;

    @Autowired
    private TestService testService;

    @Autowired
    private CatalogReferenceResolver resolver;

    @Override
    public String getDomainName() {
        return "terminology";
    }

    @Override
    public int getLoadOrder() {
        return 320;
    }

    @Override
    protected String[] requiredColumns() {
        return new String[] { "testName", "source", "code" };
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
                fileName + " line " + first.lineNumber() + " (terminology)");
        if (test == null) {
            group.forEach(r -> outcomes.add(LoadedRow.skipped("test '" + testName + "' not found")));
            return outcomes;
        }
        Set<String> specimenIds = new HashSet<>();
        for (TypeOfSample type : testService.getTypeOfSamples(test)) {
            specimenIds.add(type.getId());
        }

        Map<String, TestTerminologyMapping> byKey = new LinkedHashMap<>();
        for (TestTerminologyMapping existing : terminologyService.getActiveByTestId(test.getId())) {
            byKey.put(key(existing.getComponentId(), existing.getSampleTypeId(), existing.getSource(),
                    existing.getCode()), existing);
        }
        List<Boolean> createdFlags = new ArrayList<>();
        for (CsvRow row : group) {
            String source = row.get("source").toUpperCase(Locale.ROOT);
            String code = row.get("code");
            if (!SOURCES.contains(source)) {
                throw new IllegalArgumentException("source must be one of " + SOURCES);
            }
            if (code.isEmpty()) {
                throw new IllegalArgumentException("line " + row.lineNumber() + " has no code");
            }
            String relationship = row.get("relationship").toUpperCase(Locale.ROOT);
            if (relationship.isEmpty()) {
                relationship = "SAME_AS";
            }
            if (!RELATIONSHIPS.contains(relationship)) {
                throw new IllegalArgumentException("relationship must be one of " + RELATIONSHIPS);
            }
            String context = fileName + " line " + row.lineNumber();
            String sampleTypeId = null;
            if (!row.isBlank("sampleType")) {
                TypeOfSample specimen = resolver.resolveSampleType(row.get("sampleType"), context);
                if (specimen == null || !specimenIds.contains(specimen.getId())) {
                    throw new IllegalArgumentException("'" + test.getDescription() + "' is not linked to sample type '"
                            + row.get("sampleType") + "'");
                }
                sampleTypeId = specimen.getId();
            }
            String componentId = null;
            if (!row.isBlank("componentCode")) {
                TestResultComponent component = resolver.resolveComponent(test.getId(), row.get("componentCode"),
                        context);
                if (component == null) {
                    throw new IllegalArgumentException(
                            "component '" + row.get("componentCode") + "' is not on '" + test.getDescription() + "'");
                }
                componentId = component.getId();
            }
            String key = key(componentId, sampleTypeId, source, code);
            TestTerminologyMapping mapping = byKey.get(key);
            boolean created = mapping == null;
            if (created) {
                mapping = new TestTerminologyMapping();
                mapping.setComponentId(componentId);
                mapping.setSampleTypeId(sampleTypeId);
                mapping.setSource(source);
                mapping.setCode(code);
                byKey.put(key, mapping);
            }
            mapping.setRelationship(relationship);
            if (!row.isBlank("displayName")) {
                mapping.setDisplayName(row.get("displayName"));
            }
            createdFlags.add(created);
        }
        terminologyService.saveMappingsForTest(test.getId(), new ArrayList<>(byKey.values()), SYS_USER_ID);
        for (Boolean created : createdFlags) {
            outcomes.add(created ? LoadedRow.created(test.getId()) : LoadedRow.updated(test.getId()));
        }
        return outcomes;
    }

    private static String key(String componentId, String sampleTypeId, String source, String code) {
        return (componentId == null ? "" : componentId) + "|" + (sampleTypeId == null ? "" : sampleTypeId) + "|"
                + source.toUpperCase(Locale.ROOT) + "|" + code.trim().toUpperCase(Locale.ROOT);
    }
}
