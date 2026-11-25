package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingState;
import org.openelisglobal.terminology.Coding;
import org.openelisglobal.testresult.service.TestResultService;
import org.springframework.stereotype.Service;

/**
 * Resolves a profile's defaults against the local catalog by exact code match,
 * and says why when a row cannot be resolved. A test or answer is selected by a
 * coding it shares with the profile, in any system. Never chooses between two
 * usable candidates.
 */
@Service
public class AnalyzerMappingDefaults {
    private final AnalyzerMappingCatalogService catalog;
    private final TestResultService testResults;

    public AnalyzerMappingDefaults(AnalyzerMappingCatalogService catalog, TestResultService testResults) {
        this.catalog = catalog;
        this.testResults = testResults;
    }

    public AnalyzerMappingDraft resolve(BridgeAnalyzerProfile profile) {
        return resolve(profile, catalog.searchActiveTests(null));
    }

    /**
     * As {@link #resolve(BridgeAnalyzerProfile)}, against an active-test list the
     * caller already read.
     */
    public AnalyzerMappingDraft resolve(BridgeAnalyzerProfile profile,
            List<AnalyzerMappingCatalogService.TestOption> active) {
        List<AnalyzerMappingTestDraft> tests = new ArrayList<>();
        List<AnalyzerMappingResultDraft> results = new ArrayList<>();
        for (var definition : profile.testDefinitions()) {
            TestResolution resolution = resolve(definition, active);
            if (resolution.options() == null) {
                AnalyzerUnresolvedReason reason = resolution.draft().unresolvedReason();
                // No local test carries the assay's code: most likely a cartridge this lab
                // does not run, so it starts off; the operator turns it on if it does.
                tests.add(reason == AnalyzerUnresolvedReason.NO_MATCH ? resolution.draft().withAssay(false)
                        : resolution.draft());
                definition.resultValues().forEach(raw -> results.add(unresolvedAnswer(definition, raw, reason)));
                for (var component : definition.recordComponents()) {
                    tests.add(unresolvedRecord(definition, component, reason));
                    component.resultValues().forEach(raw -> results
                            .add(unresolvedAnswer(definition.analyzerCode(), component.subIdentity(), raw, reason)));
                }
                continue;
            }
            String testId = resolution.draft().testId();
            List<AnalyzerMappingCatalogService.ComponentOption> components = catalog.getActiveComponents(testId);

            String callTarget = definition.callComponent() == null ? null
                    : componentId(components, definition.callComponent());
            if (definition.callComponent() != null && callTarget == null) {
                tests.add(new AnalyzerMappingTestDraft(definition.analyzerCode(), AnalyzerMappingState.UNRESOLVED, null,
                        AnalyzerUnresolvedReason.NO_MATCH));
                definition.resultValues().forEach(
                        raw -> results.add(unresolvedAnswer(definition, raw, AnalyzerUnresolvedReason.NO_MATCH)));
            } else {
                tests.add(new AnalyzerMappingTestDraft(definition.analyzerCode(), AnalyzerMappingState.BOUND, testId,
                        null, null, null, "", callTarget));
                var mainOptions = callTarget == null ? primaryOptions(resolution.options(), components)
                        : optionsOf(resolution.options(), callTarget);
                definition.resultValues().forEach(raw -> results.add(resolveAnswer(definition, raw, mainOptions)));
            }

            for (var component : definition.recordComponents()) {
                String componentId = componentId(components, component.code());
                if (componentId == null) {
                    tests.add(unresolvedRecord(definition, component, AnalyzerUnresolvedReason.NO_MATCH));
                    component.resultValues().forEach(raw -> results.add(unresolvedAnswer(definition.analyzerCode(),
                            component.subIdentity(), raw, AnalyzerUnresolvedReason.NO_MATCH)));
                    continue;
                }
                tests.add(new AnalyzerMappingTestDraft(definition.analyzerCode(), AnalyzerMappingState.BOUND, testId,
                        componentId, null, null, component.subIdentity(), null));
                var componentOptions = optionsOf(resolution.options(), componentId);
                component.resultValues().forEach(raw -> results.add(resolveAnswer(definition.analyzerCode(),
                        component.subIdentity(), component.valueCodes(), raw, componentOptions)));
            }
        }
        return excludedByDefault(profile, new AnalyzerMappingDraft(tests, results));
    }

