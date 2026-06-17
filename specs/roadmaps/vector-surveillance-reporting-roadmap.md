# Roadmap: Vector Surveillance Reporting (OGC-585 / V-04) + Reporting-FHIR Architecture

**Date:** 2026-06-15
**Owner:** Piotr Mankowski
**Target branch:** `demo-silnas` (SILNAS Indonesia demo) — **NOT develop** (see §1)
**Status:** Scoping complete. Spec (`/speckit.specify`) pending the decisions below.

> Purpose: capture the 2026-06-15 investigation so the context isn't lost. This
> is the source of truth for *why* V-04 is scoped the way it is. The committed
> design FRS and the Jira ticket trail disagree (see §5); **this roadmap
> supersedes both for scoping intent.**

---

## TL;DR (what was decided)

1. **Target is `demo-silnas`.** This PR series lands there. develop / upstreaming
   is explicitly out of scope for now.
2. **Build the dashboard over OpenELIS's own data.** The surveillance reporting
   feature is computed directly from the vector tables already on `demo-silnas`.
3. **Park the FHIR/Superset/OHS-ETL pipeline.** It has no current consumer (see
   §4). It is not built and not decided anywhere — do not build it speculatively.
4. **National submission = v1.5 Manual Entry Helper** (copy-to-portal + audit),
   NOT the v1.4 CSV exporters. The expert (Ida, APHL Indonesia) confirmed
   SILANTOR is manual entry and CSV ingestion is unused.
5. **Read model is deferred to `/speckit.plan`** — spec stays read-model-agnostic.
   Default lean: OLTP-direct / materialized views in OE's own schema.
6. **OGC-586 (FHIR architecture review) is NOT a blocker** for the demo dashboard
   under this scope. It only matters if/when a real external consumer appears.

---

## 1. Scope boundary

- **demo-silnas is the working trunk.** The entire vector/env/compliance module
  (V-01 OGC-555, V-02 OGC-581, V-03 OGC-582, compliance, LHU, sampling-site —
  ~47 files under `org.openelisglobal.vector`) lives **only** on `demo-silnas`
  (0 of these files exist on develop). So V-04's data-model dependencies are
  present on the target branch.
- For this work, ignore develop. Upstreaming the module is a separate, large
  effort and is not part of OGC-585.

---

## 2. What OGC-585 / V-04 actually is (pure functionality)

> Turn raw vector/environmental lab data into surveillance indices — then show
> them, alert on them, and get them out to the people who need them.

| Capability | Notes | Source |
|---|---|---|
| Computed indices: collection density, species distribution, **MIR**, pathogen positivity, QC pass-rate | QC samples excluded from surveillance numbers | US-V04-01/04/05; Jira v1.2 |
| Filter by date range + sampling site; charts move together | | US-V04-02 |
| Trust indicators on the math: classic MIR + deconvolution-aware infection rate + resolution %, sporozoite rate, (Wilson CI/MLE) | | Jira v1.3/v1.4/5-26 |
| Export report to PDF | | US-V04-03 |
| Threshold alerts (outbreak early-warning) | | US-V04-06 |
| **National-system submission** (SILANTOR) — **v1.5 Manual Entry Helper** | copy-to-portal + "mark submitted" audit + admin field-map | Jira v1.5 |
| Role-gated access (+ optional site-scoping, deferrable) | | US-V04-08 |

~90% of this (compute → show → alert → PDF) is **data-source-agnostic** and
identical across every spec revision. Only the "submit to national system" slice
ever cared about a data boundary.

---

## 3. Codebase reality (verified 2026-06-15, demo-silnas)

