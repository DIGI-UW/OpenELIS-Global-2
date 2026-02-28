# OpenELIS ASTM Analyzer Mapping Specification — Addendum v1.2

**Addendum to:** OpenELIS ASTM Analyzer Mapping Specification v1.0 + Addendum
v1.1 **Addendum Version:** 1.2 **Date:** February 25, 2026 **Status:** Draft for
Review **Jira Story:** OGC-337 (Generic ASTM Plugin v1.1) **Technology:** Java
Spring Framework, Carbon React **Protocol:** ASTM LIS2-A2 / E1394-97 / E1381-95
(TCP/IP)

---

## Change Log

| Version | Date       | Author | Changes                                                                                                                                                                                                       |
| ------- | ---------- | ------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1.2     | 2026-02-25 | Casey  | Addendum: Analyzer Profile System (FR-22, FR-23, FR-24), Lab Unit association (FR-25), profile JSON schema, profile library management, import/export, built-in profile catalog, community repository support |

---

## Purpose of This Addendum

The v1.1 addendum defines the full configuration surface for a generic ASTM
plugin — connection settings, test code mappings, QC rules, value
transformations, field extraction overrides, result aggregation, and abnormal
flag mapping. With this configuration surface complete, a critical operational
question arises: **how does an administrator get a new analyzer configured
quickly and correctly?**

Currently, the v1.0/v1.1 workflow requires building every configuration from
scratch — manually entering each test code, QC rule, and extraction override.
This is error-prone and slow, especially for instruments like the Indiko Plus
with 50+ test codes.

This addendum introduces the **Analyzer Profile System**: a portable, shareable
JSON format that captures a complete analyzer configuration. Profiles serve as
the new unit of deliverable for analyzer support — instead of shipping a
compiled Java plugin, the OpenELIS community ships a JSON profile file that any
site can import, try, and customize.

### Key Capabilities

1. **Analyzer Profile (FR-22):** Portable JSON file containing the complete
   analyzer configuration (connection defaults, test code mappings, QC rules,
   value transformations, extraction overrides, abnormal flag mappings,
   aggregation settings). Profiles are the new standard deliverable for analyzer
   onboarding.

2. **Profile Library (FR-23):** Built-in profiles ship with OpenELIS for common
   instruments. Administrators can also upload profiles to a shared site
   library, or import profiles downloaded from the OpenELIS community
   repository.

3. **Profile Selection in Analyzer Setup (FR-24):** The "Add Analyzer" modal
   replaces the former "Analyzer Type" field with an "Analyzer Profile"
   selector. Selecting a profile pre-fills all configuration including
   connection defaults, protocol version, test code mappings, and QC rules. All
   pre-filled values are editable. "None (Start from Scratch)" is always
   available.

4. **Lab Unit Assignment (FR-25):** New multi-select field on the analyzer
   registration form, tied to the system-configured lab units (ref: Lab Units
   Management FRS). Replaces the former single "Analyzer Type" dropdown which
   was a lab-department categorization. Lab units are configurable per site,
   making this both more accurate and more flexible.

---

## New Functional Requirements

### FR-22: Analyzer Profile Format

The system shall define a portable JSON format for complete analyzer
configurations. A profile captures every setting that is configurable through
the analyzer UI (v1.0 + v1.1), excluding site-specific identifiers (analyzer
name, unique database IDs) and runtime state (pending codes, last-modified
timestamps).

#### FR-22.1: Profile JSON Schema

