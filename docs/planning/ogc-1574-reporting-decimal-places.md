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
