# Compliance Standards Seed CSVs

This folder is the deploy-time seed source for the compliance module (FRS S-01).
Drop CSV files here and the `ConfigurationInitializationService` will pick them
up on application start via four sibling `DomainConfigurationHandler`s in this
same domain.

The handlers each match a different filename suffix so each file routes to
exactly one handler. Load order is enforced so a child file (e.g. thresholds)
always sees its parents persisted before it runs.

| File suffix                  | Handler                                           | Load order | Loads                            |
| ---------------------------- | ------------------------------------------------- | ---------: | -------------------------------- |
| `*-standards.csv`            | `ComplianceStandardConfigurationHandler`          |        200 | `compliance_standard`            |
| `*-parameter-groups.csv`     | `ComplianceParameterGroupConfigurationHandler`    |        210 | `parameter_group`                |
| `*-thresholds.csv`           | `ComplianceThresholdConfigurationHandler`         |        220 | `compliance_threshold`           |
| `*-threshold-value-maps.csv` | `ComplianceThresholdValueMapConfigurationHandler` |        230 | `compliance_threshold_value_map` |

Files are read in this order:
`standards → parameter-groups → thresholds → threshold-value-maps`.

A standard's filename prefix is a convention — pick one stem per regulation
(e.g. `baku-mutu-pp22-2021-water-`) and apply it across all four files for that
regulation. The reference seed in this folder is the one for PP No. 22/2021.

## Authoring rules

- **Encoding:** UTF-8.
- **Headers:** case-insensitive. Lines starting with `#` and blank lines are
  skipped.
- **Quoting:** double-quote any cell that contains a comma. Embedded
  double-quotes inside a quoted cell follow RFC 4180 — write them doubled
  (`""`), e.g. `"WHO ""Drinking Water"" Guidelines"`.
- **Dates:** ISO `yyyy-MM-dd`.
- **Numbers:** plain decimals (e.g. `6.0`, `1000`, `0.001`). Empty cell =
  `null`.
- **Booleans:** `true` / `false` / `1` / `0` / `yes` / `no` (case-insensitive).
- **Idempotent re-runs:** every handler upserts by a natural key, so loading the
  same file twice updates rather than duplicates. Files whose checksum matches
  the prior run are skipped entirely.
- **Partial failures:** a single bad row is logged at WARN and skipped; the rest
  of the file still loads (FR-6-006).
- **Pre-seeding:** every standard _created_ by a seed file is forced to
  `isPreSeeded=true` regardless of what the CSV says, so the runtime
  delete-protection applies (FR-6-003).

## Cross-references

Children reference parents by **natural key**, not by ID:

```
ParameterGroup → ComplianceStandard       via (regulationNumber, standardName)
ComplianceThreshold → ParameterGroup      via (regulationNumber, standardName, groupName)
ComplianceThresholdValueMap → Threshold   via (regulationNumber, standardName, groupName, parameterCode)
                                              and thresholdType=SELECT_MAP (implicit)
```

If a child row references a parent that doesn't exist, the child row is skipped
with a WARN log naming the missing parent. Load the parent file first (or fix
the typo), then re-run; the loader's checksum tracker will notice the file
changed and re-process.

Upsert keys per entity:

| Entity                        | Natural key                               | Notes                                                                                  |
| ----------------------------- | ----------------------------------------- | -------------------------------------------------------------------------------------- |
| `ComplianceStandard`          | `(regulationNumber, name)`                |                                                                                        |
| `ParameterGroup`              | `(standardId, name)`                      |                                                                                        |
| `ComplianceThreshold`         | `(groupId, parameterCode, thresholdType)` | Multiple threshold types per parameter are allowed (e.g. `MAXIMUM` plus `BORDERLINE`). |
| `ComplianceThresholdValueMap` | `(thresholdId, optionValue)`              | Always under a `SELECT_MAP` threshold.                                                 |

---

## Standards CSV — `*-standards.csv`

Required columns: `name`, `regulationNumber`.

Optional columns:

