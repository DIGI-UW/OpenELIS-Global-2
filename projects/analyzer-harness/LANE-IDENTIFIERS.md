# Analyzer harness lane identifiers

Canonical accession strings for the seven analyzer lanes (four ASTM/file + three
HL7). All use the `HARN-` prefix (≤ 20 characters for
`sample.accession_number`), are loaded by
`src/test/resources/fixtures/analyzer-harness-lane-data.sql`, and are reset by
`src/test/resources/fixtures/file-import-e2e.sql` before each `--analyzers=full`
fixture load.

| Lane                | Analyzer (seed name)          | Instrument id in exports / ASTM O-field                                                                                                  | Reserved / notes                                                                                                                                                                                                                                         |
| ------------------- | ----------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **ASTM GeneXpert**  | Cepheid GeneXpert (ASTM Mode) | `HARN-GX-2026-00001`                                                                                                                     | **Harness CI/local:** `e2e-fixtures/genexpert_astm.json` is bind-mounted over the mock image (`ci.analyzer-harness.yml`). **COVID-only** ASTM + `enable_qc: false`; DB seeds sample/item + **one COVID (`94500-6`) analysis** for accept/persist parity. |
| **QuantStudio 7**   | QuantStudio 7                 | `HARN-QS7-2026-00001`, `HARN-QS7-2026-00002`, `HARN-QS7-2026-00003`, `HARN-QS7-2026-00004`, `HARN-QS7-2026-00005`, `HARN-QS7-2026-00007` | `Sample Name` column in `frontend/playwright/fixtures/quantstudio-e2e-results.xlsx`. Demo asserts rows **00001**, **00002**, **00005**.                                                                                                                  |
| **QuantStudio 5**   | QuantStudio 5                 | `HARN-QS5-2026-00001`, `HARN-QS5-2026-00002`, `HARN-QS5-2026-00005`, …                                                                   | `Sample Name` column in `frontend/playwright/fixtures/quantstudio-e2e-results-qs5.xls`. Playwright `file-import-results.spec.ts` asserts **00001**, **00002**, **00005**.                                                                                |
| **FluoroCycler XT** | FluoroCycler XT               | `HARN-FC-2026-00001`, `HARN-FC-2026-00002`, `HARN-FC-2026-00003`                                                                         | `SampleID` column in `frontend/playwright/fixtures/fluorocycler-e2e-results.xlsx`. Playwright asserts CP / interpretation for those samples.                                                                                                             |
| **HL7 BC-5380**     | Mindray BC-5380               | `HARN-BC5380-2026-00001`                                                                                                                 | Fixture ID `2007`. HL7 MLLP via bridge port 2575. Strict 013 profile: `projects/analyzer-profiles/hl7/mindray-bc5380.json`. Validated by `scripts/verify-strict-013-fixtures.sh` and `scripts/test-hl7-profiles.sh`.                                     |
| **HL7 BS-200**      | Mindray BS-200                | `HARN-BS200-2026-00001`                                                                                                                  | Fixture ID `2009`. HL7 MLLP via bridge port 2575. Strict 013 profile: `projects/analyzer-profiles/hl7/mindray-bs200.json`. Validated by `scripts/verify-strict-013-fixtures.sh` and `scripts/test-hl7-profiles.sh`.                                      |
| **HL7 BS-300**      | Mindray BS-300                | `HARN-BS300-2026-00001`                                                                                                                  | Fixture ID `2010`. HL7 MLLP via bridge port 2575. Strict 013 profile: `projects/analyzer-profiles/hl7/mindray-bs300.json`. BS-300 equivalence with BS-200 remains a validation question (see `evidence-boundary.md`).                                    |

**Do not** reuse storage E2E accessions (`E2E001`, …) for harness analyzer
demos; they are owned by `storage-e2e.xml` and overlap caused CI/local drift.

## Local / CI parity

- **GeneXpert specimen id:** edit
  `projects/analyzer-harness/e2e-fixtures/genexpert_astm.json` and **recreate**
  the `astm-simulator` container (no image rebuild required).
- After changing lane SQL or cleanup, run
  `./src/test/resources/load-test-fixtures.sh --analyzers=full` against the
  harness DB container.
