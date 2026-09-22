# Specification Package Quality Checklist

**Feature**: [Configurable Reporting MVP](../spec.md)  
**Review date**: 2026-09-13  
**Purpose**: Assess the specification package, not application readiness.

## Scope and Clarification

- [x] Defines a useful native reporting outcome with four prioritized,
      independently testable user stories.
- [x] Makes a public Catalyst deployment and review-tooling UAT readiness an
      explicit completion gate without treating checklist availability as human
      acceptance.
- [x] Pins the product source and distinguishes the mock's three types from its
      Sample & Testing worked example.
- [x] Records instance-aware fields without a fixed numerical catalog limit.
- [x] Requires both Sample & Testing layouts, spreadsheet by default, with all
      repeated results preserved through additional rows.
- [x] Includes shared instance-wide named definitions, fresh dates on reuse and
      existing access without personal-library or invitation machinery.
- [x] Uses one configurable builder, execution path and queue; source mappings
      handle different record relationships and date meanings.
- [x] Labels preserving all three mock types and proving Sample & Testing first
      as the implementation interpretation of the generalization discussion.
- [x] Keeps ordinary access checks in background application behavior and offers
      completed downloads in the current flow.
- [x] Excludes the withdrawn, unrelated query-panel UI request.

**Specification checkpoint**: Completed. The functional draft was presented for
discussion, and the initial fixed-field restriction was superseded.

**Initial clarification checkpoint**: Completed. Five topics were resolved or explicitly
interpreted: instance-aware coverage/low friction, both layouts, preservation of
repeats, shared saved definitions and common configuration across report types.
No product-answer placeholder remains. The last topic's implementation
interpretation is visible in the specification rather than represented as an
explicit user selection of three independent workstreams.

Implementation exposed three additional product questions. The pinned mock's
sent-date rule and the user's repeat-preservation requirement resolve the two
Referral questions; Referrals is connected and has passed public CSV checks.
Only native rejection records without an event date remain unresolved. Their
inclusion rule pauses Non-Conformance activation, not the other sources. See
`execution.md` for the dated decision and validation evidence.

The later turnaround clarification is also resolved: spreadsheet durations
belong to each test/result, including repeats; collection-to-receipt stays
specimen-level. User Story 1 includes the corresponding acceptance scenario.

## Implementation Readiness

- [x] Separates functional requirements from implementation responsibilities.
- [x] Grounds reuse in inspected source at the baseline recorded in the plan.
- [x] Gives configured fields stable identities and captured labels; renames do
      not change stored files or silently retarget requests.
- [x] Defines date anchors, specimen grouping, repeat preservation, CSV
      formatting, default status filters, empty results and stale configuration
      behavior.
- [x] Distinguishes shared report configuration from owner-scoped jobs/files.
- [x] Defines retained drafts, immutable submissions, retry, cancellation,
      restart, expiry and bounded execution without a new external service.
- [x] Keeps two functional milestones for traceability: common capability/Sample
      & Testing, then additional source mappings and recovery. The ten-PR stack
      in `review-stopping-point.md` supersedes the old branch packaging.
- [x] Maps all 23 functional requirements and ten success criteria to 38 tasks.
- [x] Plans meaningful fixture, unit, ORM, integration and real core-app
      Playwright checks before accepting implementation.
- [x] Schedules actual component/result linkage and referral/event mapping
      proofs; configuration is not presented as automatic discovery of arbitrary
      joins.
- [x] Separates proposed engineering choices and future validation from verified
      document quality, CI, deployment and user acceptance.

## Cross-Artifact Review

The ten documents are the specification, plan, tasks, research, logical model,
API contract, acceptance quickstart, execution ledger, UAT contract and this
checklist. They use the same scope:
dynamic catalog; two Sample & Testing layouts; all repeats; shared definitions;
three source types through one engine; direct ready download and a basic queue.

The 90-day range, five active jobs and seven-day retention are initial
configurable defaults inherited from the product source. A single background job
path with inline ready download is the MVP engineering choice. Separate
estimation and synchronous generation, personal libraries, scheduling, arbitrary
source discovery and new external integrations remain outside scope.

Document validation covers SpecKit prerequisite discovery, Markdown formatting,
relative/source file links, balanced fenced blocks, the JSON request example,
four user stories, unique sequential FR-001–FR-023 and T001–T038 identifiers,
ten success criteria and requirement/task coverage. Final review checks the
same semantics across artifacts; a passing identifier count alone is
insufficient.

**Validation result**: Passed on 2026-09-13 after the UAT extension. Formatting
passed for all ten documents; 41 local links and the JSON example resolved;
four stories, FR-001–FR-023, SC-001–SC-010 and T001–T038 were sequential and
all requirements had task mappings. Manual consistency review
reconciled shared definitions versus private files, source-specific dates,
default versus selectable statuses, layout-specific row counts and inline
delivery.

T002 must prove Sample & Testing values, components and instance catalog
changes. T020 must prove referral/event identities and dates, rejection links
and another configuration over an existing source. At this specification
checkpoint all application tasks were unchecked; their current implementation
status is tracked in `tasks.md`. Completing this document checklist means the package can guide
implementation; it does not claim application tests, CI, deployment or user
acceptance have passed.
