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
- [x] T7.0 OE2 sends the Bridge what the Assays step stores. The Bridge holds the applied mapping's instrument codes as `values.codeOverrides` (profile code to instrument code, only for enabled, declared assays the instrument renames): every connection create and save sends them in place of any supplied, and Apply re-sends them with the pin, re-activating a running analyzer so its new codes take effect. A draft mapping never reaches the Bridge. `numberFormat` is a profile connection field, so it already travels with the connection values and reads back from the view's fields; OE2 lacked its labels, now guarded by a contract test over every shipped profile. The create and update schemas give `codeOverrides` its shape (Bridge #75)
- [ ] T7.1 Red: E2E, fresh setup of every shipped analyzer binds every declared test, value and component. Against GeneXpert rev 8 the stock codes are HIVVL, SARSCOV2, FLUA, FLUB, RSV, SARSCOV2_3, MTB, RIF; MTB and RIF are text results with no answers, held until a lab maps them, so the old MTB-RIF, RIF (46244-0) and COVID19 scenarios are retired, not rewritten
- [ ] T7.1b Red: E2E, an assay's instrument code set in the Assays step is used for result translation and for an outbound order (moved from T4.3; needs the Bridge's `codeOverrides` from step 6)
- [ ] T7.2 Red: E2E, every vendor outcome from the mock lands on the right test, component or QC result, or is held with its note; readback by accession, test, value
- [ ] T7.2b E2E for each placement state of step 1b through native mock traffic: tube ID to its tube, rerun on a held result, two tubes, unordered test, unknown ID, a mistyped ID placed on its order, a mismatched patient, and a FILE plate with one mistyped sample name
- [x] T7.3a Config import puts an answer on a named result component: the `test-results` CSV takes an optional `componentCode` column, an option is matched by value and component, and the primary result's type comes from its own options only. Found while building T7.3: the HIV-1 call and the analyte and control records need answers on components, and import could only reach the primary result
- [ ] T7.3 Create projects/analyzer-harness/dictionary/ as a copy of volume/configuration/backend/ with: answers for SARS-CoV-2 PCR as the instrument reports them (Positive, Negative, Invalid; LOINC LL2021-5), tests for Influenza A, Influenza B and RSV with the same answers for the 302-7279 panels (LOINC from the step-5 note), MTB and RIF as text tests without answers (the three results of rule 12 wait for a vendor source, step 5), HIV-1 Viral Load as one test per rule 11 (numeric copies/mL primary; components for the call with Detected, Not detected and Invalid, the LOG, and the HIV-1, IQS-H and IQS-L analyte records with their Ct), components per assay, corrected LOINC on the legacy rows, answer codings in the step-2c answer terminology CSV (every LOINC, SNOMED and CIEL code step 5 cites)
- [ ] T7.4 Point harness-catalog-init at the new directory; delete config-templates/; fix .gitignore
- [ ] T7.5 Pins first, tags later: the submodule pins point at the Bridge #75 and mock #53 heads (done 6 Oct; OE2 builds both from the submodules, so the harness E2E runs against them). The image tags in `docker-compose.analyzers.yml` follow the releases a maintainer cuts after review; until then the `deployment-contract` check is red by design
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
