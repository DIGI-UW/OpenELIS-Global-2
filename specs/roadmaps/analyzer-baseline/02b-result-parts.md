# Step 2b: Each result part lands in its own place

Part of [analyzer-baseline-roadmap.md](../analyzer-baseline-roadmap.md). Read that file's Rules and Repo working agreements first; they apply here.

Goal: OE2 maps each record of an analyzer result, by code and sub-identity
(rule 17), and a record's number and call to their own (test, component), so a
viral load's number with its comparator, its call, its log and its analyte
records each land in their own place, and the review row shows what the
instrument reported about the result.

### Facts

- Mapping rows today are keyed by analyzer code: `analyzer_mapping_test`
  (`source_row_key`) and `analyzer_mapping_result` (`source_row_key`,
  `raw_value`). Import stages one row per Observation in
  `AnalyzerNormalizedResultImportServiceImpl.toStagedResult`, keyed by the raw
  code; a code with any declared answer holds every value without an answer
  mapping as an unknown value (`sourcesWithResultMappings`).
- `AnalyzerNormalizedResultContract.parseResult` reads the raw code, the
  raw-value extension, `valueQuantity` units, `Quantity.comparator`, notes,
  `dataAbsentReason` and the instrument-reported patient. Since step 2 the
  staged value is the comparator prefixed to the raw text. Cepheid sends an
  off-scale result with no number in R.4 (303-0251 §2.1.1), so the staged
  value must come from the bundle's `valueQuantity` (value and comparator);
  the raw text stays as sent and may be empty for that number.
- From step 6 the Bridge emits one Observation per record (rule 17): the
  test code in `Observation.code`, the record's sub-identity (`HIV-1`,
  `HIV-1&Ct`, `&LOG`; absent for the main result) in
  `http://hl7.org/fhir/StructureDefinition/observation-v2-subid`
  (`original-sub-identifier`), the number in `valueQuantity`, and on a record
  with both, the call in `Observation.interpretation` (`DET`, `ND`, with the
  instrument's text). A record with no number carries its call as the
  value.
- The profile declares a test's records in its `components[]` (rule 17;
  the Bridge validates the field in step 6, OE2 reads it here):

  ```json
  {
    "test_code": "HIVVL",
    "loinc": "20447-9",
    "result_type": "quantitative",
    "unit": "copies/mL",
    "values": ["DETECTED", "NOT DETECTED", "INVALID"],
    "value_codes": {},
    "translations": { "NOT DETECTED": ["NON DÉTECTÉ", "NICHT NACHGEWIESEN"] },
    "call_component": "call",
    "components": [
      { "code": "call", "result_type": "qualitative" },
      {
        "code": "LOG",
        "sub_identity": "&LOG",
        "result_type": "quantitative",
        "unit": "log copies/mL"
      },
      {
        "code": "HIV-1",
        "sub_identity": "HIV-1",
        "result_type": "qualitative",
        "values": ["POS", "NEG", "NO RESULT", "INVALID"]
      },
      {
        "code": "HIV-1-Ct",
        "sub_identity": "HIV-1&Ct",
        "result_type": "quantitative"
      }
    ]
  }
  ```

  `BridgeAnalyzerProfile` reads `default_test_mappings` entries today and
  ignores unknown fields.

- OGC-1129 (`openelis-work/designs/results-validation/analyzer-multicomponent-ingestion.md`):
  resolve the test first, then the component by a stable code (the
  component's `code` or its terminology code), never by display text; a part
  with no component mapping is surfaced, never dropped; a mapping with no
  component resolves to the primary (FR-A2).
- Rule 11: the number with its comparator on the primary, the call on a call
  component, Not detected only on the call. The log and the analyte records
  go on components of the same test (rule 10).
- Rule 18: a declared value lists the vendor's translations (Cepheid
  303-0251 §3), each bound to the same answer code. Defaults create a result
  row for every translation, so an instrument binds whatever language it
  runs; the editor shows them under their value.
- Rule 9: the review row shows the instrument's flag, assay name and version
  and operator as sent, labelled instrument-reported; nothing is shown when
  the bundle does not carry them.
