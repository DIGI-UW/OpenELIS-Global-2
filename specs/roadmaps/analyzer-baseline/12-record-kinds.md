# Step 12: one taxonomy for what an instrument says

Goal: every record an analyzer sends is sorted by one rule, written in one place, declared in one
profile shape, and checked by tests in every repository, so no instrument re-derives it and no
profile needs a later refactor to express it.

### Decisions (11 Oct, the user's words)

- The rule: "OE2 cares mostly about the results, control --> qc module, and the rest is just
  informational." A value for a test the lab orders (or one of its parts) is a result on the
  ordered analysis; a value on a control material goes to the QC module; everything else an
  instrument says about a sample is informational, never a result and never gating; a declared
  record the lab's catalog has no test for is held until the lab maps it or marks it Do not receive.
- Authority: "the spec and openelis-work should be the ground truth, and all repos should follow
  it." The site workbook: "assume the mock is faithful (that's what we designed it around) and
  clearly outline it in the local spec for now. we can update openelis-work later."
- Templates: "We need to have flexible mapping to a variety of file templates", so a spec-driven
  profile and an example-driven profile for the FluoroCycler XT, each describing one workbook.
- No deferral: "i don't want to defer profile fixes to 'next revision'", and "i'm especially
  worried that the profiles will again need refactoring if we don't get this right": the profile
  contract takes its kind-first shape now, on the open PRs, before anything ships.
- No questions about kinds: the rule is applied, not asked about. A design question reaches the
  user only when no repository holds the fact.
- Order: "the main goal is recording proper evidence. then we can deal with cleanup of the
  situation" and "we are trying to record the evidence locally. why do we need to deal with PR
  issues before doing so?" The dev stack builds the Bridge and the mock from the submodule
  checkouts, so only code gates the recording; pushes, pins, renames, docs and PR bodies follow
  the clips.
- Revisions: "it's all revision 1 for now." No distro or OpenELIS release pins a baseline profile,
  so every change to one lands in its revision 1 until the baseline ships. A shipped type's status
  change minting the next revision number "is going to be an issue but defer for now"; it is on
  the after-baseline list in `specs/analyzers/roadmap.md`.

### Facts

- `specs/analyzers/spec.md` rule 11 states the rule and the invariant that a warning never gates
  a result.
- The BC-5380's alarms sit in the same OBX positions as its results (`OBX.3.1`, `OBX.5`), so a
  flag record is read where a result is.
- The QuantStudio spec (openelis-work, §8): one `IC` row per well; the internal control fails when
  its `CT` is `Undetermined`; this matters for a negative sample only; the action is review, not a
  result. The Madagascar FluoroCycler workbook (mock commit 835261c): one row per detection
  channel, `Col` 2 is the internal control, `Result` `Valid` or `Invalid`.
- A profile classifies records in exactly four sections, one per kind: `default_test_mappings`
  (results and parts), `controlResultRecognition` (control samples), `sample_flags`
  (informational), `resultRecordSelection` (not received). Reading settings (`resultParts`,
  `column_mapping`, `code_values`) say where things sit and never what kind they are.

### Build

Before the recording:

```
- [x] R2 Contract (Bridge #90): the four classifying sections; `sample_flags` protocol-agnostic as `{ raisedValue, codes, names }`, read from the result parts' positions for ASTM and HL7 and from the row's code and value for FILE; the validator refuses a code declared under two sections; the contract README's "Record kinds" section; mindray-bc5380 flags with the manual's names and no pointers
- [x] R3 Profiles (Bridge #91): thermo-quantstudio with `IC` a flag raised on `Undetermined`, not a test; hain-fluorocycler-xt-madagascar r1 with VIH-1 a result and `IC` a flag raised on `Invalid`; fingerprints recomputed; the FILE renderer sorts flag rows out on the live, retained and HTTP paths; the tests that describe those profiles say so (FileSampleFlagsTest, PriorityProfileMockFixtureTest, ShippedProfileCatalogTest)
- [x] R6a OpenELIS (#4663): the FluoroCycler results story sets up on the Madagascar profile and asserts nothing held; the harness dictionary drops Internal Control DNA and its test adds the Madagascar profile; every `@evidence` story green on the reset stack
- [x] R4a Mock (#58): a renamed sample's internal-control row takes the same new ID as its result row (it kept the fixture's name, so the Bridge sent a flag-only bundle that OpenELIS refused); the fixture cross-check resolves declared sample flags
- [x] R8 Kinds the first clips exposed. OpenELIS (#4663): saving patient results removes only the rows the reviewer acted on; the accept flow had deleted any staged control that sat beside another control in row order, so a QuantStudio save dropped its standards and NTC and kept PC and NC. Bridge (#91): a sample with no result, or only raised flags, is held as `PARSE_NO_RESULTS` naming the accession and never sent; cepheid-genexpert-astm declares the cartridge's internal controls (SPC on the respiratory tests, IQS-H and IQS-L on HIV-1 VL, each with Ct and EndPt) under `sample_flags`, raised on `FAIL`; the ASTM reader matches a part's identity within its test as a flag code and keeps a control the cartridge reports under each test as one flag. Mock (#58): the GeneXpert fixtures resolve against it. Every baseline profile is revision 1 (decision of 11 Oct above): the changes are folded into revision 1 and the revision 2 files removed, in the Bridge, the mock and OpenELIS
- [x] R7 Evidence: every `@evidence` story recorded on the reset stack after R8, every clip reviewed, no refused delivery in the Bridge log, packaged by lane and stage, and posted as the single evidence comment on #4663
```

After the clips exist (each story is its own video, so later stories are additive):

```
- [ ] R2b Contract: `test_code_values` renamed `code_values` across schema, runtime, tests and docs; the contract README says a new kind needs a spec rule before a profile field; hain-fluorocycler-xt's evidence page carries the record-kinds table the other baseline pages have
- [ ] R4 Mock (#58): the generator writes what the file carries (flag rows included); the profile copies refreshed; two variants replay a raised flag (a QuantStudio negative with an undetermined IC, a FluoroCycler sample with channel 2 Invalid)
- [ ] R6b OpenELIS (#4663): one file-lane sample with an invalid internal control shows its flag on review and in Validation, and the QuantStudio story the same for an undetermined IC on a negative; those two stories recorded and added to the package
- [ ] R1 Authority: rule 11 in specs/analyzers/spec.md (done); the same rule and a record-kinds table per instrument in openelis-work (every record, row or column the instrument emits, its kind, its citation), with the Madagascar workbook filed beside the FluoroCycler spec with its provenance
```

### Verify

```bash
cd tools/openelis-analyzer-bridge && mvn test -DanalyzerMockDir=../analyzer-mock-server   # contract and fixture checks
cd tools/analyzer-mock-server && ANALYZER_BRIDGE_PROFILES_DIR=../openelis-analyzer-bridge/src/main/resources/analyzer-profiles python -m pytest -m needs_bridge
mvn test -Dtest=HarnessDictionaryIntegrationTest                                          # every baseline profile binds on a fresh setup
cd frontend && npm run pw:test -- --project=harness-foundational --project=harness-demo --grep @evidence
```

### Done when

1. No shipped profile declares a record outside the four sections, and the contract test says so.
2. Each instrument's record-kinds table exists in openelis-work and its profile's evidence page
   matches it row for row.
3. The mock replays a raised flag for each file instrument and every fixture row resolves against
   its profile.
4. OpenELIS shows an instrument's warning on review and in Validation for ASTM, HL7 and file
   analyzers, and never as a result.