| Column                                                                                         | Notes                                                                                                                                                   |
| ---------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `issuingBody`, `version`, `effectiveDate`, `expiryDate` (or `expirationDate`), `countryRegion` | `countryRegion` is required by the entity validator. The handler accepts either `expiryDate` (entity property name) or `expirationDate` (legacy alias). |
| `applicableSampleTypes`                                                                        | Comma-delimited inside the cell (e.g. `"Surface Water,Groundwater"`). Persisted to the `compliance_standard_sample_type` join table.                    |
| `status`                                                                                       | `DRAFT`, `ACTIVE`, `SUPERSEDED`, `ARCHIVED`. Defaults to `ACTIVE`.                                                                                      |
| `description`, `regulatoryContext`, `enforcementAuthority`                                     | Free-text.                                                                                                                                              |
| `supersededById`                                                                               | Optional FK to another standard's id. Empty = none.                                                                                                     |
| `isPreSeeded`                                                                                  | Hint only — forced to `true` on creates.                                                                                                                |
| `localization:<locale>`                                                                        | Reserved for translated names; currently a no-op.                                                                                                       |

Example:

```csv
name,regulationNumber,issuingBody,version,effectiveDate,expiryDate,countryRegion,applicableSampleTypes,enforcementAuthority,regulatoryContext,description,status,isPreSeeded
"PP No. 22/2021 - Water Quality","PP 22/2021","Government of Indonesia","2021","2021-02-02",,"Indonesia","Surface Water,Groundwater,Drinking Water","Ministry of Environment and Forestry","Indonesian environmental protection framework","Indonesian government regulation establishing water quality standards.","ACTIVE","true"
```

---

## Parameter groups CSV — `*-parameter-groups.csv`

Required columns: `regulationNumber`, `standardName`, `name`.

Optional columns: `description`, `sortOrder` (integer), `isMandatory` (boolean).

Example:

```csv
regulationNumber,standardName,name,description,sortOrder,isMandatory
"PP 22/2021","PP No. 22/2021 - Water Quality","Physical Parameters","Sensory and physical-chemical parameters",1,true
"PP 22/2021","PP No. 22/2021 - Water Quality","Microbiology","Indicator-organism limits",4,true
```

---

## Thresholds CSV — `*-thresholds.csv`

Required columns: `regulationNumber`, `standardName`, `groupName`,
`parameterCode`, `displayName`, `thresholdType`.

`thresholdType` values:

| Type          | Numeric fields used    | When to use                                                                                                                               |
| ------------- | ---------------------- | ----------------------------------------------------------------------------------------------------------------------------------------- |
| `RANGE`       | `minValue`, `maxValue` | Value must satisfy `min ≤ x ≤ max`.                                                                                                       |
| `MINIMUM`     | `minValue`             | `x ≥ min` (e.g. dissolved oxygen).                                                                                                        |
| `MAXIMUM`     | `maxValue`             | `x ≤ max` (most pollutant limits).                                                                                                        |
| `EXACT`       | `targetValue`          | `x == target`.                                                                                                                            |
| `BORDERLINE`  | `minValue`, `maxValue` | Advisory zone — flags review without marking non-compliant. Usually paired with a hard `RANGE`/`MAXIMUM` row at the same `parameterCode`. |
| `DESCRIPTIVE` | none                   | Qualitative condition stored in `valueDescriptive` (e.g. `"No objectionable odor"`). Engine flags as Manual Review Required.              |
| `SELECT_MAP`  | none                   | Parent row for select-list parameters. Numeric fields stay null; mappings live in the value-maps CSV.                                     |

Optional columns: `minValue`, `maxValue`, `targetValue`, `valueDescriptive`,
`units`, `methodReference`, `detectionLimit`, `isMandatory`, `sortOrder`,
`validationRules`, `notes`, `testName`.

`testName` (optional) pre-links a threshold to a real `Test` row in the lab
catalog — matched by exact `Test.name` via `TestService.getTestByName`. When
populated, the seeded standard immediately shows a non-zero count in the
**Tests** column on the Standards list, and the test appears in the standard's
**View Linked Tests** panel without manual UI work. If the named test isn't in
the catalog the loader logs a WARN and leaves the threshold template-level (null
`test_id`) — the seed never fails just because a deployment doesn't ship a
matching test row. Leave `testName` blank to keep a threshold portable across
deployments with different test catalogs.