```json
{
  "$schema": "https://openelis-global.org/schemas/analyzer-profile/1.0",
  "profileVersion": "1.0",
  "profileMeta": {
    "id": "indiko-plus-chemistry-v1",
    "name": "Thermo Fisher Indiko Plus — Chemistry Panel",
    "description": "Standard chemistry profile for Indiko Plus with 48 test codes, ASTM LIS2-A2 over TCP/IP. Based on LIS Interface Manual v3.2.",
    "manufacturer": "Thermo Fisher Scientific",
    "model": "Indiko Plus",
    "protocolVersion": "ASTM_LIS2_A2",
    "version": "1.0.0",
    "author": "OpenELIS Community",
    "createdDate": "2026-02-25",
    "tags": ["chemistry", "ASTM", "indiko"],
    "sourceUrl": "https://community.openelis-global.org/profiles/indiko-plus-chemistry-v1",
    "compatibleOpenelisVersion": ">=3.2.0"
  },
  "connection": {
    "role": "SERVER",
    "defaultPort": 5000,
    "connectionTimeoutSec": 30,
    "retryCount": 6,
    "bidirectionalEnabled": false
  },
  "extraction": {
    "specimenIdField": "O.3",
    "testIdField": "R.3",
    "testIdComponent": 4,
    "resultValueField": "R.4",
    "resultUnitsField": "R.5",
    "abnormalFlagField": "R.7",
    "resultStatusField": "R.8",
    "resultTimestampField": "R.13",
    "senderNameField": "H.5"
  },
  "aggregation": {
    "mode": "PER_MESSAGE",
    "windowSeconds": 30
  },
  "testCodeMappings": [
    {
      "analyzerCode": "GLU",
      "displayName": "Glucose",
      "suggestedTestName": "Glucose",
      "suggestedTestCode": "GLU",
      "resultType": "NUMERIC",
      "analyzerUnit": "mg/dL",
      "suggestedOpenelisUnit": "mg/dL",
      "transformation": {
        "type": "PASS_THROUGH"
      }
    },
    {
      "analyzerCode": "TBIL",
      "displayName": "Total Bilirubin",
      "suggestedTestName": "Total Bilirubin",
      "suggestedTestCode": "TBIL",
      "resultType": "NUMERIC",
      "analyzerUnit": "mg/dL",
      "suggestedOpenelisUnit": "mg/dL",
      "transformation": {
        "type": "GREATER_LESS_FLAG",
        "config": { "operators": [">", "<", ">=", "<="] }
      }
    }
  ],
  "qcRules": [
    {
      "ruleType": "SPECIMEN_ID_PREFIX",
      "matchValue": "QC-"
    },
    {
      "ruleType": "FIELD_EQUALS",
      "fieldReference": "O.16",
      "matchValue": "QC"
    }
  ],
  "abnormalFlagMapping": [
    { "analyzerFlag": "N", "interpretation": "NORMAL" },
    { "analyzerFlag": "L", "interpretation": "LOW" },
    { "analyzerFlag": "H", "interpretation": "HIGH" },
    { "analyzerFlag": "LL", "interpretation": "CRITICAL_LOW" },
    { "analyzerFlag": "HH", "interpretation": "CRITICAL_HIGH" },
    { "analyzerFlag": "A", "interpretation": "ABNORMAL" },
    { "analyzerFlag": "", "interpretation": "NORMAL" }
  ]
}
```

#### FR-22.2: Profile Scope

A profile contains all configuration **except**:

| Included in Profile                                      | NOT Included (Site-Specific)                                    |
| -------------------------------------------------------- | --------------------------------------------------------------- |
| Connection defaults (role, default port, timeout, retry) | Analyzer name                                                   |
| Protocol version                                         | Actual IP address (for client mode)                             |
| Field extraction positions                               | Actual listen port (pre-filled from default, but user confirms) |
| Test code mappings with suggested OpenELIS test matches  | OpenELIS test ID bindings (resolved at import time)             |
| QC identification rules                                  | Database primary keys                                           |
| Abnormal flag mappings                                   | Runtime state (pending codes, last modified)                    |
| Value transformation rules + configs                     | Lab unit assignments (site-specific)                            |
| Aggregation mode + window                                | Analyzer status (always starts as Setup)                        |

#### FR-22.3: Test Code Mapping Resolution

When a profile is imported, each `testCodeMapping` entry includes a
`suggestedTestName` and `suggestedTestCode`. The system attempts automatic
matching:

