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
  raw-value extension left as the instrument sent it; `Device.version` from assay version; `Observation.note`
  from C records; a value the profile declares a run failure (ERROR, NO
  RESULT) goes out with no value and `Observation.dataAbsentReason` (`error`),
  the raw text kept in the raw-value extension, which OE2 holds as a failed run
  (step 2); `Observation.performer` from operator; existing extension
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

### Build

```
- [ ] T6.0 Red: replay 303-0251 §2.1.1 (every outcome) and 301-2002 Rev E §6.3.4.1.9 to 6.3.4.1.11; the LOG main result arrives as its own Observation with sub-identity `&LOG`, never under the viral load's; a quantified main result carries 1009.64 in valueQuantity and DETECTED as its interpretation; `<40` arrives as valueQuantity 40 with comparator `<` and DETECTED as interpretation; NOT DETECTED arrives as the value with no quantity; analyte records arrive with their sub-identity (`HIV-1`, `HIV-1&Ct`)
- [ ] T6.1 Red: validator tests, each contract rule rejects its violation
- [ ] T6.2 Red: parser tests from the Cepheid example messages (301-2002 Rev E, 303-0251, 302-7279, MTB/RIF Ultra once verified): every record type, flag, component and C record parsed
- [ ] T6.3 Red: bundle round-trip test, every parsed fact present in the bundle in its slot
- [ ] T6.4 Red: outbox test, delivered entries survive the purge by default
- [ ] T6.5 Red: runtime test, a connection codeOverride changes inbound translation and outbound order code; a connection numberFormat of `,` reads `40,00` as 40
- [ ] T6.6 Schema and validator per Facts
- [ ] T6.7 docs/profile-authoring.md and templates/{astm,hl7,file}.json
- [ ] T6.8 genexpert-astm rev 8: the three assays in scope (step 5) from docs/profiles/genexpert-astm.md; no hints; components; value codes; translations; sources
- [ ] T6.9 Parsers and bundle per Facts; HL7 PID fallback removed
- [ ] T6.10 Outbox retention default; codeOverrides and numberFormat
- [ ] T6.11 Green; PR; release tag; then FluoroCycler XT and QuantStudio as baseline revisions, then one PR per Madagascar profile, each with its step-5 note
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

### Background (optional)

- [P1](https://claude.ai/artifact/87JjCR7fg87DCqFeS5TEfH#p1), [P2](https://claude.ai/artifact/87JjCR7fg87DCqFeS5TEfH#p2), [P3](https://claude.ai/artifact/87JjCR7fg87DCqFeS5TEfH#p3), [P4](https://claude.ai/artifact/87JjCR7fg87DCqFeS5TEfH#p4), [B1](https://claude.ai/artifact/87JjCR7fg87DCqFeS5TEfH#b1), [S1 protocol notes](https://claude.ai/artifact/87JjCR7fg87DCqFeS5TEfH#s1)
- Decisions: [contract](https://claude.ai/artifact/87JjCR7fg87DCqFeS5TEfH#d-contract), [bundle](https://claude.ai/artifact/87JjCR7fg87DCqFeS5TEfH#d-bundle), [MTB/RIF](https://claude.ai/artifact/87JjCR7fg87DCqFeS5TEfH#d-mtb), [other profiles](https://claude.ai/artifact/87JjCR7fg87DCqFeS5TEfH#d-other-profiles)
