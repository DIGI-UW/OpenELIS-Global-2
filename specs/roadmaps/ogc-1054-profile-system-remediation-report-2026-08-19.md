# OGC-1054 Profile System Grounded Remediation Report

**Date:** 2026-08-19

**Status:** Grounded remediation companion to the authoritative roadmap.

## 1. Executive finding

The established analyzer profile system is the implementation baseline. It is
Bridge-owned in architecture and already supports the two intended profile jobs:

1. define communication with one analyzer type; and
2. define defaults used when OpenELIS creates an instance of that analyzer type.

The current BR-E0/BR-M1 work did not evolve that system. It introduced a second,
thinner `portable-profile` contract and built a separate lifecycle catalog around
it. That contract omits important established communication fields and the
profile's OE instance defaults. The current BR-M1 branch also ships none of the
20 established profiles. OE-M1 consequently selects profile metadata but fills
profile-owned settings from frontend constants.

This is an unmerged stack defect. The established system remains intact on
`develop`. PRs #45, #46, #4055, and #4056 must not merge in their current form.

## 2. Evidence examined

- Current OE and Bridge `develop` code and history.
- The active OE R0/F0/E0/M1 and Bridge E0/M1 worktrees and PR diffs.
- The established 20-profile corpus under `projects/analyzer-profiles`.
- OE profile discovery, profile application, analyzer creation, and Bridge
  registration code.
- Bridge registration, ASTM/HL7/FILE parsing, FILE watching, and startup sync.
- Analyzer-mock profile adapter and transport templates.
- The running local `ogc1054` stack, including container mounts and live OE and
  Bridge responses.
- GeneXpert ASTM and FluoroCycler profile bodies returned by the running stack.

This report does not use Jira or technical-looking material in `openelis-work`
as engineering authority. `openelis-work` remains functional/visual input only.

## 3. What the established system actually does

### 3.1 Profile source and discovery

- The running OE container mounts the E0 worktree's
  `projects/analyzer-profiles` directory read-only at
  `/data/analyzer-profiles`.
- Live `GET /rest/analyzer/profiles` returned 20 profiles: 6 ASTM, 7 HL7, and 7
  FILE.
- The repository README describes the files as profiles consumed by the three
  generic Bridge plugins. Their current physical packaging through OE is not
  their architectural ownership.

### 3.2 Analyzer setup

The established OE form fetches the selected full profile and uses many of its
communication/configuration fields to prefill the new analyzer. It is not yet a
complete consumer: protocol version and some empty-state values still come from
frontend constants/fallbacks instead of the selected profile. The create request
sends `defaultConfigId`. OE then loads that profile and currently:

- stores instance values on `Analyzer`;
- creates local analyzer-code-to-test bindings;
- copies plugin/file defaults;
- copies control-recognition rules into `AnalyzerQcRule`; and
- registers the resulting effective instance configuration with Bridge.

The last two bullets are remediation targets, but they do not negate the
working profile-driven setup.

### 3.3 Bridge runtime

Bridge receives an expanded registration rather than the complete profile
document. It owns and executes listeners, protocol parsing, FILE watching,
normalization, and transport. The running Bridge registry proves profile-derived
configuration is active:

- GeneXpert is registered at `10.42.20.10` as ASTM with profile-derived
  `O.12 == Q` control recognition.
- FluoroCycler is registered with its FILE pattern, XLSX format, profile column
  mappings, test codes, and control-recognition rules.

This is why the existing GeneXpert and Fluoro flows and videos work. The current
packaging/registration shape is imperfect, but it is a functioning Bridge-owned
profile system, not an absent system.

### 3.4 Mock

The mock has a profile adapter that derives assay fields from the established
profile files while transport templates provide simulation mechanics and test
values. Only part of the template library is profile-backed, so drift remains
possible, but GeneXpert/Fluoro are valid compatibility anchors.

## 4. What was actually wrong with the established implementation

The architecture and two profile jobs were not wrong. The concrete debt is:

1. **No durable profile pin.** `defaultConfigId` is transient. A configured
   analyzer does not retain an authoritative profile ID and immutable revision.
2. **Packaging leakage.** Bridge-owned profiles are mounted and served through
   OE instead of a Bridge catalog.
3. **Loose schema.** The current schema permits inconsistent fields and unknown
   properties; several profiles do not describe communication/defaults with the
   same completeness.