1. **Exact code match:** If an OpenELIS test exists with the same `testCode`,
   auto-bind (status: `mapped`)
2. **Fuzzy name match:** If test code doesn't match but test name matches
   (case-insensitive), suggest as probable match (status: `suggested`)
3. **No match:** Leave unmapped with the suggested name visible for the
   administrator to resolve (status: `unmapped`)

The import summary shows match statistics:
`X auto-matched, Y suggested, Z need manual mapping`.

#### FR-22.4: Profile Version Compatibility

- `profileVersion` tracks the schema version (currently `1.0`)
- `compatibleOpenelisVersion` uses semver range to indicate minimum OpenELIS
  version
- On import, the system validates both and warns if the profile was created for
  a newer version
- Backward compatibility: profiles from older schema versions are imported with
  defaults for missing sections

---

### FR-23: Profile Library

The system shall maintain a library of available analyzer profiles, sourced from
three channels:

#### FR-23.1: Built-in Profiles

OpenELIS ships with profiles for common analyzers. These are read-only and
cannot be deleted or modified. They can be duplicated.

**Initial built-in catalog (target):**

| Profile ID                 | Manufacturer  | Model                 | Protocol | Tests       |
| -------------------------- | ------------- | --------------------- | -------- | ----------- |
| `indiko-plus-chemistry-v1` | Thermo Fisher | Indiko Plus           | ASTM     | ~48         |
| `maldi-biotyper-astm-v1`   | Bruker        | MALDI Biotyper sirius | ASTM     | Organism ID |
| `vidas-immunoassay-v1`     | bioMérieux    | VIDAS                 | ASTM     | ~12         |
| `stago-st4-coag-v1`        | Stago         | ST4/STA-R             | ASTM     | ~8          |
| `sysmex-xn-hema-v1`        | Sysmex        | XN Series             | ASTM     | ~30         |
| `mindray-bs-chem-v1`       | Mindray       | BS-200/300/400        | HL7      | ~40         |
| `mindray-bc5380-hema-v1`   | Mindray       | BC-5380               | HL7      | ~26         |

Built-in profiles are stored as JSON resources in the application package and
loaded at startup.

#### FR-23.2: Uploaded Profiles (Site Library)

Administrators can upload `.json` profile files to the site library. Uploaded
profiles:

- Are validated against the profile JSON schema on upload
- Appear in the profile selector alongside built-in profiles
- Can be edited (metadata only — name, description, tags) or deleted by admins
- Are stored in the database (not filesystem) for portability

#### FR-23.3: Community Repository Integration

The system shall support importing profiles from the OpenELIS community
repository:

- **Download & Import:** Administrator downloads a `.json` file from the
  community site and uploads it via the standard import flow (FR-23.2)
- **Future (Phase 2):** In-app browser for community profiles with one-click
  import
- **No auto-sync:** Profiles are not automatically updated from the repository.
  Each import is a point-in-time snapshot.

#### FR-23.4: Profile Export

Any configured analyzer can be exported as a profile file:

