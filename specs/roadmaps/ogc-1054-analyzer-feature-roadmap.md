# Core analyzer remediation roadmap

Updated: 28 September 2026. Current work: [OE2 #4332](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/4332). The [implementation plan](analyzer-test-remediation-plan.md) is the single source for current status, the ordered work table, dependency/evidence identities and acceptance checks.

## Intended workflow

A laboratory selects a supported Bridge-shipped analyzer type, receives compatible default mappings from ordinary OE2 initialization, enters its site connection details and starts receiving results. Staff review and accept usable observations. An unknown or invalid observation retains its original data and explains what needs correction; unrelated valid observations continue. Correcting the affected mapping can recover the original observation without resending it or creating a duplicate.

Core OE2 and Bridge provide that reusable workflow. Madagascar qualification follows core qualification and checks only its distro/configuration differences.

## Current boundary

#4332 delivers the repaired test foundation, API-based clinical prerequisites, bounded catalog/default/display fixes and persistent harness storage. It targets `develop`; obsolete stack grouping is removed. Its latest published source is `df5335ed9b`. Final-commit checks are still required. No merge, deployment or complete analyzer qualification is claimed here.

The PR intentionally retains the released Bridge pin. It does not deliver the complete profile candidate, core FILE/HL7 qualification, original-result recovery closure, outage/replay or populated-upgrade proof. Those have explicit owners and acceptance in the implementation plan. Finishing #4332 is the first checkpoint, not completion of this roadmap.

## Ownership and durable decisions

Bridge owns immutable portable profiles, connection/runtime configuration, protocol execution, parsing, shared listeners, control recognition, directory watching/archive/error handling and durable delivery. OE2 owns lab-facing orchestration, the Bridge reference, local clinical catalog bindings, review/audit, activation intent, held results and operational QC. Do not duplicate Bridge runtime authority in OE2.

Profiles remain data-driven and reusable. Generic production code must not special-case a manufacturer, model, profile ID or analyzer code. Instrument evidence determines default meaning and supported specimen/units. Site-specific customization remains supported; a fixture must not silently compensate for a broken supplied default.

Operational QC and connection probes must not create blanket activation/mapping gates. Unknown results must remain recoverable. Independent usable observations should continue through normal review.

Upgrades preserve original analyzer IDs, clinical records and migration inputs until transfer is verified. Post-startup service/API migration may be rerun without duplicating Bridge connections; database startup must not depend on Bridge availability. Destructive cleanup is a later explicit decision after upgrade evidence.

## Work ownership

- **R0 / #4332:** faithful tests, bounded OE2 defaults/display and persistent harness storage; require exact-commit CI.
- **R5 / Bridge #69 and linked core consumers:** qualify reusable profiles, the additive contract, shared-listener setup, FILE/HL7 and default meanings before consuming release pins.
- **R1 / #4448:** original-result retry, held-sibling retention, specimen correction and truthful delivery errors. Include the original QC-lot recovery and unsaved-work preservation checks carried forward from closed #4256.
- **R2 / #4449:** invalid mappings affect their observations instead of holding every result for the analyzer.
- **R3:** simple setup/correction through the normal workflow without exposing internal revision management as operator work.
- **R4 / #4347 and #4421:** queue retention, restart/retry/replay and duplicate prevention; upgrade and populated-catalog reconciliation retain their existing service owners.
- **R6:** review useful unmerged deltas, preserve their assertions and close superseded PRs with evidence; do not merge #4336 wholesale over newer recovery behavior.
- **R7:** full core workflow and reviewed-video qualification on recorded component builds.
- **R8:** Madagascar packaging and limited configuration verification after the core candidate passes.

These labels identify capability ownership. Follow the single ordered table in the implementation plan; do not treat this list as a second merge train.

## Acceptance and test boundaries

Use production APIs for synthetic clinical prerequisites and independent persisted-result readback. Use the visible UI for the setup/review/correction/activation/acceptance actions being claimed. Native mock traffic crosses the real Bridge transport. No analyzer SQL feature fixture, mapping-repair script or replacement internal service can manufacture acceptance.

Passing evidence names the expected patient, order, specimen, test, units/value and result count independently of the application's selected mapping. Retry and repeat prove no additional clinical result. Runtime/persistence tests cover lower-level contracts without turning every edge case into a browser scenario.

CI and recordings execute the same registered scenarios and assertions. Present accessible reviewed videos with their exact source/profile/image identities and observed limits. A passing old recording is not final-commit qualification. Required checks, merge, deployment, publication and human acceptance are distinct states.

The complete release gate remains working core ASTM/FILE/HL7, correction of original retained observations, independent valid-result progression, durable outage/replay and populated upgrades. The implementation plan states which belong to the bounded current PR and which are follow-ups.
