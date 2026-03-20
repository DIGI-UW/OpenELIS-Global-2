# Analyzer harness lane identifiers

Canonical accession strings for the four analyzer lanes. All use the `HARN-`
prefix (≤ 20 characters for `sample.accession_number`), are loaded by
`src/test/resources/fixtures/analyzer-harness-lane-data.sql`, and are reset by
`src/test/resources/fixtures/file-import-e2e.sql` before each `--analyzers=full`
fixture load.

| Lane                | Analyzer (seed name)          | Instrument id in exports / ASTM O-field                                                                                                  | Reserved / notes                                                                                                                                                                                                                 |
| ------------------- | ----------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **ASTM GeneXpert**  | Cepheid GeneXpert (ASTM Mode) | `HARN-GX-2026-00001`                                                                                                                     | **Harness CI/local:** `e2e-fixtures/genexpert_astm.json` is bind-mounted over the mock image (`ci.analyzer-harness.yml`). Submodule template may still be used when the mount is not applied. DB seeds **sample + item only** (no `analysis` rows). |
| **QuantStudio 7**   | QuantStudio 7                 | `HARN-QS7-2026-00001`, `HARN-QS7-2026-00002`, `HARN-QS7-2026-00003`, `HARN-QS7-2026-00004`, `HARN-QS7-2026-00005`, `HARN-QS7-2026-00007` | `Sample Name` column in `frontend/playwright/fixtures/quantstudio-e2e-results.xlsx`. Demo asserts rows **00001**, **00002**, **00005**.                                                                                          |
| **QuantStudio 5**   | QuantStudio 5                 | `HARN-QS5-26-00001`                                                                                                                      | Reserved for future file-import demos (not in DB yet).                                                                                                                                                                           |
| **FluoroCycler XT** | FluoroCycler XT               | `HARN-FC-26-00001`                                                                                                                       | Reserved for future file-import demos (not in DB yet).                                                                                                                                                                           |

**Do not** reuse storage E2E accessions (`E2E001`, …) for harness analyzer
demos; they are owned by `storage-e2e.xml` and overlap caused CI/local drift.

## Local / CI parity

- **GeneXpert specimen id:** edit
  `projects/analyzer-harness/e2e-fixtures/genexpert_astm.json` and **recreate**
  the `astm-simulator` container (no image rebuild required).
- After changing lane SQL or cleanup, run
  `./src/test/resources/load-test-fixtures.sh --analyzers=full` against the
  harness DB container.
