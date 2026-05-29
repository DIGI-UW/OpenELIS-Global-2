# Analyzer Config + Test-Data Baseline — Remediation Plan

**Status:** Draft for sign-off (2026-05-29). Produced from a 6-agent grounded
audit (PHI exposure, format/schema drift, fixture faithfulness, consumer
contracts) + adversarial critique. All file:line citations verified against the
working tree; confidence tiers cross-referenced to Confluence page 1097531396
(Analyzer Integration Tracker, last modified 2026-05-19).

**Goal:** a consistent, honest baseline — no real patient data in any repo;
synthetic fixtures faithful to real instrument formats; one normalized config
format per artifact class; per-analyzer confidence explicit and gated; nothing
speculative shipped as validated. Format-consistency and data-confidence are
treated as **separate axes**.

---

## Findings summary

1. **No real PHI is committed — anywhere, in any of the three repos, working
   tree OR history.** Verified: `git ls-files docs/debug-local/` = 0 tracked;
   `git check-ignore -v` resolves every real capture to
   `.gitignore:131 docs/debug-local/`; `git log --all -S` for real specimen IDs
   (LM0418044, CG-M4-00-004, Arbo-extraitQS5) returns no adding commit. **No
   history scrub needed.** Real captures live correctly under gitignored
   `docs/debug-local/` plus a tarball at `/home/ubuntu/` (outside any git repo).

2. **One working-tree hygiene gap:** the **harness** repo root holds five
   untracked, NON-gitignored files — `patient-history-after.png`,
   `test history.png`, `ta.png`, `pathis.png`, `e-SIL UAT-Details.csv`. The PNGs
   are patient-history UI screenshots (appear to be synthetic team test
   patients; **severity is unverified — see Open Q6**). One `git add .` from
   exposure.

3. **Two governing contracts, opposite authority, both stale.** Profiles point
   `$schema → https://openelis-global.org/schemas/analyzer-defaults/1.0`, which
   is **dangling** (no in-repo `$id` defines it). Mock templates have a **real**
   draft-07 schema (`tools/analyzer-mock-server/templates/schema.json`) but
   ~10/19 templates fail it (4 migrated to a
   `profile`/`seedValues`/`fieldOverrides` indirection that drops inline
   `fields[]`; several use categories/formats outside the enum). The live server
   does not validate against schema.json, so evolved templates ship anyway.

4. **Profiles split into two structural families.** 13 socket profiles
   (ASTM/HL7): `$schema`, top-level `analyzer_name`/`manufacturer`/`category`,
   `identifier_pattern`, `transport`/`transport_config`, mappings with
   `result_type`+`values`. 7 FILE profiles: NO `$schema`, identity in
   `profileMeta`, `protocol.format`, `supported_extensions`, `column_mapping`.
   `dtprime.json` legitimately has **no** `default_test_mappings`. (Corpus =
   **20** profile JSONs, not 21 — the brief counted the README.)

5. **Confidence recorded three incompatible ways, parsed by no consumer:**
   boolean `verification_required` (ba88a, abbott); FILE-only free-text
   `profileMeta.validation_status` (genexpert-csv DRAFT; multiskan/tecan/wondfo
   PENDING); and 14 profiles encode nothing. `profileMeta.version` scatters
   1.0.0→3.0.0 and is read only for non-blank validation — **normalization is
   safe** (pending Open Q5 grep).

6. **Consumer-required keys are narrow and divergent under shared parents:**

   - OE2 requires `profileMeta.{id,version,displayName}` non-blank
     (`AnalyzerRestController.java:1619`; invalid → 422 + dropped from
     `GET /profiles`).
   - Per-mapping `test_code` **and** `loinc` both non-empty
     (`AnalyzerServiceImpl.java:404-411`); renaming `test_code` → **silently
     creates ZERO mappings** (no error). The `analyzer_code`/`obx_identifier`
     fallbacks are **dead code** (all 20 profiles use `test_code`).
   - **`protocol` key collision:** OE2 reads `protocol.name`/`protocol.format`
     (`FileImportServiceImpl.java:74`); mock + SQL gen read `protocol.type`
     (`generate_analyzer_sql.py:159`, `server.py:991`). **Must not be
     collapsed.**
   - `configDefaults` rename loses QC rules / file format / plugin config.

