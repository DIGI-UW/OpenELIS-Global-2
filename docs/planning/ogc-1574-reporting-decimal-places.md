# Reporting decimal places remediation

OGC-1574 separates the clinical result from optional presentation formatting
throughout OpenELIS. The analyzer acceptance fix is the prerequisite PR #4877.
This follow-up implements the policy below; deployment and laboratory acceptance
are separate from code completion.

## Contract

The laboratory configures **Reporting decimal places** on a numeric test result
component, in the context of its unit and method. Unset means preserve the
supplied representation. Zero explicitly requests whole-number presentation. A
positive integer requests that many digits after the decimal point. This is not
significant figures, measurement accuracy, or an input constraint.

Preserve the original analyzer receipt and the clinical result. Corrections and
calculations have their own audit and provenance. Reporting decimal places must
not affect persistence, calculation inputs or outputs, normal/critical/validity
flags, acknowledgements, notifications, compliance decisions, or clinical FHIR
quantities. A clinical procedure that requires numerical processing must define
it explicitly rather than borrowing this setting.

Editing uses the full clinical value; presentation cannot be submitted back as
an unchanged edit. Where presentation obscures a threshold crossing, show the
value used to determine the flag. Qualified values such as `<1.17` must never
acquire a different bound through formatting. Scientific notation must not
silently reinterpret decimal places as significant figures.

## Implementation tasks

1. Rename active Java/JavaScript fields and catalog contracts to
   `reportingDecimalPlaces`; rename database columns to
   `reporting_decimal_places` with a reversible Liquibase migration. Keep
   historical migrations intact. Accept legacy configuration column names at the
   import boundary during transition, rejecting multiple headers for the same
   setting.
2. Remove implicit zero defaults from database seeds, migrations, objects, forms
   and imports. Clearing a configured value must persist as unset; omission in
   an update preserves the existing setting.
3. Remove formatting from analyzer/manual saving, calculations, dilution,
   clinical flags, acknowledgements, report flags and compliance decisions. Keep
   machine-readable exports and FHIR clinical values unrounded.
4. Give formatted values explicit method names and share a presentation
   formatter across screens and reports. Remove export truncation. Keep authored
   reference bounds intact. Separate editable values from display values and
   explain visible rounding without calling a valid measurement invalid.
5. Import an explicit reporting count only under a documented field contract.
   `allow_decimal` describes whether fractions are permitted and cannot
   determine a decimal count. Do not infer counts from a Boolean, missing
   metadata, or an ambiguously named external precision field.
6. Preserve historical result values and issued reports. Inventory existing zero
   configurations with their test, component and unit for laboratory review; do
   not bulk-clear authored values or attempt to reconstruct lost digits.
7. Validate unset/0/1/2 settings using the same measurements, including
   threshold boundaries, editing round trips, calculations, report/export paths,
   scientific notation and comparators. Verify migration on existing and fresh
   databases, rollback, and configuration clear/reload behavior. Use the
   repository standard local test commands and CI runner; use
   analyzers.openelis-global.org for subsequent deployed workflow validation.

## Compatibility and ownership

The canonical setting is `reportingDecimalPlaces`. Legacy
`significantDigits`/`decimalPlaces` configuration headers are input aliases, not
separate settings. Imported OCL `extras.decimal_places` is an OpenELIS-specific
decimal-place contract, not an OCL-wide standard. Local catalog approval remains
authoritative. The Java/JSON field and database column rename requires a
coordinated backend/frontend upgrade. Roll back the Liquibase change before
running an older application image.

The nullable setting copied to a result is a reporting-policy snapshot, not a
second clinical measurement. Catalog changes must not rewrite historical values
or issued reports. Existing zero settings remain until reviewed because stored
data cannot reliably distinguish an intentional zero from an old fallback.

OGC-1569 tracks the broader result API round-trip overwrite issue; this work
must verify that its own numeric edit/display boundary cannot recreate that
defect. Keep coordination with that ticket without creating another rounding
ticket. Legacy catalog entry points touched by this refactor must use the same
contract rather than develop a parallel policy.

## Verified defects motivating the change

- The field called `significantDigits` rounds decimal places.
- The base database defines zero defaults for result and test-result formatting,
  and includes explicit zero test configurations, including creatinine.