- **Export action:** Available from the analyzer overflow menu (⋮ → "Export
  Profile")
- **Export behavior:** Serializes the current configuration to the profile JSON
  schema (FR-22.1)
- **Site-specific stripping:** The export process removes site-specific data
  (analyzer name, IP addresses, database IDs, OpenELIS test ID bindings) and
  replaces them with suggested names/codes
- **Metadata prompt:** Before export, user is prompted to provide profile name,
  description, author, and tags
- **File download:** Browser downloads `{profileId}.json`

---

### FR-24: Profile Selection in Analyzer Setup

The "Add Analyzer" modal shall include profile selection as the primary
onboarding mechanism, replacing the former "Analyzer Type" field.

#### FR-24.1: Revised Add Analyzer Modal

| Field                   | Type                      | Source            | Behavior                                                                                                  |
| ----------------------- | ------------------------- | ----------------- | --------------------------------------------------------------------------------------------------------- |
| Analyzer Name           | Text input                | Manual            | Required. User enters site-specific name.                                                                 |
| Analyzer Profile        | Dropdown + Import         | Profile Library   | "None (Start from Scratch)" + all available profiles, grouped by source. Import button opens file picker. |
| Status                  | Dropdown                  | System            | Default: Setup. Same as v1.0.                                                                             |
| Lab Units               | Multi-select (filterable) | System lab units  | Select one or more lab units this analyzer serves.                                                        |
| Protocol Version        | Dropdown                  | Profile or manual | Pre-filled when profile selected. Editable.                                                               |
| Connection Role         | Dropdown                  | Profile or manual | Pre-filled when profile selected. Editable.                                                               |
| Listen Port / IP + Port | Conditional               | Profile or manual | Pre-filled with profile defaults. User must confirm/adjust.                                               |
| Connection Timeout      | Number                    | Profile or manual | Pre-filled. Editable.                                                                                     |
| NAK Retry Count         | Number                    | Profile or manual | Pre-filled. Editable.                                                                                     |

#### FR-24.2: Profile Selector UI

The "Analyzer Profile" field is a searchable dropdown with grouped options:

```
┌──────────────────────────────────────────────────────────┐
│ Analyzer Profile                                          │
│ ┌──────────────────────────────────────────────────┐     │
│ │ 🔍 Search profiles...                            │     │
│ ├──────────────────────────────────────────────────┤     │
│ │  None (Start from Scratch)                        │     │
│ ├──────────────────────────────────────────────────┤     │
│ │  BUILT-IN                                         │     │
│ │  ⬡ Thermo Fisher Indiko Plus — Chemistry          │     │
│ │  ⬡ Bruker MALDI Biotyper sirius — Organism ID     │     │
│ │  ⬡ bioMérieux VIDAS — Immunoassay                  │     │
│ │  ⬡ Stago ST4 — Coagulation                        │     │
│ │  ⬡ Sysmex XN Series — Hematology                  │     │
│ │  ⬡ Mindray BS-200/300/400 — Chemistry             │     │
│ │  ⬡ Mindray BC-5380 — Hematology                   │     │
│ ├──────────────────────────────────────────────────┤     │
│ │  SITE LIBRARY                                      │     │
│ │  ⬡ Custom Indiko (modified QC rules)               │     │
│ ├──────────────────────────────────────────────────┤     │
│ │  ┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈        │     │
│ │  📁 Import from File...                            │     │
│ └──────────────────────────────────────────────────┘     │
└──────────────────────────────────────────────────────────┘
```

**Carbon components:** `<ComboBox>` with `<FilterableMultiSelect>` grouping
pattern, custom group headers.

#### FR-24.3: Profile Apply Behavior

When a profile is selected:

1. **Connection fields** pre-fill: role, default port, timeout, retry
2. **Protocol version** pre-fills from `profileMeta.protocolVersion`
3. **Info banner** appears below profile selector showing: profile name,
   version, author, test count, description snippet
4. **Modal saves** the analyzer with the profile reference
5. **On first visit to Field Mappings page:** Test code mappings, QC rules,
   extraction config, flag mappings, and transforms are populated from the
   profile. Auto-matching runs against existing OpenELIS tests (FR-22.3).
6. **Import summary notification** shows: "Profile loaded: X test codes (Y
   auto-matched, Z need mapping), N QC rules, flag mappings applied"

When "None (Start from Scratch)" is selected:

- All fields start empty/default
- Field Mappings page starts with empty tables
- Protocol version defaults to ASTM LIS2-A2

#### FR-24.4: Import from File

The "Import from File" option at the bottom of the profile selector:

1. Opens the browser file picker (accept: `.json`)
2. Validates the file against the profile JSON schema
3. On success: applies the profile to the current modal (same as selecting a
   built-in profile) AND adds it to the site library
4. On failure: displays validation errors inline
5. Option to "Import to library only" (save for later use without applying now)

---

