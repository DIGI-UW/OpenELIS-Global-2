# Roadmap: True Collection Density (organisms per trap-night)

**Feature:** V-04 Vector Surveillance — effort-normalized collection density
**Status:** Planned (deferred from PR #3732; that PR ships the honest interim label)
**Related:** OGC-585 / V-04, PR #3732 (remediation)

## Context / problem

In vector surveillance, **"collection density" is a term of art = abundance
normalized to sampling effort** — organisms per trap per night (the WHO/CDC
standard; raw counts are "abundance"). The V-04 dashboard currently computes a
raw `SUM(sample_item.quantity)` per site/week with **no effort denominator**,
because the OE data model captures trap _type_ (`vecTrapTypeId` observation) but
never trap _count_ or _nights deployed_. Calling that "density" overstates it.

**Interim (shipped in #3732):** the panel is relabeled **"Specimen counts by
site"** — honest for raw counts. This roadmap is the full fix that restores a
true **"Collection density"** metric.

Sources: mosquitoes-per-trap-night is the standard effort metric; WHO 2021
Operational Manual on Aedes Surveillance; entomologic indices (density/trap-night,
Breteau/House).

## Design

Capture trapping effort at collection and divide by it.

1. **Capture** — add two fields to the vector sample-entry step (the form that
   already shows **Trap Type** / **Quantity in Pool** / **Lifecycle Stage**,
   reached via `/SamplePatientEntry` → `AddOrder`, per reagan):
   - **Traps deployed** (count, integer ≥ 1)
   - **Nights deployed** (count, integer ≥ 1)
     Their product is the collection event's **trap-nights** (sampling effort).
2. **Persist** — mirror the existing `vecTrapTypeId` mechanism: add
   `observation_history_type` rows (`vecTrapCount`, `vecTrapNights`) and store the
   entered values as `observation_history` on the sample — **no new table**, same
   pattern the collection form already uses for trap type + notes. (Alternative:
   dedicated columns on the vector collection row; the observation route is
   lower-friction and matches the current design.)
3. **Compute** — `getCollectionDensity` returns, per site/period,
   `SUM(quantity) / SUM(trap_count × nights)` (organisms per trap-night) alongside
   the raw count. Rename the panel back to **"Collection density"**; keep
   `data-testid="panel-density"` stable.
4. **Degrade cleanly (mandatory)** — effort data is optional/legacy-absent. When a
   period/site has **no** trap-effort captured, show the raw count with an
   explicit "effort not recorded" state — **never fabricate a density** (a
   divide-by-missing must degrade, per the config-driven "don't fabricate zeros"
   rule this feature already follows for positivity).

## Phases

- **Phase 0 — Capture schema:** `observation_history_type` Liquibase changeset for
  `vecTrapCount` + `vecTrapNights`; decide observation vs column (recommend
  observation, matching `vecTrapTypeId`).
- **Phase 1 — Intake UI:** add the two numeric fields to the vector sample-entry
  form beside Trap Type / Quantity in Pool; validation (integer ≥ 1); wire into
  the order submission payload; i18n keys in `en.json` only.
- **Phase 2 — Computation:** rewrite `VectorSurveillanceDAOImpl.getCollectionDensity`
  to join the effort observations and compute organisms/trap-night + carry the raw
  count; add the effort-absent degrade state to the DTO/service/dashboard; rename
  the panel label back to "Collection density."
- **Phase 3 — Tests + seed:** service unit test (density = specimens ÷ trap-nights,
  divide-by-missing degrades); integration test over seeded effort observations;
  extend `seed-vector-demo.sh` (distro) with trap counts + nights so the demo shows
  a real density; frontend render test for the panel + the effort-absent state.

## Critical files

- `src/main/resources/liquibase/3.5.x.x/…-vector-trap-effort.xml` (new — observation types)
- vector sample-entry component under `frontend/src/components/addOrder/…` (the
  Trap Type / Quantity in Pool form) + order-submission wiring
- `src/main/java/org/openelisglobal/reports/vectorsurveillance/daoimpl/VectorSurveillanceDAOImpl.java` (`getCollectionDensity`)
- `frontend/src/languages/en.json` (`vectorReport.density.title` → back to "Collection density", + effort labels)
- `frontend/src/components/reports/vectorSurveillance/VectorSurveillanceDashboard.jsx` + `vectorPdfGenerator.js`
- `openelis-indonesia-distro/scripts/seed-vector-demo.sh` (trap-effort in the seed)

## Verification

- Enter a vector collection with traps=N, nights=M, quantity=Q → density panel
  shows `Q / (N×M)` per trap-night for that site/week (unit + integration tests).
- A collection with no effort recorded → panel shows the raw count + "effort not
  recorded", not a fabricated density.
- Boot the dashboard on the demo with the enriched seed; density trend is
  effort-normalized across sites/weeks.

## Not in scope

MLE / Wilson CIs on density (separate LHU scope, OGC-552); retrofitting effort
onto historical collections (new collections only; historical show raw counts +
"effort not recorded").
