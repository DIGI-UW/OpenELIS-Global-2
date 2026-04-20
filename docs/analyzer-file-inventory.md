# Analyzer File Inventory

Catalog of every real, anonymized, and synthetic analyzer result file in this
repo and its submodules as of 2026-04-10. This is the ground-truth reference for
the non-destructive bridge + real-file E2E parity work described in
`mellow-honking-cascade.md` (Phase 0.1).

## Why this manifest exists

The unified analyzer form PR #3372 uncovered that our E2E tests run against
synthetic fixtures with stripped headers and pre-parsed mock metadata, not real
QuantStudio / Tecan / Wondfo exports. Herbert Yiga confirmed on 2026-04-09 that
real files from Madagascar fail to parse and — separately — that the bridge was
deleting them from his NFS mount. The refactor plan needs an authoritative list
of what real vs. synthetic data we have, where each file lives, what its shape
is, and what anonymization it still needs before it can be committed as a golden
fixture.

## File origin taxonomy

- **real** — an unmodified export from a physical analyzer at a real lab. May
  carry PII (patient initials in sample names, technician names in metadata,
  plate barcodes, file paths, instrument serial numbers, xlsx
  `docProps/core.xml`/`docProps/app.xml`). **Not safe to commit.**
- **anonymized** — originally a real export, with PII scrubbed by
  `tools/analyzer-mock-server/scripts/anonymize-analyzer-fixture.py` (see Phase
  4.0 of the plan; script is not yet implemented). Safe to commit; structural
  parity with the real file is preserved.
- **synthetic** — hand-crafted to exercise a parser code path. Structurally
  simpler than a real export (e.g. single sheet, headers at row 0, no metadata
  preamble). Safe to commit but does not prove real-world parity.

## 1. Herbert Yiga's real files (`docs/debug-local/`)

Uploaded locally by Piotr on 2026-04-09 for debugging. **Not in git.** These are
the source for Phase 4.0 anonymization.

| File                                 | Format    | Origin | Analyzer           | Sheets                                                                  | Header row                     | Data                                                                                                                                    | Notes                                                                                                                      |
| ------------------------------------ | --------- | ------ | ------------------ | ----------------------------------------------------------------------- | ------------------------------ | --------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------- |
| `Arbo-extraitQS5.xls`                | CDFV2 XLS | real   | QuantStudio 5      | `Sample Setup(221x13)`, `Amplification Data(8021x6)`, `Results(177x31)` | `Results` row 20               | Arbovirus panel (CHIKV, DENV, ZIKV); Sample Names are plate-well indices like "188"; Task column distinguishes UNKNOWN / STANDARD / NTC | Ships anonymized as Phase 4 QS5-Arbovirus fixture. Source of the 2026-04-09 parse-failure bug report.                      |
| `CV VIH 05 03 2024 serie 01.xls`     | CDFV2 XLS | real   | QuantStudio 7 Flex | `Sample Setup(147x13)`, `Amplification Data(17x5)`, `Results(86x26)`    | `Results` row 16               | HIV VL **standard curve only** — all rows have `Task=STANDARD`, Sample Names `STD1_E7` etc. Not a clinical run.                         | **Skipped** — not the correct file. Waiting on a real patient-run QS7 HIV VL export from Herbert. See plan Phase 2.2.      |
| `CV VIH 05 03 2024 serie 01 (1).xls` | CDFV2 XLS | real   | QuantStudio 7 Flex | same as above                                                           |                                | Duplicate of the above with ` (1)` suffix (browser re-download)                                                                         | Same disposition.                                                                                                          |
| `INFINITE F50 MAGELLAN.xlsx`         | OOXML     | real   | Tecan Infinite F50 | `Plan_plaque(16x14)`, `DO_palque(26x14)`                                | French plate map — not tabular | Wantai HIV ELISA plate. `Plan_plaque` = sample→well map, `DO_palque` = well→absorbance. Comma decimal separator (`0,0608`).             | Ships anonymized as Phase 4 Tecan-F50 fixture. Needs NEW `PlateMapParser.java` — no existing decoder. Phase 3 of the plan. |

## 2. Mock server fixtures (`tools/analyzer-mock-server/fixtures/`)

Synthetic fixtures used by the existing Playwright "file analyzer" harness via
`POST /simulate/file/{template}`. Each fixture is a hand-crafted minimal file:
single sheet (for XLS/XLSX), headers at row 0, sample names in the `DEV0126*`
family that match test-database accessions. The mock server pre-parses these
with `fixture_parser.py` and returns `metadata.results` that the Playwright
assertion compares against the UI. **This is the E2E short-circuit** — the real
bridge parser technically runs against these fixtures but only ever sees a
happy-path shape.

