# Step 6: Profile contract, templates, and the shipped set

Part of [analyzer-baseline-roadmap.md](../analyzer-baseline-roadmap.md). Read that file's Rules and Repo working agreements first; they apply here.

Goal: the Bridge enforces one profile contract, ships templates and a guide,
ships GeneXpert as the first baseline profile, and carries every parsed fact
into the bundle.

### Facts

- Repo: `DIGI-UW/openelis-analyzer-bridge`. Schema
  `contracts/analyzer/v1/analyzer-profile.schema.json`; validator
  `src/main/java/org/itech/ahb/profile/AnalyzerProfileValidator.java`
  (structural checks only, lines 77-207). Shipped profiles
  `src/main/resources/analyzer-profiles/`; loader requires
  `catalog.source == "SHIPPED"` and a valid `publishedAt`
  (`AnalyzerProfileCatalog.loadShipped`, 388-414). Revisions are immutable;
  OE2 rejects a changed fingerprint on the same revision, so baseline
  profiles are new revisions (GeneXpert: 8). Core ships three families:
  `genexpert-astm` (rev 7), `fluorocycler-xt` (rev 4, specimen hint
  `Plasma`, no result type) and `quantstudio` (rev 3); all three are brought
  to the contract here.
- Profile fields today: `profileMeta{id, version, displayName, manufacturer,
model}`, `catalog{revision, source, status, publishedAt, publishedBy,
revisionFingerprint, recognitionFingerprint}`, `default_test_mappings[]`
  with `test_code, loinc, unit, result_type, values[], aliases[],
test_name_hint, result_value_hints{}, specimen_type_hint`, protocol config
  (`astm_config` or `column_mapping`), `control_recognition`, connection
  fields.
- Reproduced 2026-10-05 against Bridge 3.2.6 with the profile's
  `FIELD_NON_BLANK R.3.5` selection, replaying Cepheid's own examples:
  - Quantified (303-0251 §2.1.1; 301-2002 Rev E §6.3.4.1.10): two results
    with the same code, both numeric copies/mL (`1009.64` and the LOG
    `3.00`), so OE2 cannot tell the log from the viral load.
  - `<40`: one result, `DETECTED`; the R.7 `<` and the R.6 limit are
    dropped, so "below 40" is lost.
  - Every outcome: the analyte records (HIV-1 call, IQS-H, IQS-L, their Ct,
    EndPt and Delta Ct) carry no assay name in R.3 component 5, so the
    selection drops them all.
- Contract changes: `catalog.revision` required; `result_type` required per
  test; result parts with field references per test (rule 17); value
  translations and number format (rule 18); categorical tests require `values[]` and `value_codes{value ->
[{system, code}]}` (every coding cited for the value, rule 1); `components[]` per test (`code, label, result_type,
unit, values, value_codes, translations, sub_identity`) and `call_component` (rule 17); `translations{value -> [text]}` beside `values` on a test or component (rule 18); `source` per test (document and section); `assay{name, version}`
  per test; remove `result_value_hints`, `specimen_type_hint`; keep
  `aliases`.
- Parsers: `src/main/java/org/itech/ahb/fhir/ASTMResultParser.java` reads
  O.3 (accession), R.3 (test ID, component 4 only), R.4 (value), R.5
  (units), R.12/R.13 (times), Q records. Add: P record (patient ID, name),
  O.16 (specimen descriptor; GeneXpert always sends `ORH`, Rev E §6.3.4.1.4,
  other instruments send a type),
  R.3 components 5, 6 and 8 (assay name, version, complementary name such as
  `LOG`), R.4 component 1 (the call) and 2 (the number), R.7 (every flag),
  R.11 (operator), R.14 (instrument identity), C records (Notes and Error).
  Numbers are read per the profile's number format (rule 18).
  `HL7ResultParser.java`: OBR filler/placer; remove the PID-3 accession
  fallback (132-138); read PID for patient context and SPM-4 for the
  specimen descriptor. The bundle carries the descriptor as sent in
  `Specimen.type.text`.
