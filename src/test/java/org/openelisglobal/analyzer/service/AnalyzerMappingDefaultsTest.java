package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingState;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;

public class AnalyzerMappingDefaultsTest {
    private static final String LOINC = "11111-1";
    private static final String DETECTED = "LA11111-1";
    private static final String NOT_DETECTED = "LA22222-2";
    private static final String INVALID = "LA15841-2";
    private static final String POSITIVE = "LA6576-8";

    private final AnalyzerMappingCatalogService catalog = mock(AnalyzerMappingCatalogService.class);
    private final TestResultService testResults = mock(TestResultService.class);
    private final AnalyzerMappingDefaults defaults = new AnalyzerMappingDefaults(catalog, testResults);

    @Before
    public void catalog() {
        when(catalog.searchActiveTests(null)).thenReturn(List.of(test("1")));
    }

    @Test
    public void bindsTheUsableCandidateWhenAnAnswerlessTestSharesItsLoinc() throws Exception {
        when(catalog.searchActiveTests(null)).thenReturn(List.of(test("1"), test("2")));
        when(catalog.getActiveResultOptions("2"))
                .thenReturn(List.of(new AnalyzerMappingCatalogService.ResultOption("21", "991", "Detected", DETECTED)));
        var draft = defaults.resolve(profile("qualitative", codes("DETECTED", DETECTED)));
        assertEquals(AnalyzerMappingState.BOUND, draft.tests().get(0).mappingState());
        assertEquals("2", draft.tests().get(0).testId());
        assertNull(draft.tests().get(0).unresolvedReason());
        assertEquals("21", draft.results().get(0).testResultId());
    }

    @Test
    public void leavesTwoUsableCandidatesUnresolvedAsAmbiguous() throws Exception {
        when(catalog.searchActiveTests(null)).thenReturn(List.of(test("1"), test("2")));
        for (String id : List.of("1", "2")) {
            when(catalog.getActiveResultOptions(id)).thenReturn(
                    List.of(new AnalyzerMappingCatalogService.ResultOption("2" + id, "99" + id, "Detected", DETECTED)));
        }
        var draft = defaults.resolve(profile("qualitative", codes("DETECTED", DETECTED)));
        assertEquals(AnalyzerMappingState.UNRESOLVED, draft.tests().get(0).mappingState());
        assertNull(draft.tests().get(0).testId());
        assertEquals(AnalyzerUnresolvedReason.AMBIGUOUS, draft.tests().get(0).unresolvedReason());
        assertNull(draft.results().get(0).testResultId());
        assertEquals(AnalyzerUnresolvedReason.AMBIGUOUS, draft.results().get(0).unresolvedReason());
    }

    @Test
    public void reportsNoMatchWhenNoActiveTestCarriesTheLoinc() throws Exception {
        when(catalog.searchActiveTests(null)).thenReturn(List.of(
                new AnalyzerMappingCatalogService.TestOption("1", "Other", "OTH", List.of("99999-9"))));
        var draft = defaults.resolve(profile("qualitative", codes("DETECTED", DETECTED)));
        assertEquals(AnalyzerUnresolvedReason.NO_MATCH, draft.tests().get(0).unresolvedReason());
    }

    @Test
    public void doesNotMatchOnLocalCodeOrNameWithoutTheLoinc() throws Exception {
        when(catalog.searchActiveTests(null)).thenReturn(List.of(
                new AnalyzerMappingCatalogService.TestOption("1", "RAW-A", "RAW-A", List.of("99999-9"))));
        when(catalog.getActiveResultOptions("1"))
                .thenReturn(List.of(new AnalyzerMappingCatalogService.ResultOption("21", "991", "Detected", DETECTED)));
        var draft = defaults.resolve(profile("qualitative", codes("DETECTED", DETECTED)));
        assertNull(draft.tests().get(0).testId());
        assertEquals(AnalyzerUnresolvedReason.NO_MATCH, draft.tests().get(0).unresolvedReason());
    }

    @Test
    public void reportsIncompatibleWhenEveryCandidateCannotHoldACategoricalResult() throws Exception {
        var draft = defaults.resolve(profile("qualitative", codes("DETECTED", DETECTED)));
        assertEquals(AnalyzerMappingState.UNRESOLVED, draft.tests().get(0).mappingState());
        assertEquals(AnalyzerUnresolvedReason.INCOMPATIBLE, draft.tests().get(0).unresolvedReason());
        assertEquals(AnalyzerUnresolvedReason.INCOMPATIBLE, draft.results().get(0).unresolvedReason());
    }