    /**
     * Rows the profile says a new analyzer starts without: a test or part marked
     * {@code "default": "EXCLUDED"}, and each canonical value in
     * {@code value_defaults}.
     */
    private static AnalyzerMappingDraft excludedByDefault(BridgeAnalyzerProfile profile, AnalyzerMappingDraft draft) {
        Set<AnalyzerMappingRowKey> records = new HashSet<>();
        Set<List<String>> values = new HashSet<>();
        for (JsonNode test : profile.document().path("default_test_mappings")) {
            String code = test.path("test_code").asText();
            collectExclusions(code, "", test, records, values);
            for (JsonNode component : test.path("components")) {
                if (component.hasNonNull("sub_identity")) {
                    collectExclusions(code, component.path("sub_identity").asText(), component, records, values);
                }
            }
        }
        if (records.isEmpty() && values.isEmpty()) {
            return draft;
        }
        List<AnalyzerMappingTestDraft> tests = draft.tests().stream()
                .map(row -> records.contains(row.rowKey())
                        ? new AnalyzerMappingTestDraft(row.sourceRowKey(), AnalyzerMappingState.EXCLUDED, null, null,
                                null, row.origin(), row.subIdentity(), null, row.enabled())
                        : row)
                .toList();
        List<AnalyzerMappingResultDraft> results = draft.results().stream()
                .map(row -> records.contains(row.rowKey())
                        || values.contains(List.of(row.sourceRowKey(), row.subIdentity(), row.rawValue()))
                                ? new AnalyzerMappingResultDraft(row.sourceRowKey(), row.rawValue(),
                                        AnalyzerMappingState.EXCLUDED, null, null, row.origin(), row.subIdentity())
                                : row)
                .toList();
        return new AnalyzerMappingDraft(tests, results);
    }

    private static void collectExclusions(String code, String subIdentity, JsonNode owner,
            Set<AnalyzerMappingRowKey> records, Set<List<String>> values) {
        if ("EXCLUDED".equals(owner.path("default").asText(null))) {
            records.add(new AnalyzerMappingRowKey(code, subIdentity));
        }
        owner.path("value_defaults").fields().forEachRemaining(entry -> {
            if ("EXCLUDED".equals(entry.getValue().asText())) {
                values.add(List.of(code, subIdentity, entry.getKey()));
            }
        });
    }

    /** Resolves one profile test against the active catalog. */
    public AnalyzerMappingTestDraft resolveTest(BridgeAnalyzerProfile.TestDefinition definition,
            List<AnalyzerMappingCatalogService.TestOption> activeTests) {
        return resolve(definition, activeTests).draft();
    }

    /**
     * Resolves one declared answer of a profile test against the active answers of
     * a test, which may be one the operator chose rather than a default.
     */
    public AnalyzerMappingResultDraft resolveAnswer(BridgeAnalyzerProfile.TestDefinition definition, String rawValue,
            List<AnalyzerMappingCatalogService.ResultOption> options) {
        return resolveAnswer(definition.analyzerCode(), "", definition.valueCodes(), rawValue, options);
    }

    /**
     * Resolves one declared answer of a record against the options of the component
     * it lands on.
     */
    public AnalyzerMappingResultDraft resolveAnswer(String code, String subIdentity,
            Map<String, List<Coding>> valueCodes, String rawValue,
            List<AnalyzerMappingCatalogService.ResultOption> options) {
        List<Coding> codings = valueCodes.getOrDefault(rawValue, List.of());
        if (codings.isEmpty()) {
            return unresolvedAnswer(code, subIdentity, rawValue, AnalyzerUnresolvedReason.NO_MATCH);
        }
        var matches = options.stream().filter(option -> sharesACoding(option.codings(), codings)).toList();
        if (matches.size() > 1) {
            return unresolvedAnswer(code, subIdentity, rawValue, AnalyzerUnresolvedReason.AMBIGUOUS);
        }
        if (matches.isEmpty()) {
            return unresolvedAnswer(code, subIdentity, rawValue, AnalyzerUnresolvedReason.NO_MATCH);
        }
        return new AnalyzerMappingResultDraft(code, rawValue, AnalyzerMappingState.BOUND, matches.get(0).id(), null,
                null, subIdentity);
    }

    private static String componentId(List<AnalyzerMappingCatalogService.ComponentOption> components, String code) {
        var matches = components.stream().filter(component -> code.equals(component.code())).toList();
        return matches.size() == 1 ? matches.get(0).id() : null;
    }

