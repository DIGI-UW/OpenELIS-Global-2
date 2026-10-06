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
a.test.id = ?`, no ordering, no status filter. Caller
  `AnalyzerResultsAcceptServiceImpl.getExistingAnalysis` (993-997) takes
  `get(0)`. Retests are separate `Analysis` rows on the same tube with a
  higher `revision` (`LogbookResultsController.java:693-695`); the superseded
  one is linked via `parentAnalysis`.
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
- [ ] T1b.1 Red: integration test, result for ACC-1 lands on tube 1's current analysis
- [ ] T1b.2 Red: integration test, INVALID then rerun on ACC: second result lands on the current revision, not the superseded one
- [ ] T1b.3 Red: integration test, same test on two tubes of ACC: row is not pre-ticked, carries both analyses
- [ ] T1b.4 Red: integration tests, unordered test with one fitting tube / several / none: row not pre-ticked, proposal and reason present
- [ ] T1b.5 Red: integration test, unknown accession: row not pre-ticked, "new sample" proposal; accept still creates it (results-before-registration)
- [ ] T1b.6 Red: integration test, bundle stored per delivery, readable after accept
- [ ] T1b.7 Resolver: in import, resolve instrument ID -> tube (getSampleItemsByExternalID) -> else accession; store tube id and resolution kind on the staging row
- [ ] T1b.8 Analysis match: replace getExistingAnalysis with a query on sampleItem + test + current revision (no parentAnalysis child, not Canceled); return all matches, never get(0)
- [ ] T1b.9 Placement state: compute per row (RESOLVED, RETEST_CHOICE, MULTI_TUBE, UNORDERED_ONE_FITS, UNORDERED_MANY_FIT, UNORDERED_NONE_FIT, NEW_SAMPLE, UNRECOGNISED_ID, PATIENT_MISMATCH) with proposal and reason; pre-tick only RESOLVED
- [ ] T1b.10 Accept: honour the reviewer's chosen tube/analysis/specimen type; require justification when the choice differs from the proposal; write it as an INTERNAL note
- [ ] T1b.11 Bundle storage: add bundle_json (TEXT) to analyzer_delivery_receipt via new changeset; write at import; never delete; expose GET /rest/analyzer/deliveries/{id}/bundle
- [ ] T1b.12 Review page: per row show resolution, proposal, reason, accession's tubes (type, ordered tests), matching analyses (status, revision), instrument patient beside OE2 patient, "View bundle"; controls to choose tube/analysis, create tube with explicit type, redirect accession, hold; justification field when overriding
- [ ] T1b.13 i18n keys in en.json for every new state and control
- [ ] T1b.14 Red then green: E2E for each state in T1b.9, plus FILE plate with one mistyped sample name
- [ ] T1b.15 Format cold; commit; stack PR on step 0 (beside step 1)
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
4. After INVALID then rerun, the new result is on the current revision.
   (T1b.2)
5. A FILE plate with one mistyped sample name holds only that row. (E2E)
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