    @Test
    public void bindsNumericDefaultOnlyWhenLocalTestAcceptsNumericResults() throws Exception {
        var unresolved = defaults.resolve(profile("quantitative", codes())).tests().get(0);
        assertNull(unresolved.testId());
        assertEquals(AnalyzerUnresolvedReason.INCOMPATIBLE, unresolved.unresolvedReason());
        TestResult number = new TestResult();
        number.setTestResultType("N");
        when(testResults.getActiveTestResultsByTest("1")).thenReturn(List.of(number));
        assertEquals("1", defaults.resolve(profile("quantitative", codes())).tests().get(0).testId());
    }

    @Test
    public void bindsAnAnswerOnItsCodeNotItsLabel() throws Exception {
        when(catalog.getActiveResultOptions("1")).thenReturn(List.of(
                new AnalyzerMappingCatalogService.ResultOption("21", "991", "Positive for target", DETECTED),
                new AnalyzerMappingCatalogService.ResultOption("22", "992", "Not detected", NOT_DETECTED)));
        var draft = defaults.resolve(profile("qualitative", codes("DETECTED", DETECTED, "NOT DETECTED", NOT_DETECTED)));
        assertEquals("21", draft.results().get(0).testResultId());
        assertEquals("22", draft.results().get(1).testResultId());
    }

    @Test
    public void doesNotBindAnAnswerWhoseLabelMatchesButWhoseCodeDiffers() throws Exception {
        when(catalog.getActiveResultOptions("1")).thenReturn(List.of(
                new AnalyzerMappingCatalogService.ResultOption("21", "991", "Detected", "LA99999-9")));
        var draft = defaults.resolve(profile("qualitative", codes("DETECTED", DETECTED)));
        assertEquals("1", draft.tests().get(0).testId());
        assertNull(draft.results().get(0).testResultId());
        assertEquals(AnalyzerUnresolvedReason.NO_MATCH, draft.results().get(0).unresolvedReason());
    }

    @Test
    public void reportsNoMatchForAValueTheProfileGivesNoCode() throws Exception {
        when(catalog.getActiveResultOptions("1")).thenReturn(List.of(
                new AnalyzerMappingCatalogService.ResultOption("21", "991", "Detected", DETECTED)));
        var draft = defaults.resolve(profile("qualitative", codes("DETECTED", DETECTED), "NOT DETECTED"));
        assertEquals("21", draft.results().get(0).testResultId());
        assertNull(draft.results().get(1).testResultId());
        assertEquals(AnalyzerUnresolvedReason.NO_MATCH, draft.results().get(1).unresolvedReason());
    }

    @Test
    public void retainsTwoAnswersWithTheSameCodeAsAmbiguous() throws Exception {
        when(catalog.getActiveResultOptions("1")).thenReturn(List.of(
                new AnalyzerMappingCatalogService.ResultOption("21", "991", "Detected", DETECTED),
                new AnalyzerMappingCatalogService.ResultOption("22", "992", "DETECTED", DETECTED)));
        var draft = defaults.resolve(profile("qualitative", codes("DETECTED", DETECTED)));
        assertEquals("1", draft.tests().get(0).testId());
        assertNull(draft.results().get(0).testResultId());
        assertEquals(AnalyzerUnresolvedReason.AMBIGUOUS, draft.results().get(0).unresolvedReason());
    }

    @Test
    public void ignoresTheRetiredSpecimenAndResultValueHints() throws Exception {
        when(catalog.searchActiveTests(null)).thenReturn(List.of(
                new AnalyzerMappingCatalogService.TestOption("1", "Test 1", null, List.of(LOINC), List.of("Serum")),
                new AnalyzerMappingCatalogService.TestOption("2", "Test 2", null, List.of(LOINC), List.of("Plasma"))));
        for (String id : List.of("1", "2")) {
            when(catalog.getActiveResultOptions(id)).thenReturn(List.of(
                    new AnalyzerMappingCatalogService.ResultOption("2" + id, "99" + id, "Target RNA detected")));
        }
        var document = profile("qualitative", codes(), "POSITIVE").document();
        var mapping = (ObjectNode) document.path("default_test_mappings").get(0);
        mapping.put("specimen_type_hint", "Plasma");
        mapping.putObject("result_value_hints").put("POSITIVE", "Target RNA detected");
        var draft = defaults.resolve(BridgeAnalyzerProfile.from(document));
        assertNull(draft.tests().get(0).testId());
        assertEquals(AnalyzerUnresolvedReason.AMBIGUOUS, draft.tests().get(0).unresolvedReason());
        assertNull(draft.results().get(0).testResultId());
    }

    @Test
    public void resolvesATestWithoutALoincAsNoMatch() throws Exception {
        var definition = new BridgeAnalyzerProfile.TestDefinition("OBSERVED", List.of(), null, null, null, null,
                List.of(), null, Map.of());
        var row = defaults.resolveTest(definition, List.of(test("1")));
        assertEquals(AnalyzerMappingState.UNRESOLVED, row.mappingState());
        assertEquals(AnalyzerUnresolvedReason.NO_MATCH, row.unresolvedReason());
    }