    /**
     * The answers of the test's primary result: those on no component or on its
     * active primary one. An analyte record's answers may share the same codes, and
     * an answer left on a deactivated component belongs to no result.
     */
    private static List<AnalyzerMappingCatalogService.ResultOption> primaryOptions(
            List<AnalyzerMappingCatalogService.ResultOption> options,
            List<AnalyzerMappingCatalogService.ComponentOption> components) {
        java.util.Set<String> primary = components.stream()
                .filter(AnalyzerMappingCatalogService.ComponentOption::primary)
                .map(AnalyzerMappingCatalogService.ComponentOption::id).collect(java.util.stream.Collectors.toSet());
        return options.stream().filter(option -> option.componentId() == null || primary.contains(option.componentId()))
                .toList();
    }

    private static List<AnalyzerMappingCatalogService.ResultOption> optionsOf(
            List<AnalyzerMappingCatalogService.ResultOption> options, String componentId) {
        return options.stream().filter(option -> componentId.equals(option.componentId())).toList();
    }

    private static AnalyzerMappingTestDraft unresolvedRecord(BridgeAnalyzerProfile.TestDefinition definition,
            BridgeAnalyzerProfile.ComponentDefinition component, AnalyzerUnresolvedReason reason) {
        return new AnalyzerMappingTestDraft(definition.analyzerCode(), AnalyzerMappingState.UNRESOLVED, null, null,
                reason, null, component.subIdentity(), null);
    }

    private TestResolution resolve(BridgeAnalyzerProfile.TestDefinition definition,
            List<AnalyzerMappingCatalogService.TestOption> activeTests) {
        var candidates = activeTests.stream().filter(test -> sharesACoding(test.codings(), definition.codings()))
                .toList();
        if (candidates.isEmpty()) {
            return unresolvedTest(definition, AnalyzerUnresolvedReason.NO_MATCH);
        }
        Map<AnalyzerMappingCatalogService.TestOption, List<AnalyzerMappingCatalogService.ResultOption>> usable = new LinkedHashMap<>();
        for (var candidate : candidates) {
            var options = catalog.getActiveResultOptions(candidate.id());
            if (canHold(definition, candidate, options)) {
                usable.put(candidate, options);
            }
        }
        if (usable.isEmpty()) {
            return unresolvedTest(definition, AnalyzerUnresolvedReason.INCOMPATIBLE);
        }
        if (usable.size() > 1) {
            return unresolvedTest(definition, AnalyzerUnresolvedReason.AMBIGUOUS);
        }
        var selected = usable.entrySet().iterator().next();
        return new TestResolution(new AnalyzerMappingTestDraft(definition.analyzerCode(), AnalyzerMappingState.BOUND,
                selected.getKey().id()), selected.getValue());
    }

    private boolean canHold(BridgeAnalyzerProfile.TestDefinition definition,
            AnalyzerMappingCatalogService.TestOption test, List<AnalyzerMappingCatalogService.ResultOption> options) {
        // With a call component, the test's values are the call's and live on that
        // component; the test itself holds the number.
        boolean categorical = !definition.resultValues().isEmpty() && definition.callComponent() == null
                || "qualitative".equalsIgnoreCase(definition.resultType());
        boolean numeric = "quantitative".equalsIgnoreCase(definition.resultType());
        if (categorical && options.isEmpty()) {
            return false;
        }
        return !numeric || testResults.getActiveTestResultsByTest(test.id()).stream()
                .anyMatch(option -> "N".equals(option.getTestResultType()));
    }

    private static TestResolution unresolvedTest(BridgeAnalyzerProfile.TestDefinition definition,
            AnalyzerUnresolvedReason reason) {
        return new TestResolution(
                new AnalyzerMappingTestDraft(definition.analyzerCode(), AnalyzerMappingState.UNRESOLVED, null, reason),
                null);
    }

    private static AnalyzerMappingResultDraft unresolvedAnswer(BridgeAnalyzerProfile.TestDefinition definition,
            String rawValue, AnalyzerUnresolvedReason reason) {
        return unresolvedAnswer(definition.analyzerCode(), "", rawValue, reason);
    }

    private static AnalyzerMappingResultDraft unresolvedAnswer(String code, String subIdentity, String rawValue,
            AnalyzerUnresolvedReason reason) {
        return new AnalyzerMappingResultDraft(code, rawValue, AnalyzerMappingState.UNRESOLVED, null, reason, null,
                subIdentity);
    }

    private static boolean sharesACoding(List<Coding> local, List<Coding> profile) {
        return local.stream().anyMatch(coding -> profile.stream().anyMatch(coding::sameAs));
    }

    private record TestResolution(AnalyzerMappingTestDraft draft,
            List<AnalyzerMappingCatalogService.ResultOption> options) {
    }
}