4. **Incomplete default ownership.** The current OE form hardcodes protocol
   version and fallback values even after loading the selected profile. This is
   existing debt to remove, not behavior to preserve.
5. **Copied configuration.** OE expands one profile into analyzer fields,
   mapping rows, plugin JSON, and `AnalyzerQcRule`, obscuring provenance.
6. **No managed lifecycle.** There is no proper Draft, Publish, Duplicate,
   immutable revision, retirement, or explicit adoption workflow.
7. **Hidden analyzer-specific runtime behavior.** Bridge contains hardcoded
   FluoroCycler synonyms, ASTM `O.12 == Q` control fallback, FILE control
   prefixes, and Cepheid-specific result filtering in generic parser code.
8. **Partial mock alignment.** Many mock templates still duplicate profile
   content instead of consuming the same revision.
9. **Uneven profile data quality.** Existing mapping rows require evidence-based
   curation. Equal LOINC values do not prove aliases, and existing rows are not
   automatically obligations to preserve.

These require hardening and cleanup around the existing system, not replacement
of the profile model.

## 5. How BR-E0/M1 diverged

### 5.1 BR-E0 contract

`portable-profile.schema.json` defines identity, protocol family, three
capability booleans, test rows, control recognition, and limited FILE settings.
It omits or cannot faithfully express established fields including:

- protocol version;
- supported transports and transport-specific defaults;
- communication direction/role;
- connection/default field values;
- parser/extraction/aggregation options;
- serial settings;
- several FILE semantics used by existing profiles; and
- the general `configDefaults` contract used to create an OE instance.

It is therefore not a versioned form of the established contract.

### 5.2 BR-M1 lifecycle

BR-M1 contains useful lifecycle infrastructure, but it catalogs the wrong
contract. Its current branch contains no shipped analyzer profile resources.
The safety branch once contained mechanically transformed copies, but those
copies also omitted the established default-config semantics and preserved rows
without semantic curation. They must not be restored as-is.

### 5.3 OE-M1 consumer and UX

OE-M1 introduces a Bridge-backed type catalog and profile revision fields, but
the selected catalog item is metadata-only. The established form's incomplete
protocol fallback remains, while its full-profile fetch and application of
communication and FILE defaults disappear. `AnalyzerForm` sets protocol version
from `PLUGIN_PROTOCOL_DEFAULTS`, initializes communication mode from
`DEFAULT_COMMUNICATION_MODE`, and never receives the selected revision's
complete defaults. This broadens an existing default-ownership defect into a
direct regression from the profile-driven setup the UX is supposed to improve.

### 5.4 Acceptance failure

No E0 gate required the same established GeneXpert and Fluoro profiles to
produce equivalent form defaults, instance configuration, Bridge registration,
mock traffic, and visible result flow. Schema tests passed while behavioral
parity was unproven.

## 6. Proper profile contract

The target must be an additive, strict evolution of the established
`analyzer-defaults` contract. Naming can be normalized in an approved contract
amendment, but none of the two jobs may be lost.

### 6.1 Contract principles

- One profile represents one analyzer type and supported communication mode.
- Bridge is the canonical profile owner and runtime interpreter.
- Published revisions are immutable; an analyzer pins profile ID plus revision.
- Updating or duplicating a profile never moves a configured analyzer.
- OE stores instance values and local catalog bindings, not an authoritative
  copied profile.
- Analyzer-specific behavior must be expressed by profile data or a versioned
  generic plugin option, never a model-name/code fallback.
- Production code contains no hard-coded profile-ID/revision, manufacturer,
  model, display-name, analyzer-code, fixture-name, or vendor-value special case
  and no duplicate of a profile-owned default. Generic lookup uses values read
  from profile data/pins. Named analyzers exist only in profile data and
  parameterized tests; the validator/consumer/runtime path remains generic.
- Operational QC is not profile configuration and never gates activation.

### 6.2 Required, conditional, generated, and optional content