- Bundle: `src/main/java/org/itech/ahb/fhir/FhirBundleBuilder.java`;
  `AnalyzerResult` record (449-461) has no flag, assay, operator, note.
  Extend it and the builder: `Patient` resource with identifier and name
  plus extension `analyzer-patient-source=instrument`, referenced from
  `Observation.subject` (OE2 reads exactly this since step 1b,
  `AnalyzerNormalizedResultContract`; a Patient without the extension is
  ignored); `Quantity.comparator`
  from the flag, with the documented limit as the value when the instrument
  sends no number (rules 11 and 17; OE2 maps it in step 2b), and the
  raw-value extension left as the instrument sent it; the assay name as
  `Observation.method.text` and its version in the extension
  `https://openelis-global.org/fhir/StructureDefinition/analyzer-assay-version`
  on `Observation.method` (decided 6 Oct: the bundle's one Device is the
  connection, so it cannot carry a per-result assay); the instrument's other
  flags (R.7 `N`, `A`, `H`, `L`) as `Observation.interpretation`; `Observation.note`
  from C records; a value the profile declares a run failure (ERROR, NO
  RESULT) goes out with no value and `Observation.dataAbsentReason` (`error`),
  the raw text kept in the raw-value extension, which OE2 holds as a failed run
  (step 2); `Observation.performer.display` from operator; existing extension
  namespace `https://openelis-global.org/fhir/...` for anything without a
  slot.
