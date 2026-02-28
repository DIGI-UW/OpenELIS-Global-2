# Feature Specification: Generic ASTM Plugin v1.2 — Analyzer Profiles, QC Rules, Value Transforms, Message Simulator, Connection Role, Lab Units

**Feature Branch**: `spec/012-ogc-337-generic-astm-plugin-profiles`  
**Created**: 2026-02-27  
**Status**: Draft  
**Jira**: OGC-337  
**Input**: User description: "Generic ASTM Plugin v1.2 — Analyzer Profiles, QC
Rules, Value Transforms, Message Simulator, Connection Role, Lab Units
(OGC-337)"

**Extends**: `004-astm-analyzer-mapping` (v1.0 complete)  
**Relationship to 011**: Independent but complementary to
`011-madagascar-analyzer-integration`

## Executive Summary

This feature closes the remaining gaps in the generic ASTM plugin so OpenELIS
can replace per-instrument compiled parser logic with reusable configuration and
portable JSON profiles.

The unified v1.2 scope includes:

- **Core configuration surface (FR-14 through FR-21)**: Connection Role, QC
  identification rules, value transforms, field extraction overrides, result
  aggregation mode, abnormal flag mapping, message simulator, and passive
  auto-detect of unmapped test codes.
- **Analyzer profile + lab unit capabilities (FR-22 through FR-25)**: Portable
  profile schema, profile library (Built-in/Site/Community import), profile
  selection/import/export in analyzer setup, and lab unit assignment for
  analyzer organization.

Primary design references (kept local, not in repo): mockup and addendum in
`specs/012-generic-astm-plugin-profiles/docs/`.

---

## Clarifications

### Session 2026-02-27

- Q: Should the specification retain legacy version references? → A: No. Use
  unified v1.2 terminology across this spec.
- Q: Which RBAC model should govern analyzer/profile operations? → A: Follow
  existing analyzer pattern: `LAB_USER` read, `LAB_SUPERVISOR`
  configure/simulate/activate, `LAB_ADMIN` destructive profile actions.
- Q: What is the role of `profileMeta.id` in lifecycle management? → A:
  `profileMeta.id` is stable profile lineage/family; versions are tracked via
  (`id`, `version`) with one designated latest.
- Q: Should analyzers remain live-linked to profile updates after creation? → A:
  No. Use snapshot-on-apply with strict separation of instance-specific config
  from profile-level defaults.
- Q: How is designated latest profile version determined per lineage? → A:
  Auto-select highest SemVer by default, with explicit admin override.
- Q: Should this release include a dedicated profile reapply workflow for
  existing analyzers? → A: No. Reapply workflow is out of scope for this
  release.

## User Scenarios & Testing _(mandatory)_

### User Story 1 - Configure an analyzer from profile or scratch (Priority: P1)

As a laboratory administrator, I want to add/edit an analyzer by either
selecting a profile or starting from scratch, so onboarding is fast and still
allows full site-level customization.

**Why this priority**: Analyzer onboarding speed and correctness are the
highest-value outcomes for replacing compiled parser plugins.

**Independent Test**: Create one analyzer from a built-in profile and one from
"None (Start from Scratch)", save both, and verify all expected
defaults/editability and setup outcomes.

**Acceptance Scenarios**:

1. **Given** I open Add Analyzer, **When** I search/select a profile from
   grouped sources (Built-in/Site Library/Import), **Then** protocol, connection
   role, and default connection values pre-fill while remaining editable.
2. **Given** I select "None (Start from Scratch)", **When** I save the analyzer,
   **Then** it is created in Setup with no profile-derived mappings.
3. **Given** I assign lab units in Add/Edit Analyzer, **When** I save, **Then**
   assignments persist and appear in Analyzer List column/filter.

---

### User Story 2 - Complete core mapping configuration for generic ASTM analyzers (Priority: P1)

As a laboratory administrator, I want to configure transforms, extraction
fields, aggregation mode, and flag mappings in the mapping UI, so one generic
parser can correctly interpret analyzer-specific messages.

**Why this priority**: This is the core capability that replaces compiled
analyzer-specific parser logic.

