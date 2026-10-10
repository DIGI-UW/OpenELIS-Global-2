# Step 2: Mappings owned by each analyzer

Part of [analyzer-baseline-roadmap.md](../analyzer-baseline-roadmap.md). Read that file's Rules and Repo working agreements first; they apply here.

Goal: each analyzer owns its mapping (defaults plus overrides, rows targeting
a test or a component), the shared type mapping is gone, every record type the
analyzer sends has a home, and existing analyzers are migrated to a fresh
baseline.

### Facts

- Shared model today (Liquibase 085, 086, 089, 095):
  `analyzer_profile_binding(id, profile_id, profile_revision, profile_fingerprint)`;
  `analyzer_site_binding(id, profile_binding_id, created_by, created_at)`;
  `analyzer_site_binding_revision(id, site_binding_id, revision_number,
binding_fingerprint, supersedes_revision_id, created_by, created_at)`;
  `analyzer_site_binding_test(site_binding_revision_id, source_row_key,
mapping_state, test_id)`;
  `analyzer_site_binding_result(site_binding_revision_id, source_row_key,
raw_value, mapping_state, test_result_id)`;
  `analyzer_site_binding_confirmation(id, site_binding_revision_id,
profile_id, profile_revision, binding_fingerprint, recognition_fingerprint,
confirmed_rows_json, excluded_rows_json, confirmed_by, confirmed_at)`.
  `analyzer.site_binding_revision_id` pins an analyzer to one revision.
  Entities under `src/main/java/org/openelisglobal/analyzer/valueholder/`;
  services `AnalyzerSiteBindingServiceImpl`, `AnalyzerProfileBindingServiceImpl`,
  `AnalyzerSiteBindingConfirmationServiceImpl`, `AnalyzerTypeMappingServiceImpl`.
- Target model: `analyzer_mapping(id, analyzer_id, profile_id,
profile_revision, profile_fingerprint, revision_number, fingerprint,
created_by, created_at)`; `analyzer_mapping_test(mapping_id, source_row_key,
mapping_state, origin DEFAULT|OVERRIDE, test_id, component_id NULL,
unresolved_reason)`; `analyzer_mapping_result(mapping_id, source_row_key,
raw_value, mapping_state, origin, test_result_id, unresolved_reason)`;
  `analyzer_mapping_confirmation` as today's confirmation but keyed on
  `mapping_id`. `analyzer.mapping_id` replaces `site_binding_revision_id`.
  Keep the append-only revision pattern per analyzer; keep audit via
  `AuditTrailService.saveNewHistory` as `AnalyzerSiteBindingServiceImpl:146`
  does today.
- Components: `test_result_component(id VARCHAR(36), test_id, code, label,
display_order, result_type, uom_id, significant_digits, is_primary, ...)`
  (Liquibase 041, 052). A component value is a `Result` whose `TestResult`
  has `componentId`. `AnalyzerResults.componentId` exists and accept binds
  on it (`findResultForComponent`, 981-990), but import never sets it
  (`toStagedResult`, 202-269). Mapping a profile row to a component means
  `analyzer_mapping_test.component_id` is set and import copies it to the
  staging row.
- Qualitative QC: `QCResult.qualitativeOutcome` (`QCQualitativeOutcome`:
  VALID, INVALID, PASS, FAIL) exists for `QCSource` RDT and MANUAL. Import
  skips non-numeric controls at
  `AnalyzerNormalizedResultImportServiceImpl.processControl` (298-304).
  `QCResultProcessingService.processQCResult(analyzerId, testId,
accessionNumber, lotNumber, ...)` takes a number; add a qualitative
  overload.
- Error detail: after step 6 the bundle carries `Observation.note` for C
  records. Notes: `NoteService.createSavableNote(analysis,
NoteType.INTERNAL, subject, text, ...)` as used at accept 806.
- Migration precedent: changeset
  `116-remove-pre-bridge-analyzer-storage.xml` exports to
  `configuration_import_run(id, source, status, started_at, finished_at,
summary)` with `source='ANALYZER_LEGACY'`, `status='EXPORTED'`, one JSONB
  document; documented in `docs/analyzers/pre-bridge-settings-export.md`
  (50 lines). Use `source='ANALYZER_MAPPING_BASELINE'`.
- Highest changeset at baseline: `119-stat-turnaround-config.xml`. Use 120
  onward.
- Editor: `frontend/src/components/analyzers/AnalyzerTypeMapping/AnalyzerTypeMappingEditor.jsx`;
  `excludeTest` (240-253) clears `testId` and every answer; `save` (337-363)
  shows no summary. Endpoints `PUT/POST /rest/analyzer-types/{id}/mapping[/confirm]`
  become `PUT/POST /rest/analyzer/analyzers/{id}/mapping[/confirm]`.
