package org.openelisglobal.compliance.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.openelisglobal.compliance.controller.rest.TestCatalogEntry;
import org.openelisglobal.compliance.controller.rest.TestCatalogEntryWithCompliance;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl.ResultType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ComplianceTestCatalogServiceImpl implements ComplianceTestCatalogService {

    @Autowired
    private TestService testService;

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @Autowired
    private TestResultService testResultService;

    @Autowired
    private DictionaryService dictionaryService;

    @Autowired
    private ComplianceThresholdService complianceThresholdService;

    @Override
    @Transactional(readOnly = true)
    public List<TestCatalogEntry> getCatalog() {
        List<Test> activeTests = testService.getAllActiveTests(false);
        // Per-request cache so dictionary entries shared across tests (e.g.
        // many tests reusing the same Absent/Present option) are resolved
        // once instead of once per test result. Without this we fan out N×M
        // dictionary lookups for the catalog.
        Map<String, String> dictionaryTextCache = new HashMap<>();
        return activeTests.stream().map(t -> toEntry(t, dictionaryTextCache)).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestCatalogEntryWithCompliance> getCatalogWithCompliance() {
        // 1. One aggregation query: testId → [thresholdCount, standardCount]
        List<Object[]> summaryRows = complianceThresholdService.getTestThresholdSummary();
        Map<String, int[]> countsByTestId = new HashMap<>();
        if (summaryRows != null) {
            for (Object[] row : summaryRows) {
                if (row[0] == null) {
                    continue;
                }
                String testId = row[0].toString();
                int thresholdCount = row[1] == null ? 0 : ((Number) row[1]).intValue();
                int standardCount = row[2] == null ? 0 : ((Number) row[2]).intValue();
                countsByTestId.put(testId, new int[] { thresholdCount, standardCount });
            }
        }

        if (countsByTestId.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. Walk the catalog and emit only rows with linked thresholds.
        List<Test> activeTests = testService.getAllActiveTests(false);
        Map<String, String> dictionaryTextCache = new HashMap<>();
        List<TestCatalogEntryWithCompliance> entries = new ArrayList<>();
        for (Test test : activeTests) {
            int[] counts = countsByTestId.get(test.getId());
            if (counts == null) {
                continue;
            }
            TestCatalogEntryWithCompliance entry = new TestCatalogEntryWithCompliance();
            populateEntry(entry, test, dictionaryTextCache);
            entry.setThresholdCount(counts[0]);
            entry.setStandardCount(counts[1]);
            entries.add(entry);
        }
        return entries;
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getSampleTypeCategories() {
        List<TypeOfSample> all = typeOfSampleService.getAllTypeOfSamples();
        if (all == null) {
            return new ArrayList<>();
        }
        return all.stream().map(TypeOfSample::getDescription).filter(s -> s != null && !s.isEmpty()).distinct().sorted()
                .collect(Collectors.toList());
    }

    private TestCatalogEntry toEntry(Test test, Map<String, String> dictionaryTextCache) {
        TestCatalogEntry entry = new TestCatalogEntry();
        populateEntry(entry, test, dictionaryTextCache);
        return entry;
    }

    /**
     * Copies catalog metadata onto a pre-allocated {@link TestCatalogEntry}.
     * Extracted so subclasses (e.g. {@link TestCatalogEntryWithCompliance}) can be
     * populated without duplicating the field-mapping logic.
     */
    private void populateEntry(TestCatalogEntry entry, Test test, Map<String, String> dictionaryTextCache) {
        entry.setId(test.getId());
        entry.setValue(test.getName());
        entry.setCode(test.getDescription());
        entry.setLoinc(test.getLoinc());

        List<TypeOfSample> sampleTypes = typeOfSampleService.getTypeOfSampleForTest(test.getId());
        if (sampleTypes != null) {
            entry.setSampleTypes(sampleTypes.stream().map(TypeOfSample::getDescription)
                    .filter(s -> s != null && !s.isEmpty()).distinct().collect(Collectors.toList()));
        }

        String typeCode = testService.getResultType(test);
        boolean isDictionaryBacked = ResultType.DICTIONARY.getCharacterValue().equals(typeCode)
                || ResultType.MULTISELECT.getCharacterValue().equals(typeCode)
                || ResultType.CASCADING_MULTISELECT.getCharacterValue().equals(typeCode);
        entry.setResultType(isDictionaryBacked ? "select"
                : (ResultType.NUMERIC.getCharacterValue().equals(typeCode) ? "numeric" : "numeric"));

        if (isDictionaryBacked) {
            List<TestResult> results = testResultService.getActiveTestResultsByTest(test.getId());
            List<String> options = new ArrayList<>();
            if (results != null) {
                for (TestResult r : results) {
                    String optionText = resolveOptionText(r, dictionaryTextCache);
                    if (optionText != null && !optionText.isBlank() && !options.contains(optionText)) {
                        options.add(optionText);
                    }
                }
            }
            entry.setSelectOptions(options);
        }
    }

    /**
     * For dictionary-backed result types the TestResult.value is a dictionary
     * primary key; resolve to the entry's display text. For non-dictionary variants
     * (e.g. ALPHA fixed-text choices) value is the literal text.
     *
     * <p>
     * {@code dictionaryTextCache} is a per-request memo of dictionary id →
     * dictEntry. Many tests reuse the same options (Absent/Present, etc.); we dodge
     * an N×M fan-out by hitting the dictionary service once per distinct id within
     * a single catalog response.
     */
    private String resolveOptionText(TestResult result, Map<String, String> dictionaryTextCache) {
        if (result == null) {
            return null;
        }
        String raw = result.getValue();
        if (raw == null || raw.isBlank()) {
            return null;
        }
        if (raw.matches("\\d+")) {
            String cached = dictionaryTextCache.get(raw);
            if (cached != null) {
                return cached;
            }
            try {
                Dictionary dict = dictionaryService.getDictionaryById(raw);
                if (dict != null && dict.getDictEntry() != null) {
                    dictionaryTextCache.put(raw, dict.getDictEntry());
                    return dict.getDictEntry();
                }
            } catch (Exception ignored) {
                // Fall through to raw value
            }
        }
        return raw;
    }
}
