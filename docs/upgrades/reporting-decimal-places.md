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

Migration 146 adds an opt-in log10 presentation setting to numeric components.
The laboratory must review which components display it. It does not authorize
deriving a log value from a qualitative result or changing the clinical value.
The analyzer harness also has a separate instrument-delivered LOG component;
review that component before enabling an additional derived log display.

Before release, identify the exact shipped viral-load catalog entries and their
upgrade provenance, then enable the agreed log display through a separately
tested catalog update/migration. Do not identify lab-created tests by name or
LOINC alone. The current migration deliberately leaves them opt-in. Also verify
the clinical-value annotation in the released French report bundle; the current
Transifex configuration covers frontend JSON, not backend report properties.

For rollback, stop writers and restore a compatible database backup, or roll
back the applicable Liquibase changes in reverse order before starting an older
application image. An older image expects the former column names. Do not run it
against the renamed schema. Test rollback with the same database version and
external consumers used for the upgrade.