**Independent Test**: Configure all core mapping tabs for one analyzer and
verify settings persist and drive parsing behavior.

**Acceptance Scenarios**:

1. **Given** a mapped analyzer test code, **When** I select a transform type and
   valid transform config, **Then** transformed result output follows that
   transform.
2. **Given** non-default field extraction positions, **When** a message is
   parsed, **Then** parser field reads use overridden positions.
3. **Given** aggregation mode is `BY_SESSION` with a valid window, **When**
   multiple related messages arrive in-window, **Then** they aggregate into one
   result set.
4. **Given** abnormal flag mapping includes custom flags, **When** messages
   include those flags, **Then** OpenELIS interpretation uses configured
   mapping.

---

### User Story 3 - Enforce QC identification before activation (Priority: P1)

As a laboratory supervisor, I want QC identification rules to be required before
activating an analyzer, so QC samples are reliably separated from patient
samples.

**Why this priority**: Misclassified QC/patient samples are a critical clinical
risk.

**Independent Test**: Attempt activation without QC rules (blocked), then add
rules and activate (allowed), then verify OR rule behavior on messages.

**Acceptance Scenarios**:

1. **Given** analyzer status change to Active is requested, **When** no QC rules
   exist, **Then** activation is blocked with actionable validation feedback.
2. **Given** multiple QC rules exist, **When** any one rule matches a message,
   **Then** message is classified as QC.
3. **Given** QC rules include Field Equals, Specimen ID Prefix, Pattern, and
   Field Contains, **When** tested with representative messages, **Then** each
   rule type matches only valid cases.

---

### User Story 4 - Simulate and validate message parsing safely (Priority: P2)

As a laboratory administrator, I want to paste raw ASTM payloads into a
simulator and preview parse outcomes, so I can validate configuration before
live traffic.

**Why this priority**: Reduces production errors and shortens safe configuration
cycles.

**Independent Test**: Run simulator with mapped/unmapped test codes and verify
parsed output, warnings, and non-persistence behavior.

**Acceptance Scenarios**:

1. **Given** current analyzer configuration, **When** I parse a raw ASTM message
   in simulator, **Then** parsed fields, mapped tests, transformed values, and
   warnings are displayed.
2. **Given** simulator output is shown, **When** I inspect database state,
   **Then** no patient/QC/result records are persisted from simulator runs.

---

### User Story 5 - Detect and resolve unmapped live test codes quickly (Priority: P2)

As a laboratory administrator, I want passive auto-detect of unknown test codes
with one-click mapping, so production drift is resolved without parser code
updates.

**Why this priority**: Production analyzers evolve and emit new/changed codes
over time.

**Independent Test**: Send messages with unmapped codes, verify pending code
queue behavior, create mapping from pending queue, and validate purge limits.

**Acceptance Scenarios**:

1. **Given** live traffic contains unknown test codes, **When** messages are
   processed, **Then** pending code records are created (up to max 100 per
   analyzer).
2. **Given** pending code entries exist, **When** user maps a code from pending
   list, **Then** mapping is created and code leaves pending queue.
3. **Given** pending entries are older than 30 days, **When** purge job runs,
   **Then** those entries are removed.

---

### User Story 6 - Share analyzer knowledge through profile import/export (Priority: P2)

As an OpenELIS site administrator, I want to import/export analyzer profiles, so
configuration can be shared across deployments without code changes.

**Why this priority**: Portable profiles are the strategic replacement for
compiled parser plugins.

**Independent Test**: Import a valid profile JSON, apply it, export a configured
analyzer, and re-import in a clean environment.

**Acceptance Scenarios**:

1. **Given** a valid profile JSON file, **When** I import, **Then**
   schema/version checks run and the profile is saved in site library.
2. **Given** a configured analyzer, **When** I export profile, **Then**
   site-specific data is stripped and output conforms to profile schema.
3. **Given** built-in profiles, **When** admin attempts edit/delete, **Then**
   those actions are denied while duplication to site library is allowed.

---

### User Story 7 - Bidirectional ASTM communication with GeneXpert (Priority: P1)

