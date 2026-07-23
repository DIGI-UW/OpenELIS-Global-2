# LOINC validation audit — 2026-04-16 (authoritative, NLM-verified)

**Scope:** All LOINC codes referenced by the GeneXpert ASTM profile
(`projects/analyzer-profiles/astm/genexpert-astm.json`), QuantStudio +
Fluorocycler file profiles, and the distro's `example-tests.csv`.

**Why this audit exists:** The user (Piotr) flagged during plan review that
LOINC codes had been sourced from training-data recall without authoritative
validation. After a first pass using WebSearch (over-confident verdicts), the
user pushed back that search absence is not proof of absence. A second pass
using the **NLM ClinicalTables API** (free, no auth, authoritative:
`https://clinicaltables.nlm.nih.gov/api/loinc_items/v3/search?terms=<CODE>`)
confirmed the verdicts with actual LOINC data.

**The NLM API returns
`[count, [codes], null, [[code, long_common_name, component, system]]]`.
`count=0` means the code does not exist.**

## Findings (NLM-verified)

| Host Test Code                    | Old LOINC   | NLM says old LOINC is                                                                               | Correct LOINC | NLM says correct LOINC is                                                     |
| --------------------------------- | ----------- | --------------------------------------------------------------------------------------------------- | ------------- | ----------------------------------------------------------------------------- |
| `HIV-1 Viral` / aliases           | 20447-9     | ✅ HIV 1 RNA viral load in Ser/Plas                                                                 | 20447-9       | keep                                                                          |
| `Xpress` / `COVID19` / `SARSCOV2` | 94500-6     | ✅ SARS-CoV-2 RNA in Respiratory specimen                                                           | 94500-6       | keep                                                                          |
| `RIF`                             | 46244-0     | ✅ MTB complex rpoB gene rifampin resistance mutation                                               | 46244-0       | keep                                                                          |
| `HCV Viral`                       | 11011-4     | ✅ HCV RNA viral load in Ser/Plas                                                                   | 11011-4       | keep                                                                          |
| `MTB` / `MTB-RIF` / `MTBRif`      | ~~23826-1~~ | ❌ **Bordetella pertussis DNA** (inherited error in distro catalog)                                 | **85362-2**   | ✅ MTB complex DNA in Sputum/Bronchial by NAA probe                           |
| `HBV`                             | ~~20543-5~~ | ❌ **Mescaline in Urine by Confirmatory method**                                                    | **42595-9**   | ✅ HBV DNA [Units/volume] VL in Ser/Plas by NAA probe                         |
| `HPV HR`                          | ~~73959-0~~ | ❌ count=0 (code does not exist)                                                                    | **82675-0**   | ✅ HPV 16+18+31+33+35+39+45+51+52+56+58+59+66+68 DNA in Cervix by NAA probe   |
| `HPV 16_18-45`                    | ~~77399-5~~ | ❌ count=0                                                                                          | **82354-2**   | ✅ HPV 16 and 18+45 E6+E7 mRNA in Cervix by NAA probe                         |
| `CT`                              | ~~43304-5~~ | ❌ C. trachomatis **rRNA** (Cepheid uses DNA)                                                       | **21613-5**   | ✅ C. trachomatis DNA in Specimen by NAA probe                                |
| `NG`                              | ~~43305-2~~ | ❌ N. gonorrhoeae **rRNA** (Cepheid uses DNA)                                                       | **24111-7**   | ✅ N. gonorrhoeae DNA in Specimen by NAA probe                                |
| `HIV-1 Qual`                      | ~~52974-1~~ | ❌ count=0                                                                                          | **5017-9**    | ✅ HIV 1 RNA [Presence] in Blood by NAA probe                                 |
| `EV` (Enterovirus)                | ~~49171-4~~ | ❌ count=0                                                                                          | **29558-4**   | ✅ Enterovirus RNA in Cerebral spinal fluid by NAA probe                      |
| `CDIFF`                           | ~~54067-6~~ | ❌ count=0 (typo for -4)                                                                            | **54067-4**   | ✅ C. difficile toxin genes in Stool by NAA probe                             |
| `MRSA`                            | ~~92775-6~~ | ❌ **Staphylococcus lugdunensis DNA in Positive blood culture** (wrong organism + specimen)         | **35492-8**   | ✅ MRSA DNA in Specimen by NAA probe                                          |
| `SA`                              | ~~85507-2~~ | ❌ **PIK3CA gene mutations in Colorectal cancer specimen** (oncology — completely different domain) | **94512-1**   | ✅ S. aureus and MRSA identified in Isolate or Specimen by Molecular genetics |
| `TV`                              | ~~82170-1~~ | ❌ count=0                                                                                          | **69937-1**   | ✅ T. vaginalis DNA in Specimen by NAA probe                                  |
| `VANA` / `VANB`                   | ~~91860-7~~ | ❌ **Chlamydia trachomatis Ag in Genital specimen by Immunofluorescence** (wrong organism + method) | **62261-3**   | ✅ Vancomycin resistance vanA + vanB genes [Presence] by Molecular method     |
| `Mpox` (+ MPXV/MPox aliases)      | ~~82171-9~~ | ❌ count=0 (closest-by-number 82171-0 is Parainfluenza 1)                                           | **100383-9**  | ✅ Monkeypox virus DNA in Specimen by NAA probe                               |

