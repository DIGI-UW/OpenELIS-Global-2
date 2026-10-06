# Step 7: Harness copy of the generic dictionary

Part of [analyzer-baseline-roadmap.md](../analyzer-baseline-roadmap.md). Read that file's Rules and Repo working agreements first; they apply here.

Goal: the harness runs on a copy of OE2's generic dictionary that lets every
shipped analyzer bind out of the box; the Bridge and mock pins are bumped;
the baseline E2E is green.

### Facts

- Today the harness loads `projects/analyzer-harness/config-templates/`
  (`tests/molecular-tests.csv`, `test-results/molecular-test-results.csv`)
  into a `configuration-data` volume via the `harness-catalog-init` service
  (`projects/analyzer-harness/docker-compose.base.yml:2-20`,
  `docker-compose.dev.yml:4,50`, `.github/ci/ci.analyzer-harness.yml:5,98`).
- Generic dictionary shape: `volume/configuration/backend/` with domains
  `tests/`, `test-results/`, `dictionaries/`, `sample-types/`,
  `test-sections/`; CSV headers as in
  `tests/example-tests.csv` (`testName,testSection,sampleType,loinc,isActive,
isOrderable,sortOrder,unitOfMeasure,localization:*`) and
  `test-results/example-test-results.csv` (`testName,resultType,resultValue,
dictionaryCategory,sortOrder,isQuantifiable,isActive,isNormal,
significantDigits,flags,significance`). Dictionary rows carry `loincCode`.
  The loader updates an existing test matched by local code or name,
  including `loinc` and `isActive` (`TestConfigurationHandler.java:271-282,
373-380`); a row listing `A|B` sample types creates one test linked to
  both.
- Generic dictionary gaps for GeneXpert: `SARS-CoV-2 PCR`, `Xpert MTB/RIF`,
  `Rifampin Resistance` have no answer choices; `HIV-1 Viral Load` numeric
  only; no components. Legacy Liquibase rows carrying 94500-6 wrongly:
  `COVIDPCR(Sputum)`, `COVIDPCR(Fluid)`, `COVID-19ANTIBODYIgM/IgG(*)`,
  `DENGUEPCR(Serum)` and HIV VL variants (`liquibase/2.3.x.x/new_tests.xml`).
  `CovidResultsBuilderImpl` reads 94500-6.
- `.gitignore` line 92 ignores `**/test-results/`; add an explicit negation
  for the harness dictionary path.
- Pins: submodules `tools/openelis-analyzer-bridge`,
  `tools/analyzer-mock-server`; image tags in
  `docker-compose.analyzers.yml` and
  `projects/analyzer-harness/docker-compose.*.yml`.
- E2E: `frontend/playwright/tests/foundational/harness/stock-analyzer-defaults.spec.ts`
  (rewritten here, T7.1), `analyzer-clinical-order.spec.ts`,
  `ogc-1054-analyzer-mvp.spec.ts`; traffic helper
  `frontend/playwright/helpers/analyzer-native-traffic.ts:92-100` overrides
  `test_code` and `value` per message; remove those overrides.

### Build

```
- [ ] T7.1 Red: E2E, fresh setup of every shipped analyzer binds every declared test, value and component
- [ ] T7.2 Red: E2E, every vendor outcome from the mock lands on the right test, component or QC result, or is held with its note; readback by accession, test, value
- [ ] T7.3 Create projects/analyzer-harness/dictionary/ as a copy of volume/configuration/backend/ with: answers for SARS-CoV-2 PCR as the instrument reports them (Positive, Negative, Invalid; LOINC LL2021-5), tests for Influenza A, Influenza B and RSV with the same answers for the 302-7279 panels (LOINC from the step-5 note), Xpert MTB/RIF -> three tests per rule 12, HIV-1 Viral Load as one test per rule 11 (numeric copies/mL primary; components for the call with Detected, Not detected and Invalid, the LOG, and the HIV-1, IQS-H and IQS-L analyte records with their Ct), components per assay, corrected LOINC on the legacy rows, answer codings in the step-2c answer terminology CSV (every LOINC, SNOMED and CIEL code step 5 cites)
- [ ] T7.4 Point harness-catalog-init at the new directory; delete config-templates/; fix .gitignore
- [ ] T7.5 Bump Bridge and mock submodule pins and image tags to the step-6 and step-8 releases
- [ ] T7.6 Remove per-message overrides from analyzer-native-traffic.ts; use mock fixtures
- [ ] T7.7 Green; format cold; commit; top of stack PR
```

### Verify

```bash
ls projects/analyzer-harness/config-templates 2>&1   # No such file
cd frontend && npm run pw:test -- --project=harness-foundational
projects/analyzer-harness/ci-parity-test.sh
grep -rn "test_code:\|value:" frontend/playwright/helpers/analyzer-native-traffic.ts   # no per-message overrides
gh pr checks <PR>
```

### Done when

1. `config-templates/` is gone; the harness loads the dictionary copy through
   the CSV path. (`ls`, compose read)
2. T7.1 passes. (`pw:test`)
3. T7.2 passes. (`pw:test`)
4. No two active tests in the harness dictionary share a LOINC and a
   specimen type; `CovidResultsBuilderImpl` still finds its COVID test.
   (SQL check in the E2E; existing COVID report test)
5. ERROR, INVALID and NO RESULT remain distinct. (T7.2)
6. All three CI checkpoints pass on the top of the stack. (`gh pr checks`)

### Background (optional)

- [C1](https://claude.ai/artifact/87JjCR7fg87DCqFeS5TEfH#c1), [Four catalogs that never met](https://claude.ai/artifact/87JjCR7fg87DCqFeS5TEfH#coverage), [Proper concepts](https://claude.ai/artifact/87JjCR7fg87DCqFeS5TEfH#concepts)
- Decisions: [harness catalog](https://claude.ai/artifact/87JjCR7fg87DCqFeS5TEfH#d-harness), [legacy rows](https://claude.ai/artifact/87JjCR7fg87DCqFeS5TEfH#d-legacy-rows), [packaging](https://claude.ai/artifact/87JjCR7fg87DCqFeS5TEfH#d-packaging)
