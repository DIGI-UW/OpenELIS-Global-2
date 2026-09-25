package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingMappingState;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;

public class AnalyzerMappingDefaultsTest {
    private final AnalyzerMappingCatalogService catalog = mock(AnalyzerMappingCatalogService.class);
    private final TestResultService testResults = mock(TestResultService.class);
    private final AnalyzerMappingDefaults defaults = new AnalyzerMappingDefaults(catalog, testResults);

    @Before
    public void catalog() {
        when(catalog.searchActiveTests(null)).thenReturn(List.of(test("1", "11111-1")));
    }

    @Test
    public void fillsUniqueTestAndExactAnswerLabelsWithoutInventingSynonyms() throws Exception {
        when(catalog.getActiveResultOptions("1")).thenReturn(List.of(
                new AnalyzerMappingCatalogService.ResultOption("21", "991", "Detected"),
                new AnalyzerMappingCatalogService.ResultOption("22", "992", "Negative")));
        var draft = defaults.resolve(profile("qualitative", "DETECTED", "NOT DETECTED"));
        assertEquals("1", draft.tests().get(0).testId());
        assertEquals("21", draft.results().get(0).testResultId());
        assertEquals(AnalyzerSiteBindingMappingState.UNRESOLVED, draft.results().get(1).mappingState());
        assertNull(draft.results().get(1).testResultId());
    }

    @Test
    public void doesNotPickTheFirstOfMultipleLoincMatches() throws Exception {
        when(catalog.searchActiveTests(null)).thenReturn(List.of(test("1", "11111-1"), test("2", "11111-1")));
        var draft = defaults.resolve(profile("qualitative", "DETECTED"));
        assertEquals(AnalyzerSiteBindingMappingState.UNRESOLVED, draft.tests().get(0).mappingState());
        assertNull(draft.tests().get(0).testId());
        assertNull(draft.results().get(0).testResultId());
    }

    @Test
    public void retainsAmbiguousAnswerLabelsAsUnresolved() throws Exception {
        when(catalog.getActiveResultOptions("1")).thenReturn(List.of(
                new AnalyzerMappingCatalogService.ResultOption("21", "991", "Detected"),
                new AnalyzerMappingCatalogService.ResultOption("22", "992", "DETECTED")));
        var draft = defaults.resolve(profile("qualitative", "DETECTED"));
        assertEquals("1", draft.tests().get(0).testId());
        assertNull(draft.results().get(0).testResultId());
    }

    @Test
    public void doesNotBindCategoricalProfileToTestWithoutCategoricalAnswers() throws Exception {
        var draft = defaults.resolve(profile("qualitative", "DETECTED"));
        assertNull(draft.tests().get(0).testId());
        assertNull(draft.results().get(0).testResultId());
    }

    @Test
    public void bindsNumericDefaultOnlyWhenLocalTestAcceptsNumericResults() throws Exception {
        assertNull(defaults.resolve(profile("quantitative")).tests().get(0).testId());
        TestResult number = new TestResult();
        number.setTestResultType("N");
        when(testResults.getActiveTestResultsByTest("1")).thenReturn(List.of(number));
        assertEquals("1", defaults.resolve(profile("quantitative")).tests().get(0).testId());
    }

    private AnalyzerMappingCatalogService.TestOption test(String id, String loinc) {
        return new AnalyzerMappingCatalogService.TestOption(id, "Local test " + id, null, List.of(loinc));
    }

    private BridgeAnalyzerProfile profile(String type, String... values) throws Exception {
        var mapper = new ObjectMapper();
        var document = mapper.createObjectNode();
        document.putObject("profileMeta").put("id", "fixture.defaults").put("displayName", "Default resolution");
        document.putObject("catalog").put("revision", 1).put("revisionFingerprint", "sha256:" + "a".repeat(64))
                .put("source", "SHIPPED").put("status", "ACTIVE");
        document.putObject("protocol").put("name", "ASTM");
        var definition = document.putArray("default_test_mappings").addObject();
        definition.put("test_code", "RAW-A").put("loinc", "11111-1").put("result_type", type);
        var rawValues = definition.putArray("values");
        for (String value : values)
            rawValues.add(value);
        return BridgeAnalyzerProfile.from(document);
    }
}
