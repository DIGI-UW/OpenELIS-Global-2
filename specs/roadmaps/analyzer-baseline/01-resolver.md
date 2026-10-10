# Step 1: One exact-match resolver

Part of [analyzer-baseline-roadmap.md](../analyzer-baseline-roadmap.md). Read that file's Rules and Repo working agreements first; they apply here.

Goal: setup and the mapping editor resolve defaults with one rule, by exact
standard-code match, and say why when a row stays unresolved.

### Facts

- Resolver today: `src/main/java/org/openelisglobal/analyzer/service/AnalyzerMappingDefaults.java`.
  Lines 28-41 filter candidates by LOINC, then specimen hint, then require
  uniqueness, then tie-break on code, alias or name hint. Lines 47-52 drop
  a selected test that cannot hold the result, after uniqueness. Lines
  57-67 match answers by label, then by `result_value_hints`.
- Second matcher to delete: `AnalyzerTypeMappingServiceImpl.uniqueSuggestion`
  and `matches` (lines 250-270 of
  `src/main/java/org/openelisglobal/analyzer/service/AnalyzerTypeMappingServiceImpl.java`);
  called at lines 224-226.
- Catalog lookups: `AnalyzerMappingCatalogService.searchActiveTests(null)`
  returns `TestOption(id, name, code, loincCodes, specimenTypes)`;
  `getActiveResultOptions(testId)` returns `ResultOption(id, testResultId, label)`.
  Answer code lives on `Dictionary.loincCode` (loaded by
  `DictionaryConfigurationHandler`, CSV column `loincCode`); `ResultOption`
  must be extended to carry it.
- Profile input: `BridgeAnalyzerProfile.TestDefinition(analyzerCode, aliases,
testNameHint, loinc, unit, resultType, resultValues, specimenTypeHint,
normalizedCoding, resultValueHints)`. After step 6 the profile carries an
  answer code per value; until then the resolver treats a missing code as
  `NO_MATCH` for that answer.
- Mapping states: `AnalyzerSiteBindingMappingState` is `BOUND`, `EXCLUDED`,
  `UNRESOLVED`. Add an unresolved-reason enum `NO_MATCH`, `AMBIGUOUS`,
  `INCOMPATIBLE` carried on `AnalyzerSiteBindingTestDraft` and
  `AnalyzerSiteBindingResultDraft` and returned by the mapping API.
- Tests: `src/test/java/org/openelisglobal/analyzer/service/AnalyzerMappingDefaultsTest.java`
  (unit, Mockito), `AnalyzerTypeMappingServiceTest.java`; E2E
  `frontend/playwright/tests/foundational/harness/stock-analyzer-defaults.spec.ts`
  asserts one answer per test via
  `frontend/playwright/helpers/analyzer-clinical-order.ts:93-102`; it
  relies on label matching, so it fails from this step until step 7 rewrites
  it (T7.1).
- Profile draft editor: `frontend/src/components/analyzers/AnalyzerTypeManagement/ProfileTestDefinitions.jsx`
  writes `specimen_type_hint` (line 35) and `result_value_hints` (88-124);
  i18n keys `analyzerType.editor.specimenType`, `resultValueHint`,
  `resultValueHintHelp`.
- Profile answer codes: step 6 adds `value_codes{value -> {system, code}}`
  per categorical test. Read it as an optional field here.

### Build

```
- [x] T1.1 Red: unit test, one usable + one answerless candidate on the same LOINC and specimen binds the usable one
- [x] T1.2 Red: unit test, two usable candidates stay UNRESOLVED with reason AMBIGUOUS
- [x] T1.3 Red: unit test, answer binds on Dictionary.loincCode, not label
- [x] T1.4 Red: unit test, editor suggestion for an UNRESOLVED row equals AnalyzerMappingDefaults output for the same catalog
- [x] T1.5 Move the compatibility check before uniqueness; add the reason enum; remove hint handling
- [x] T1.6 Extend ResultOption with answerCode; match answers on it
- [x] T1.7 Delete uniqueSuggestion and matches; have composeTestRow call the resolver for UNRESOLVED rows (tests and answers)
- [x] T1.8 Remove the specimen-hint and result-value-hint inputs and their i18n keys from the profile draft editor (ProfileTestDefinitions.jsx and ProfileDraftEditor.test.jsx), so nothing authors a field OE2 no longer reads
- [x] T1.9 Green; format cold; commit; stack PR on step 0
```

### Verify

```bash
mvn -Dtest=AnalyzerMappingDefaultsTest,AnalyzerTypeMappingServiceTest test
grep -rn "uniqueSuggestion\|result_value_hints\|resultValueHints\|specimenTypeHint" src/main/java   # 0 hits outside BridgeAnalyzerProfile parsing
cd frontend && npx vitest run src/components/analyzers/AnalyzerTypeManagement
gh pr checks <PR>
```

### Done when

1. `uniqueSuggestion` and `matches` are deleted; T1.4 passes. (`grep`, `mvn`)
2. T1.1 to T1.3 pass. (`mvn`)
3. Every `UNRESOLVED` row carries a reason in the API response. (`mvn`;
   read `AnalyzerTypeMappingView`)
4. No code path reads `result_value_hints` or `specimen_type_hint`. (`grep`)
5. The profile draft editor offers no specimen or result-value hint input.
   (`vitest`, `grep`)
6. The harness E2E for this behaviour (a fresh setup of every shipped
   analyzer binds every declared row) is T7.1. It cannot pass here: shipped
   profiles gain answer codes in step 6 and the harness dictionary in step 7,
   so the stack lands as a unit and E2E is judged on its top PR.

### Background (optional)

- [M1](https://claude.ai/artifact/87JjCR7fg87DCqFeS5TEfH#m1), [M2](https://claude.ai/artifact/87JjCR7fg87DCqFeS5TEfH#m2), [M3](https://claude.ai/artifact/87JjCR7fg87DCqFeS5TEfH#m3), [P1](https://claude.ai/artifact/87JjCR7fg87DCqFeS5TEfH#p1)
- [Decision: answer matching](https://claude.ai/artifact/87JjCR7fg87DCqFeS5TEfH#d-answer-matching)
