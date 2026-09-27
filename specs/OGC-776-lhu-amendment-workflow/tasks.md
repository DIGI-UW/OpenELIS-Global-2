# OGC-776 — S-15e: LHU Amendment Workflow on Order (MVP)

**Jira:** [OGC-776](https://uwdigi.atlassian.net/browse/OGC-776)
**Parent epic:** OGC-527 (Vector — Environmental & Vector Testing Module)
**Consumed by:** OGC-552 (S-06 LHU v2.0 — §5.1.5 amendment notice block)
**Standard:** ISO/IEC 17025:2017 §7.8.8 (amended reports)
**Estimated size:** M (unblocked slice) + S (each gated item)

---

## Overview

Add report-level amendment support to the LHU (Laporan Hasil Uji) certificate flow. When a
released LHU is found to be wrong, the lab reissues it as a formal amendment: the report
identifies itself as an amendment, names the version it supersedes, captures a reason, and
the original is preserved. This is **report-level** amendment, distinct from the existing
**result-level** amendment (OGC-660 / OGC-713).

### Two reality corrections vs. the Jira ticket (read before starting)

The ticket's acceptance criteria assume two things that **do not exist in the codebase today**.
This was verified directly. Tasks below are organized so the unblocked work ships independently
and the two gated items are called out explicitly.

1. **No certificate-numbering scheme exists.** There is no certificate number, no `LHU-YYYY-NNNN`
   format, and no `/R` suffix anywhere in the code. The LHU PDF is keyed only by the sample
   accession number (filename `LH-<accession>.pdf`). The ticket's AC "append `/Am.N`, preserving
   `/R`" therefore cannot be implemented as written until a certificate number exists.
   See **Phase 4** — gated on a grooming decision.

2. **No PDF is preserved or hashed.** Neither the patient-report `DocumentTrack` mechanism
   (`reports/valueholder/DocumentTrack.java`, `common/services/ReportTrackingService.java`) nor
   the compliance `compliance_report_generation` table stores PDF bytes or a SHA-256 hash — both
   store only metadata (which sample, when, by whom). The ticket's AC "original LHU preserved —
   rely on existing S-06 audit trail (SHA-256 hashed stored PDF)" rests on a feature that is not
   built. To actually satisfy §7.8.8 ("preserve the original"), PDF archival must be added.
   See **Phase 5** — gated on a grooming decision.

### What already exists (DO NOT rebuild)

- **"Order" = `Sample`.** There is no `order` table. Add columns to `clinlims.sample`; mirror on
  the `Sample` valueholder (`sample/valueholder/Sample.java`). This matches the established Vector
  pattern (e.g. `identification_status`, `deconvolution_status` were added to `sample`).
- **LHU generation:** `compliance/controller/rest/ComplianceReportRestController.java`
  - `GET /rest/complianceReport/exportPdf?sampleId={id}` → `exportPdf(Long sampleId, HttpServletResponse)`
  - iText5 PDF built on the fly; filename `LH-<accession>.pdf`; calls `recordGeneration(...)` at the end.
- **Generation tracking (use this for "already released?" detection):**
  - Table `compliance_report_generation` (sample_id, generated_at, generated_by_user_id) — append-only.
  - `compliance/service/ComplianceReportGenerationService`:
    - `void recordGeneration(Long sampleId, String userId)`
    - `Optional<OffsetDateTime> getLastGenerated(Long sampleId)`
  - Entity `compliance/valueholder/ComplianceReportGeneration.java`.
- **Frontend report UI:** `frontend/src/components/reports/compliance/LaporanHasilReport.jsx`
  (filters, stats tiles, table, "Generate PDF" action via `window.open(...)`).
- **Liquibase pattern:** next file is `src/main/resources/liquibase/3.5.x.x/051-*.xml`, registered in
  `3.5.x.x/base.xml`. Highest existing is `050-resample-notification-trigger.xml`. Use `clinlims` schema and
  `columnExists` preconditions for idempotency.

### Architecture invariants (constitution)

- 5-layer pattern: Valueholder → DAO → Service → Controller → Form/DTO.
- `@Transactional` in services ONLY (never controllers). Services compile all data inside the
  transaction (no `LazyInitializationException`).
- Liquibase for all schema changes (no hand SQL).
- React Intl — no hardcoded strings; new keys in `en.json` only.
- Carbon Design System for any UI.
- TDD: write the failing test first.

---

## Phase 1 — Schema (UNBLOCKED) — AC1

**T-101  Liquibase changelog: amendment columns on `sample`**
- New file `src/main/resources/liquibase/3.5.x.x/051-sample-amendment-metadata.xml`.
- Add to `clinlims.sample`, all nullable, each guarded by a `not columnExists` precondition
  (`onFail="MARK_RAN"`):
  - `amends_lhu_number` TEXT — the prior certificate/lab number this order amends.
  - `amendment_number` INTEGER — sequential amendment counter (null = never amended).
  - `amendment_reason` TEXT — free-text reason captured at reissue.
- changeSet id pattern: `3.5.0.0-sample-add-amendment-columns`, author = your GH handle.
- Register `<include file="051-sample-amendment-metadata.xml" relativeToChangelogFile="true"/>`
  in `3.5.x.x/base.xml`, in numeric order.

**T-102  Map columns on `Sample` valueholder**
- Add fields + getters/setters to `sample/valueholder/Sample.java`:
  `String amendsLhuNumber`, `Integer amendmentNumber`, `String amendmentReason`.
- Match the existing Hibernate mapping style used for the other Vector columns on `Sample`
  (annotation vs `.hbm.xml` — follow whatever `Sample` already uses; verify before writing).

**T-103  Verify migration runs clean**
- `mvn liquibase:update` (or app boot) against a dev DB; confirm columns exist and re-run is a no-op.

---

## Phase 2 — Detection + persistence service (UNBLOCKED) — AC2, AC4, AC7

**T-201  "Has a released LHU already been released?" check**
- ⚠️ **Correction (verified in code):** generation is NOT release. The frontend "Generate PDF"
  button calls `window.open(...exportPdf...)` directly (`LaporanHasilReport.jsx:164-189`) and the
  backend calls `recordGeneration(...)` on EVERY open (`ComplianceReportRestController.java:281-283`),
  including previews of unsigned/partial drafts. So `compliance_report_generation` is a generation
  log, not a release log. Keying the amendment trigger off `getLastGenerated().isPresent()` would
  fire on a mere preview and wrongly force the reissue flow on never-released reports.
- The real release marker is the manager `VALIDATED_AND_RELEASED` electronic signature, which the
  controller already reads (`ComplianceReportRestController.java:357-390`):
  `electronicSignatureService.getSignaturesForRecord("VALIDATION_BATCH", analysisId)` →
  `SignatureMeaning.VALIDATED_AND_RELEASED`.
- Add `boolean hasBeenReleased(Long sampleId)` keyed off that signature (a released analysis exists
  for the sample), NOT off the generation count. This is the amendment trigger.
- `compliance_report_generation` is still useful as a "was a PDF ever produced" audit, but it does
  NOT gate amendment.

**T-202  Amendment persistence on the Sample**
- New service method (in a Sample-scoped service, `@Transactional`) to apply an amendment:
  `void applyLhuAmendment(Long sampleId, String priorCertificateNumber, String reason)`:
  - `amendmentNumber = (current == null ? 1 : current + 1)`
  - `amendsLhuNumber = priorCertificateNumber` (until Phase 4 exists, this is the accession/lab number)
  - `amendmentReason = reason`
  - persist; do NOT touch existing `compliance_report_generation` rows (append-only audit stays intact).
- Backward compatible by construction: untouched orders keep `amendmentNumber = null` (AC7).

**T-203  Unit tests (TDD — write first)**
- First amendment sets number → 1; second → 2 (increment).
- `amendment_reason` required: blank/null reason rejected with a clear error.
- Non-amended order returns `amendmentNumber == null` and is unaffected.
- Inversion test: if the increment logic is broken, the test fails.

---

## Phase 3 — Reissue API + UI (UNBLOCKED for capture; rendering gated) — AC2

**T-301  Reissue endpoint**
- Add `POST /rest/complianceReport/reissue` (or extend `exportPdf` with an `amend`/`reason` param)
  on `ComplianceReportRestController`:
  - requires `sampleId` and non-blank `reason`;
  - calls `applyLhuAmendment(...)` then regenerates the PDF and `recordGeneration(...)`.
  - Controller stays thin — no `@Transactional`, no business logic.
- 400 when `reason` is blank; 404 when sample has not been released (use `hasBeenReleased`, T-201 —
  NOT generation count; nothing to amend if never released).
- **Auth (verified):** the controller already carries a class-level
  `@PreAuthorize("hasRole('ROLE_RESULTS') or hasRole('ROLE_SUPERVISOR') or hasRole('ADMIN')")`
  (`ComplianceReportRestController.java:44-46`). A reissue endpoint added to this controller inherits
  it — which matches the out-of-scope decision "any user who can generate the LHU may reissue." No
  new permission key needed. State this explicitly so no one adds a redundant per-method check.
- **Auth-ordering test (constitution V.6):** assert a caller WITHOUT `ROLE_RESULTS`/`ROLE_SUPERVISOR`/
  `ADMIN` gets 403 from `/reissue`, and that the 403 fires BEFORE any 400/404 body validation.

**T-302  "Reissue with amendment" dialog (Carbon)**
- In `LaporanHasilReport.jsx`: when a row already has a `lastGenerated`, the action becomes
  "Reissue with amendment" (alongside / instead of "Generate PDF").
- Carbon `Modal` with a required `TextArea` for the reason (no rich taxonomy — plain text per MVP).
- On submit, POST to the reissue endpoint, then refresh the row's generation status.
- All strings via React Intl → new keys in `frontend/src/languages/en.json` only
  (e.g. `lhu.amendment.reissue`, `lhu.amendment.reason.label`, `lhu.amendment.reason.required`).

**T-303  Frontend test**
- Reason required (submit disabled / validation message when empty).
- Successful reissue updates the row state.

---

## Phase 4 — Certificate number `/Am.N` suffix (DECIDED — interim scheme) — AC3

> **Grooming resolved (2026-06-23):** no `LHU-YYYY-NNNN/R` scheme exists, so OGC-776 owns the suffix
> on an **interim** basis against today's accession/lab number. Confirm with OGC-552's owner whether
> 552 will eventually deliver the full certificate number (the 776 → 552 dependency arrow looks
> backwards for this AC); raised in the Jira comment (T-225).

**T-401  Grooming decision (DECIDED 2026-06-23)**
- **Decision: OGC-776 defines the certificate number (interim).** Build the `/Am.N` suffix against
  today's accession/lab number now; flag clearly as interim until the full `LHU-YYYY-NNNN/R` scheme
  lands (presumed OGC-552). **Risk to manage:** two competing numbering systems — when the real
  scheme arrives, a migration must reconcile `amends_lhu_number` values captured under the interim
  meaning. Track that as a v2 follow-up.

**T-402  Build the interim suffix helper (decided path)**
- Add a suffix helper on the amendment service:
  `String certificateNumberWithAmendmentSuffix(String baseCertificateNumber, Integer amendmentNumber)`.
- Append `/Am.N` when `amendmentNumber >= 1`; no suffix when null/0. Preserve any existing `/R`
  revision suffix by appending after it (e.g. `LHU-2026-0042/R` → `LHU-2026-0042/R/Am.1`), so the
  same helper works unchanged once a real `/R` scheme exists.
- `baseCertificateNumber` is the accession/lab number today (interim). When OGC-552 delivers the
  real certificate number, only the caller that supplies `baseCertificateNumber` changes — the
  suffix logic is unaffected.
- Store the *prior* certificate number into `amends_lhu_number` at reissue time (the accession
  number, per the interim scheme).
- On the generated LHU PDF, print two header lines: `Certificate No.:` (the suffixed certificate
  number) and `Lab Number:` (the raw accession number, unsuffixed) — so the certificate identity and
  the underlying accession stay distinguishable. Plus an amendment notice block (amendment no.,
  `Supersedes:` the prior number from `amends_lhu_number`, `Amendment reason:`) when
  `amendmentNumber >= 1`.
- Also expose the suffixed certificate number on the report DTO (`certificateNumber`) for the listing
  and the reissue response.

**T-403  Tests** — `/Am.1` on first amendment, `/Am.2` on second, no suffix when null/0,
`/R` preserved when present, null base returns null.

---

## Phase 5 — Original-PDF preservation (DECIDED — add PDF archival) — AC5

> **Grooming resolved (2026-06-23):** the ticket's "rely on existing S-06 SHA-256 hashed stored PDF"
> is false — no PDF is stored or hashed today, and metadata-only tracking does **not** meet §7.8.8
> "preserve the original" (the ticket's own justification; KAN audits ask to see the original
> report). Decision is to **add real PDF archival** (option b). Raised in the Jira comment (T-225).

**T-501  Grooming decision (DECIDED 2026-06-23)**
- **Decision: option (b) — add PDF archival.** Persist the rendered PDF bytes + SHA-256 on
  release/reissue so the original is genuinely retained, meeting §7.8.8. Sizing is not a
  server-weight concern (see T-502).

**T-502  Archive the rendered PDF (decided path)**
- Archive **only on release and on reissue — NOT on every `exportPdf`/preview.** This is the single
  most important constraint: previews are frequent (see T-201), releases/amendments are rare, so
  archiving on the release path keeps volume tiny. Archiving on every generation would balloon storage.
  Implementation: in `exportPdf`, archive only when `hasBeenReleased(sampleId)` is true.
- New **dedicated cold table** `compliance_report_archive` via Liquibase changelog `052-*.xml`:
  columns `sample_id`, `amendment_number`, `pdf_content` (bytea), `sha256_hash`, `generated_at`,
  `generated_by_user_id`. `amendment_number = 0` is the original released report; `1..N` are
  amendments. Unique constraint on `(sample_id, amendment_number)` to enforce immutability at the DB.
  Keep it separate from `sample` and from any table the report listing SELECTs — the `bytea` is read
  only on audit retrieval, never on normal listing. Do NOT extend
  `DocumentTrack`/`ReportTrackingService` (verified metadata-only; co-locating bytes there would
  load them on unrelated reads).
- 5-layer: `ComplianceReportArchive` valueholder → `ComplianceReportArchiveDAO`/Impl →
  `ComplianceReportArchiveService`/Impl (`@Transactional`, computes SHA-256, immutability guard:
  returns the existing row and skips save when one already exists for `(sample, amendmentNumber)`).
- Render the PDF into a buffer in `exportPdf` so the same bytes are both streamed to the client and
  handed to the archive (no double render).
- Column type: PostgreSQL `bytea` (the codebase is Postgres). LHU is text/table-only iText5, no
  embedded images → ~50–200 KB/PDF. At ~1–3 versions per released sample, even 10k samples ≈ a few GB
  accumulated over years — not a server-weight concern. If that ever matters, swap the `bytea` for a
  SHA-keyed filesystem/object-store blob and keep only the hash + metadata in the row; defer that
  until volume justifies it.
- Originals are immutable: never overwrite a prior archived row.

**T-503  Tests** — reissue/re-open does not mutate or replace the prior archived PDF (immutability
guard returns the existing row, never calls save); SHA-256 is stable, 64 hex chars, and differs for
different content; `amendment_number` keys versions separately; null amendment number treated as 0.

---

## Phase 6 — Template hook (DEFERRED to OGC-552) — AC6

**T-601  Confirm field contract with OGC-552**
- The §5.1.5 amendment-notice rendering is OGC-552's responsibility. OGC-776 only guarantees the
  three fields (`amends_lhu_number`, `amendment_number`, `amendment_reason`) are populated and
  exposed in the report DTO so the template can read them and skip the block when
  `amendment_number == null`. No rendering work in this ticket.

---

## Out of scope (deferred to v2, per ticket)

- Dedicated `superseded_by` schema pointer.
- `lhu.amend` permission key (any user who can generate the LHU may reissue).
- `LHU_AMENDED` audit event type.
- Manager-approval flow on amendment.
- Structured amendment-type taxonomy (reason is plain free text).
- Re-evaluation under a different decision rule when amending.

---

## Suggested delivery order

1. **Core slice (independent, low-risk M):** Phase 1 + Phase 2 + Phase 3 (capture/persist/dialog).
2. **Build (decided, interim):** Phase 4 — `/Am.N` suffix against the accession number.
3. **Build (decided, option b):** Phase 5 — PDF archival on release/reissue; metadata-only does not
   meet §7.8.8.
4. **Coordinate:** Phase 6 field contract with OGC-552.

## Pre-PR checklist

- [ ] `mvn spotless:apply` and `cd frontend && npm run format`
- [ ] Backend unit tests + frontend tests green; TDD order followed (red → green → refactor)
- [ ] Liquibase migration is idempotent (re-run = no-op) and registered in `base.xml`
- [ ] No hardcoded UI strings; new keys in `en.json` only
- [ ] `@Transactional` in services only; controllers thin
- [ ] **T-225** Jira comment posted correcting the certificate-number and PDF-preservation premises,
      and the generation-vs-release premise (draft: `JIRA-COMMENT.md`)