Example:

```csv
regulationNumber,standardName,groupName,parameterCode,displayName,thresholdType,minValue,maxValue,targetValue,valueDescriptive,units,methodReference,detectionLimit,isMandatory,sortOrder,notes,testName
"PP 22/2021","PP No. 22/2021 - Water Quality","Physical Parameters","PH","pH","RANGE",6.0,9.0,,,"pH","SNI 06-6989.11-2004",,true,1,,"pH"
"PP 22/2021","PP No. 22/2021 - Water Quality","Chemical Parameters","BOD","BOD (5-day, 20°C)","MAXIMUM",,2.0,,,"mg/L",,,true,2,,
"PP 22/2021","PP No. 22/2021 - Water Quality","Chemical Parameters","BOD","BOD advisory zone","BORDERLINE",1.5,2.0,,,"mg/L",,,false,3,"Triggers review without marking non-compliant.",
"PP 22/2021","PP No. 22/2021 - Water Quality","Physical Parameters","ODOR","Odor","DESCRIPTIVE",,,,"No objectionable odor",,,,true,3,,
"PP 22/2021","PP No. 22/2021 - Water Quality","Microbiology","E_COLI_PRESENCE","E. coli presence","SELECT_MAP",,,,,,,,true,2,,
```

The first row would land already linked to a test named `pH` (if such a test
exists in the catalog); the rest stay template-level until an admin links them
via the Compliance Standards Administration UI.

---

## Threshold value maps CSV — `*-threshold-value-maps.csv`

Required columns: `regulationNumber`, `standardName`, `groupName`,
`parameterCode`, `optionValue`, `complianceStatus`.

`complianceStatus` values:

| Value           | Meaning                               |
| --------------- | ------------------------------------- |
| `COMPLIANT`     | Result meets compliance requirements. |
| `BORDERLINE`    | Advisory zone; flag for review.       |
| `NON_COMPLIANT` | Result fails.                         |

The parent threshold for each row is resolved as
`(groupId, parameterCode, thresholdType=SELECT_MAP)`. If no SELECT_MAP threshold
exists for that triple, the row is skipped — make sure the `*-thresholds.csv`
declares the SELECT_MAP parent first.

Mappings persist via the cascade on `ComplianceThreshold.valueMappings`
(cascade=ALL, orphanRemoval=true), so adding or removing a row here updates the
parent threshold's collection.

Example:

```csv
regulationNumber,standardName,groupName,parameterCode,optionValue,complianceStatus
"PP 22/2021","PP No. 22/2021 - Water Quality","Microbiology","E_COLI_PRESENCE","Absent","COMPLIANT"
"PP 22/2021","PP No. 22/2021 - Water Quality","Microbiology","E_COLI_PRESENCE","Present","NON_COMPLIANT"
```

---

## Adding a new regulation

1. Pick a stem (e.g. `who-drinking-water-`) and create the four files in this
   folder. You can omit any file whose entities don't apply (e.g. no value-maps
   CSV is needed if the regulation has no SELECT_MAP thresholds).
2. Fill `*-standards.csv` first — every other file looks up its standard by
   `(regulationNumber, name)`.
3. Restart the application. Watch the logs for `Loaded ... from <file>` and any
   per-row `Skipped row ...` warnings; skipped rows usually mean a typo in a
   parent's natural key.
4. To re-import after edits, just save the file. The loader stores a checksum
   per file in `compliance-standards-checksums.properties` and skips unchanged
   files; an edit changes the checksum and triggers a re-run.

## Where the load happens

Source: `src/main/java/org/openelisglobal/compliance/service/`

- `ComplianceStandardConfigurationHandler.java`
- `ComplianceParameterGroupConfigurationHandler.java`
- `ComplianceThresholdConfigurationHandler.java`
- `ComplianceThresholdValueMapConfigurationHandler.java`
- `ComplianceCsvUtil.java` — shared CSV parsing helpers.

The framework wiring lives in `ConfigurationInitializationService.java`; read it
if you need to debug why a file isn't being picked up.