- Existing tests to replace: `AnalyzerSiteBindingServiceTest`,
  `AnalyzerSiteBindingPersistenceIntegrationTest`,
  `AnalyzerProfileBindingServiceTest`, `AnalyzerTypeMappingServiceTest`,
  `AnalyzerInstanceLocalStateServiceTest` (incl.
  `cannotReplaceTheProfileOfAConfiguredAnalyzer`),
  `AnalyzerTypeMappingEditor.test.jsx`, E2E
  `demo/harness/ogc-1054-m2-shared-mapping.spec.ts`.

### Build

```
- [ ] T2.1 Red: integration test, two analyzers on one profile; editing one leaves the other's rows identical
- [ ] T2.2 Red: integration test, Cepheid-shaped HIV VL bundle (main, LOG, HIV-1, Ct, EndPt, IQS-H, IQS-L) lands main on primary and the rest on components of the same analysis
- [ ] T2.3 Red: integration test, POS control cartridge yields a QCResult with qualitativeOutcome set
- [ ] T2.4 Red: integration test, ERROR bundle with a note yields a held row whose INTERNAL note carries the note text
- [ ] T2.5 Red: component test, exclude then un-exclude restores test and answers; Save shows a summary listing BOUND->EXCLUDED rows
- [ ] T2.6 Red: integration test on a fixture with two configured analyzers: after migration each keeps id, name, lab units, bridge_connection_id, activation records; mapping_id is null; status INACTIVE; configuration_import_run has one ANALYZER_MAPPING_BASELINE row holding both old mappings
- [ ] T2.7 Changeset 120: create analyzer_mapping, analyzer_mapping_test, analyzer_mapping_result, analyzer_mapping_confirmation; add analyzer.mapping_id
- [ ] T2.8 Changeset 121 (migration): export every analyzer_site_binding_revision + tests + results + confirmation to configuration_import_run; set analyzer.active=false, status='INACTIVE', site_binding_revision_id=null; drop analyzer_site_binding_* and analyzer_profile_binding and the analyzer column
- [ ] T2.9 Services: AnalyzerMappingService (resolveDefaults at setup via step 1; appendRevision; confirm) replacing the four site-binding services; AnalyzerInstanceLocalStateService.create resolves and stores the analyzer's own mapping
- [ ] T2.10 Import: set componentId from the mapping row; route qualitative controls to QC; attach Observation.note as a note on held run failures
- [ ] T2.11 Editor: re-point to the analyzer endpoints; keep prior selections on exclude; change summary before save; show origin per row
- [ ] T2.12 Analyzer Types page: remove Edit mappings; show read-only defaults preview from step 1's resolver against the current catalog
- [ ] T2.13 docs/analyzers/mapping-baseline-migration.md (under one page): what the changeset does, how to read the export, how to re-verify an analyzer
- [ ] T2.14 Delete the superseded tests; green; format cold; commit; stack PR on step 1
```

### Verify

```bash
mvn -Dtest='AnalyzerMapping*Test,AnalyzerNormalizedResultImportIntegrationTest,AnalyzerResultsAccept*IntegrationTest,AnalyzerMappingBaselineMigrationIntegrationTest' test
grep -rn "AnalyzerSiteBinding\|AnalyzerProfileBinding\|site_binding_revision" src/main/java src/main/resources/liquibase/3.5.x.x/1[2-9][0-9]-*.xml   # 0 hits
cd frontend && npm test -- AnalyzerTypeMappingEditor
cd frontend && npm run pw:test -- --project=harness-foundational
wc -l docs/analyzers/mapping-baseline-migration.md
gh pr checks <PR>
```

### Done when

1. No `AnalyzerSiteBinding*` or `AnalyzerProfileBinding*` class or table
   remains; each analyzer has its own mapping with `origin` per row.
   (`grep`)
2. T2.1 passes. (`mvn`)
3. T2.2 passes. (`mvn`)
4. T2.3 and T2.4 pass. (`mvn`)
5. T2.5 passes. (`npm test`)
6. T2.6 passes; the migration doc exists and is under one page. (`mvn`,
   `wc -l`)
7. The Analyzer Types page has no save or confirm action. (E2E, read)
8. All three CI checkpoints pass. (`gh pr checks`)

### Background (optional)

- [Model: how mappings are layered](https://claude.ai/artifact/87JjCR7fg87DCqFeS5TEfH#model), [P3](https://claude.ai/artifact/87JjCR7fg87DCqFeS5TEfH#p3), [M5](https://claude.ai/artifact/87JjCR7fg87DCqFeS5TEfH#m5), [OE2 today](https://claude.ai/artifact/87JjCR7fg87DCqFeS5TEfH#today)
- Decisions: [ownership](https://claude.ai/artifact/87JjCR7fg87DCqFeS5TEfH#d-ownership), [records](https://claude.ai/artifact/87JjCR7fg87DCqFeS5TEfH#d-records), [baseline](https://claude.ai/artifact/87JjCR7fg87DCqFeS5TEfH#d-baseline), [run failures](https://claude.ai/artifact/87JjCR7fg87DCqFeS5TEfH#d-run-failures)