### FR-25: Lab Unit Assignment

The system shall allow assignment of one or more lab units to each analyzer,
replacing the former "Analyzer Type" (single-select lab department category)
field.

#### FR-25.1: Lab Unit Multi-Select

| Field     | Type                      | Data Source                    |
| --------- | ------------------------- | ------------------------------ |
| Lab Units | `<FilterableMultiSelect>` | `/api/lab-units?isActive=true` |

- Displays all active lab units configured in the system (ref: Lab Units
  Management FRS)
- User can select one or more lab units that this analyzer serves
- Selected lab units appear as tags/pills below the dropdown
- Searchable/filterable by lab unit name
- Optional — an analyzer can be saved without lab unit assignment during initial
  setup

#### FR-25.2: Lab Unit Display

- Lab units appear on the Analyzer List table as a new "Lab Units" column,
  displaying tags
- Lab units are included in the Analyzer List search and filter capabilities
- The Analyzer List "Status" filter is supplemented with a "Lab Unit" filter
  dropdown

#### FR-25.3: Lab Unit in Profile

- Profiles do NOT include lab unit assignments (these are site-specific)
- When a profile is applied, the Lab Units field remains empty for manual
  selection
- Lab units are included in profile export metadata as `suggestedLabUnits`
  (informational only, not auto-applied)

---

## New Business Rules

### BR-18: Profile Schema Validation

All imported profile files must validate against the published JSON schema.
Invalid files are rejected with specific validation error messages.

### BR-19: Profile ID Uniqueness

Within the site library, `profileMeta.id` must be unique. Importing a profile
with an existing ID prompts the user to either replace the existing profile or
save as a new profile with a generated suffix.

### BR-20: Built-in Profile Immutability

Built-in profiles cannot be modified or deleted. They can only be:

- Selected for a new analyzer (applies a copy)
- Duplicated to the site library for customization

### BR-21: Profile Export Sanitization

Export strips all site-specific data:

- Database IDs (UUIDs, foreign keys)
- Analyzer name
- IP addresses and actual port numbers (replaced with defaults)
- OpenELIS test ID bindings (replaced with `suggestedTestName` +
  `suggestedTestCode`)
- Lab unit assignments (replaced with `suggestedLabUnits`)
- Runtime state (pending codes, timestamps)

### BR-22: Lab Unit Association

- Lab unit assignments do not affect analyzer functionality (no filtering of
  results by lab unit in this version)
- Lab units are informational/organizational for the analyzer list view
- Future versions may use lab unit assignments for role-based access control and
  result routing

---

## New User Stories

### Epic 9: Analyzer Profiles

#### US-9.1: Apply Built-in Profile to New Analyzer

_As a_ laboratory administrator _I want to_ select a built-in analyzer profile
when adding a new analyzer _So that_ I can start with a known-good configuration
and only customize what differs at my site

**Acceptance Criteria:**

- Given I click "Add Analyzer" on the Analyzer List page
- When I open the "Analyzer Profile" dropdown
- Then I see built-in profiles grouped under a "BUILT-IN" header
- And selecting one pre-fills protocol, connection defaults, and displays
  profile info
- And after saving, visiting Field Mappings shows pre-populated test codes with
  auto-match results

#### US-9.2: Import Profile from File

_As a_ laboratory administrator _I want to_ import an analyzer profile from a
JSON file _So that_ I can use profiles shared by other sites or downloaded from
the community repository

**Acceptance Criteria:**

- Given I am in the "Add Analyzer" modal
- When I click "Import from File" and select a valid `.json` profile file
- Then the profile is validated and applied to the modal
- And the profile is saved to the site library for future use
- And an error is shown if the file is invalid with specific validation messages

#### US-9.3: Export Analyzer as Profile

_As a_ laboratory administrator _I want to_ export a configured analyzer as a
reusable profile file _So that_ I can share my configuration with other OpenELIS
sites or back it up

**Acceptance Criteria:**

