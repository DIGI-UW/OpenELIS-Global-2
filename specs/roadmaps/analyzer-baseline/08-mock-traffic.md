# Step 8: Manufacturer-shaped mock traffic

Part of [analyzer-baseline-roadmap.md](../analyzer-baseline-roadmap.md). Read that file's Rules and Repo working agreements first; they apply here.

Goal: the mock replays vendor-documented messages for every outcome, and a
contract test keeps it aligned with the pinned baseline profile.

### Facts

- Repo: `DIGI-UW/analyzer-mock-server`. Templates
  `templates/genexpert_astm.json` (profile-driven via `profileRef`
  `{profileId: genexpert-astm, revision: 4}`, `fieldOverrides` seeding one
  negative per test) and `templates/genexpert.json` (HL7, hand-written codes
  `MTB`, `RIF`, `CT`, values Resistant/Sensitive/Detected). Profile resolution
  in `profile_adapter.py` (`ANALYZER_BRIDGE_PROFILES_DIR`, exact revision,
  fails closed). Push endpoint `POST /simulate/astm/genexpert_astm` with
  `destination`, `sample_id`, `sender_id`, `results[]`.
- Where the mock departs from 301-2002 Rev E today: H.2 delimiters `\^&`
  instead of `@^\`; patient name in P.5 instead of P.6; a quantity in R.4
  component 1 instead of component 2; its LOG complementary record also
  carries the assay name, so it passes the R.3.5 selection like the main
  result.
- Cepheid example messages to replay: 301-2002 Rev E §6.3.4.1.9 to
  6.3.4.1.11 (multi-result, single-result, quantitative with LOG, errors and
  notes); 303-0251 Rev A pages 3-7 (HIV VL XC:
  <40, numeric, >1E7, NOT DETECTED, ERROR with C record, INVALID; ASTM and
  HL7); 302-7279 Rev A page 5 onward (CoV-2/Flu/RSV plus panels: every
  POSITIVE/NEGATIVE combination, ERROR, INVALID, NO RESULT; ASTM and HL7);
  MTB/RIF Ultra once step 5 verifies it.
- Fixture layout: `fixtures/genexpert/<assay>/<outcome>.astm` and `.hl7`;
  placeholders `{sample_id}`, `{patient_id}`, `{patient_name}`,
  `{instrument_code:<profile_code>}` substituted at send time.

### Build

```
- [ ] T8.1 Red: contract test, every fixture's codes, values and record types are declared by the pinned baseline profile, and every declared outcome has a fixture
- [ ] T8.2 Fixtures from the Cepheid examples per Facts
- [ ] T8.3 Endpoint: POST /simulate/fixture/{profile}/{assay}/{outcome} with sample_id, patient, optional code overrides
- [ ] T8.4 Rebuild templates/genexpert.json from the profile or delete it; delete fieldOverrides seeding
- [ ] T8.5 Green; PR; release tag
```

### Verify

```bash
python -m pytest   # in the mock repo
grep -rn "Resistant\|Sensitive" templates/   # 0
```

### Done when

1. A fixture exists for every documented outcome of each supported GeneXpert
   assay, shaped like Cepheid's examples. (T8.1)
2. T8.1 fails on any undeclared code or value, or any missing outcome.
   (`pytest`)
3. No template sends codes or values no profile declares. (`grep`)
4. The OE2 harness E2E in step 7 uses only these fixtures. (step 7 verify)

### Background (optional)

- [T1](https://claude.ai/artifact/87JjCR7fg87DCqFeS5TEfH#t1), [P2](https://claude.ai/artifact/87JjCR7fg87DCqFeS5TEfH#p2)
- [Decision: mock traffic](https://claude.ai/artifact/87JjCR7fg87DCqFeS5TEfH#d-mock)