As a laboratory administrator, I want OpenELIS to support bidirectional ASTM
communication with GeneXpert analyzers, so orders can be sent to the device and
results can be queried on demand, in addition to the existing push-based result
ingest.

**Why this priority**: GeneXpert supports bidirectional ASTM per the Rev-E LIS
specification. Full utilization requires all 4 communication pathways to be
validated end-to-end.

**Independent Test**: Validate all 4 ASTM pathways (results push, orders pull,
orders push, results pull) against mock analyzer and real GeneXpert device.

**Acceptance Scenarios**:

1. **Given** a GeneXpert sends ASTM results to OpenELIS (results push), **When**
   the message is received, **Then** results are imported via the existing
   ingest pipeline.
2. **Given** a GeneXpert sends an ASTM Q-segment query for pending orders
   (orders pull), **When** OpenELIS receives the query, **Then** it responds
   with an ASTM H/P/O/L message containing pending analyses for the queried
   sample.
3. **Given** an operator triggers order send for a sample (orders push),
   **When** the ASTM order message is sent via bridge to the analyzer, **Then**
   the analyzer receives a spec-compliant H/P/O/L message.
4. **Given** an operator triggers results query for a sample (results pull),
   **When** the ASTM Q-segment is sent via bridge and the analyzer responds,
   **Then** the response is ingested through the standard ASTM pipeline and
   results are persisted.

---

### Edge Cases

- Connection role `SERVER` chosen with a port already used by another active
  listener.
- Connection role `CLIENT` chosen without valid IP/port combination.
- QC rule regex is invalid or overly broad and matches unintended samples.
- Transform config mismatches transform type (e.g., threshold values missing).
- Extraction positions use invalid ASTM references or non-positive component
  indexes (ASTM remains 1-indexed).
- Aggregation window outside 5-300 seconds.
- Profile import conflict on duplicate `profileMeta.id`.
- Profile created on newer OpenELIS/profile schema version.
- Auto-detect queue reaches max 100 pending records.
- Analyzer has no lab unit assignment (allowed in setup flow).

---

## Requirements _(mandatory)_

### Functional Requirements

#### Core Configuration Surface (FR-14 to FR-21)

- **FR-014 Connection Role**: System MUST support analyzer connection role
  selection with `SERVER` (OpenELIS listens) and `CLIENT` (OpenELIS connects).

  - `SERVER` mode MUST show listen port field.
  - `CLIENT` mode MUST show IP address + port fields.
  - Default MUST be `SERVER`.
  - Role controls MUST be available in Add/Edit Analyzer and Advanced mapping
    tab.

- **FR-015 QC Sample Identification Rules**: System MUST provide QC rule builder
  supporting:

  - `FIELD_EQUALS`
  - `SPECIMEN_ID_PREFIX`
  - `SPECIMEN_ID_PATTERN`
  - `FIELD_CONTAINS`
  - Rule evaluation MUST be logical OR ("ANY rule match means QC").
  - Analyzer MUST NOT transition to Active unless at least one QC rule is
    configured.

- **FR-016 Value Transformation Rules**: System MUST allow per test-code mapping
  transformation with supported types:

  - `PASS_THROUGH` (default)
  - `GREATER_LESS_FLAG`
  - `VALUE_MAP`
  - `THRESHOLD_CLASSIFY`
  - `CODED_LOOKUP`
  - Transform configuration MUST be validated by transform type before save.

- **FR-017 Field Extraction Configuration**: System MUST provide configurable
  ASTM extraction references for:

  - Specimen ID field
  - Test ID field
  - Test ID component
  - Result value field
  - Result units field
  - Abnormal flag field
  - Result status field
  - Result timestamp field
  - Sender/instrument field
  - Defaults MUST match standard ASTM positions and remain editable as an
    advanced/power-user feature.

- **FR-018 Result Aggregation Mode**: System MUST support aggregation modes:

  - `PER_MESSAGE` (default)
  - `BY_SPECIMEN`
  - `BY_SESSION`
  - `BY_SESSION` MUST require window configuration in seconds.

- **FR-019 Abnormal Flag Mapping**: System MUST provide editable analyzer-flag
  to OpenELIS-interpretation mapping, including support for custom analyzer
  flags.

