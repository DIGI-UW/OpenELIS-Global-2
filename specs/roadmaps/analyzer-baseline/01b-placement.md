# Step 1b: Placement of analyzer results

Part of [analyzer-baseline-roadmap.md](../analyzer-baseline-roadmap.md). Read that file's Rules and Repo working agreements first; they apply here.

Goal: an analyzer result lands on the exact tube and current analysis when
that is unambiguous, and otherwise shows the reviewer what it will do and
lets them decide on the page. The bundle is kept and viewable.

### Facts

- Import copies the instrument ID into the accession:
  `AnalyzerNormalizedResultImportServiceImpl.toStagedResult`, line 209
  (`row.setAccessionNumber(result.accessionNumber())`). The ID arrives
  verbatim: Bridge `ASTMResultParser.java:166-169` trims only.
- Tube IDs are `<accession>-<index>` (`SampleAddService.java:210`,
  `SpecimenTransformServiceImpl.java:126`) and printed on specimen labels
  (`SpecimenLabel.java:367-371`). Lookup exists:
  `SampleItemService.getSampleItemsByExternalID(String)`.
- Analysis lookup today: `AnalysisDAOImpl.getAnalysisByAccessionAndTestId`
  (lines 1764-1784), HQL `a.sampleItem.sample.accessionNumber = ? and
a.test.id = ?`, no ordering, no status filter. Callers take the first
  match: `AnalyzerResultsAcceptServiceImpl.getExistingAnalysis` (993-998,
  `get(0)`) and a loop in `createGroupForSampleAndDemographicsEntered`
  (762-767, `break`).
- A retest is the same `Analysis` row, not a second one: re-entering a
  result sets `revision` + 1 (`ResultUtil.java:577-579`,
  `LogbookResultsController.java:693-695`), and a non-zero revision marks the
  result modified in validation (`ResultUtil.java:695`) and corrected in
  reports (`ResultsReportProvider.java:282`). `parentAnalysis` links reflex
  and calculated analyses only (`ReflexAction.java:138`,
  `TestCalculatedUtil.java:749`). The review page's Retest marks the
  analysis `TechnicalRejected` with result `XXXX`.
- The analyzer path ignores analysis status today: `populateAnalysis`
  (1064-1078) resets the status to `TechnicalAcceptance` and the revision to
  `"0"`, and `getResult` (1000-1031) overwrites the existing `Result`, so a
  rerun silently replaces an accepted or finalized result. Statuses
  (`StatusService.AnalysisStatus`): a result is expected on `NotStarted`,
  `TechnicalRejected`, `BiologistRejected`; `Canceled`, `SampleRejected`,
  `RejectedByReferenceLab` never take one; any other status already holds
  one.
- Accept branches by entry state in `createRecordsForNewResult` (579-615):
  no entry (`createGroupForNoSampleEntryDone`, creates sample, patient is the
  unknown-patient placeholder at 900-903), demographics entered, sample
  entered (`createGroupForSampleAndDemographicsEntered`, 741+). Unordered
  tests go on a tube chosen by `sampleItemForNewAnalyses` (835-860): a tube
  whose `TypeOfSample` is in the test's `SAMPLETYPE_TEST` links, else a new
  tube with the first allowed type. Ambiguity hold already exists:
  `IMPORT_ISSUE_AWAITING_SPECIMEN` set at 316, chooser rendered in
  `AnalyserResults.jsx:422-439`.
- Review page: `frontend/src/components/analyserResults/AnalyserResults.jsx`;
  columns Sample Info (accession only, 170-175), Test Name, Result, Test
  Date, Save, Retest, Ignore, Notes. Rows pre-ticked by default.
- Justification precedent: unconditional acceptance requires a non-blank
  note, enforced server-side (`ResultUtil.java:321-331`, OGC-745).
- Staging row (`AnalyzerResults`) keeps `source_payload` (one Observation's
  JSON, set at import line 285) and is deleted on accept (147-160). Delivery
  receipt (`AnalyzerDeliveryReceipt`: id, connectionId, messageId,
  analyzerId, profileId, profileRevision, resultsStaged, resultsHeld) has no
  payload column. The Bridge serves the bundle at
  `GET /api/outbox/{id}/payload` keyed by the same message ID, but OE2 must
  store its own copy (rule 9).
- Patient identity: after step 6 the bundle carries an instrument-reported
  Patient; until then the comparison is skipped and no patient-mismatch
  state is shown. Build the state now; it activates when the data arrives.
- Protocols: ASTM (O record), HL7 (OBR filler then placer), FILE (column
  mapped to `sampleId`) all deliver one `Specimen.identifier` string; no
  per-profile logic is needed.
- Blocker and i18n keys: `analyzer.activation.blocker.*` in
  `AnalyzerActivationServiceImpl.java:27-33`; strings in
  `frontend/src/languages/en.json`.

### Build