- One Observation per record (rule 17): its code is the test code; its
  sub-identity, the analyte and complementary names in the vendor's HL7
  sub-ID notation (`HIV-1&Ct`, `&LOG`; ASTM R.3 components 7 and 8, HL7
  OBX-4), goes in the extension
  `http://hl7.org/fhir/StructureDefinition/observation-v2-subid`
  (`original-sub-identifier`), absent for the main result. The number goes
  in `valueQuantity`; when the record also carries a call, the call goes in
  `Observation.interpretation` (`DET`, `ND` from HL7 ObservationInterpretation
  where one applies, with the instrument's text); a record with no number
  carries its call as the value. OE2 maps (code, sub-identity) (step 2b).
- Outbox: `OutboxProperties.Retention.delivered = 30 days`, `dismissed = 90
days`; purge in `OutboxDispatcher.purgeIfDue` and
  `SqliteOutboxStore` (raw purged when unreferenced, line 139). Set
  `delivered` default to unlimited; keep the setting.
- Per-connection overrides: `AnalyzerConnectionCatalog` `values` map;
  add `codeOverrides{profileCode -> instrumentCode}`, applied in
  `AnalyzerRuntimeRegistry.codeToLoinc` materialisation and
  `getCodeForLoinc` for outbound orders; and `numberFormat`, which replaces
  the profile's number format for that connection (rule 18) and is offered
  as a connection field.
- Mock pin and OE2 pin are bumped in step 7, not here.
- Startup audit (6 Oct, read in source at Bridge `19e5cc7`, not reproduced).
  Each of these stops the Bridge from booting, so one bad file or one
  connection nobody uses takes result delivery down for every analyzer:

  | Startup path                                                                                                                                                                                               | Where                                                                                                                                        |
  | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------- |
  | A shipped profile fails to parse or validate, its fingerprint does not match, or it repeats an `id@revision`                                                                                               | `AnalyzerProfileCatalog.java:391-416, 476-483, 528-535`                                                                                      |
  | A persisted revision or draft fails to load                                                                                                                                                                | `AnalyzerProfileCatalog.java:419-470`                                                                                                        |
  | The profile catalog or draft directory cannot be read                                                                                                                                                      | `ProfileCatalogFileStore.java:40-41, 56-57`                                                                                                  |
  | A saved connection file cannot be read, or its pinned revision is missing or has another fingerprint                                                                                                       | `AnalyzerConnectionCatalog.java:408-419, 715-745`                                                                                            |
  | An active connection has no activated configuration                                                                                                                                                        | `AnalyzerConnectionCatalog.java:382-386`                                                                                                     |
  | An active connection fails to restore: invalid values or profile, an ASTM port that cannot bind, a FILE directory that cannot be watched, a disabled FILE, HL7 or serial runtime, an unsupported transport | `AnalyzerConnectionCatalog.java:388-389`; `BridgeAnalyzerConnectionRuntime.java:161, 228-294`; `ManagedAstmConnectionListeners.java:150-165` |
  | The FILE polling monitor fails to start                                                                                                                                                                    | `FileWatcher.java:226-250`                                                                                                                   |

  Already graceful, and the pattern to follow: indistinguishable active
  connections restore with a warning and their messages are held as
  `AMBIGUOUS_SOURCE` (`BridgeAnalyzerConnectionRuntime.java:103-120`); an
  occupied HL7 boot port is logged (`ManagedHl7ConnectionListeners.java:50-61`);
  an absent serial device waits for reconnect; a corrupt outbox database is
  recovered (`SqliteOutboxStore.java:72-83`); a render failure at delivery is
  dead-lettered with its reason (`NormalizedBundleRenderer.java:248-253`,
  `OutboxDispatcher.renderRecovered`); a failed retention purge is retried.
  Deliberately fail-closed, proposed to stay: security disabled, or the
  default password outside dev and test (`SecurityConfig.java:70-90`); it is
  a credential, not data, and an exposed Bridge with a known password is not
  a degraded mode.

- Profile identity (decided 6 Oct: "we need a new profile id for sure").
  Revisions 1 to 7 of `genexpert-astm` (and 1 to 4 of `fluorocycler-xt`, 1
  to 3 of `quantstudio`) are pre-baseline; rule 6 says none is an earlier
  version of a baseline profile. The baseline profiles ship under new
  profile IDs at revision 1, so the data says what rule 6 says, one ID never
  holds both a 1.0 and a 2.0 revision, and no pre-baseline analyzer is
  offered the baseline as an update. A revision number is an identity
  (`profileId`, `revision`, `fingerprint`) in OE2, the Bridge and history,
  so an existing ID's revision 1 is not reused. Proposed IDs, for review:
  `cepheid-genexpert-astm`, `hain-fluorocycler-xt`, `thermo-quantstudio`.
- GeneXpert codes found wrong on 6 Oct (CDC LIVD SARS-CoV-2, 2026-07-21,
  "LOINC Mapping" sheet, Cepheid rows; NLM LOINC): the Xpert Xpress
  CoV-2/Flu/RSV plus maps Flu A, Flu B and RSV to 85477-8, 85478-6 and
  85479-4 ("in Upper respiratory specimen by NAA with probe detection").
  Rev 8 declares 92142-9, 92141-1 and 92131-2, which LIVD gives to the older
  Xpert Xpress SARS-CoV-2/Flu/RSV (whose SARS-CoV-2 is 94502-2). SARS-CoV-2
  at 94500-6 is right for the plus panel and for Xpress CoV-2 plus.
- Internal controls (decided 6 Oct, agreeing to codes rather than text
  matching). SPC (CoV-2/Flu/RSV plus) and IQS-H, IQS-L (HIV-1 VL XC) report
  PASS, FAIL, NA (SPC ignored because a target amplified) and NO RESULT (run
  aborted): 302-7279 §6 and 303-0251 §2.1.1 examples; a failed control makes
  the main result INVALID. LOINC 90101-7 "Internal control result" with
  answer list LL3837-3: Pass LA10392-1, Fail LA25389-0 (NLM LOINC,
  tx.fhir.org); SNOMED 385432009 Not applicable. The OGC-1054 import FRS
  (`openelis-work/designs/system/analyzer-import-redesign-v2.md` FR-F1, and
  `analyzer-profile-mapping.md` MC-4) models an instrument's internal
  control as a result component of the patient's analysis, normally not on
  the patient report, with the analyzer's verdict authoritative.

### Build

```
- [ ] T6.0 303-0251 §2.1.1 and 302-7279 §6 done (`ASTMResultPartsParserTest`, `ASTMResultPartsBundleTest`, `GeneXpertBaselineProfileTest`); 301-2002 Rev E §6.3.4.1.9 to 6.3.4.1.11 still open as parser-level tests (their assays are not in the profile, so they check structure: multi-result, single-result, quantitative with LOG and C notes). Red: replay 303-0251 §2.1.1 (every outcome) and 301-2002 Rev E §6.3.4.1.9 to 6.3.4.1.11; the LOG main result arrives as its own Observation with sub-identity `&LOG`, never under the viral load's; a quantified main result carries 1009.64 in valueQuantity and DETECTED as its interpretation; `<40` arrives as valueQuantity 40 with comparator `<` and DETECTED as interpretation; NOT DETECTED arrives as the value with no quantity; analyte records arrive with their sub-identity (`HIV-1`, `HIV-1&Ct`)
- [x] T6.1 (`BaselineProfileContractTest`) Red: validator tests, each contract rule rejects its violation
- [ ] T6.2 303-0251 and 302-7279 done; 301-2002 with T6.0; MTB/RIF Ultra stays unverified (step 5). Red: parser tests from the Cepheid example messages (301-2002 Rev E, 303-0251, 302-7279, MTB/RIF Ultra once verified): every record type, flag, component and C record parsed
- [x] T6.3 (`ASTMResultPartsBundleTest`, validated against `normalized-fhir-bundle.schema.json`; OE2's `AnalyzerNormalizedResultContract` read the bundles as intended) Red: bundle round-trip test, every parsed fact present in the bundle in its slot
- [x] T6.4 Red: outbox test, delivered entries survive the purge by default
- [x] T6.5 (found 6 Oct: the connection catalog refused any value a profile did not declare as a field, so a runtime-only test passed while the API rejected `codeOverrides`; the test now goes through `AnalyzerConnectionCatalog.create` and `update`, and the reading is built for every protocol) Red: through the connection API, a connection codeOverride changes inbound translation and outbound order code; a connection numberFormat of `,` reads `40,00` as 40
- [x] T6.6 Schema and validator per Facts (`schemaVersion` 2.0; 1.0 revisions keep their hints because a published revision never changes)
- [x] T6.7 (`ProfileTemplatesTest`: each template is a valid draft as it stands) docs/profile-authoring.md and templates/{astm,hl7,file}.json
- [x] T6.8 (MTB and RIF added as text results on the codes 302-2261 cites; profiles authored or duplicated in the Bridge are written to 2.0) genexpert-astm rev 8: the three assays in scope (step 5) from docs/profiles/genexpert-astm.md; no hints; components; value codes; translations; sources
- [ ] T6.9 ASTM parsers and bundle done; HL7 PID fallback removed. Open: HL7 result parts (OBX-4 sub-identity, OBX-5 components, OBX-8, NTE), which land with the first HL7 baseline profile
- [x] T6.10 Outbox retention default; codeOverrides and numberFormat
- [ ] T6.11 Green and PR done (Bridge #75, draft, with FluoroCycler XT rev 5 and QuantStudio rev 4). Open: the release tag is a maintainer step after review (Claude does not cut releases); then one PR per Madagascar profile, each with its step-5 note
- [ ] T6.12 Red (for review before building): Bridge context tests, the Bridge boots and serves every other profile and connection when there is one invalid shipped profile, one invalid persisted revision and draft, one saved connection pinned to a missing revision and one to a changed fingerprint, one unreadable connection file, and one active connection whose ASTM port is taken and one whose FILE directory is gone; each set-aside item is listed with its reason in health and, for connections, in the connection view
- [ ] T6.13 Profile catalog loads each file on its own: a file that fails is skipped and recorded as a catalog issue (source, path, reason); a repeated `id@revision` keeps the first and records the second; an unreadable persisted or draft directory leaves the shipped profiles loaded and records an issue. The profile API lists the issues
- [ ] T6.14 Connection catalog: an unreadable connection file is skipped and recorded; a connection whose pin does not resolve loads, never runs, and its view carries the readiness blocker `profile-unavailable` (messageKey `analyzer.connection.readiness.profileUnavailable`); an update that moves it to a resolvable profile is accepted, which is how OE2 re-verifies it
- [ ] T6.15 Restore: each active connection restores on its own; a failure (no activated configuration, invalid values, port, FILE directory, disabled runtime, unsupported transport) leaves it `actualRuntimeState` FAILED with the reason in its view, and the rest restore; a FILE monitor that cannot start marks FILE connections FAILED instead of stopping the Bridge
- [ ] T6.16 Health: `/actuator/health` carries a `catalog` component listing set-aside profiles, drafts and connections with reasons; overall status stays UP while any analyzer can receive (status rules for review)
- [ ] T6.17 OE2 consumer: `en.json` label for `analyzer.connection.readiness.profileUnavailable`; the analyzer page shows the blocker and offers setup to re-verify; a message arriving for a connection that is not running stays held with its reason (existing unknown-source handling, asserted)
- [ ] T6.18 Baseline profiles under new IDs at revision 1 (IDs per Facts, for review): rev 8 content becomes the GeneXpert baseline with Flu A 85477-8, Flu B 85478-6, RSV 85479-4, and the SPC, IQS-H and IQS-L records declaring PASS LA10392-1, FAIL LA25389-0, NA SNOMED 385432009, and NO RESULT as their run failure; FluoroCycler XT rev 5 and QuantStudio rev 4 become revision 1 of theirs. Docs, templates, mock fixtures and contract test, and OE2 references follow
- [ ] T6.19 Pre-baseline revisions of the core IDs (for review: delete them in this release, which T6.13 to T6.15 make safe because a connection pinned to one shows `profile-unavailable` and is re-verified, as rule 6 and changeset 124 already require; or keep them INACTIVE and delete later). Distro folders are unaffected
```

### Verify

```bash
mvn test   # in the Bridge repo (no Maven wrapper; README uses mvn)
grep -ln "result_value_hints\|specimen_type_hint" src/main/resources/analyzer-profiles/*.json   # only pre-baseline revisions
grep -n "PID.3" src/main/java/org/itech/ahb/fhir/HL7ResultParser.java   # only patient-context read, no accession fallback
```

### Done when

1. T6.1 passes for every rule. (`mvn test`)
2. A profile written from the template validates with no edits beyond the
   instrument's facts. (validator test on the template)
3. T6.2 passes for every documented example. (`mvn test`)
4. T6.3 passes. (`mvn test`)
5. No PID-3 accession fallback remains. (`grep`)
6. T6.4 passes; retention is documented in the Bridge README. (`mvn test`,
   read)
7. T6.5 passes. (`mvn test`)
8. Each later profile lands as its own PR with its evidence note and the
   same tests. (PR review)
9. T6.12 passes: no profile, draft, connection or listener failure stops
   the Bridge; each is listed with its reason. (`mvn test`)
10. The shipped baseline profiles are revision 1 of their new IDs, and no
    shipped file declares the pre-baseline Flu A/B/RSV codes. (`grep`,
    `mvn test`)

### Background (optional)

- [P1](https://claude.ai/artifact/87JjCR7fg87DCqFeS5TEfH#p1), [P2](https://claude.ai/artifact/87JjCR7fg87DCqFeS5TEfH#p2), [P3](https://claude.ai/artifact/87JjCR7fg87DCqFeS5TEfH#p3), [P4](https://claude.ai/artifact/87JjCR7fg87DCqFeS5TEfH#p4), [B1](https://claude.ai/artifact/87JjCR7fg87DCqFeS5TEfH#b1), [S1 protocol notes](https://claude.ai/artifact/87JjCR7fg87DCqFeS5TEfH#s1)
- Decisions: [contract](https://claude.ai/artifact/87JjCR7fg87DCqFeS5TEfH#d-contract), [bundle](https://claude.ai/artifact/87JjCR7fg87DCqFeS5TEfH#d-bundle), [MTB/RIF](https://claude.ai/artifact/87JjCR7fg87DCqFeS5TEfH#d-mtb), [other profiles](https://claude.ai/artifact/87JjCR7fg87DCqFeS5TEfH#d-other-profiles)