| File                                | Format    | Origin    | Target analyzer profile       | Shape                                                                                                                                                     | Notes                                                                                                                    |
| ----------------------------------- | --------- | --------- | ----------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------ |
| `quantstudio5/results.xls`          | CDFV2 XLS | synthetic | QuantStudio 5 (today: HIV VL) | `Results(9x11)` headers at row 0: `Well`, `Well Position`, `Sample Name`, `Target Name`, `Task`, `Reporter`, `Quencher`, `Quantity Mean`, `CT`, `Ct Mean` | 8 data rows, all `Target Name=VIH-1`, sample names `DEV01262100*`. Does not match real Herbert file structure at all.    |
| `quantstudio5/results-demo.xls`     | CDFV2 XLS | synthetic | QuantStudio 5                 | same                                                                                                                                                      | Demo variant for video recording.                                                                                        |
| `quantstudio7/results.xlsx`         | OOXML     | synthetic | QuantStudio 7 Flex            | `Results(9x11)` headers identical to QS5 stub                                                                                                             | 8 data rows, all `Target Name=VIH-1`, sample names `DEV01262000*`.                                                       |
| `quantstudio7/results-demo.xlsx`    | OOXML     | synthetic | QuantStudio 7 Flex            | same                                                                                                                                                      | Demo variant.                                                                                                            |
| `fluorocycler-xt/results.xlsx`      | OOXML     | synthetic | FluoroCycler XT               | `Results(7x10)` headers at row 0: `SampleID`, `WellPosition`, `AssayName`, `TargetName`, `TargetNo`, `CP` + 4 more                                        | Synthetic.                                                                                                               |
| `fluorocycler-xt/results-demo.xlsx` | OOXML     | synthetic | FluoroCycler XT               | same                                                                                                                                                      | Demo variant.                                                                                                            |
| `multiskan-fc/results.csv`          | CSV       | synthetic | Thermo Multiskan FC           | 1 metadata row + header `WellPosition;SampleID;Absorbance;TestCode` + data rows                                                                           | Semicolon-delimited; single data row in current fixture.                                                                 |
| `tecan-f50/results.csv`             | CSV       | synthetic | Tecan Infinite F50            | header `WellPosition;SampleID;OD_450;TestCode` + data rows                                                                                                | Semicolon-delimited. **Does not match real Tecan Magellan format** (which is a plate-map xlsx — see INFINITE F50 above). |
| `wondfo-finecare/results.csv`       | CSV       | synthetic | Wondfo Finecare FS-205        | 1 metadata row + 40-column header + data row                                                                                                              | Semicolon-delimited; the only mock fixture with a realistic metadata preamble.                                           |
| `wondfo-finecare/results-demo.csv`  | CSV       | synthetic | Wondfo Finecare FS-205        | same                                                                                                                                                      | Demo variant.                                                                                                            |

## 3. Playwright fixtures (`frontend/playwright/fixtures/`)

Fixtures used by `file-import-results.spec.ts` and related E2E specs. Most are
copies of the mock-server stubs but the `quantstudio-qs7.xls` entry is a curious
one (only 3 data rows, different header layout).

| File                                   | Format    | Origin    | Shape / notes                                                                                                                                                                                                                                     |
| -------------------------------------- | --------- | --------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `quantstudio-e2e-results-qs5.xls`      | CDFV2 XLS | synthetic | `Results(9x11)` — clone of mock `quantstudio5/results.xls`                                                                                                                                                                                        |
| `quantstudio-e2e-results-qs5-demo.xls` | CDFV2 XLS | synthetic | demo variant of above                                                                                                                                                                                                                             |
| `quantstudio-e2e-results.xlsx`         | OOXML     | synthetic | `Results(9x11)` — clone of mock `quantstudio7/results.xlsx`                                                                                                                                                                                       |
| `quantstudio-e2e-results-demo.xlsx`    | OOXML     | synthetic | demo variant                                                                                                                                                                                                                                      |
| `quantstudio-qs7.xls`                  | CDFV2 XLS | synthetic | `QS(3x4)` headers at row 0: `Sample Name`, `Assay`, `CT`, `Units`. **Shape diverges from the other QS fixtures** — only 3 data rows, different header set. Predates the unification work. Candidate for deletion once Phase 4 real fixtures land. |
| `fluorocycler-e2e-results.xlsx`        | OOXML     | synthetic | clone of mock fluorocycler                                                                                                                                                                                                                        |
| `fluorocycler-e2e-results-demo.xlsx`   | OOXML     | synthetic | demo variant                                                                                                                                                                                                                                      |
| `multiskan-fc-e2e-results.csv`         | CSV       | synthetic | clone of mock multiskan                                                                                                                                                                                                                           |
| `tecan-f50-e2e-results.csv`            | CSV       | synthetic | clone of mock tecan (does not match real format)                                                                                                                                                                                                  |
| `wondfo-finecare-e2e-results.csv`      | CSV       | synthetic | clone of mock wondfo                                                                                                                                                                                                                              |
| `wondfo-finecare-e2e-results-demo.csv` | CSV       | synthetic | demo variant                                                                                                                                                                                                                                      |
| `FileImport.json`                      | JSON      | config    | legacy file-import Playwright fixture config (not analyzer data)                                                                                                                                                                                  |