```
- [x] T1b.1 Red: integration test, result for ACC-1 lands on tube 1's current analysis
- [x] T1b.2 Red: test, a result for an analysis that already holds an accepted or final result is RETEST_CHOICE, not pre-ticked; accepting it replaces the result and bumps the revision; an analysis awaiting a rerun (TechnicalRejected, BiologistRejected) resolves
- [x] T1b.3 Red: integration test, same test on two tubes of ACC: row is not pre-ticked, carries both analyses
- [x] T1b.4 Red: integration tests, unordered test with one fitting tube / several / none: row not pre-ticked, proposal and reason present
- [x] T1b.5 Red: integration test, unknown accession: row not pre-ticked, "new sample" proposal; accept still creates it (results-before-registration)
- [x] T1b.6 Red: integration test, bundle stored per delivery, readable after accept
- [x] T1b.7 Resolver: in import, resolve instrument ID -> tube (getSampleItemsByExternalID) -> else accession; store tube id and resolution kind on the staging row
- [x] T1b.8 Analysis match: replace getExistingAnalysis with a query on sampleItem + test + current revision (no parentAnalysis child, not Canceled); return all matches, never get(0)
- [x] T1b.9 Placement state per row: RESOLVED, RETEST_CHOICE, MULTI_TUBE, UNORDERED_ONE_FITS, UNORDERED_MANY_FIT, UNORDERED_NONE_FIT, NEW_SAMPLE, each with proposal, reason, tubes and analyses. The instrument patient is a separate check beside the state (NOT_REPORTED, MATCH, MISMATCH, NO_ORDER_PATIENT), not a state. Pre-tick a grouping only when every result it saves is RESOLVED and its patient is not a MISMATCH or unverifiable
- [x] T1b.10 Accept: choosing an analysis resolves a several-way match; placing a grouping on another existing order needs that order and a reason, recorded as an INTERNAL note on each result; a patient mismatch needs a note; anything else stays staged as awaiting_placement. An explicit sample type for an unordered test is the existing chooser; holding a row is leaving it unticked
- [x] T1b.11 Bundle storage: add bundle_json (TEXT) to analyzer_delivery_receipt via new changeset; write at import; never delete; expose GET /rest/analyzer/deliveries/{id}/bundle
- [x] T1b.12 Review page: per row the state, reason, tubes, matching analyses, the instrument patient beside the order's patient, "View bundle", a chooser for several matches, and "Place on another order" with a required reason
- [x] T1b.13 i18n keys in en.json for every new state and control
- [x] T1b.14 (moved 7 Oct to step 10 F5, the placement user stories) Moved to T7.2: E2E for each placement state, and a FILE plate with one mistyped sample name. They need the harness to bind answers, which arrives with steps 6 and 7
- [x] T1b.15 (#4583) Format cold; commit; stack PR on step 1
```

### Verify

```bash
mvn -Dtest='AnalyzerResultsAccept*IntegrationTest,AnalyzerNormalizedResultImportIntegrationTest' test
grep -n "get(0)" src/main/java/org/openelisglobal/analyzerresults/service/AnalyzerResultsAcceptServiceImpl.java   # none on analysis lists
cd frontend && npm run pw:test -- --project=harness-foundational
gh pr checks <PR>
```

### Done when

1. T1b.1 and the accession-with-one-tube case: row pre-ticked, one click
   saves. (E2E)
2. Each non-RESOLVED state arrives not pre-ticked with proposal, reason,
   tubes and analyses on the row. (E2E per state)
3. Each state is resolvable on the page; overriding the proposal requires a
   justification recorded in the audit trail. (E2E, integration)
4. A rerun on an analysis that holds a result is a visible choice, never a
   silent overwrite, and an accepted replacement bumps the revision. (T1b.2)
5. A FILE plate with one mistyped sample name holds only that row. (T7.2;
   backend: `AnalyzerResultsPlacementIntegrationTest`)
6. Instrument-reported patient, when present, is shown beside OE2's patient;
   a mismatch is its own state; no code path creates or updates a patient
   from instrument data. (`grep -rn "patientService.insert\|new Patient()"`
   in analyzer packages returns only the existing placeholder path)
7. Every delivery's bundle is stored, readable from the row before and after
   accept, with no purge path. (T1b.6)
8. Results before registration still work end to end. (T1b.5, E2E)
9. All three CI checkpoints pass. (`gh pr checks`)

### Background (optional)

- [S1](https://claude.ai/artifact/87JjCR7fg87DCqFeS5TEfH#s1), [B1](https://claude.ai/artifact/87JjCR7fg87DCqFeS5TEfH#b1), [OE2 today](https://claude.ai/artifact/87JjCR7fg87DCqFeS5TEfH#today)
- [Decision: placement](https://claude.ai/artifact/87JjCR7fg87DCqFeS5TEfH#d-placement), [Decision: bundle boundary](https://claude.ai/artifact/87JjCR7fg87DCqFeS5TEfH#d-bundle)