    @Test
    public void resolvesAnAnswerAgainstAnOperatorChosenTestsOptions() throws Exception {
        var definition = profile("qualitative", codes("DETECTED", DETECTED)).testDefinitions().get(0);
        var options = List.of(new AnalyzerMappingCatalogService.ResultOption("21", "991", "Whatever", DETECTED));
        var row = defaults.resolveAnswer(definition, "DETECTED", options);
        assertEquals(AnalyzerMappingState.BOUND, row.mappingState());
        assertEquals("21", row.testResultId());
        assertEquals("RAW-A", row.sourceRowKey());
    }

    // Cepheid 303-0251: HIVVL reports a number and a call on its main record, a
    // LOG record and an HIV-1 analyte record, all under HIVVL.
    @Test
    public void eachRecordBindsToTheLocalComponentWithItsCode() throws Exception {
        viralLoadCatalog(List.of("call", "LOG", "HIV-1"));

        var draft = defaults.resolve(viralLoadProfile());

        var main = row(draft, "");
        assertEquals(AnalyzerMappingState.BOUND, main.mappingState());
        assertEquals("1", main.testId());
        assertNull("the number goes on the test itself", main.componentId());
        assertEquals("comp-call", main.callComponentId());
        assertEquals("comp-LOG", row(draft, "&LOG").componentId());
        assertEquals("comp-HIV-1", row(draft, "HIV-1").componentId());
        assertEquals("opt-call-DETECTED", answer(draft, "", "DETECTED").testResultId());
        assertEquals("opt-HIV-1-POS", answer(draft, "HIV-1", "POS").testResultId());
    }

    @Test
    public void aMainRecordWhoseCallComponentIsMissingStaysUnresolved() throws Exception {
        viralLoadCatalog(List.of("LOG", "HIV-1"));

        var draft = defaults.resolve(viralLoadProfile());

        var main = row(draft, "");
        assertEquals("a call must never land on the number", AnalyzerMappingState.UNRESOLVED, main.mappingState());
        assertEquals(AnalyzerUnresolvedReason.NO_MATCH, main.unresolvedReason());
        assertEquals(AnalyzerMappingState.BOUND, row(draft, "&LOG").mappingState());
    }

    @Test
    public void aRecordWhoseComponentIsMissingStaysUnresolvedAlone() throws Exception {
        viralLoadCatalog(List.of("call", "HIV-1"));

        var draft = defaults.resolve(viralLoadProfile());

        assertEquals(AnalyzerMappingState.UNRESOLVED, row(draft, "&LOG").mappingState());
        assertEquals(AnalyzerUnresolvedReason.NO_MATCH, row(draft, "&LOG").unresolvedReason());
        assertEquals(AnalyzerMappingState.BOUND, row(draft, "").mappingState());
        assertEquals(AnalyzerMappingState.BOUND, row(draft, "HIV-1").mappingState());
    }

    @Test
    public void anAnswerBindsOnlyToAnOptionOfItsOwnComponent() throws Exception {
        viralLoadCatalog(List.of("call", "LOG", "HIV-1"));

        var draft = defaults.resolve(viralLoadProfile());

        assertEquals("opt-call-INVALID", answer(draft, "", "INVALID").testResultId());
        assertEquals("opt-HIV-1-INVALID", answer(draft, "HIV-1", "INVALID").testResultId());
    }

    // Cepheid 303-0251 §3: a French-language instrument sends NON DÉTECTÉ for NOT
    // DETECTED.
    @Test
    public void aTranslatedValueBindsToTheSameAnswerAsItsValue() throws Exception {
        viralLoadCatalog(List.of("call", "LOG", "HIV-1"));

        var draft = defaults.resolve(viralLoadProfile());

        assertEquals("opt-call-NOT DETECTED", answer(draft, "", "NOT DETECTED").testResultId());
        assertEquals("opt-call-NOT DETECTED", answer(draft, "", "NON DÉTECTÉ").testResultId());
        assertEquals(AnalyzerMappingState.BOUND, answer(draft, "", "NON DÉTECTÉ").mappingState());
    }

    private void viralLoadCatalog(List<String> componentCodes) {
        when(catalog.getActiveComponents("1")).thenReturn(componentCodes.stream()
                .map(code -> new AnalyzerMappingCatalogService.ComponentOption("comp-" + code, code)).toList());
        TestResult number = new TestResult();
        number.setTestResultType("N");
        when(testResults.getActiveTestResultsByTest("1")).thenReturn(List.of(number));
        when(catalog.getActiveResultOptions("1")).thenReturn(List.of(
                option("call", "DETECTED", DETECTED), option("call", "NOT DETECTED", NOT_DETECTED),
                option("call", "INVALID", INVALID), option("HIV-1", "POS", POSITIVE),
                option("HIV-1", "INVALID", INVALID)));
    }

