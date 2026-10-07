# Step 4: Remediation inside setup and verification

Part of [analyzer-baseline-roadmap.md](../analyzer-baseline-roadmap.md). Read that file's Rules and Repo working agreements first; they apply here.

Goal: the setup wizard shows every unresolved row with its reason, lets the
operator fix it in place, and never shows raw server text.

### Facts

- Wizard: `frontend/src/components/analyzers/AnalyzerSetup/AnalyzerSetup.jsx`
  (Instrument, Verify, Connect), `AnalyzerConnectionSetup.jsx`,
  `AnalyzerLifecycleModal/AnalyzerLifecycleModal.tsx`. Routes in
  `frontend/src/App.jsx:1405-1439`: `/analyzers` and `/analyzers/types`
  allow `ANALYSER_IMPORT` or `GLOBAL_ADMIN`; `/analyzers/types/:profileId/mapping`
  allows `ANALYSER_IMPORT` only.
- Gate: `AnalyzerSetup.jsx:241-256` computes `testsReady`/`resultsReady`
  but `complete` depends only on `confirmation.state === "CURRENT"`.
  "Verification needs attention" (643-652) has no cause. Save errors collapse
  to `analyzer.setup.error.save` (303-312); apply errors to
  `analyzer.setup.verify.error.apply` (368-378). Editor shows
  `response.error || response.message` raw (`AnalyzerTypeMappingEditor.jsx:49-50`);
  lifecycle modal shows `response.failure || error || message` raw
  (`AnalyzerLifecycleModal.tsx:78-83, 98`).
- Hand-off: Verify's editor link (347-354) omits `analyzerId`; the editor's
  return button label is `analyzerType.mappingEditor.return` ("Analyzer
  Types"). Edit setup is titled `analyzer.setup.title.new`. A SETUP analyzer
  has only Deactivate in the list menu (`AnalyzersList.tsx:80-90`).
- Codes: the profile's `test_code` per row; the analyzer's override stored on
  `analyzer_mapping_test.source_row_key` override column (add
  `instrument_code` on the mapping row) and sent to the Bridge as a
  per-connection code override in `values` (Bridge support lands in step 6).
- Server messages to map: "A configured analyzer cannot be moved" (gone after
  step 3), "Analyzer Type mappings changed after Verify was loaded", "Confirm
  the current Analyzer Type mappings before applying them", "Bound test rows
  must reference a current catalog Test", "Bound result rows must reference a
  current Result Option", blocker codes `analyzer.activation.blocker.*`,
  Bridge `connectionErrorKey`.

### Decided 6 Oct

Setup mirrors the instrument's own Host Test Code table ("Yes, mirror the instrument"). A
generic profile declares every assay the vendor documents; a lab enables the
few its instrument runs, as it does on the GeneXpert itself, and Verify maps
only those. Researched: GeneXpert Dx Operator Manual 301-0045 Rev L §2.11.5-2.11.6
(per assay "Enable" and "Host Test Code"), Cepheid 303-3003 (assays come from
imported definition files), HL7 LIVD (labs map the tests they perform from the
vendor's full catalog).

### Build

```
- [x] T4.1 Red: component tests, each listed server message renders its en.json string, never raw text
- [x] T4.2 E2E on a catalog the spec seeds through the CSV import (one test with its own LOINC, two sharing one, a code no test carries), so it holds after step 7 changes the harness dictionary: the Assays step pre-ticks the assays the catalog can bind; Verify lists each enabled assay that is unresolved, with its reason, and never an assay that is off; resolve in place; Continue is enabled only when every enabled assay is mapped and the mapping is confirmed
- [ ] T4.3 Moved to T7.1b: E2E, a changed instrument code is used for result translation and an outbound order. It needs the Bridge's `codeOverrides` from step 6
- [x] T4.4 Verify step: covers the enabled assays only; render step-1 reasons per row; inline resolve controls (same components as the editor); gate = confirmed AND every enabled assay mapped; counts derive from the same rows. No per-row acknowledgement: an assay the lab does not run is not enabled
- [x] T4.5 Assays step, between Instrument and Verify, mirroring the GeneXpert Host Test Code table (Operator Manual 301-0045 Rev L §2.11.5-2.11.6: per assay, Enable and Host Test Code): one row per profile assay with Enable and its instrument code (default the profile's `test_code`, editable). Pre-ticked when the catalog binds the assay or has candidates for it (BOUND, AMBIGUOUS, INCOMPATIBLE); unticked on NO_MATCH. Stored per analyzer assay on the mapping (`enabled`, `instrument_code`); a change is a new mapping revision. Not enabled is not EXCLUDED: a result for an assay that is not enabled is held as `assay_not_enabled`, never dropped. Sending the code to the Bridge as a per-connection override in `values` waits for step 6, which adds Bridge support
- [x] T4.5b E2E, a result under a code the profile does not declare is held as an unknown test, and a result for an assay that is not enabled is held as not enabled; the operator enables or adds it from the held row (test, and answers when categorical), stored as OVERRIDE; the held result recovers (rule 13). A code no profile declares is off until the operator maps it, and mapping a row turns it on
- [x] T4.6 Error mapping: one errorKeyFor(response) helper; every path uses it; add keys to en.json. The server names an operator-facing refusal with `messageKey` and `messageArgs` (`AnalyzerRequestException`); setup, Verify, the mapping editor, adoption and the lifecycle modal show its words or their own message. Derived 6 Oct: analyzer type authoring keeps showing the Bridge's profile validation text, which is the author's only detail, until step 6 gives the Bridge's profile contract keyed errors
- [x] T4.7 Hand-offs: Verify embeds the editor for the analyzer, so the separate "Review mappings" link and its missing `analyzerId` are gone; the editor's return button reads "Back" and returns to `returnTo`; the mapping routes allow `ANALYSER_IMPORT` or `GLOBAL_ADMIN` like `/analyzers`; the setup heading names the analyzer once it exists ("Set up GX bench 1") and says "a new analyzer" only before; a SETUP analyzer offers Activate (the lifecycle modal on the activation endpoint, with its blockers listed) instead of Deactivate; the mapping editor says so when a type declares no tests
- [ ] T4.8 Green; format cold; commit; stack PR on step 2
```

### Verify

```bash
cd frontend && npm test -- AnalyzerSetup AnalyzerConnectionSetup AnalyzerLifecycleModal AnalyzerTypeMappingEditor
grep -rn "response\.error \|\| response\.message\|response\.failure \|\|" frontend/src/components/analyzers   # 0
cd frontend && npm run pw:test -- --project=harness-foundational
gh pr checks <PR>
```

### Done when

1. T4.2 passes. (`pw:test`)
2. No state shows "n of m ready" beside an enabled Continue with
   unacknowledged unresolved rows. (T4.2, read of the gate expression)
3. T4.1 passes; the raw-text fall-throughs are gone. (`npm test`, `grep`)
4. T4.5b passes. (`pw:test`) T4.3 is checked in step 7.
5. Hand-off and role fixes in T4.7 are present. (read, E2E)
6. All three CI checkpoints pass. (`gh pr checks`)

### Background (optional)

- [W1](https://claude.ai/artifact/87JjCR7fg87DCqFeS5TEfH#w1), [Setup journey](https://claude.ai/artifact/87JjCR7fg87DCqFeS5TEfH#journey)
- Decisions: [instrument codes](https://claude.ai/artifact/87JjCR7fg87DCqFeS5TEfH#d-codes), [admin location: deferred](https://claude.ai/artifact/87JjCR7fg87DCqFeS5TEfH#d-admin)
