# Analyzer harness lane identifiers

Canonical accession strings for the ten analyzer lanes. All use valid
SiteYearNum format: `DEV0126{LANE}{SEQ:011d}` (prefix `DEV01`, year `26`,
2-digit lane code, 11-digit sequence). Total: exactly 20 characters.

Pre-seeded accessions (for file-import-results tests) use sequence range
0000000001–0000000099. Demo flow accessions (auto-created on accept) use
sequence range 0000000100–0000000199.

Loaded by `src/test/resources/fixtures/analyzer-harness-lane-data.sql` and reset
by `src/test/resources/fixtures/file-import-e2e.sql` before each
`--profile=harness` fixture load.

| Lane                | Analyzer (seed name)          | Lane Code | Example Accession    | Notes                                                             |
| ------------------- | ----------------------------- | --------- | -------------------- | ----------------------------------------------------------------- |
| **ASTM GeneXpert**  | Cepheid GeneXpert (ASTM Mode) | 10        | DEV01261000000000001 | COVID-only ASTM; mock template `e2e-fixtures/genexpert_astm.json` |
| **QuantStudio 7**   | QuantStudio 7                 | 20        | DEV01262000000000001 | FILE/EXCEL; `quantstudio-e2e-results.xlsx`                        |
| **QuantStudio 5**   | QuantStudio 5                 | 21        | DEV01262100000000001 | FILE/EXCEL; `quantstudio-e2e-results-qs5.xls`                     |
| **FluoroCycler XT** | FluoroCycler XT               | 30        | DEV01263000000000001 | FILE/EXCEL; `fluorocycler-e2e-results.xlsx`                       |
| **Mindray BC-5380** | Mindray BC-5380               | 40        | DEV01264000000000001 | HL7/MLLP; mock template `mindray_bc5380.json`                     |
| **Mindray BS-200**  | Mindray BS-200                | 41        | DEV01264100000000001 | HL7/MLLP; mock template `mindray_bs200.json`                      |
| **Mindray BS-300**  | Mindray BS-300                | 42        | DEV01264200000000001 | HL7/MLLP; mock template `mindray_bs300.json`                      |
| **Wondfo Finecare** | Wondfo Finecare FS-205        | 50        | DEV01265000000000001 | FILE/CSV; `wondfo-finecare-e2e-results.csv`                       |
| **Tecan F50**       | Tecan Infinite F50            | 51        | DEV01265100000000001 | FILE/CSV; `tecan-f50-e2e-results.csv`                             |
| **Multiskan FC**    | Thermo Multiskan FC           | 52        | DEV01265200000000001 | FILE/CSV; `multiskan-fc-e2e-results.csv`                          |

**Do not** reuse storage E2E accessions (`E2E001`, ...) for harness analyzer
demos; they are owned by `storage-e2e.xml` and overlap caused CI/local drift.

## Accession format

The harness site is configured with `acessionFormat = SITEYEARNUM` and
`Accession number prefix = DEV01`. Valid accessions must be exactly 20
characters: `{PREFIX:5}{YEAR:2}{SEQUENCE:13}`.

## Local / CI parity

- **GeneXpert specimen id:** edit
  `projects/analyzer-harness/e2e-fixtures/genexpert_astm.json` and **recreate**
  the `astm-simulator` container (no image rebuild required).
- After changing lane SQL or cleanup, run
  `./src/test/resources/load-test-fixtures.sh --profile=harness` against the
  harness DB container.
