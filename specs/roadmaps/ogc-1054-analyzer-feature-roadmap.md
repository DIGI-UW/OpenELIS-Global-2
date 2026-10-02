# Analyzer roadmap (OGC-1054)

The single roadmap for OE2 analyzer work: the intended workflow, who owns what,
where things stand, and the ordered remaining work.

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

## Current state (30 September 2026)

On `develop`:

- The faithful test foundation and harness catalog: #4332 (`e0c98726ba`) and
  its review corrections #4470 (`7d94adf63e`).
- Held-result retention and recovery, specimen correction and truthful delivery
  errors: #4448 (`2aec1c10da`), with #4485 (`df63bf123a`) keeping a grouping's
  new tests on a specimen they can all use.
- Invalid mappings hold only their own observations: #4449 (`c2aed566ff`).
- The `/analyzer/fhir` response carries the delivery receipt reference: #4347
  (`3f42f5615d`).
- Pins: Bridge 3.2.4 (`1eff7b4`; GeneXpert profile revision 7, FluoroCycler
  revision 4) and mock 0.1.3 (`8c64750`). Source pins and image tags match.

The analyzer harness in CI runs these result workflows with independent
clinical readback: GeneXpert ASTM results for MTB, the three RIF outcomes, HIV
viral load and COVID positive and negative; two GeneXpert instruments on one
shared listener; a usable result accepted beside a held sibling that later
recovers; an invalid RIF binding that holds only RIF; and FluoroCycler numeric
watched-file import. It also runs checks of the shipped clinical defaults, the
dismissal of an undelivered result in the UI, and the guided setup demos.