    private static AnalyzerMappingCatalogService.ResultOption option(String component, String value, String code) {
        return new AnalyzerMappingCatalogService.ResultOption("opt-" + component + "-" + value, value, value, code,
                "comp-" + component);
    }

    private BridgeAnalyzerProfile viralLoadProfile() throws Exception {
        var mapper = new ObjectMapper();
        var document = mapper.createObjectNode();
        document.putObject("profileMeta").put("id", "fixture.records").put("displayName", "Viral load records");
        document.putObject("catalog").put("revision", 1).put("revisionFingerprint", "sha256:" + "b".repeat(64))
                .put("source", "SHIPPED").put("status", "ACTIVE");
        document.putObject("protocol").put("name", "ASTM");
        var test = document.putArray("default_test_mappings").addObject();
        test.put("test_code", "HIVVL").put("loinc", LOINC).put("result_type", "quantitative").put("unit", "copies/mL")
                .put("call_component", "call");
        test.putArray("values").add("DETECTED").add("NOT DETECTED").add("INVALID");
        var mainCodes = test.putObject("value_codes");
        mainCodes.putObject("DETECTED").put("system", "http://loinc.org").put("code", DETECTED);
        mainCodes.putObject("NOT DETECTED").put("system", "http://loinc.org").put("code", NOT_DETECTED);
        mainCodes.putObject("INVALID").put("system", "http://loinc.org").put("code", INVALID);
        test.putObject("translations").putArray("NOT DETECTED").add("NON DÉTECTÉ");
        var components = test.putArray("components");
        components.addObject().put("code", "call").put("result_type", "qualitative");
        components.addObject().put("code", "LOG").put("sub_identity", "&LOG").put("result_type", "quantitative")
                .put("unit", "log copies/mL");
        var analyte = components.addObject().put("code", "HIV-1").put("sub_identity", "HIV-1").put("result_type",
                "qualitative");
        analyte.putArray("values").add("POS").add("INVALID");
        var analyteCodes = analyte.putObject("value_codes");
        analyteCodes.putObject("POS").put("system", "http://loinc.org").put("code", POSITIVE);
        analyteCodes.putObject("INVALID").put("system", "http://loinc.org").put("code", INVALID);
        return BridgeAnalyzerProfile.from(document);
    }

    private static AnalyzerMappingTestDraft row(AnalyzerMappingDraft draft, String subIdentity) {
        return draft.tests().stream().filter(row -> subIdentity.equals(row.subIdentity())).findFirst()
                .orElseThrow(() -> new AssertionError("no row for '" + subIdentity + "'"));
    }

    private static AnalyzerMappingResultDraft answer(AnalyzerMappingDraft draft, String subIdentity, String value) {
        return draft.results().stream()
                .filter(row -> subIdentity.equals(row.subIdentity()) && value.equals(row.rawValue())).findFirst()
                .orElseThrow(() -> new AssertionError("no answer row for '" + subIdentity + "' " + value));
    }

    private static Map<String, String> codes(String... pairs) {
        Map<String, String> ordered = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            ordered.put(pairs[i], pairs[i + 1]);
        }
        return ordered;
    }

    private AnalyzerMappingCatalogService.TestOption test(String id) {
        return new AnalyzerMappingCatalogService.TestOption(id, "Local test " + id, null, List.of(LOINC));
    }

    private BridgeAnalyzerProfile profile(String type, Map<String, String> codes, String... uncodedValues)
            throws Exception {
        var mapper = new ObjectMapper();
        var document = mapper.createObjectNode();
        document.putObject("profileMeta").put("id", "fixture.defaults").put("displayName", "Default resolution");
        document.putObject("catalog").put("revision", 1).put("revisionFingerprint", "sha256:" + "a".repeat(64))
                .put("source", "SHIPPED").put("status", "ACTIVE");
        document.putObject("protocol").put("name", "ASTM");
        var definition = document.putArray("default_test_mappings").addObject();
        definition.put("test_code", "RAW-A").put("loinc", LOINC).put("result_type", type);
        var rawValues = definition.putArray("values");
        var valueCodes = definition.putObject("value_codes");
        for (var entry : codes.entrySet()) {
            rawValues.add(entry.getKey());
            valueCodes.putObject(entry.getKey()).put("system", "http://loinc.org").put("code", entry.getValue());
        }
        for (String value : uncodedValues) {
            rawValues.add(value);
        }
        if (valueCodes.isEmpty()) {
            definition.remove("value_codes");
        }
        if (rawValues.isEmpty()) {
            definition.remove("values");
        }
        return BridgeAnalyzerProfile.from(document);
    }
}