- Given I have a configured analyzer with test code mappings
- When I select "Export Profile" from the analyzer overflow menu
- Then I am prompted for profile metadata (name, description, author, tags)
- And a sanitized `.json` file downloads with all site-specific data stripped
- And the exported file can be imported at another site

#### US-9.4: Manage Site Profile Library

_As a_ laboratory administrator _I want to_ view and manage the profiles in my
site's library _So that_ I can organize shared profiles and remove ones no
longer needed

**Acceptance Criteria:**

- Given I navigate to the profile library (from Analyzers menu or Admin)
- Then I see all profiles organized by source (Built-in, Site Library)
- And I can edit metadata (name, description, tags) on site library profiles
- And I can delete site library profiles
- And built-in profiles show as read-only with a "Duplicate" option

### Epic 10: Lab Unit Assignment

#### US-10.1: Assign Lab Units to Analyzer

_As a_ laboratory administrator _I want to_ assign one or more lab units to an
analyzer _So that_ I can organize analyzers by the lab sections they serve

**Acceptance Criteria:**

- Given I am in the "Add Analyzer" or "Edit Analyzer" modal
- When I see the "Lab Units" field
- Then it shows a filterable multi-select of all active lab units in the system
- And I can select multiple lab units which appear as removable tags
- And the selected lab units are saved with the analyzer

#### US-10.2: Filter Analyzer List by Lab Unit

_As a_ laboratory staff member _I want to_ filter the analyzer list by lab unit
_So that_ I can quickly find analyzers relevant to my lab section

**Acceptance Criteria:**

- Given I am on the Analyzer List page
- When I use the Lab Unit filter dropdown
- Then the table shows only analyzers assigned to the selected lab unit(s)

---

## New API Endpoints

### Profile Library

**GET /api/analyzer-profiles**

List all available profiles (built-in + site library).

- Query params: `?source=BUILT_IN|SITE_LIBRARY`, `?search=`,
  `?protocol=ASTM|HL7`
- Returns: Array of profile metadata (no full config body)
- **Authorization:** LAB_USER

**GET /api/analyzer-profiles/{profileId}**

Get full profile content.

- Returns: Complete profile JSON per FR-22.1 schema
- **Authorization:** LAB_USER

**POST /api/analyzer-profiles/import**

Import a profile JSON file to the site library.

- Body: Multipart file upload (`.json`)
- Validates against schema
- Returns:
  `{ profileId, name, matchSummary: { total, autoMatched, suggested, unmapped } }`
- **Authorization:** LAB_ADMIN

**PUT /api/analyzer-profiles/{profileId}/metadata**

Update metadata for a site library profile.

- Body: `{ name, description, tags }`
- Returns: Updated profile metadata
- **Authorization:** LAB_ADMIN
- **Restriction:** Cannot modify built-in profiles

**DELETE /api/analyzer-profiles/{profileId}**

Delete a site library profile.

- **Authorization:** LAB_ADMIN
- **Restriction:** Cannot delete built-in profiles. Returns 409 if profile is in
  use by an active analyzer.

### Profile Export

**GET /api/analyzers/{id}/export-profile**

Export an analyzer's current configuration as a profile file.

- Query params: `?name=&description=&author=&tags=`
- Returns: Profile JSON (Content-Disposition: attachment)
- **Authorization:** LAB_ADMIN

### Lab Unit Assignment

**GET /api/analyzers/{id}/lab-units**

Get lab units assigned to an analyzer.

- Returns: Array of `{ id, name }`
- **Authorization:** LAB_USER

**PUT /api/analyzers/{id}/lab-units**

Set lab unit assignments for an analyzer.

- Body: `{ labUnitIds: [1, 3, 7] }`
- **Authorization:** LAB_ADMIN

---

## Database Schema Additions