- The old OCL mapper defaults to zero, invents two places from text
  `allow_decimal=true`, and ignores JSON Boolean values.
- Several clinical flags and acknowledgement paths use rounded presentation
  values; report queue flags have the same coupling.
- Calculated result storage uses the reporting count to round its output.
- A numeric data-export formatter truncates fractions for a zero count.
- Generic result-value getters return formatted strings to callers, obscuring
  the clinical/presentation boundary.

## Evidence required for acceptance

Changing only reporting decimal places must leave clinical storage,
calculations, flags, acknowledgement requirements and FHIR quantities unchanged.
Presentation is the only permitted difference. An unchanged edit must retain the
full value. Empty settings must survive imports and save/reload. Upgrades must
preserve rows and explicit settings while removing implicit defaults. Historical
zero review and laboratory approval are required rollout work, not claims that
can be established by automated tests.

## Delivery ledger — 29 October 2026

PR #4882 implements the plan above and is ready for review, above #4877. The
application candidate at `8d6a04f678` is deployed on
<https://analyzers.openelis-global.org>. The published images were built by
GitHub Shared Build 40921113116 from integrated commit `f9e09afd988d`, which
also includes newer upstream changes; it is not the same tree as the PR head.
The acceptance-test and ledger follow-up does not change the deployed
application.

| Plan item                               | Verified evidence                                                                                                                                                                                                                                                                                                                                    | Remaining work                                                                                           |
| --------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------- |
| Canonical setting and nullable defaults | All three deployed columns are `reporting_decimal_places`, nullable, with no defaults. Upgrade/rollback regressions pass. The pre/post-upgrade zero inventory is identical.                                                                                                                                                                          | Review and merge coordinated backend/frontend changes; rollback migration before using an older image.   |
| Clinical values and decisions           | 248 focused backend tests pass, covering calculations, dilution, flags, imports, migration and FHIR. On analyzers, `6.004` remains critical above `6` with unset/0/1/2 presentation settings, including entry acknowledgement, validation and report queue.                                                                                          | Complete broad committed-revision CI and laboratory policy review.                                       |
| Edit/display boundary                   | Four analyzer scenarios pass locally and remotely: synthetic ASTM traffic through mock and Bridge retains receipt `1.176`; unchanged edits retain it; explicit corrections save `1.18` with audit provenance; release preserves the selected clinical value. Manual browser inspection confirms a display of `1` opens an editor containing `1.176`. | Laboratory acceptance.                                                                                   |
| Reporting transparency                  | Four additional boundary scenarios pass locally and remotely. Real patient PDFs were generated and visually inspected for all settings: `6.004`, `6`, `6.0`, `6.00`; formatted reports retain the critical flag and explain the clinical value `6.004`. The annotation wraps in the result cell without clipping.                                    | Team review of presentation and report wording.                                                          |
| Configuration import and clear/reload   | Focused import tests cover explicit `extras.decimal_places` and legacy CSV aliases. Each deployed boundary case clears an authored count and separately reloads it as unset before applying its requested setting.                                                                                                                                   | Review local catalog policy.                                                                             |
| FHIR representation                     | The deployed authenticated OpenELIS FHIR API returns a final Observation with `valueQuantity.value = 1.176` for a zero-decimal result.                                                                                                                                                                                                               | Direct read-back from the downstream FHIR store remains unverified.                                      |
| Historical zero settings                | All 358 pre-existing zero rows across 117 tests survive upgrade unchanged (116 component rows, 242 legacy rows), before acceptance tests add their own isolated catalog rows.                                                                                                                                                                        | Laboratory review must decide which settings are intended; some rows contain nonnumeric legacy metadata. |

The final combined server run passed **9 tests**: authentication plus four
analyzer workflows and four threshold/report workflows, with no failures, skips
or retries. These are synthetic measurements sent through the real transport and
application; they are not physical-instrument acceptance. The full frontend
suite passed **4,233 tests**, with five skipped. Playwright lint and diff
whitespace checks passed for the added acceptance coverage.