- The review page lists one row per staged result
  (`frontend/src/components/analyserResults/AnalyserResults.jsx`) and has no
  notion of a component today. Agreed with the section 2 scope (5 Oct): a
  test's components show beneath its main result, not as separate rows.

### Build

```
- [x] T2b.1 Red: integration test, bundles shaped from every 303-0251 §2.1.1 outcome as step 6 emits them; the main record's number on the primary and its call on the call component, LOG (`&LOG`) on its component, analyte records (`HIV-1`, `HIV-1&Ct`) on theirs; "<40" stages <40 from the quantity; NOT DETECTED fills only the call
- [x] T2b.2 Red: integration test, a record with a sub-identity the mapping does not declare is held as an unknown test (rule 13); the number is never held as an unknown answer
- [x] T2b.3 Red: component test, the review row shows the instrument's flag, assay name and version and operator; a test's components (call, LOG, analytes) show beneath its main result
- [x] T2b.4 Changeset: mapping test and result rows keyed by (code, sub-identity); the test row gains a call target (component) for a record with both a call and a number; existing rows take the empty sub-identity, unchanged
- [x] T2b.5 Contract and import: read the sub-identity and the interpretation; map (code, sub-identity); the number from valueQuantity to the row's target, the call to its call target; staged number from valueQuantity, replacing the raw-text prefix
- [x] T2b.6 Editor and defaults: each (code, sub-identity) the profile declares is its own row; defaults resolve each to the component by its stable code, and create a result row for every declared translation; red first: a translated call (NON DÉTECTÉ) binds to the same answer as NOT DETECTED. The operator places a record on a component, and the main record's call on its call component, with a picker that pre-selects the one component whose code matches the profile's when the record's test changes (decided 6 Oct); a received record the profile does not declare is placed the same way (rule 13). Staged results keep their record's sub-identity, so a held record shows as its own row
- [x] T2b.7 Review row: instrument-reported fields; components grouped beneath their main result. Decided 6 Oct: on the analyzer results review page, decisions about the specimen (placement, redirect, specimen type) stay per accession, and Accept / Retest / Ignore are per test with its components beneath it and covered by its decision (a test's parts are one measurement; tests on one specimen succeed or fail independently, as MTB detected with rifampicin indeterminate, rule 12); Accept all stays
- [x] T2b.7b A number on a component is shown and saved with that component's decimal places, not the test's main result's. Found 7 Oct by the vendor-outcome E2E (T7.2): the review page and the saved result took the test's first result definition, so a 3.00 log viral load showed as 3 and was saved with no decimal places, and every Ct and EndPt lost its decimal on the review page. One rule now serves both (`AnalyzerResultsAcceptService.significantDigitsFor`)
- [x] T2b.7c A below- or above-range viral load keeps its number when its call is mapped. Found 7 Oct by T7.2: the Bridge sends Cepheid's `<40` as quantity 40 with comparator `<` and DETECTED as the call, and the raw value is the call alone; the import looked up an answer for that raw value on the number's row too, so with the baseline profile's DETECTED mapped, the viral load was staged as Detected instead of `<40`, beside the call. T2b.1's test fed the raw text with its trailing `^` and mapped no answer, so it never met the case. A record with a call now maps its raw value only on the call's row
- [x] T2b.8 (#4592, #4593) Green; format cold; commit; stack PR on step 2
```

### Verify

```bash
mvn -Dtest='AnalyzerNormalizedResult*' test
cd frontend && npx vitest run src/components/analyserResults src/components/analyzers && cd ..
```

### Done when

1. Every 303-0251 outcome lands each record, and the main record's number
   and call, in their own place, and no value is staged twice on one target.
   (T2b.1)
2. Nothing is held as an unknown answer because it is a number. (T2b.2)
3. A reviewer sees the instrument's flag, assay and operator on the row,
   and each test's components beneath its main result. (T2b.3)
4. Analyzers whose rows have one part keep their results and placement as
   before; on the review page their decisions move from one per accession to
   one per test. (existing import and editor tests; review tests)

### Background (optional)

- [OGC-1129 ingestion FRS](https://github.com/DIGI-UW/openelis-work/blob/main/designs/results-validation/analyzer-multicomponent-ingestion.md)
- Cepheid 303-0251 Rev A, HIV-1 VL XC LIS guidance, §2.1
