# Analyzer codes settled at setup

Part of the [analyzer roadmap](../analyzers/roadmap.md), item 9. Repositories:
the Analyzer Bridge first, then the analyzer mock, then OpenELIS. Raised by
the #4611 review thread on `instrument_code`.

Goal: the profile suggests codes; setup settles, per analyzer, which code the
instrument sends for each assay it runs; from then on every record of that
analyzer uses only those codes. Mappings belong to each analyzer, and the
profile's assay list is an order of work, not a limit (agreed 5 Oct).

### Facts

- A profile entry's `test_code` does two jobs: it names the entry, which
  carries the entry's LOINC, run-failure values, parts and answers, and it is
  the code the instrument is assumed to send. Labs set host test codes per
  instrument (Cepheid LIS guidance 303-0251 §1.3, 302-7279 §2.3), so the two
  often differ.
- Bridge: a connection's `codeOverrides` maps profile code to instrument code
  (`BridgeAnalyzerConnectionRuntime.codeOverrides`). Reading rewrites the
  code the instrument sent back to the profile's code before building the
  bundle (`HL7ResultParser` lines 333 to 338, `FileResultRenderer`,
  `ResultReading.profileCode`). Outbound orders take the code from the LOINC
  (`OutboundOrderController` line 90, `getCodeForLoinc`). The Javadoc at
  `AnalyzerRuntimeRegistry` lines 503 to 510 says OpenELIS never sees analyzer
  codes; it does, since its mapping rows are keyed by them.
- OpenELIS: mapping rows are keyed by `(mapping_id, source_row_key)`, the
  profile's code, which this instrument may never send. Since R24 OpenELIS
  stores no instrument code (changeset 133), the Assays step has no code box,
  and Apply checks and restores only the profile pin.
- Today a lab whose instrument sends another code either maps it as a code no
  profile declares (rule 13), which loses the entry's LOINC, parts, answers
  and run-failure values, or sets `codeOverrides` on the Bridge connection
  directly, which OpenELIS neither sets nor shows.

### Target model

- Profile entry: a stable `id`, a suggested code, and its reading rules. The
  `id` is not a code anyone sends.
- Bridge connection (owns the codes): for each entry this instrument runs,
  the code it sends, written explicitly at setup. The Bridge reads that code
  and applies the entry's rules, puts the code it received in the bundle, and
  sends that code on outbound orders.
- OpenELIS mapping: rows are keyed by the code the instrument sends, each
  linked to the profile entry it was settled from, or to none for a code no
  profile declares. The link drives defaults at setup and matching rows
  across revisions in adoption. OpenELIS stores no copy of the Bridge's code
  table.
- One case, not two: a renamed code and a code the profile never declared
  are both "a code this instrument sends"; the only difference is whether it
  inherits an entry's rules.
- A code that arrives with no row is an unmapped code: a visible state on the
  review row that the lab resolves through the existing undeclared-code path.
  Never blocked, never silent.

### Open decisions

1. Answer codes too? Cepheid treats host result codes as per instrument as
   well. Recommended: the same model for answers (raw values) in this work,
   or answers keep leaking profile codes.
2. Entry `id` format. Recommended: a new slug field; an entry without one
   uses its `test_code` as its `id`, so revisions pinned today keep working.
3. Components (sub-identities): check the vendor documents for whether labs
   rename them. If they do not, they stay defined by the profile.

### Build

```
- [ ] C1 Bridge profile schema: entries gain an id, falling back to test_code
- [ ] C2 Bridge connection values: per-entry codes replace codeOverrides; each entry must exist in the pinned revision, and one code cannot go to two entries
- [ ] C3 Bridge reading: rules are looked up by entry, and the bundle carries the code received; the profile-code rewriting goes (HL7ResultParser, FileReceiptContext, FileResultRenderer, ResultReading)
- [ ] C4 Bridge outbound orders: the code comes from the entry, not from a LOINC lookup (OutboundOrderController, getCodeForLoinc)
- [ ] C5 Bridge: the AnalyzerRuntimeRegistry Javadoc says what OpenELIS sees; release
- [ ] C6 Mock: messages use the codes a configured connection sends, including a renamed one
- [ ] C7 OpenELIS mapping rows keyed by the code sent, with an entry link; defaults and adoption join through the link, not the code
- [ ] C8 OpenELIS Assays step: a code per enabled entry, written to the Bridge connection's per-entry codes, and the row created under that code in the same flow
- [ ] C9 OpenELIS reads the connection's codes back and shows them on the analyzer, so a code changed on the Bridge is visible
- [ ] C10 Migration: OpenELIS rows are re-keyed to the code the connection sends (its current codeOverrides, read from the Bridge, else the profile's code), linked to the entry when the pinned revision declares it; Bridge connections' codeOverrides become per-entry codes with every entry's code written explicitly; held results keep the code they arrived with, on their arrival revision
- [ ] C11 Harness E2E: a connection that renames a code, inbound and outbound
- [ ] C12 Spec: rule 2 and the ownership table say the Bridge connection owns the codes an instrument sends and OpenELIS owns what each code means locally
```

### Done when

1. After setup, no record of the analyzer (Bridge connection, bundle,
   mapping, review row) holds a code the instrument does not send.
2. A renamed code and an undeclared code take the same path end to end.
3. `codeOverrides` and the profile-code rewriting are gone from the Bridge.
4. The harness E2E covers a renamed code inbound and outbound. (C11)
