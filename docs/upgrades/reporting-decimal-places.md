# Reporting decimal places upgrade

Clinical numeric values remain as received or calculated. Reporting decimal
places are optional presentation settings; they do not change stored values,
clinical flags, thresholds, or numeric exports. Zero is an explicit choice to
display whole numbers, never an implicit default. When display rounding changes
a patient-report value, the report also identifies the clinical value.

Before upgrading, inventory external SQL, reporting tools, and integrations that
read `significant_digits`. Migration 145 renames that column to
`reporting_decimal_places` in `result`, `test_result`, and
`test_result_component`. Update consumers with the application deployment. Back
up the database and verify those consumers against a restored upgrade copy.

Configuration imports accept the new decimal-places column and the legacy alias.
A blank cell preserves an existing authored setting; an explicit `0` selects
whole-number display. Clear a setting using the test catalog editor. Do not use
a blank configuration cell as an implicit command to erase laboratory policy.

Migration 146 adds the opt-in log10 presentation setting. Migration 147 adds an
explicit received-result association, `log10SourceComponentCode`, within the
same test. In Test Catalog, select **Show log10 on reports** and, when
applicable, the **Received log10 result field**. That source must be another
visible numeric component without its own calculated log. Renaming it in the
editor updates the association; an invalid or removed source is rejected on
save. CSV imports use the same fields and validate all components of a test
together. Explicit CSV values update policy; blank cells preserve existing
settings.

A received log is printed through its normal component, with no additional
calculated log on the measurement. When that received field is absent or blank,
an exact positive measurement may get a **Calculated log10** annotation. A
present but unvalidated received log remains awaiting validation; it does not
trigger substitution. Bounds, text, zero and negative measurements never get a
calculated logarithm. Alpha-type legacy measurements can opt into this display
without changing their stored type or text. No derived log is persisted or used
for clinical decisions.

The harness explicitly links HIV-1 Viral Load PRIMARY to LOG. The upgrade only
enables the base-image viral-load primary component identified by ecosystem GUID
`b50d156e-0f6f-40cd-921c-4e831602a623`, even if renamed, and only when its
version predates migration 146. Components edited since then, lab-created
same-name or same-LOINC tests, and historical CSV-created tests with ambiguous
provenance require laboratory review. They are not silently reconfigured. The
migration records the exact changed rows and versions so rollback preserves
later edits.

Clinical-value and calculated-log annotations now use the frontend JSON catalog
packaged into the report renderer. English keys ship with this change;
translated wording remains managed through the existing Transifex workflow.

For rollback, stop writers and restore a compatible database backup, or roll
back the applicable Liquibase changes in reverse order before starting an older
application image. An older image expects the former column names. Do not run it
against the renamed schema. Test rollback with the same database version and
external consumers used for the upgrade.