| | Built? | Evidence |
|---|---|---|
| Generic FHIR result push plumbing (clinical) | ✅ Built | `FhirTransformService` (33 files), `transformResultToDiagnosticReport`/`...ToObservation`/`...ToSpecimen`, `FhirPersistanceService.createFhirResourcesInFhirStore`, `EQAFhirSubmissionService`; `fhir_uuid` in 81 files |
| **Superset** integration | ❌ Zero | no code/config/compose anywhere |
| OHS ETL / sql-on-fhir / guest-token embed | ❌ Zero | no matches |
| **Any** surveillance/disease dashboard | ❌ Zero | no matches |
| Vector → FHIR push | ❌ Zero | no FHIR code references `org.openelisglobal.vector`; vector entities carry no `fhir_uuid` |

**Conclusion:** the only thing built is generic clinical-result FHIR plumbing.
Every surveillance-reporting capability (Superset, ETL, dashboards, vector FHIR)
is **0% built and 0% decided** — in demo-silnas and the main app.

---

## 4. Why FHIR got entangled (the core architecture finding)

- A **report over OE's own data does not need FHIR.** FHIR only earns its keep at
  a **boundary** — when something that *isn't* OpenELIS needs to read structured
  OE data. The v1.4 FRS manufactured a boundary *inside one system* (push to FHIR
  → read back via OHS ETL → Superset) and made it the dashboard's data path. That
  is the over-engineering.
- **No current external consumer exists:** the dashboard reads OE directly;
  national reporting is manual entry (Manual Entry Helper); SATUSEHAT / DHIS2 /
  national surveillance integration was descoped ("we have no integration scope",
  "Surveillance was removed from our scope" — janflowers, 2026-04-22).
- Therefore the FHIR pipeline is **parked** until a real external consumer
  mandates it. If/when one does, it follows the established push pattern
  (`FhirTransformService` + `fhir_uuid` + EQA-submission), not a bespoke stack.

---

## 5. Why it looked like a "mess" (drift + aspiration, NOT a decided architecture)

- **Spec drift:** the committed FRS in `DIGI-UW/openelis-work` froze at **v1.4**
  (2026-04-30). The Jira ticket trail advanced to **v1.5** (Manual Entry Helper
  replacing CSV exporters, 5/11) + a **5/26** Wilson-CI/MLE addition. The v1.5
  spec + mockups were **never pushed to the repo** — they exist only as Jira
  comments. Source of truth is split.
- **Aspirational, inconsistent specs — not a settled pattern.** Multiple *unbuilt*
  backlog epics each assume "Superset reads central FHIR," but disagree in detail
  (OGC-585 invented HAPI+OHS-ETL; OGC-435 says "Superset reads FHIR directly";
  OGC-918 says push-to-consolidated-then-external-aggregate). Repetition across
  unbuilt specs is **not** an architecture decision. Nothing is ratified; nothing
  is built.

---

## 6. The Jira cluster & related work

**The three SILNAS tickets (epic OGC-527, all assigned to Piotr):**

| Ticket | What | Status |
|---|---|---|
| **OGC-585** | V-04 Vector Surveillance Reporting (this feature) | Selected for Dev |
| **OGC-586** | V-04 FHIR architectural review (7 open FHIR/ETL questions) | Selected for Dev |
| **OGC-592** | S-05b Final Storage Disposition FHIR Publishing (ENV/Vector) | Selected for Dev |

**Related (aspirational, unbuilt) surveillance-via-FHIR work — inputs, not deps:**
OGC-435 (Disease Surveillance Dashboard — FHIR + Superset), OGC-918/919/920 (M-15
GLASS via consolidated FHIR), OGC-440/441 (event-driven FHIR push to central
HAPI), OGC-897 (Management Dashboard aggregated-FHIR boundary), OGC-553/602
(Environmental dashboard + chart export). OGC-530 (Done) wired analyzer metadata →
FHIR → Superset — the only built precedent of a FHIR→Superset path.

---

## 7. Open architecture decisions (TO SETTLE — genuinely undecided)

These are decisions **to make**, not patterns to ratify. The aspirational epics
above are inputs to reconcile.

1. **Internal vs external reporting boundary** — which reporting is OE-internal
   (read OE data) vs external/interoperable (push FHIR to a real consumer).