## Severity summary

- **4 codes correct**: 20447-9, 94500-6, 46244-0, 11011-4 (three already in
  distro, one verified via audit).
- **12 codes wrong**: 11 from my candidates + 1 inherited (MTB 23826-1).
  - **3 catastrophic** (wrong organism/domain, not merely synonym drift):
    - 92775-6 (MRSA) was S. lugdunensis
    - 85507-2 (SA) was PIK3CA oncology
    - 91860-7 (vanA/vanB) was Chlamydia Ag
    - 23826-1 (MTB) was Bordetella pertussis
  - **2 common-name mismatches** (same detection target, wrong methodology):
    CT/NG rRNA vs DNA.
  - **6 nonexistent codes**: 73959-0, 77399-5, 52974-1, 49171-4, 54067-6,
    82170-1, 82171-9 (plus 20543-5 Mescaline which was still a valid but
    unrelated code).

## Method

**API used:** NLM ClinicalTables (free, no auth)
`https://clinicaltables.nlm.nih.gov/api/loinc_items/v3/search?terms=<CODE>`

**Known-valid sanity check:** `20447-9` returns count=1 and the expected FSN.
**Invalid-code signature:** `[0,[],null,[]]` — count=0, no matches.

WebSearch alone is NOT sufficient for validation — many valid LOINCs are absent
from search results, and false-positives can come from third-party sites that
misreport codes. The NLM API is the authoritative source because it's the
Regenstrief/NLM data directly.

## Files corrected

| Repo   | File                                                    | Edit                                                                                                                                                                                  |
| ------ | ------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| OE     | `projects/analyzer-profiles/astm/genexpert-astm.json`   | 12 `loinc` values updated (MTB/MTB-RIF fix applied on top of other 10)                                                                                                                |
| OE     | `projects/analyzer-profiles/file/quantstudio.json`      | Mpox LOINC 82171-9 → 100383-9                                                                                                                                                         |
| OE     | `projects/analyzer-profiles/file/fluorocycler-xt.json`  | Mpox LOINC 82171-9 → 100383-9                                                                                                                                                         |
| distro | `configs/analyzer-profiles/astm/genexpert-astm.json`    | Mirror                                                                                                                                                                                |
| distro | `configs/analyzer-profiles/file/quantstudio.json`       | Mirror                                                                                                                                                                                |
| distro | `configs/analyzer-profiles/file/fluorocycler-xt.json`   | Mirror                                                                                                                                                                                |
| distro | `configs/configuration/backend/tests/example-tests.csv` | Update 11 rows (HBVVIRALLOAD, HCVVIRALLOAD unchanged, HPVHR, HPV1618, XPERTCT, XPERTNG, HIV1QUAL, XPERTEV, XPERTCDIFF, XPERTMRSA, XPERTSA, XPERTTV, XPERTVAN, XPERTMPOX, XPERTMTBRIF) |

## Lesson learned / policy

When extending a catalog or profile with new LOINCs:

1. **Always query NLM ClinicalTables API** for the exact code.
2. **Sanity-check with a known code** to confirm API behavior.
3. Treat `count=0` as definitive "does not exist."
4. If correcting a wrong code, **verify the replacement** via the same API.
5. Inherit-and-validate: re-check LOINCs carried forward from earlier plans. The
   Bordetella pertussis substitution for MTB was in place for weeks and nobody
   caught it because no one validated the inherited code.