| Contract area                  | Presence                                                                                           | Required semantics                                                                                                                                                                                                                                                                      |
| ------------------------------ | -------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Profile identity               | Required                                                                                           | Stable profile ID, display name, manufacturer, model/type label, category, contract version, and evidence/confidence sufficient for publication review                                                                                                                                  |
| `protocol`                     | Required                                                                                           | ASTM/HL7/FILE family plus every protocol version, transport, direction/role, analyzer-identity, parser/extraction, aggregation, and capability value needed for deterministic Bridge execution                                                                                          |
| `configDefaults`               | Required                                                                                           | Every safe profile-owned default used to create an OE analyzer instance, including the declared communication/data-flow default and applicable port, serial, or FILE defaults. It never contains the analyzer name at a site, lab units, address, credentials, or watch directory       |
| `default_test_mappings`        | Required for a publishable fixed-vocabulary profile                                                | Every distinct emitted analyzer test/result concept known for that revision, with raw code, display/result type, unit when meaningful, raw qualitative vocabulary when applicable, portable coding hints when evidenced, and proven aliases. No OpenELIS database identifier is allowed |
| `controlResultRecognition`     | Required                                                                                           | Exactly `RULES` with one or more evaluable OR matchers, or affirmed `NONE`; Bridge evaluates it before normalized delivery. It is neither `AnalyzerQcRule` nor Westgard/operational QC                                                                                                  |
| ASTM or HL7 execution settings | Protocol-conditional                                                                               | Framing/message version, supported socket/serial transport, connection role/direction, identity fields, parser/extraction options, and order/result capabilities actually supported by that profile                                                                                     |
| FILE execution settings        | Protocol-conditional                                                                               | Format/extensions, filename pattern, encoding, delimiter/header/sheet/row handling, complete column semantics, locale/date/unit behavior, and every other setting Bridge needs to watch and transport the instance's configured directory                                               |
| Raw values, aliases, and units | Field-conditional                                                                                  | Present only when the emitted concept uses them. Aliases require evidence that they are alternate encodings of one concept; a shared LOINC alone never makes two rows aliases                                                                                                           |
| Authoring notes/evidence links | Optional runtime content; required by publication policy where confidence is not already validated | Human-review provenance that does not alter runtime execution                                                                                                                                                                                                                           |
| Revision/fingerprint/status    | Catalog-generated                                                                                  | Immutable revision, content fingerprint, lifecycle status, lineage, publication actor/time, and retirement metadata. Authors do not supply or override these values                                                                                                                     |

A fixed-vocabulary profile with no result definition, or a profile missing a
setting needed by its selected protocol/transport, cannot be published. Future
dynamic-vocabulary support requires its own approved contract amendment; E0/M1
must not silently treat an empty mapping list as that capability.

The established `configDefaults.qcRules` arrays are a mixed-name source field,
not a target domain. During profile-by-profile curation, valid analyzer-message
identification semantics are rewritten into `controlResultRecognition`; invalid,
duplicated, or unsupported rules are corrected or removed with evidence. The old
array is then deleted. Nothing creates an OE `AnalyzerQcRule`, operational-QC
configuration, a compatibility reader, or a legacy rule representation.

### 6.3 Forbidden profile content

- OE test/result/control-lot database IDs;
- analyzer instance name, site IP, lab unit, credentials, or watch directory;
- QC means, SDs, Westgard policies, release policy, or alert state;
- copied plugin JSON or arbitrary unvalidated extension keys;
- hidden fallback behavior or a second per-analyzer profile copy.

### 6.4 Lifecycle

1. Create or Duplicate produces a draft based on an existing revision or a
   complete new profile.
2. Publish validates the full contract and creates an immutable revision.
3. A new analyzer selects and pins that revision; the profile supplies visible
   defaults and OE collects site-specific values.
4. Update shared creates a new revision. Existing analyzers remain pinned until
   a user reviews the diff and explicitly adopts it.
5. Retired revisions remain readable while referenced.

## 7. Retain, rewrite, and remove

### Retain and rewire

- BR-M1 append-only storage, atomic writes, immutable revision history,
  fingerprints, authentication, audit publications, Duplicate/Update lifecycle,
  and durable catalog path.
- OE-M1 Analyzer Types URL/breadcrumb/table scaffolding and the concept of an
  analyzer profile ID/revision pin.
- Existing GeneXpert and Fluoro profiles, mocks, captures, and user stories as
  mandatory compatibility fixtures.
- Bridge generic protocol, listener, transport, normalization, and FILE watcher
  implementations where behavior is genuinely protocol-generic.

### Rewrite

- BR-E0 profile schema, fixtures, and documentation as an evolution of the
  established contract.