2. **FHIR persistence parity** — is "OE persists vector/env data to FHIR" a
   *standalone* requirement (canonical record / interop), independent of
   reporting? Today: **no** (vector has no `fhir_uuid`, no transform). Decision
   gates OGC-586/592.
3. **Reporting read-model / CQRS** — OLTP-direct → materialized views → dedicated
   CQRS read store → FHIR-as-read-model. (External surveillance, *if* built, would
   imply FHIR-as-read-model; internal is open.)

---

## 8. Phased roadmap (demo-silnas)

- **Phase 0 — Lock the spec (no code).** `/speckit.specify` OGC-585 as the
  *internal* reporting feature: read-model-agnostic, FHIR-decoupled, v1.5 Manual
  Entry Helper. Re-baseline one authoritative spec (merge committed v1.4 + Jira
  v1.5 / 5-26 deltas) so the drift is closed. Recover the real v1.5 mockup from
  Casey in parallel (not blocking).
- **Phase 1 — Analytics layer (backend).** Read-only aggregations over existing
  vector tables (density, species, MIR classic + deconvolution-aware, positivity,
  QC). MIR/CI math in SQL/Java. `vectorReport.view` permission.
- **Phase 2 — Dashboard UI (frontend).** Reports → Vector Surveillance Carbon
  page: date/site filters, charts, loading/empty states, PDF/print.
- **Phase 3 — Manual Entry Helper.** v1.5 helper screen + admin field-map + audit.
- **Parked (separate stories, gated on a real external consumer + an actual ADR):**
  FHIR push + HAPI/OHS/Superset infra (OGC-586/592), RLS, in-app alerts (V-04b),
  MLE (V-04c), national API push (V-04d).

Each phase = one reviewable PR onto `demo-silnas`.

---

## 9. Proposed tickets (lean — pending Piotr's OK; nothing created in Jira yet)

1. **NEW — ADR ticket: "Surveillance & Reporting architecture is unbuilt and
   undecided."** Records the open questions in §7, lists the aspirational backlog
   specs (435/918/585/592/440/897) as inputs, and states the decision is *to be
   made*. Home: cross-cutting (candidate: OGC-897, or a small platform epic) —
   NOT under OGC-527.
2. **REFRAME OGC-585** → internal vector surveillance reporting, read-model-
   agnostic, FHIR-decoupled, v1.5 Manual Entry Helper.
3. **REFRAME OGC-586** → "extend established FHIR push to vector resources *if/when*
   an external consumer is confirmed," explicitly downgraded from blocker.
4. **LINK** OGC-585/586/592 ↔ 435/440/918/897 as one family (kills recurrence of
   this confusion).

Deferred: a separate "internal reporting read-model / CQRS" decision ticket only
if/when internal reporting load demands it.

---

## 10. Remediation: close the source-of-truth split

The committed FRS (v1.4) and the Jira trail (v1.5 + 5/26) disagree, and the v1.5
mockups were never pushed. As part of Phase 0, re-baseline a **single**
authoritative spec under `specs/vector-surveillance/` (or this feature's SpecKit
dir) and treat it as source of truth going forward. Ask Casey for the real v1.5
mockup; if unavailable, reconstruct from the (detailed) Jira v1.5 comment.

---

## Appendix — key evidence

- **Slack (#digi-pm, 2026-04-22, Piotr):** OGC-586's 7 FHIR decisions gate V-04
  (585) and S-05b (592); proposed a 90-min decision session with a FHIR-stub
  fallback. **janflowers:** "we have no integration scope", "Surveillance was
  removed from our scope."
- **Design repo:** `vector-surveillance-reporting.md` last commit 2026-04-30
  (v1.4); repo otherwise active through 2026-06-12 → v1.5 deltas never landed.
- **Code (demo-silnas):** generic FHIR plumbing built; Superset/OHS/dashboards/
  vector-FHIR = zero (§3).