```sql
-- Analyzer profile library
CREATE TABLE analyzer_profile (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id VARCHAR(100) NOT NULL UNIQUE,
    source VARCHAR(20) NOT NULL DEFAULT 'SITE_LIBRARY',
    name VARCHAR(200) NOT NULL,
    description TEXT,
    manufacturer VARCHAR(100),
    model VARCHAR(100),
    protocol_version VARCHAR(30),
    version VARCHAR(20) NOT NULL DEFAULT '1.0.0',
    author VARCHAR(100),
    tags TEXT[],
    profile_json JSONB NOT NULL,
    compatible_openelis_version VARCHAR(20),
    source_url VARCHAR(500),
    created_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_modified TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    modified_by INTEGER REFERENCES system_user(id),
    CONSTRAINT chk_source CHECK (source IN ('BUILT_IN', 'SITE_LIBRARY'))
);

-- Analyzer ↔ Profile reference (tracks which profile was used)
ALTER TABLE analyzer ADD COLUMN profile_id UUID REFERENCES analyzer_profile(id);

-- Analyzer ↔ Lab Unit junction table
CREATE TABLE analyzer_lab_unit (
    analyzer_id INTEGER NOT NULL REFERENCES analyzer(id),
    lab_unit_id INTEGER NOT NULL REFERENCES lab_unit(id),
    PRIMARY KEY (analyzer_id, lab_unit_id)
);

-- Lab Unit filter index
CREATE INDEX idx_analyzer_lab_unit_unit ON analyzer_lab_unit(lab_unit_id);
```

---

## Complete Localization Tags

### Profile System Tags

| Tag                                                | Default English                                                                                                                                    |
| -------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------- |
| `label.analyzer.profile`                           | Analyzer Profile                                                                                                                                   |
| `label.analyzer.profile.none`                      | None (Start from Scratch)                                                                                                                          |
| `label.analyzer.profile.builtIn`                   | Built-in                                                                                                                                           |
| `label.analyzer.profile.siteLibrary`               | Site Library                                                                                                                                       |
| `label.analyzer.profile.search`                    | Search profiles...                                                                                                                                 |
| `label.analyzer.profile.importFile`                | Import from File...                                                                                                                                |
| `label.analyzer.profile.importFileDesc`            | Upload an OpenELIS Analyzer Profile (.json)                                                                                                        |
| `label.analyzer.profile.exportProfile`             | Export Profile                                                                                                                                     |
| `label.analyzer.profile.exportPrompt`              | Provide metadata for the exported profile                                                                                                          |
| `label.analyzer.profile.info`                      | Profile Info                                                                                                                                       |
| `label.analyzer.profile.version`                   | Profile Version                                                                                                                                    |
| `label.analyzer.profile.author`                    | Author                                                                                                                                             |
| `label.analyzer.profile.testCount`                 | Test Codes                                                                                                                                         |
| `label.analyzer.profile.qcRuleCount`               | QC Rules                                                                                                                                           |
| `label.analyzer.profile.description`               | Description                                                                                                                                        |
| `label.analyzer.profile.manufacturer`              | Manufacturer                                                                                                                                       |
| `label.analyzer.profile.model`                     | Model                                                                                                                                              |
| `label.analyzer.profile.tags`                      | Tags                                                                                                                                               |
| `label.analyzer.profile.compatVersion`             | Compatible OpenELIS Version                                                                                                                        |
| `label.analyzer.profile.applied`                   | Profile Applied                                                                                                                                    |
| `label.analyzer.profile.applied.summary`           | {testCount} test codes ({autoMatched} auto-matched, {suggested} suggested, {unmapped} need mapping), {qcRuleCount} QC rules, flag mappings applied |
| `label.analyzer.profile.validation.invalid`        | Invalid profile file                                                                                                                               |
| `label.analyzer.profile.validation.schemaError`    | Profile does not match expected schema version                                                                                                     |
| `label.analyzer.profile.validation.versionWarning` | This profile was created for a newer version of OpenELIS                                                                                           |
| `label.analyzer.profile.duplicate`                 | A profile with this ID already exists                                                                                                              |
| `label.analyzer.profile.duplicate.replace`         | Replace Existing                                                                                                                                   |
| `label.analyzer.profile.duplicate.saveNew`         | Save as New                                                                                                                                        |
| `label.analyzer.profile.library`                   | Profile Library                                                                                                                                    |
| `label.analyzer.profile.library.manage`            | Manage Profiles                                                                                                                                    |
| `label.analyzer.profile.readOnly`                  | Built-in profiles cannot be modified                                                                                                               |
| `label.analyzer.profile.duplicateAction`           | Duplicate to Site Library                                                                                                                          |