- BR-M1 validation/catalog responses so they expose complete profile behavior
  and defaults and ship the curated profile catalog.
- OE-E0 cutover characterization: old files are baseline profile content to
  curate, not legacy rows to preserve mechanically.
- OE-M1 profile adapter and setup form so selection fetches/applies actual
  profile defaults and retains a revision pin.
- Mock adapter/templates to consume the same accepted profile revision.
- Roadmap E0/M1 language and acceptance gates.

### Remove before G0

- The parallel thin `portable-profile` model and fixtures.
- Any mechanical preserve-every-row/`LEGACY_UNBOUND` migration model.
- Profile-selected frontend defaults sourced from
  `PLUGIN_PROTOCOL_DEFAULTS`/`DEFAULT_COMMUNICATION_MODE`.
- OE `AnalyzerQcRule` entity/service/controller/UI and profile-to-QC-rule copy.
- Bridge model-name, analyzer-code, QC-classifier, and scanner-synonym fallbacks.
- OE filesystem profile authority, `defaultConfigId`, and old profile-apply
  pathway after the Bridge-backed path has behavioral parity.
- Duplicate per-analyzer mapping/profile editors and independently maintained
  mock assay menus.

## 8. PR-stack disposition

| PR            | Disposition                                                                                                    |
| ------------- | -------------------------------------------------------------------------------------------------------------- |
| OE #4049 R0   | Amend roadmap principles, E0/M1 requirements, and acceptance gates before implementation resumes               |
| OE #4053 F0   | Keep acceptance infrastructure; add established-contract and GeneXpert/Fluoro parity fixtures                  |
| Bridge #45 E0 | Substantially rewrite the profile contract; keep versioned contract-test infrastructure                        |
| OE #4055 E0   | Delete preservation-oriented migration semantics; rewrite as consumer/parity contract and removal plan         |
| Bridge #46 M1 | Keep lifecycle engine; rewire it to the evolved established contract and ship curated profiles                 |
| OE #4056 M1   | Keep linkable Analyzer Types UI/pin scaffolding; rewrite profile loading, defaults, counts, and setup behavior |

Preserve the current commits as Git provenance. Correct the open PRs with
explicit remediation commits and updated descriptions; do not merge the
current behavior or create a second permanent stack.

## 9. Deterministic recovery gates

1. **Contract gate:** GeneXpert and Fluoro established profile documents validate
   under the evolved contract without losing communication/default semantics.
2. **Catalog gate:** Bridge ships and returns those exact revisions, including
   complete defaults; no OE profile mount is needed for the new path.
3. **OE component gate:** RTL with a real router proves selecting each profile
   populates its real defaults, preserves URL state, and sends a profile pin plus
   user-entered instance values.
4. **Bridge gate:** contract/integration tests prove pinned profiles drive ASTM
   and FILE runtime behavior with no OE classifier or model-specific fallback.
5. **Mock gate:** GeneXpert and Fluoro mock traffic is derived from the same
   accepted revisions and reaches the expected normalized output.
6. **Assembled parity gate:** run the existing and replacement paths against the
   isolated OE/Bridge/mock stack and compare form defaults, effective instance
   config, Bridge registration, inbound traffic, and visible result outcome.
7. **Removal gate:** only after parity, delete the OE-hosted profile/apply path,
   copied QC-rule path, hardcoded fallbacks, and duplicate mock/profile data.
8. **UI acceptance gate:** UI-only Playwright covers profile discovery, analyzer
   creation, connection test, verification, activation, and result/control flow;
   inspect trace/screenshots/console before MP4 and remote UAT.

## 10. Immediate next action

Amend R0 first with this corrected baseline and the parity gates. Then add the F0
compatibility guards, correct Bridge #45 under those failing tests, flow the
accepted contract through OE #4055, and rewire Bridge #46 and OE #4056 in that
order. M1 remains the active roadmap checkpoint, but no M1 remediation is
accepted until the corrected F0/E0 stack proves established GeneXpert and Fluoro
behavior is preserved. No later milestone proceeds around the broken contract.

## 11. Executable remediation checklist

The checklist is dependency ordered. A box changes only when its evidence is
present in the owning PR. Passing a schema/unit test without the named behavior
does not complete an item.

### R0 - Correct the source of truth

