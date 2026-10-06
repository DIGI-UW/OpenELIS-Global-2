# Step 3: Adopt a newer profile revision

Part of [analyzer-baseline-roadmap.md](../analyzer-baseline-roadmap.md). Read that file's Rules and Repo working agreements first; they apply here.

Goal: an analyzer can move from revision N to N+1 of its profile through one
review screen, keeping its identity, with every row's fate visible.

### Facts

- Refusal today: `AnalyzerInstanceLocalStateServiceImpl.update` (103-110)
  throws "A configured analyzer cannot be moved to another profile revision";
  pinned by `AnalyzerInstanceLocalStateServiceTest.cannotReplaceTheProfileOfAConfiguredAnalyzer`
  (line 263). "Update available" is one boolean in
  `AnalyzerTypeCatalogServiceImpl.affectedAnalyzer` (106-115), rendered by
  `AffectedAnalyzerList.jsx:26-30` with no action.
- Bridge re-pin: OE2 calls `BridgeAnalyzerConnectionClient.updateConnection(
connectionId, request)` (`PUT /api/connections/{id}`) with
  `requestId`, `connectionId`, `expectedConfigRevision`, `displayName`,
  `profileRef{profileId, revision}`, `values`. The Bridge accepts a new
  `profileRef` and carries existing values forward
  (`AnalyzerConnectionCatalog.java:147-170`). An active connection keeps its
  `activeRuntimeConfiguration` until re-activation (159-165). No Bridge change.
- Profile lookup: `BridgeProfileCatalogService.getProfile(profileId, revision)`;
  OE2 rejects a revision whose fingerprint changed
  (`AnalyzerProfileBindingServiceImpl:52-57` today; keep that check on
  `analyzer_mapping.profile_fingerprint`).
- Row comparison inputs per (code, sub-identity) (rule 17): declared values with
  their translations, unit, LOINC, answer codes, where the part sits (from
  the profile); current row's `origin`, target, state (from steps 2 and
  2b).
- Held results: `AnalyzerResults` rows with `import_issue_reason` set and
  `source_profile_revision` = N; recovery path
  `AnalyzerNormalizedResultImportService.recoverHeldMappingResults(analyzerId, actor)`.
- Activation: `AnalyzerActivationService.activate/deactivate`; blockers
  `analyzer.activation.blocker.{profile,name,labUnit,mappings,recognition,connection,bridgeAcknowledgement}`.

### Build

```
- [ ] T3.1 Red: integration test, analyzer on N adopts N+1; id, activation records, lab units, bridge_connection_id unchanged; Bridge connection profileRef.revision == N+1
- [x] T3.2 Red: unit tests for bucketing (`AnalyzerMappingAdoption.plan`; a dropped code is RETIRED, a record no revision declares carries over UNCHANGED; derived from rule 5: a DEFAULT row compares its decision with the new default, so a catalog change surfaces as CHANGED): identical row -> UNCHANGED (both origins); changed LOINC on DEFAULT -> CHANGED with new default; changed value set on OVERRIDE -> CHANGED showing both; new code -> NEEDS_MAPPING; renamed code -> old row retired, new NEEDS_MAPPING; override == new default -> UNCHANGED, origin stays OVERRIDE, marked as now also the default
- [ ] T3.3 Red: integration tests, override on inactive test -> BLOCKED; removed code with held results -> BLOCKED; each names the row
- [ ] T3.4 Red: integration test, traffic during adoption lands on N until confirm + re-activate
- [ ] T3.5 Red: integration test, held results on N recover via recoverHeldMappingResults after adoption
- [ ] T3.6 Service: AnalyzerMappingService.prepareAdoption(analyzerId, revision) -> buckets; adopt(analyzerId, revision, decisions, actor) -> new mapping revision, Bridge re-pin, confirm record
- [ ] T3.7 Endpoints: GET/POST /rest/analyzer/analyzers/{id}/adoption?revision=
- [ ] T3.8 Catalog view: split updateAvailable into newerProfileRevision and newerMappingRevision; UI tag links to Adopt or Verify respectively
- [ ] T3.9 Adoption screen: the step-2 editor in adoption mode with bucket grouping and side-by-side changed rows
- [ ] T3.10 Replace cannotReplaceTheProfileOfAConfiguredAnalyzer with tests for the new rule
- [ ] T3.11 Red then green: E2E numeric profile revision N -> N+1 (one LOINC fix, one new code); E2E an analyzer migrated by step 2 offers no Adopt action and is set up on its baseline profile (rule 6)
- [ ] T3.12 Format cold; commit; stack PR on step 2
```

### Verify

```bash
mvn -Dtest='AnalyzerMappingAdoption*Test,AnalyzerInstanceLocalStateServiceTest' test
grep -n "cannot be moved to another profile revision" src/main/java   # 0
cd frontend && npm run pw:test -- --project=harness-foundational
gh pr checks <PR>
```

### Done when

1. T3.1 passes. (`mvn`)
2. T3.2 passes for every bucket case. (`mvn`)
3. T3.3 passes; nothing else blocks. (`mvn`)
4. T3.4 passes. (`mvn`)
5. T3.5 passes. (`mvn`)
6. "Update available" is two states with two actions; the old refusal test is
   gone. (`grep`, read)
7. Both E2E scenarios in T3.11 pass. (`pw:test`)
8. All three CI checkpoints pass. (`gh pr checks`)

### Background (optional)

- [M4](https://claude.ai/artifact/87JjCR7fg87DCqFeS5TEfH#m4), [Adopting a profile: standards and edge cases](https://claude.ai/artifact/87JjCR7fg87DCqFeS5TEfH#upgrade)
- Decisions: [adoption](https://claude.ai/artifact/87JjCR7fg87DCqFeS5TEfH#d-adoption), [adopting while active](https://claude.ai/artifact/87JjCR7fg87DCqFeS5TEfH#d-adopt-active), [baseline](https://claude.ai/artifact/87JjCR7fg87DCqFeS5TEfH#d-baseline)