7. **Fixture faithfulness is mixed; the tracker reorders confidence.**
   Faithful-to-real today: fluorocycler-xt **mock** fixture, multiskan-fc
   **OE2** dual-grid fixture. Wrong instrument: `quantstudio-results.csv` is
   **CBC HB/WBC, not PCR**. The tracker marks **QuantStudio 5/7 as the sole
   VALIDATED** analyzer ("WORKING at LA2M"); **GeneXpert ASTM is only HIGH**
   (wire captures pending) — but **genexpert-csv has 4 real UTF-16 captures**
   and is real-data-backed.

---

## Target format (two schemas — do NOT fuse profiles and templates)

### A. Canonical profile schema (author it at the dangling URL)

Write a real JSON Schema; host at the `$schema` URL (or a repo-relative path the
readers resolve). **Unified core + `oneOf` discriminated on `protocol.name`.**

- **Core (all profiles):** `profileMeta{ id, version, displayName, confidence }`
  (id/version/displayName non-blank — OE2 hard req); `protocol{ name, format? }`
  with `name ∈ {ASTM, HL7, FILE}` (**keep `protocol.name`/`format`; never rename
  to `protocol.type`**); `configDefaults`.
- **Socket branch (ASTM/HL7):** top-level
  `analyzer_name`/`manufacturer`/`category` (scanTemplates fallback depends on
  these, `AnalyzerRestController.java:1581-1595`), `identifier_pattern`,
  `transport`/`transport_config`, `default_test_mappings[]` with
  `result_type`+`values` (**required**); HL7 adds `msh3_pattern`.
- **FILE branch:** identity via `profileMeta`, `protocol.format`,
  `supported_extensions[]`, `column_mapping{}`; `default_test_mappings`
  **OPTIONAL** (so dtprime validates without fabricated mappings).
- **Mapping shape (when present):** `test_code` + `loinc` required non-empty;
  `result_type`/`values` optional.

### B. Canonical mock-template format (update schema.json to match reality)