- [x] Add this report to the roadmap's canonical engineering references.
- [x] State explicitly that the established Bridge-owned profile system is the
      implementation baseline and that ownership/lifecycle changes are additive.
- [x] Define the profile's two jobs and the required/conditional/forbidden
      contract semantics in the roadmap, feature spec, plan, and tasks.
- [x] Replace “migration input,” preserve-every-row, and `LEGACY_UNBOUND`
      language with evidence-based profile curation and clean replacement.
- [x] Add GeneXpert ASTM and FluoroCycler compatibility as blocking E0 gates.
- [x] Keep M1 as the sole active implementation checkpoint after the corrected
      E0 contract stack is review-ready.

**R0 exit evidence:** roadmap/spec/plan/tasks/acceptance matrix agree; repository
search finds no contradictory profile-replacement or row-preservation mandate;
PR #4049 description identifies the correction.

### F0 - Make the acceptance foundation detect this regression

- [x] Add fixtures representing the established GeneXpert ASTM and
      FluoroCycler profiles without translating them into a second semantic
      model or adding analyzer-specific validator/consumer branches.
- [x] Add contract checks that require both profile jobs: runtime communication
      and OE instance defaults.
- [x] Add a generic production guard rejecting hard-coded
      profile/model/manufacturer/code special cases and duplicated profile-owned
      defaults; fixture names may occur only in parameterized test data.
- [x] Add stack guards proving no accepted profile path depends on an OE FILE
      poller, `AnalyzerQcRule`, copied profile authority, or hidden classifier.
- [x] Document the exact layered parity matrix used by E0/M1.

**F0 exit evidence:** each new guard is demonstrated red against the current
wrong BR-E0/OE-M1 assumptions and green only against the corrected contract.
The focused compatibility suite passes 10 tests, the complete F0 policy suite
passes 64 tests, and the Playwright bucket/dependency guard passes. Applying the
guard to the actual stack rejects Bridge E0's metadata-only profile and OE M1's
source-owned selected-profile defaults. Both established full profile documents
pass through one parameterized path with no analyzer-specific implementation
branch.

### BR-E0 - Evolve the established contract

- [ ] Replace the thin `portable-profile` semantics with a strict additive
      evolution of `analyzer-defaults` that retains communication and defaults.
- [ ] Define protocol-discriminated ASTM, HL7, and FILE requirements.
- [ ] Define generated revision/fingerprint/publication metadata separately
      from authored profile behavior.
- [ ] Define `controlResultRecognition` as Bridge runtime behavior and exclude
      operational QC.
- [ ] Add semantic validation for code/alias/value uniqueness and conditional
      completeness; schema validity alone is insufficient.
- [ ] Make GeneXpert and Fluoro fixtures pass without losing a field used by the
      current setup or runtime.

**BR-E0 exit evidence:** Bridge contract tests validate both fixtures and reject
missing communication/default behavior, duplicate raw identities, operational-QC
content, and instance-specific/site identifiers.

### OE-E0 - Define the clean consumer and removal boundary

- [ ] Replace legacy-row migration dispositions with profile-by-profile
      curation dispositions: retain, correct, alias, split, or remove with evidence.
- [ ] Delete `LEGACY_UNBOUND` and preserve-every-row requirements.
- [ ] Define the OE consumer contract: profile ID/revision, instance values,
      local catalog bindings, verification/audit, and desired Bridge registration.
- [ ] Define one-way removal of `defaultConfigId`, OE profile serving/apply,
      copied profile JSON, and `AnalyzerQcRule` after parity.
- [ ] Prove no current analyzer is moved to another revision implicitly.

**OE-E0 exit evidence:** JUnit/contract tests fail on heuristic inference,
mechanical row preservation, copied authority, or an implicit revision move.

### BR-M1 - Put lifecycle around the real profiles

- [ ] Reuse the append-only catalog, immutable revisions, fingerprints,
      authentication, audit, and Duplicate/Update lifecycle.
- [ ] Rewire catalog validation and APIs to the corrected E0 contract.
- [ ] Convert all 20 profiles with an explicit evidence-based disposition; do
      not restore the mechanical safety-branch conversion.
- [ ] Ship the curated profiles from Bridge and expose complete profile detail
      and defaults.
- [ ] Make Duplicate create an editable draft and Publish create an immutable
      selectable revision; existing analyzers remain pinned.