- **FR-020 Message Simulator**: System MUST provide simulator tab where user can
  paste raw ASTM payload and parse with current analyzer configuration.

  - Output MUST include extracted fields, mapping resolution, transformed
    results, and warnings.
  - Simulator MUST be preview-only and MUST NOT persist clinical/QC data.
  - _Implementation note:_ "Simulator" is the user-facing UI concept
    (SimulatorTab component). The backend mechanism is the existing
    `POST /analyzers/{analyzerId}/preview-mapping` endpoint extended with v1.2
    outputs (transforms, QC evaluation, flag mapping, unmapped code warnings).
    No separate `/simulate` endpoint.

- **FR-021 Auto-Detect Test Codes**: System MUST detect and store unmapped test
  codes observed in live messages and expose them for one-click mapping.
  - Pending code queue MUST cap at 100 per analyzer.
  - Pending codes older than 30 days MUST be purged.

#### Bidirectional ASTM Communication (FR-026)

- **FR-026 Bidirectional GeneXpert ASTM**: System MUST support all 4 ASTM
  communication pathways for GeneXpert analyzers:
  - **Results Push** (Analyzer → OpenELIS): Existing inbound ASTM pipeline via
    `POST /analyzer/astm`. No new code required.
  - **Orders Pull** (Analyzer queries OpenELIS): Generic ASTM plugin MUST detect
    Q-segment messages and respond with pending analyses as H/P/O/L ASTM
    message.
  - **Orders Push** (OpenELIS → Analyzer): System MUST provide
    `POST /analyzers/{id}/send-order` endpoint that constructs and sends a
    spec-compliant ASTM order message via the bridge.
  - **Results Pull** (OpenELIS queries Analyzer): System MUST provide
    `POST /analyzers/{id}/query-results` endpoint that sends an ASTM Q-segment
    query via bridge, captures the response, and feeds it into the standard
    ingest pipeline.
  - MVP validates via service/API and harness scripts; no dedicated
    bidirectional UI in this release.
  - All pathways MUST be validated against both mock analyzer and real GeneXpert
    device.

#### Analyzer Profile + Lab Unit Capabilities (FR-22 to FR-25)

- **FR-022 Profile Format**: System MUST define portable analyzer profile JSON
  schema containing complete reusable analyzer configuration (connection
  defaults, extraction, aggregation, mappings, transforms, QC rules, abnormal
  flag mapping).

  - Profile imports MUST execute test-code auto-match with statuses: `mapped`,
    `suggested`, `unmapped`.
  - Profile schema and OpenELIS compatibility versions MUST be validated on
    import.
  - `profileMeta.id` MUST represent stable profile lineage; profile revision
    identity MUST be (`profileMeta.id`, `profileMeta.version`).

- **FR-023 Profile Library**: System MUST support three profile sources:

  - Built-in (read-only, shipped in classpath resources)
  - Site Library (admin-uploaded DB-backed profiles)
  - Community import flow (download + upload)
  - Built-in profile catalog MUST include all profiles with validated JSON
    configuration files in `projects/analyzer-profiles/`. At initial release
    this includes 6 ASTM and 5 HL7 profiles (11 total):
    1. GeneXpert ASTM (Cepheid) — `genexpert-cepheid-astm`
    2. Sysmex XN-Series (ASTM) — `sysmex-xn-astm`
    3. Mindray BA-88A (ASTM) — `mindray-ba88a-astm`
    4. Horiba Pentra 60 (ASTM) — `horiba-pentra60-astm`
    5. Horiba Micros 60 (ASTM) — `horiba-micros60-astm`
    6. Stago Start 4 (ASTM) — `stago-start4-astm`
    7. GeneXpert HL7 (Cepheid) — `genexpert-cepheid-hl7`
    8. Mindray BC2000 (HL7) — `mindray-bc2000-hl7`
    9. Mindray BC5380 (HL7) — `mindray-bc5380-hl7`
    10. Mindray BS360E (HL7) — `mindray-bs360e-hl7`
    11. Abbott Architect (HL7) — `abbott-architect-hl7`
  - Additional profiles from addendum v1.2 (Cobas 6800, m2000, Panther, CELL-DYN
    Ruby) are deferred until validated JSON config files are created.
  - For each profile lineage (`profileMeta.id`), selector default MUST be the
    designated latest version.