Not yet covered: further FluoroCycler exports, assays, units and status/control
semantics; a shipped core HL7 profile; OE2-side outage, restart and replay;
populated-catalog reconciliation; and upgrade from a supported previous version.
Bridge 3.2.5 (released 30 September) adds only #70: the image keeps its outbox
and FILE state on its data volume by default. Bridge 3.2.6 (released 1 October)
requires HTTP Basic on every Bridge endpoint except the health status and
removes `POST /` and `POST /api/query` (Bridge #73); it needs OE2 3.2.3.0 or later.
OE2 still pins 3.2.4; #4497 repins to 3.2.6.

## Remaining work, in order

| Order | Work | Next step | Acceptance |
| ----- | ---- | --------- | ---------- |
| 1 | Broader FILE and HL7 qualification | Qualify additional supported FluoroCycler exports/assays, units and status/control semantics, plus a shipped core HL7 profile. | UI directory configuration reaches Bridge watching; native files and HL7 messages save correct clinical values. Archive/error retention is verified. No distro mount or fabricated concentration makes the test pass. |
| 2 | Durable delivery and upgrade | Repin OE2 to Bridge 3.2.6 (#4497, OGC-1409; it includes 3.2.5). Prove OE2 queue outage, restart and replay on the current traffic helper, and operator retry after another transient failure (after #4421). Restore mock attachment after Bridge replacement. Reconcile populated catalogs and upgrade a supported previous version through the catalog-loading services; pre-Bridge analyzer settings are exported and their storage removed by changeset `114-remove-pre-bridge-analyzer-storage` (`docs/analyzers/pre-bridge-settings-export.md`). | Retained pending messages deliver once after recovery; processed FILE inputs are not duplicated. Existing analyzer IDs and clinical history survive upgrade, and exported pre-Bridge settings are readable. No SQL feature setup or resend is needed for claimed recovery. |
| 3 | Core qualification | Run every supported workflow and present recordings from the same registered tests. | Every supported ASTM/FILE/HL7 workflow and required recovery/upgrade scenario has passing independent readback, exact image identities and accessible reviewed video. |
| 4 | Madagascar distro | Publish qualified core versions, update distro pins and run limited packaging/configuration checks. | The distro consumes working core profiles/defaults without site-specific mapping repair. Its checks prove packaging differences, not replacement core acceptance. |

## Deferred review items

Open findings from the #4332 review. None justifies changing clinical semantics
without evidence.

| ID | Finding | Owner | Follow-up acceptance |
| -- | ------- | ----- | -------------------- |
| F-REV1 | Harness filesystem catalogs suppress bundled Horiba CBC/vector CSVs for those domains. Confirmed loader behavior; intended coverage needs an explicit decision. | FILE/HL7 qualification (order 1) | State the supported corpus; qualify additional profiles against its normal catalog load and native clinical readback. Missing coverage stays visible. Preserve the documented site-override contract. |
| F-REV2 | Transactional TRUNCATE can retain locks that block a second connection or `REQUIRES_NEW` work. Credible risk; no failing case demonstrated yet. | Backend test infrastructure | Reproduce the cross-connection case; fix fixture isolation/transaction ownership and verify it finishes or fails diagnostically without hanging a job. Do not solve it by lengthening timeouts. |
| F-REV3 | A single local-code candidate bypasses specimen disambiguation. Behavior confirmed; a defect is not yet established. | Populated-catalog reconciliation (order 2) | Define identity/update semantics with existing catalog-loader expectations; cover valid extension and conflicting identity. Add a restriction only if that contract requires it. |
| F-REV4 | Repeated `SpringContext.getBean` access and reference-table lookup add indirection and query work. No functional failure or measured performance problem. | OE2 service maintenance | Prefer injected/context-scoped dependencies where appropriate; verify lifecycle/context correctness and query reduction. Do not restore stale static caches. |
| F-REV5 | The historical 2.3.x seed assigns the COVID LOINC to HIV viral-load variants. Deployed effective values need a separate audit. | General catalog correction | Verify affected records and intended concept/specimen/unit identities; apply a narrowly scoped supported correction preserving IDs, history, report labels and site activation intent. Prove fresh and populated cases. |
| F-REV6 | Duplicate raw values render one hint editor because hints are keyed by raw value. | Profile authoring | Explain or reject duplicate value entries visibly while preserving valid values and hints; do not create a second hint contract. |

## Open related pull requests

| PR | Disposition |
| -- | ----------- |
| OE2 #4421 | Persists Retry/Dismiss of undelivered results in the audit trail. Open. |
| OE2 #4497 | Repins to Bridge 3.2.6 and publishes the harness Bridge API on localhost only. Distro repins follow it (OGC-1409). |
| OE2 #4072 | Worktree-isolated development stack. Reconcile it with `scripts/dev-stack`; keep CI and interactive entry points separate without competing launchers. |
| OE2 #3974 | TypeScript migration of analyzer forms. September's rework removed 17 of its 18 target components; close it, or narrow it to `AnalyzerTypeManagement.jsx`. |
| Madagascar test harness #4, #9, #10, #11 | After core qualification: reconcile #4 with merged #15; narrow #10; review the outbound proof in #9 and its child #11. |
| Review tooling #16, #31 | Compare #16 with merged #17; update #31's evidence manifests to the tested pins. |

## Acceptance and test rules

- Start the ordinary core application, catalog and Bridge-shipped profiles
  through `scripts/dev-stack`; use CI's supported runner for CI parity.
- Prepare synthetic patients, orders and specimens through existing validated
  APIs, and read persisted results back independently. Passing evidence names
  the expected patient, order, specimen, test, units/value and result count
  independently of the mapping the application selected.
- Exercise the visible UI for any setup, confirmation, correction, activation
  or acceptance the story claims. Supplemental API readiness/readback must not
  perform the behavior being demonstrated.
- Native mock traffic crosses the real Bridge transport. No analyzer SQL
  feature fixture, mapping-repair script or replacement internal service can
  manufacture acceptance. Do not select, exclude or repair analyzer mappings in
  fixtures.
- Retry and repeat prove no additional clinical result. Runtime and persistence
  tests cover lower-level contracts without turning every edge case into a
  browser scenario.
- CI and recordings execute the same registered scenarios and assertions.
  Present accessible reviewed videos with their exact source/profile/image
  identities and observed limits. A passing old recording is not final-commit
  qualification.
- Track code, CI, merge, deployed build, publication and human acceptance as
  separate states. Do not describe a PR as merge-ready while required checks are
  failing or unknown.
