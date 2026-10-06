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

### Build

```
- [x] T4.1 Red: component tests, each listed server message renders its en.json string, never raw text
- [ ] T4.2 Red: E2E on a populated catalog (answerless COVID duplicate, legacy RIF answers): Verify lists each unresolved row with reason; resolve in place; Continue enabled only when remaining unresolved rows are acknowledged
- [ ] T4.3 Red: E2E, a changed instrument code is used for result translation and an outbound order
- [ ] T4.4 Verify step: render step-1 reasons per row; inline resolve controls (same components as the editor); acknowledge-unresolved checkbox per row; gate = confirmed AND all unresolved acknowledged; counts derive from the same rows
- [ ] T4.5 Instrument step: code column per profile row, editable, stored as override
- [ ] T4.5b Red then green: E2E, a result under a code the profile does not declare is held as an unknown test; the operator adds it to the analyzer's mapping from the held row (test, and answers when categorical), stored as OVERRIDE; the held result recovers (rule 13)
- [x] T4.6 Error mapping: one errorKeyFor(response) helper; every path uses it; add keys to en.json. The server names an operator-facing refusal with `messageKey` and `messageArgs` (`AnalyzerRequestException`); setup, Verify, the mapping editor, adoption and the lifecycle modal show its words or their own message. Derived 6 Oct: analyzer type authoring keeps showing the Bridge's profile validation text, which is the author's only detail, until step 6 gives the Bridge's profile contract keyed errors
- [ ] T4.7 Hand-offs: editor link carries analyzerId; return label from returnTo; roles on the mapping route match /analyzers; edit title; Activate action for SETUP; zero-test type explanation
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
4. T4.3 and T4.5b pass. (`pw:test`)
5. Hand-off and role fixes in T4.7 are present. (read, E2E)
6. All three CI checkpoints pass. (`gh pr checks`)

### Background (optional)

- [W1](https://claude.ai/artifact/87JjCR7fg87DCqFeS5TEfH#w1), [Setup journey](https://claude.ai/artifact/87JjCR7fg87DCqFeS5TEfH#journey)
- Decisions: [instrument codes](https://claude.ai/artifact/87JjCR7fg87DCqFeS5TEfH#d-codes), [admin location: deferred](https://claude.ai/artifact/87JjCR7fg87DCqFeS5TEfH#d-admin)