- **FR-024 Profile Selection**: Add Analyzer modal MUST expose grouped profile
  selector with:

  - None (Start from Scratch)
  - Built-in
  - Site Library
  - Import from File
  - Selecting profile MUST pre-fill relevant analyzer settings and show profile
    info banner.
  - Export Profile action MUST strip site-specific identifiers and runtime
    state.
  - Applying a profile to an analyzer MUST create a configuration snapshot on
    the analyzer instance; later profile edits/imports MUST NOT auto-mutate
    existing analyzers.
  - Dedicated "Reapply Profile" workflow for existing analyzers is OUT OF SCOPE
    for this release.
  - Profile fields and analyzer-instance fields MUST be clearly separated:
    - Profile-level: reusable analyzer-type defaults/templates.
    - Instance-level: site runtime identity/connection values and operational
      state.
  - Profile may provide defaults for instance-level fields where sensible (for
    example default ports), but analyzer instance values remain authoritative
    after save.

- **FR-025 Lab Unit Assignment**: Add/Edit Analyzer modal MUST support
  multi-select assignment to system lab units.
  - Analyzer List MUST include Lab Units column and Lab Unit filter.
  - Lab unit assignment is organizational in v1 and MUST NOT change analyzer
    message processing behavior.

### Business Rules

- **BR-11 Port Conflict Validation**: Active/listening analyzers MUST NOT share
  conflicting listen ports on the same host scope.
- **BR-12 QC Required for Active**: Analyzer activation requires at least one
  valid QC identification rule.
- **BR-13 Transform Validation**: Transform configuration MUST be type-safe and
  complete for chosen transform type.
- **BR-14 Aggregation Window Range**: Session aggregation window MUST be in
  range 5-300 seconds.
- **BR-15 Simulator Preview-Only**: Simulator output MUST never persist to
  production result/QC records.
- **BR-16 Pending Code Limits**: Pending unmapped code queue is capped at 100
  per analyzer and purges after 30 days.
- **BR-17 ASTM Indexing**: Field/component references use ASTM 1-indexed
  conventions.
- **BR-18 Profile Schema Validation**: Imported profiles MUST pass schema
  validation.
- **BR-19 Profile Identity & Versioning**:
  - Site library MUST enforce uniqueness on (`profileMeta.id`,
    `profileMeta.version`).
  - Importing an existing lineage (`profileMeta.id`) with a new version is
    allowed.
  - Importing a duplicate (`profileMeta.id`, `profileMeta.version`) is rejected
    as duplicate artifact.
  - System MUST track one designated "latest" version per lineage for selector
    defaults.
  - "Latest" MUST auto-resolve to highest valid SemVer version by default.
  - Administrators MUST be able to override designated latest explicitly.
- **BR-20 Built-in Immutability**: Built-in profiles cannot be edited/deleted;
  duplication is allowed.
- **BR-21 Export Sanitization**: Export strips site-specific fields (IDs,
  concrete network settings, runtime state, direct OpenELIS bindings).
- **BR-22 Lab Unit Organizational Scope**: Lab unit assignments do not alter
  processing/routing behavior in v1.

### API Surface Requirements

All new endpoints MUST enforce role authorization consistent with CR-007.

#### Core APIs (new)

- QC rules CRUD for analyzer scope.
- Field extraction configuration read/write per analyzer.
- Test mapping transformation config read/write per mapping.
- Message simulation endpoint (preview-only execution).
- Pending/unmapped code query, action, and cleanup endpoints.

#### Profile + Lab Unit APIs (new)

- Profile library list/detail/import/update/delete (built-in restrictions
  apply).
- Analyzer profile export endpoint (sanitized JSON download).
- Analyzer lab unit assignment read/write endpoints.
- Profile import MUST apply BR-19 identity/versioning behavior and return
  explicit duplicate-version errors.