Broad local CI is not yet a pass: the committed-revision run encountered a
PostgreSQL cancellation during `AnalysisServiceTest` fixture cleanup. A separate
standard Maven rerun passed all 92 tests in that class, but does not erase the
aggregate failure. The existing core patient-report test could not create an
order on analyzers because its fixture referring site `9000100` is absent; its
local prerequisite also assumes a fixed database container name. Those attempts
did not reach report generation and are not counted as passed. The new isolated
catalog/order scenarios did generate and inspect the actual deployed reports.

At the last application-head check, GitHub's frontend checkpoint passed; backend
and end-to-end checks were still running. `Validation / Submodule pins` fails
because the inherited Bridge commit requires upstream PR #115 to merge. No PR
has been merged by this task. A ready-for-review PR is not a green CI or
laboratory acceptance claim.

Deployment used the existing site tooling and published image digests, with a
verified database/state backup before cutover. Other sites on the host retained
their container identities. Public health is UP, and the site's baseline ASTM,
HL7 and file smoke checks passed. The retained server backup is
`/opt/oe-analyzers-site/backups/pre-1574-f9e09afd`.

Private evidence is retained locally under
`.deployment-evidence/2026-10-29/1574-acceptance/`: the before/after catalog
inventories, deployment and backup logs, `server-acceptance.json`, screenshots,
four rendered PDFs, and the synthetic FHIR API response. Credentials and raw
evidence are excluded from the PR. The catalog inventory is a review input, not
authorization to clear all zeros. Existing issued PDF versions remain archived;
a new print may use the new reporting policy and must not replace an archived
version. Historical lost digits are not reconstructed.

## Standards context

[FHIR decimal](https://hl7.org/fhir/R4/datatypes.html#decimal) preserves
represented decimal precision.
[ObservationDefinition decimalPrecision](https://hl7.org/fhir/R4/observationdefinition-definitions.html#ObservationDefinition.quantitativeDetails.decimalPrecision)
is optional and means digits after the decimal separator. Neither specifies our
clinical decision algorithm.

[OCL mapping](https://docs.openconceptlab.org/en/latest/oclomrs/openmrstooclmapping.html)
carries numeric concept metadata, while
[custom attributes](https://docs.openconceptlab.org/en/latest/oclapi/apireference/custom_attributes.html)
are versioned metadata. OpenELIS must define how it consumes them. OpenMRS
distinguishes `allowDecimal` and `displayPrecision`; the latter's documentation
uses significant figures, so it cannot be blindly treated as decimal places.

[NIDDK guidance](https://www.niddk.nih.gov/research-funding/research-programs/kidney-clinical-research-epidemiology/laboratory/glomerular-filtration-rate-equations)
demonstrates that reporting depends on test and unit.
[CLSI AUTO15's public description](https://clsi.org/shop/standards/auto15/)
calls for predefined, validated autoverification criteria; it does not prescribe
a universal rounded-versus-unrounded comparison rule. The separation above is
the explicit OpenELIS policy adopted for this remediation.

## Acceptance follow-up: explicit report logarithms and readable annotations

The deployed report check found a correctly calculated negative logarithm whose
minus sign wrapped away from its digits, and a legacy viral-load lookup that
selected a test by localized name. Catalog names are not calculation
assignments.

- Each numeric result component now has an explicit `displayLog10` option in
  Test Catalog > Sample & Results. It is false by default, including on upgrade.
  No name-based backfill guesses which component should receive a calculation.
- The `result-components` CSV domain accepts `displayLog10`; a blank update
  preserves the existing setting. A non-numeric component cannot enable it.
- Patient reports calculate the optional annotation from the original positive,
  exact measurement, independently of reporting decimal places. Bounds, zero and
  negative measurements receive no logarithm. Received logarithmic components
  remain ordinary independently mapped results; do not enable this option on a
  component already expressed as a logarithm.
- The calculated logarithm and the original-value annotation each start on their
  own line. A numeric minus sign stays with the logarithm's digits.
- The old name-based special case in legacy result entry, validation and patient
  reports is removed. Stored values, flags, FHIR and machine-readable exports do
  not acquire this report-only annotation.

Upgrade acceptance must include explicit laboratory review of any component that
needs the report logarithm. Existing names, units and decimal-place settings do
not enable it. Already issued PDF artifacts are not rewritten.