### Lab Unit Tags

| Tag                                   | Default English                                                                |
| ------------------------------------- | ------------------------------------------------------------------------------ |
| `label.analyzer.labUnits`             | Lab Units                                                                      |
| `label.analyzer.labUnits.placeholder` | Select lab units...                                                            |
| `label.analyzer.labUnits.help`        | Lab units this analyzer serves. Lab units are configured in Admin → Lab Units. |
| `label.analyzer.labUnits.filter`      | Filter by Lab Unit                                                             |
| `label.analyzer.labUnits.allUnits`    | All Lab Units                                                                  |
| `label.analyzer.labUnits.none`        | No lab units assigned                                                          |
| `label.analyzer.labUnits.suggested`   | Suggested lab units (from profile)                                             |

---

## Acceptance Criteria (New)

### Profile System

- [ ] **AC-87**: "Analyzer Profile" dropdown appears in Add Analyzer modal with
      grouped options (None, Built-in, Site Library, Import from File)
- [ ] **AC-88**: Built-in profiles are listed and cannot be modified or deleted
- [ ] **AC-89**: Selecting a profile pre-fills protocol, connection role,
      default port, timeout, and retry count in the modal
- [ ] **AC-90**: Profile info banner displays below selector showing name,
      version, author, test count
- [ ] **AC-91**: After saving, Field Mappings page auto-populates test codes, QC
      rules, extraction config, and flag mappings from the profile
- [ ] **AC-92**: Auto-matching resolves test codes against existing OpenELIS
      tests (exact code → mapped, fuzzy name → suggested, no match → unmapped)
- [ ] **AC-93**: Import summary notification shows match statistics
- [ ] **AC-94**: "Import from File" validates JSON against profile schema and
      shows validation errors for invalid files
- [ ] **AC-95**: Imported profiles are added to the site library for future use
- [ ] **AC-96**: "Export Profile" from overflow menu serializes current config,
      strips site-specific data, and downloads JSON
- [ ] **AC-97**: Export prompts for metadata (name, description, author, tags)
      before download
- [ ] **AC-98**: Profile ID uniqueness is enforced with replace/save-new prompt
      on conflict
- [ ] **AC-99**: Profile version compatibility is checked on import with warning
      for newer schema versions

### Lab Unit Assignment

- [ ] **AC-100**: "Lab Units" filterable multi-select appears in Add/Edit
      Analyzer modal
- [ ] **AC-101**: Multi-select shows all active lab units from the system
- [ ] **AC-102**: Selected lab units appear as removable tags/pills
- [ ] **AC-103**: Lab Units column appears on Analyzer List table
- [ ] **AC-104**: Lab Unit filter dropdown appears on Analyzer List page
- [ ] **AC-105**: Lab unit assignments are saved and loaded correctly

---

## Related Documents

| Document                                 | Relationship                                                                |
| ---------------------------------------- | --------------------------------------------------------------------------- |
| ASTM Analyzer Mapping Specification v1.0 | Parent specification                                                        |
| ASTM Analyzer Mapping Addendum v1.1      | Previous addendum — defines the configuration surface this profile captures |
| Lab Units Management FRS                 | Defines the lab unit entities referenced by FR-25                           |
| Analyzer Mapping Templates FRS v2.0      | Template system architecture (broader scope, includes HL7 + flat file)      |
| Analyzer Integration Tracker             | Status of all analyzer integrations; profiles replace compiled plugins      |