Profiles are the source of truth; templates _reference_ them. Update
`templates/schema.json` to the evolved indirection (contract catch-up, not a
behavior change — server doesn't enforce it today):

- `required = [analyzer, protocol]` +
  `oneOf: [{required:[fields]}, {required:[profile]}]` (accept inline `fields[]`
  OR a `profile` ref with `seedValues`/`fieldOverrides`).
- Keep `protocol.type ∈ {ASTM,HL7,RS232,FILE}`.
- Fix enums (Open Q1, Q2): add missing `category`/`file_config.format` values.

---

## PHI remediation (no history rewrite — history is clean)

1. **Harness `.gitignore` — add now:** `*.png` and `e-SIL UAT-Details.csv` (or
   `*.csv` if no tracked CSVs are needed — verify). Closes the only exposure
   surface. (Or delete the five files if unused; gitignore is the lower-risk
   default.) **This step is isolated and safe to do immediately on approval.**
2. **Tarball + extracted dir at
   `/home/ubuntu/madagascar-mnt-backup-20260410-…`** — outside git, not
   committable, but **real PHI unencrypted on disk**. Ops task: move to secured
   storage or delete per data-handling policy. Flag to lead.
3. **`docs/debug-local/`** — already fully gitignored (`:131`, comment "never
   commit"). Keep as the canonical local working copy of real captures.
4. **Binary metadata vector (from critique):** QS5 `.xls` / Tecan `.xlsx` carry
   instrument serial / Windows paths / operator in docProps; the Attune CytPix
   PDF under the capture tree was never opened. All gitignored (not a commit
   risk) — but strip docProps when generating fixtures from them, and confirm
   the PDF before any future handling.

---

## Faithful-fixture policy

**Source of format truth = the real captures under `docs/debug-local/`.**
Recipe: preserve real **structure / columns / encoding / locale /
sheet-layout**; replace **only** sample IDs + operator/serial metadata with
synthetic tokens (`DEV0126*`/`E2E-*`). Strip docProps.

| Analyzer                                                                      | Format truth                                                            | Existing fixture                                                                                                        | Action                                                                                                                                                                 |
| ----------------------------------------------------------------------------- | ----------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| QuantStudio 5/7 (VALIDATED)                                                   | QS5 .xls (3-sheet, ~19-row preamble, header row 20), QS7 .xlsx          | OE2 `quantstudio-results.csv` = **wrong instrument (CBC)**; others omit preamble/multi-sheet, `Target` vs `Target Name` | **Delete CBC file; regenerate** from real XLS, preserving 3-sheet + preamble + row-20 header. **Verify reader invokes `sheet_detection.header_scan` first (Open Q3).** |
| FluoroCycler XT (MEDIUM_HIGH)                                                 | `Fluorocycler-XT/HIV-result.xlsx` (6-col)                               | mock = **faithful**; OE2 CSV (4-col) = wrong                                                                            | Keep mock; regenerate/retire OE2 CSV to 6-col.                                                                                                                         |
| Multiskan FC (MEDIUM_HIGH)                                                    | SkanIt CSV (`;`, comma-decimal, French dual-grid, Win-1252)             | OE2 dual-grid = **faithful**; mock substitutes `Interpretation` for `Abs`                                               | Keep OE2; regenerate mock to emit raw `Abs` OD.                                                                                                                        |
| Tecan F50 (MEDIUM_HIGH)                                                       | `INFINITE F50 MAGELLAN.xlsx` (French preamble, dual-sheet)              | OE2 missing preamble; mock substitutes `Interpretation` for `OD_450`                                                    | Regenerate both from real XLSX; mock emits raw OD.                                                                                                                     |
| GeneXpert CSV (**HIGH** — real captures)                                      | `Export GE 03102024 1.csv` (UTF-16LE+BOM, key-value, analyte sub-table) | OE2 = UTF-8, wrong labels, no sub-table                                                                                 | Regenerate preserving UTF-16LE+BOM + analyte sub-table.                                                                                                                |
| Wondfo Finecare (HIGH)                                                        | real `history.csv` (tracker; 4 rec, 40 col) — not in-repo               | delimiter conflict: profile/mock `;` vs OE2 `,`                                                                         | **Resolve delimiter (Open Q4)**, then align.                                                                                                                           |
| DT-Prime (MEDIUM)                                                             | real XML exists per tracker but in-repo capture dir **empty**           | none                                                                                                                    | **Locate real XML first (Open Q4b)**; generate windows-1251.                                                                                                           |
| All ASTM/HL7 (Horiba×2, Mindray×6, Stago, Abbott, Sysmex, genexpert-hl7/astm) | in-repo specs only — **no real capture**                                | spec-derived, well-formed                                                                                               | **Keep, label `SPEC_DERIVED`.** Fix Mindray MSH inconsistency (`MINDRAY^BC-2000` two-field vs `MINDRAY BS-200` one-field). **Do not fabricate "real" data.**           |

---

## Confidence labeling + quarantine

Single field **`profileMeta.confidence`**, enum **{VALIDATED, HIGH, MEDIUM_HIGH,
MEDIUM, ESTIMATED, LOW, NA}** — **our** identifier enum (underscores), mapped
from the tracker's display scale (MEDIUM-HIGH, N/A). Keep a separate
`profileMeta.status` for profile-build state (DRAFT/PENDING) so it doesn't
masquerade as confidence.

**Seed — tracker-covered (14):**

- **VALIDATED:** QuantStudio (QS5/QS7).
- **HIGH:** genexpert-astm (wire captures pending), **genexpert-csv (4 real
  captures)**, mindray-bc5380, mindray-bs200, mindray-bs300, sysmex-xn,
  wondfo-csv, stago-start4 _(spec HIGH — but profile protocol is mis-modeled as
  ASTM vs tracker Pattern E proprietary-serial; flag for separate protocol
  fix)_.
- **MEDIUM_HIGH:** fluorocycler-xt, tecan-f50, multiskan-fc.
- **MEDIUM:** genexpert-hl7, dtprime.

**Seed — tracker-ABSENT (6): explicit fallback rule** (resolves the critique's
blocker). Tier from, in order: (a) profile self-flag
`verification_required:true` → **LOW**; else (b) spec basis — vendor LIS doc →
HIGH, general protocol knowledge / research.md §12 samples → **MEDIUM**; never
VALIDATED/HIGH without real data or a vendor LIS doc.

- mindray-ba88a → **LOW** (`verification_required:true`, "may be
  limited/absent").
- abbott-architect → **LOW** (`verification_required:true`, "VERIFY").
- mindray-bc2000 → **MEDIUM** (asserted "identical to BC-5380", unverified).
- horiba-micros60 → **MEDIUM** (research.md §12 ASTM samples; no real capture).
- horiba-pentra60 → **MEDIUM** (profile claims doc-confirmed protocol; no real
  capture).
- genexpert-csv is **not** here (it is tracker-covered via the GeneXpert row +
  has real data → HIGH above).

**CI gate (seam, not stub):** fail if any profile is `confidence: VALIDATED`
without a faithful-from-real fixture behind it. **Hard sequencing dependency:**
the VALIDATED gate cannot go green until QuantStudio fixtures are regenerated
(today all QS fixtures are wrong-format) — fixtures (WS step 8) precede gate
activation (WS step 9). Speculative analyzers are **labeled, not deleted**
(SPEC_DERIVED fixtures + LOW/MEDIUM confidence).

---

## Normalization workstreams (ordered)

**Step 0 — pre-execution verifications (minutes):** grep all 3 repos for any
`version` read on the profile object (close Open Q5); confirm the screenshot
patients are synthetic in the mgtest DB (Open Q6); confirm the QS reader invokes
`header_scan` (Open Q3). These gate later steps but are cheap.

**Mechanical (no consumer parses the value — low risk):**

1. `profileMeta.version` → one convention across all 20 profiles.
2. Add `profileMeta.confidence` to all 20 (seed per table above); migrate
   `verification_required`/`validation_status` → `confidence` + `status`.
3. Add `$schema` to the 7 FILE profiles.
4. Author the profile JSON Schema at the dangling URL.

**Judgment (code + data in lockstep, or pick a canonical value):** 5. Vocabulary
enums (Open Q1) — schema.json + FILE profiles + OE2 `CATEGORY_TO_SECTION_ID`
together. 6. Profile identity: **keep both read paths**, populate both
(data-only) rather than migrating socket top-level into `profileMeta` (avoids
touching a live read path). Revisit only if dual-read becomes a burden. 7.
Update `templates/schema.json` (oneOf `fields`|`profile`); fix enums. 8.
**Fixtures** — delete/regenerate per the table (must precede step 9). 9. Wire
the consumer-contract guard + VALIDATED gate in CI.

**Do NOT touch:** `protocol.type` vs `protocol.name`/`format`; `test_code`; the
dead `analyzer_code`/`obx_identifier` fallbacks.

---

## Verification (the seam, not the stub)

A schema pass alone is scaffolding. Add a **consumer-contract guard test** that
runs and **blocks** in CI:

- Validate all 20 profiles against the new schema; all 19 templates against the
  updated `schema.json`.
- Load every profile through the **real**
  `profile_adapter.load_profile_backed_template` and assert OE2 semantics:
  `profileMeta.{id,version,displayName}` non-blank; every present mapping has
  non-empty `test_code`+`loinc`; column-map-only FILE profiles (dtprime)
  accepted with zero mappings.
- Assert `confidence ∈ enum` and the VALIDATED→faithful-fixture gate.

---

## Open questions (bounded; each with a resolution path — not design forks)

1. **Category vocab (IMMUNOASSAY/POCT):** read OE2 `CATEGORY_TO_SECTION_ID` +
   `test_section`; decide add-to-enum vs map to IMMUNOLOGY; fix schema + corpora
   together.
2. **`file_config.format` token (XLS/XLSX/EXCEL):** check what the mock file
   reader dispatches on; pick one canonical token.
3. **QS preamble:** confirm the OE2 reader invokes `sheet_detection.header_scan`
   and tolerates the ~19-row preamble _before_ regenerating QS fixtures.
4. **Wondfo delimiter:** OGC-344 / real `history.csv` is the authority (tracker
   doesn't state delimiter); confirm `;` vs `,` and align fixture+profile.
   **(4b) DT-Prime:** locate the real XML (tracker confirms collected) before
   authoring a windows-1251 fixture.
5. **`profileMeta.version` safety:** grep all 3 repos (incl. seed-analyzers.sh,
   CI scripts, bridge registration) for any version comparison to fully close
   "normalization safe".
6. **Screenshot severity:** confirm the patient/National IDs in the harness-root
   PNGs are synthetic test records before asserting non-breach. The gitignore
   fix is correct **either way**; only the framing depends on this.

---

_Grounding: tiers from Confluence 1097531396 (2026-05-19); file:line re-verified
against the working tree; template schema validation re-run live. Companion
context: docs/analyzer-file-inventory.md._