## 4. Backend unit test resources (`src/test/resources/testdata/files/`)

Used by `FileResultParserTest` and related JUnit tests. All synthetic, all CSV,
all stripped down to minimum viable shapes that exercise specific parser
branches.

| File                            | Format          | Shape                                                                               | Notes                                                                                                                       |
| ------------------------------- | --------------- | ----------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------- |
| `fluorocycler-results.csv`      | CSV (semicolon) | `Position;Sample ID;Result;Interpretation` + 2 data rows                            | Minimal — only validates CSV parser happy path.                                                                             |
| `genexpert-results.csv`         | CSV             | —                                                                                   | GeneXpert ASTM-over-CSV case.                                                                                               |
| `malformed-results.csv`         | CSV             | —                                                                                   | Parser failure mode coverage.                                                                                               |
| `multiskan-fc-dual-grid.csv`    | CSV             | —                                                                                   | Validates dual-grid layout handling.                                                                                        |
| `quantstudio-results.csv`       | CSV (comma)     | `Sample_ID,Test_Code,Result,Unit,Reference_Range,Test_Date,Test_Time` + 2 data rows | **Completely unrelated to real QuantStudio SDS exports.** Named after the analyzer but shape-wise tests a generic CSV flow. |
| `quantstudio7-flex-results.csv` | CSV             | —                                                                                   | Same — CSV shape, not the real xlsx SDS shape.                                                                              |
| `tecan-f50-plate-grid.csv`      | CSV             | —                                                                                   | Exercises plate-grid CSV parser — does NOT match the real xlsx plate map.                                                   |
| `wondfo-finecare-results.csv`   | CSV             | —                                                                                   | Synthetic Wondfo.                                                                                                           |

## Anonymization gaps to fill during Phase 4.0

The Phase 4 anonymization script
(`tools/analyzer-mock-server/scripts/anonymize-analyzer-fixture.py`, not yet
implemented) must scrub the following from Herbert's real files before they can
be committed as golden fixtures:

1. **Sample Name column values** → replace with `DEV0126*` synthetic accessions
   pre-staged in the E2E test database.
2. **Experiment Name / Experiment Comment** (metadata preamble rows 0-15 in QS
   exports) → `ANONYMIZED-<fixture-name>`.
3. **Plate Barcode** → `TEST-PLATE-01`.
4. **Experiment File Name** — contains Windows-style paths like
   `C:\Applied Biosystems\QuantStudio ...\...` → `ANONYMIZED`.
5. **Instrument Name** / **Instrument Serial Number** — e.g. `272529515` in
   Herbert's Arbo file → `TEST-SERIAL`.
6. **User Name** (QS metadata) / **Operator** (Magellan) → `test-user`.
7. **Date Created / Experiment Run End Time** — fixed reference timestamps to
   prevent deanonymization via schedule correlation.
8. **XLSX only**: rewrite `docProps/core.xml` + `docProps/app.xml` via direct
   ZIP manipulation to strip `lastModifiedBy`, `company`, `creator`,
   `lastPrinted`. **openpyxl does not scrub these on round-trip**, so the script
   must edit the ZIP directly.
9. **Cell comments / threaded comments** — delete.

The script must be deterministic: running it twice on the same input must
produce bit-identical output (golden-file tested).

## What's missing

Files we need from Herbert + Mekom before Phase 4 can complete:

- A **real patient-run** QS7 HIV Viral Load export (the existing
  `CV VIH 05 03 2024 serie 01.xls` is a calibration curve).
- A **real patient-run** QS5 HIV Viral Load export (for a second HIV profile
  distinct from the Arbovirus one).
- The **Casey Iiams-Hauser export guide** shared in `#ext-madagascar-mekom-medx`
  on 2026-02-19 — documents how SDS and Magellan exports should be produced.
  Reference for the anonymization script's PII scrubbing rules.
- Eventually: a **corrected-result export** variant for each analyzer (same
  sample+test with an adjusted value) to feed Phase 4.2(d)'s FHIR-amendment
  test.

Herbert is uploading his files to a Drive backup folder (linked from this
manifest once the URL lands). The `docs/debug-local/` directory is a working
copy and is **not** committed to git — it only exists on developers' machines.

## Related plan phases

- **Phase 0.1** — this file.
- **Phase 0.2** — Drive backup folder for Herbert's source files.
- **Phase 0.3** — distro `:ro` mount (PR #4 on openelis-madagascar-distro).
- **Phase 0.4** — snapshot mgtest `/tmp/openelis-analyzer-bridge/` before bridge
  cutover.
- **Phase 4.0** — anonymization script + fixture commit.
- **Phase 4.1** — `/simulate/file/real/{template}` mock server route.
- **Phase 4.2** — four-scenario E2E suite (initial, manual move, duplicate,
  corrected).
