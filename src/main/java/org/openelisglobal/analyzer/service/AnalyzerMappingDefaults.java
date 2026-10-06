package org.openelisglobal.analyzer.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingState;
import org.openelisglobal.testresult.service.TestResultService;
import org.springframework.stereotype.Service;

/**
 * Resolves a profile's defaults against the local catalog by exact
 * standard-code match, and says why when a row cannot be resolved. Test LOINC
 * selects the test; the answer code selects the answer. Never chooses between
 * two usable candidates.
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
                tests.add(resolution.draft());
                definition.resultValues().forEach(raw -> results
                        .addAll(translated(unresolvedAnswer(definition, raw, reason), definition.translations())));
                for (var component : definition.recordComponents()) {
                    tests.add(unresolvedRecord(definition, component, reason));
                    component.resultValues()
                            .forEach(raw -> results.addAll(translated(
                                    unresolvedAnswer(definition.analyzerCode(), component.subIdentity(), raw, reason),
                                    component.translations())));
                }
                continue;
            }
            String testId = resolution.draft().testId();
            List<AnalyzerMappingCatalogService.ComponentOption> components = definition.components().isEmpty()
                    ? List.of()
                    : catalog.getActiveComponents(testId);

            String callTarget = definition.callComponent() == null ? null
                    : componentId(components, definition.callComponent());
            if (definition.callComponent() != null && callTarget == null) {
                tests.add(new AnalyzerMappingTestDraft(definition.analyzerCode(), AnalyzerMappingState.UNRESOLVED, null,
                        AnalyzerUnresolvedReason.NO_MATCH));
                definition.resultValues()
                        .forEach(raw -> results
                                .addAll(translated(unresolvedAnswer(definition, raw, AnalyzerUnresolvedReason.NO_MATCH),
                                        definition.translations())));
            } else {
                tests.add(new AnalyzerMappingTestDraft(definition.analyzerCode(), AnalyzerMappingState.BOUND, testId,
                        null, null, null, "", callTarget));
                var mainOptions = callTarget == null ? resolution.options()
                        : optionsOf(resolution.options(), callTarget);
                definition.resultValues().forEach(raw -> results
                        .addAll(translated(resolveAnswer(definition, raw, mainOptions), definition.translations())));
            }

            for (var component : definition.recordComponents()) {
                String componentId = componentId(components, component.code());
                if (componentId == null) {
                    tests.add(unresolvedRecord(definition, component, AnalyzerUnresolvedReason.NO_MATCH));
                    component.resultValues()
                            .forEach(
                                    raw -> results
                                            .addAll(translated(
                                                    unresolvedAnswer(definition.analyzerCode(), component.subIdentity(),
                                                            raw, AnalyzerUnresolvedReason.NO_MATCH),
                                                    component.translations())));
                    continue;
                }
                tests.add(new AnalyzerMappingTestDraft(definition.analyzerCode(), AnalyzerMappingState.BOUND, testId,
                        componentId, null, null, component.subIdentity(), null));
                var componentOptions = optionsOf(resolution.options(), componentId);
                component.resultValues()
                        .forEach(
                                raw -> results.addAll(translated(
                                        resolveAnswer(definition.analyzerCode(), component.subIdentity(),
                                                component.valueCodes(), raw, componentOptions),
                                        component.translations())));
            }
        }
        return new AnalyzerMappingDraft(tests, results);
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
            Map<String, BridgeAnalyzerProfile.NormalizedCoding> valueCodes, String rawValue,
            List<AnalyzerMappingCatalogService.ResultOption> options) {
        BridgeAnalyzerProfile.NormalizedCoding coding = valueCodes.get(rawValue);
        if (coding == null) {
            return unresolvedAnswer(code, subIdentity, rawValue, AnalyzerUnresolvedReason.NO_MATCH);
        }
        var matches = options.stream().filter(option -> sameCode(option.answerCode(), coding.code())).toList();
        if (matches.size() > 1) {
            return unresolvedAnswer(code, subIdentity, rawValue, AnalyzerUnresolvedReason.AMBIGUOUS);
        }
        if (matches.isEmpty()) {
            return unresolvedAnswer(code, subIdentity, rawValue, AnalyzerUnresolvedReason.NO_MATCH);
        }
        return new AnalyzerMappingResultDraft(code, rawValue, AnalyzerMappingState.BOUND, matches.get(0).id(), null,
                null, subIdentity);
    }

    /**
     * A declared value's row, followed by one row per translation of it carrying
     * the same decision, so an instrument binds whatever language it runs.
     */
    private static List<AnalyzerMappingResultDraft> translated(AnalyzerMappingResultDraft row,
            Map<String, List<String>> translations) {
        List<AnalyzerMappingResultDraft> rows = new ArrayList<>();
        rows.add(row);
        for (String text : translations.getOrDefault(row.rawValue(), List.of())) {
            rows.add(new AnalyzerMappingResultDraft(row.sourceRowKey(), text, row.mappingState(), row.testResultId(),
                    row.unresolvedReason(), row.origin(), row.subIdentity()));
        }
        return rows;
    }

    private static String componentId(List<AnalyzerMappingCatalogService.ComponentOption> components, String code) {
        var matches = components.stream().filter(component -> code.equals(component.code())).toList();
        return matches.size() == 1 ? matches.get(0).id() : null;
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
        String loinc = definition.loinc();
        var candidates = loinc == null ? List.<AnalyzerMappingCatalogService.TestOption>of()
                : activeTests.stream().filter(test -> test.loincCodes().contains(loinc)).toList();
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

    /**
     * The profile names an answer by system and code; the dictionary stores the
     * code alone.
     */
    private static boolean sameCode(String local, String profile) {
        return local != null && profile != null && !local.isBlank() && local.trim().equalsIgnoreCase(profile.trim());
    }

    private record TestResolution(AnalyzerMappingTestDraft draft,
            List<AnalyzerMappingCatalogService.ResultOption> options) {
    }
}
