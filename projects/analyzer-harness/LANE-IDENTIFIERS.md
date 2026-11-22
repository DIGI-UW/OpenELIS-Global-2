# Analyzer harness lane identifiers

Canonical accession strings for the four analyzers `seed-analyzers.sh` creates.
All use valid SiteYearNum format: `DEV0126{LANE}{SEQ:011d}` (prefix `DEV01`,
year `26`, 2-digit lane code, 11-digit sequence). Total: exactly 20 characters.

Captured analyzer files contain the example accessions below. The FluoroCycler
workflow creates fresh patient orders through the ordinary OpenELIS API and asks
the mock to replace only the captured XLSX sample IDs. Other fixture-based
workflows must create matching orders before sending a fixed file. These records
are not SQL fixtures.

| Lane                | Analyzer (seed name)          | Lane Code | Example Accession    | Notes                                                |
| ------------------- | ----------------------------- | --------- | -------------------- | ---------------------------------------------------- |
| **ASTM GeneXpert**  | Cepheid GeneXpert (ASTM Mode) | 10        | DEV01261000000000001 | Bridge-profile-backed `genexpert_astm` mock template |
| **QuantStudio 7**   | QuantStudio 7                 | 20        | DEV01262000000000001 | FILE/EXCEL; `quantstudio-e2e-results.xlsx`           |
| **QuantStudio 5**   | QuantStudio 5                 | 21        | DEV01262100000000001 | FILE/EXCEL; `quantstudio-e2e-results-qs5.xls`        |
| **FluoroCycler XT** | FluoroCycler XT               | 30        | DEV01263000000000001 | FILE/XLSX; the mock substitutes sample IDs           |

**Do not** reuse storage E2E accessions (`E2E001`, ...) for harness analyzer
demos; they are owned by `storage-e2e.xml` and overlap caused CI/local drift.

## Accession format

The harness site is configured with `acessionFormat = SITEYEARNUM` and
`Accession number prefix = DEV01`. Valid accessions must be exactly 20
characters: `{PREFIX:5}{YEAR:2}{SEQUENCE:13}`.

## Local / CI parity

- **GeneXpert specimen id:** pass `sample_id` to the mock simulation request;
  profile-owned assay fields always come from the pinned Bridge profile.
- Start a fresh isolated stack with `scripts/dev-stack up`, then run the
  registered analyzer Playwright scenarios to create orders and send traffic.
