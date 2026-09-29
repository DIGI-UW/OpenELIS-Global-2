# Core analyzer remediation roadmap

Updated: 29 September 2026. [OE2 #4332](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4332)
merged into `develop` as `e0c98726ba`. Current work is its bounded review
follow-up [#4470](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4470),
then [#4448](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4448)
and [#4449](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4449). The
[implementation plan](analyzer-test-remediation-plan.md) is the single source
for current status, the ordered work table, dependency/evidence identities and
acceptance checks.

The
[review disposition and acceptance](analyzer-test-remediation-plan.md#review-disposition-and-pre-merge-acceptance)
records the pending #4332 corrections from the review at `760536edb8`. The
[deferred review ledger](analyzer-test-remediation-plan.md#explicitly-deferred-review-items)
retains catalog coverage, fixture locking, catalog identity, service lookup,
historical concept and profile-authoring follow-ups with owners and closure
criteria. These are one maintained plan, not a separate merge train.

Immediate merge sequence: **#4470 → #4448 → #4449** in
[GitHub stack #4472](https://github.com/DIGI-UW/OpenELIS-Global-2/stack/4472).
All three branches include current `develop` (`e0c98726ba`); each upper
branch contains and targets its predecessor. Keep the stack synced when
`develop` changes and retain only each PR's unique changes. Broader
profile, outage/replay and upgrade qualification does not block these bounded
PRs; their focused acceptance and current-head CI still apply.

## Intended workflow

A laboratory selects a supported Bridge-shipped analyzer type, receives
compatible default mappings from ordinary OE2 initialization, enters its site
connection details and starts receiving results. Staff review and accept usable
observations. An unknown or invalid observation retains its original data and
explains what needs correction; unrelated valid observations continue.
Correcting the affected mapping can recover the original observation without
resending it or creating a duplicate.

Core OE2 and Bridge provide that reusable workflow. Madagascar qualification
follows core qualification and checks only its distro/configuration differences.

## Current boundary

#4332 merged the repaired test foundation, API-based clinical prerequisites,
harness-owned catalog CSVs loaded by OE2's normal configuration service, bounded
default/display fixes and persistent harness storage. The harness CSVs are not
packaged as general OE2 catalog changes. Its obsolete stack grouping was
removed; the new remediation stack starts at #4470. GitHub checks passed at
`760536edb8`, including downstream
E2E. The completed local run passed backend, frontend and both analyzer suites,
but failed core Playwright and two Cypress shards. The review follow-up must
resolve that discrepancy and its nine accepted corrections. No deployed or
complete analyzer qualification is claimed here.

Bridge and mock source and image pins now match released Bridge 3.2.4 and mock
0.1.3. Mock 0.1.3 includes the cross-user FILE permissions fix from merged #50.
Source-built test validation and published-image deployment remain distinct. The
profile/default and shared-listener integration landed in #4332. The numeric
FluoroCycler FILE workflow passes through UI directory setup, Bridge watching
and native workbook traffic, with independent clinical result readback.
Additional FILE assay semantics, complete HL7 qualification, original-result
recovery closure, outage/replay and populated-upgrade proof retain their
follow-up owners. Those have explicit owners and acceptance in the
implementation plan. Merging #4332 was the first checkpoint, not completion of
this roadmap.

## Ownership and durable decisions

Bridge owns immutable portable profiles, connection/runtime configuration,
protocol execution, parsing, shared listeners, control recognition, directory
watching/archive/error handling and durable delivery. OE2 owns lab-facing
orchestration, the Bridge reference, local clinical catalog bindings,
review/audit, activation intent, held results and operational QC. Do not
duplicate Bridge runtime authority in OE2.

Profiles remain data-driven and reusable. Generic production code must not
special-case a manufacturer, model, profile ID or analyzer code. Instrument
evidence determines default meaning and supported specimen/units. Site-specific
customization remains supported; a fixture must not silently compensate for a
broken supplied default.

Operational QC and connection probes must not create blanket activation/mapping
gates. Unknown results must remain recoverable. Independent usable observations
should continue through normal review.

Upgrades preserve original analyzer IDs, clinical records and migration inputs
until transfer is verified. Post-startup service/API migration may be rerun
without duplicating Bridge connections; database startup must not depend on
Bridge availability. Destructive cleanup is a later explicit decision after
upgrade evidence.

## Work ownership

- **R0 / #4332 and review follow-up:** faithful tests, stable harness catalog through the normal
  loader, OE2 default/display fixes, current Bridge integration and persistent
  harness storage; require exact-commit CI and matching workflow evidence.
- **R5 / Bridge #69 and linked core consumers:** qualify reusable profiles, the
  additive contract, shared-listener setup, FILE/HL7 and default meanings before
  consuming release pins.
- **R1 / #4448:** original-result retry, held-sibling retention, specimen
  correction and truthful delivery errors. Include the original QC-lot recovery
  and unsaved-work preservation checks carried forward from closed #4256.
- **R2 / #4449:** invalid mappings affect their observations instead of holding
  every result for the analyzer.
- **R3:** simple setup/correction through the normal workflow without exposing
  internal revision management as operator work.
- **R4 / #4347 and #4421:** queue retention, restart/retry/replay and duplicate
  prevention; upgrade and populated-catalog reconciliation retain their existing
  service owners.
- **R6:** review useful unmerged deltas, preserve their assertions and close
  superseded PRs with evidence; do not merge #4336 wholesale over newer recovery
  behavior.
- **R7:** full core workflow and reviewed-video qualification on recorded
  component builds.
- **R8:** Madagascar packaging and limited configuration verification after the
  core candidate passes.

These labels identify capability ownership. Follow the single ordered table in
the implementation plan; do not treat this list as a second merge train.

## Acceptance and test boundaries

Use production APIs for synthetic clinical prerequisites and independent
persisted-result readback. Use the visible UI for the
setup/review/correction/activation/acceptance actions being claimed. Native mock
traffic crosses the real Bridge transport. No analyzer SQL feature fixture,
mapping-repair script or replacement internal service can manufacture
acceptance.

Passing evidence names the expected patient, order, specimen, test, units/value
and result count independently of the application's selected mapping. Retry and
repeat prove no additional clinical result. Runtime/persistence tests cover
lower-level contracts without turning every edge case into a browser scenario.

CI and recordings execute the same registered scenarios and assertions. Present
accessible reviewed videos with their exact source/profile/image identities and
observed limits. A passing old recording is not final-commit qualification.
Required checks, merge, deployment, publication and human acceptance are
distinct states.

The complete release gate remains working core ASTM/FILE/HL7, correction of
original retained observations, independent valid-result progression, durable
outage/replay and populated upgrades. The implementation plan states which
belong to the bounded current PR and which are follow-ups.