### Data Model Requirements

- Core configuration MUST introduce/persist in:

  - `astm_analyzer_config`
  - `astm_test_mapping_config`
  - `astm_qc_rule`
  - `astm_flag_mapping`
  - `astm_pending_code`

- Profile/lab unit capabilities MUST introduce:

  - `analyzer_profile` (with `profile_json` JSONB and source metadata)
  - `analyzer_profile_application` provenance table (records profile snapshot
    applications to analyzer instances; references source profile
    lineage/version without live FK on `analyzer` to avoid implying live linkage
    — consistent with snapshot-on-apply model)
  - `analyzer_lab_unit` junction table (`analyzer_id`, `lab_unit_id`)
  - Profile library storage MUST support uniqueness on (`profileMeta.id`,
    `profileMeta.version`) and designated-latest lineage metadata.

- Built-in profiles MUST load from classpath JSON resources at startup.

### Constitution Compliance Requirements (OpenELIS Global)

- **CR-001**: UI MUST use Carbon Design System components (`@carbon/react`)
  only; no mockup inline-style implementation in production.
- **CR-002**: All user-facing strings MUST use React Intl; no hardcoded UI
  strings.
- **CR-003**: Backend MUST follow 5-layer architecture
  (Valueholder→DAO→Service→Controller→Form).
- **CR-004**: Schema changes MUST use Liquibase changesets with rollback path
  where required.
- **CR-005**: FHIR/IHE constraints apply if this feature exposes externally
  shared analyzer resources.
- **CR-006**: Site/country variation MUST be configuration-driven, not
  branch-specific code.
- **CR-007**: Security MUST include RBAC for analyzer/profile management, audit
  trails, and input validation.
  - `LAB_USER`: read/list/view profile metadata and analyzer details.
  - `LAB_SUPERVISOR`: create/edit analyzer configuration, run simulator, manage
    mappings, activate analyzers.
  - `LAB_ADMIN`: profile-library destructive actions (delete/replace) and
    equivalent high-impact administrative operations.
- **CR-008**: Tests MUST cover unit/integration/E2E flows for profile handling,
  parsing config behavior, and analyzer setup UX.

### Key Entities

- **Analyzer**: Existing analyzer instance entity (2-table model with
  `analyzer_type`), now enhanced with profile and lab unit relationships.
- **AnalyzerProfile**: Portable JSON profile metadata + JSONB payload, with
  source and compatibility metadata.
- **Profile Application Snapshot**: Persisted analyzer configuration copy
  created at apply-time; references source profile lineage/version for
  provenance without live inheritance.
- **AnalyzerConfiguration** (`astm_analyzer_config`): Connection role, protocol,
  timeout/retry, extraction, aggregation settings.
- **AnalyzerTestMapping** (`astm_test_mapping`): Analyzer code to OpenELIS
  mapping plus transform settings.
- **AnalyzerQCRule** (`astm_qc_rule`): QC detection rules per analyzer.
- **AnalyzerFlagMapping** (`astm_flag_mapping`): Analyzer abnormal flag
  interpretation map.
- **AnalyzerPendingCode** (`astm_pending_code`): Auto-detected unmapped codes
  and triage state.
- **AnalyzerLabUnit** (`analyzer_lab_unit`): Many-to-many organizational
  assignment of analyzers to lab units.

---

## Acceptance Criteria Traceability

### Core Configuration (FR-14 to FR-21)

- Analyzer setup and Advanced tab both expose Connection Role with conditional
  fields and server default.
- QC rule builder supports 4 rule types and OR semantics; activation blocked
  when no rules configured.
- Transform selector supports all 5 transform types with type-specific
  validation.
- Field extraction page shows 9 overridable ASTM references with sensible
  defaults.
- Advanced tab supports 3 aggregation modes and validates session window range.
- Advanced tab supports editable abnormal flag mapping including custom flags.
- Simulator parses raw ASTM and displays mapping/transform warnings without
  persistence.
- Pending unmapped code workflow supports passive detection, queue cap, and
  retention purge.

### Profile + Lab Unit Capabilities (FR-22 to FR-25, from addendum)