- [ ] Remove model-name/code classifier behavior once the corresponding profile
      data and tests are present.

**BR-M1 exit evidence:** fresh Bridge startup exposes the curated catalog;
GeneXpert/Fluoro profile/runtime tests pass; no profile resource is required
from OE; published revisions cannot mutate or disappear while referenced.

### OE-M1 - Restore profile-driven setup and Analyzer Types UX

- [ ] Keep the linkable Analyzer Types table, filters, breadcrumbs, actions,
      and profile pin model.
- [ ] Fetch complete profile detail from Bridge and render its communication,
      mapping, recognition, and default summaries.
- [ ] Populate analyzer setup from the selected profile's real defaults; remove
      `PLUGIN_PROTOCOL_DEFAULTS` and `DEFAULT_COMMUNICATION_MODE` as selected-profile
      authorities.
- [ ] Persist the profile ID/revision and site-entered instance values without
      an authoritative copied profile.
- [ ] Make Duplicate/Update return through bookmarkable URL state and never
      repoint an analyzer implicitly.
- [ ] Remove the old OE profile/default/application path after assembled parity.

**OE-M1 exit evidence:** RTL with a real router proves exact GeneXpert and
Fluoro defaults, URL/history/reload/breadcrumb behavior, and explicit pins;
assembled UI-only Playwright proves both setup stories against Bridge/mock.

### Stack closure

- [ ] Run targeted red-green-refactor evidence at every owning layer.
- [ ] Run OE JUnit/RTL/format, Bridge unit/integration/contract/format, and mock
      profile/transport suites.
- [ ] Run the isolated assembled stack and inspect registration, console, trace,
      screenshots, runtime state, and GeneXpert/Fluoro result outcomes.
- [ ] Update PR #4049, #4053, Bridge #45, OE #4055, Bridge #46, and OE #4056
      descriptions and dependencies to describe the corrected contract.
- [ ] Run `digi-uw/code-qa` alignment, coverage, simplicity/legacy, companion,
      and evidence gates.

**Recovery exit:** the open stack implements one evolved profile system, the
roadmap and tests prevent recurrence, M1 is reviewable against real profile
parity, and roadmap execution can resume from the next unchecked M1 slice.

## 12. Primary code references

- Existing profile definition and consumers:
  [`projects/analyzer-profiles/README.md`](../../projects/analyzer-profiles/README.md)
- GeneXpert profile:
  [`genexpert-astm.json`](../../projects/analyzer-profiles/astm/genexpert-astm.json)
- Fluoro profile:
  [`fluorocycler-xt.json`](../../projects/analyzer-profiles/file/fluorocycler-xt.json)
- Existing OE setup:
  [`AnalyzerForm.jsx`](../../frontend/src/components/analyzers/AnalyzerForm/AnalyzerForm.jsx)
- Existing OE profile application:
  [`AnalyzerRestController.java`](../../src/main/java/org/openelisglobal/analyzer/controller/AnalyzerRestController.java)
- Existing Bridge registration:
  [Bridge `AnalyzerRegistrationController.java`](https://github.com/DIGI-UW/openelis-analyzer-bridge/blob/develop/src/main/java/org/itech/ahb/controller/AnalyzerRegistrationController.java)
- Existing mock profile adapter:
  [analyzer-mock `profile_adapter.py`](https://github.com/DIGI-UW/analyzer-mock-server/blob/main/profile_adapter.py)
- Wrong BR-E0 contract:
  [BR-E0 `portable-profile.schema.json`](https://github.com/DIGI-UW/openelis-analyzer-bridge/blob/codex/ogc-1054-e0-contracts/contracts/analyzer/v1/portable-profile.schema.json)
- Current BR-M1 lifecycle:
  [BR-M1 `PortableProfileCatalog.java`](https://github.com/DIGI-UW/openelis-analyzer-bridge/blob/codex/ogc-1054-m1-profile-lifecycle/src/main/java/org/itech/ahb/profile/PortableProfileCatalog.java)
- Regressed OE-M1 setup:
  [OE-M1 `AnalyzerForm.jsx`](https://github.com/DIGI-UW/OpenELIS-Global-2/blob/codex/ogc-1054-m1-analyzer-types/frontend/src/components/analyzers/AnalyzerForm/AnalyzerForm.jsx)