- **AC-87**: Analyzer Profile selector appears in Add Analyzer with grouped
  options (None/Built-in/Site Library/Import).
- **AC-88**: Built-in profiles are listed and immutable.
- **AC-89**: Selecting profile pre-fills protocol/connection defaults.
- **AC-90**: Profile info banner shows metadata (name/version/author/test
  count).
- **AC-91**: Field Mappings auto-populates from selected profile.
- **AC-92**: Auto-matching resolves mapped/suggested/unmapped outcomes.
- **AC-93**: Import summary includes match statistics.
- **AC-94**: Import validates JSON schema and reports validation errors.
- **AC-95**: Imported profile is added to site library.
- **AC-96**: Export serializes configuration and strips site-specific data.
- **AC-97**: Export prompts for metadata before download.
- **AC-98**: Profile identity/version policy is enforced: duplicate
  (`id`,`version`) is rejected; same `id` with new `version` is accepted.
- **AC-99**: Profile/version compatibility check and warning for newer versions.
- **AC-100**: Lab Units filterable multi-select appears in Add/Edit analyzer.
- **AC-101**: Lab Unit selector sources active lab units.
- **AC-102**: Selected lab units appear as removable tags.
- **AC-103**: Analyzer List includes Lab Units column.
- **AC-104**: Analyzer List supports Lab Unit filtering.
- **AC-105**: Lab unit assignment persists and reloads correctly.

---

## Success Criteria _(mandatory)_

### Measurable Outcomes

- **SC-001**: Administrator can onboard a new analyzer from profile to saved
  setup in under 10 minutes for built-in profiles. _Note: This is a usability
  target validated via manual walkthrough during M4 regression, not an automated
  timing assertion._
- **SC-002**: For imported profiles, system reports deterministic mapping
  summary (`mapped/suggested/unmapped`) for 100% of profile test codes.
- **SC-003**: Analyzer activation is blocked in 100% of cases when QC rule set
  is missing or invalid.
- **SC-004**: Simulator parse operation returns preview results without
  persisting any records in 100% of tested runs.
- **SC-005**: Auto-detect queue enforces 100-record cap and 30-day purge
  behavior per analyzer.
- **SC-006**: Exported profiles pass schema validation and exclude site-specific
  fields in 100% of tested exports.
- **SC-007**: Lab Unit filter narrows analyzer list correctly with set-overlap
  semantics for selected units.
- **SC-008**: All new UI strings for this feature are localized (at least
  English/French), with no hardcoded production labels.

---

## Assumptions & Constraints

### Assumptions

1. Behavior for FR-14 through FR-21 is sourced from Jira OGC-337 and the mockup
   in
   `specs/012-generic-astm-plugin-profiles/docs/analyzer-mapping-templates-mockup.jsx`.
2. Behavior for FR-22 through FR-25 is sourced from
   `specs/012-generic-astm-plugin-profiles/docs/astm-analyzer-mapping-addendum-v1_2.md`.
3. Existing Feature 004 analyzer mapping infrastructure and status model remain
   the base extension point.
4. Analyzer model from spec 011 / PR #2802 (analyzer + analyzer_type) remains
   authoritative for integration.

### Constraints

1. Implementation MUST use Carbon React components despite mockup inline style
   presentation.
2. Profile payload MUST remain portable and avoid embedding deployment-specific
   identifiers.
3. Database schema changes MUST be versioned Liquibase migrations in project
   versioned directories.
4. This scope does not include functional result-routing behavior by lab unit
   (organizational only in v1).
5. This scope excludes dedicated profile reapply workflow for existing
   analyzers.

---

## References

- `specs/012-generic-astm-plugin-profiles/docs/analyzer-mapping-templates-mockup.jsx`
- `specs/012-generic-astm-plugin-profiles/docs/astm-analyzer-mapping-addendum-v1_2.md`
- `specs/012-generic-astm-plugin-profiles/docs/astm-crosswalk-gap-analysis.md.pdf`
- `specs/004-astm-analyzer-mapping/spec.md`
- `specs/011-madagascar-analyzer-integration/spec.md`
- `.specify/memory/constitution.md`
