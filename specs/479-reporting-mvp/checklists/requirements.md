# Specification Package Quality Checklist

**Feature**: [Configurable Reporting MVP](../spec.md)  
**Review date**: 2026-09-13  
**Purpose**: Assess the specification package, not application readiness.

## Scope and Clarification

- [x] Defines a useful native reporting outcome with four prioritized,
      independently testable user stories.
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

**Clarification checkpoint**: Completed. Five topics were resolved or explicitly
interpreted: instance-aware coverage/low friction, both layouts, preservation of
repeats, shared saved definitions and common configuration across report types.
No product-answer placeholder remains. The last topic's implementation
interpretation is visible in the specification rather than represented as an
explicit user selection of three independent workstreams.

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
- [x] Uses two validation milestones: common capability/Sample & Testing, then
      additional source mappings and recovery; one implementation PR per
      milestone.
- [x] Maps all 22 functional requirements and nine success criteria to 31 tasks.
- [x] Plans meaningful fixture, unit, ORM, integration and real core-app
      Playwright checks before accepting implementation.
- [x] Schedules actual component/result linkage and referral/event mapping
      proofs; configuration is not presented as automatic discovery of arbitrary
      joins.
- [x] Separates proposed engineering choices and future validation from verified
      document quality, CI, deployment and user acceptance.

## Cross-Artifact Review

The eight documents are the specification, plan, tasks, research, logical model,
API contract, acceptance quickstart and this checklist. They use the same scope:
dynamic catalog; two Sample & Testing layouts; all repeats; shared definitions;
three source types through one engine; direct ready download and a basic queue.

The 90-day range, five active jobs and seven-day retention are initial
configurable defaults inherited from the product source. A single background job
path with inline ready download is the MVP engineering choice. Separate
estimation and synchronous generation, personal libraries, scheduling, arbitrary
source discovery and new external integrations remain outside scope.

Document validation covers SpecKit prerequisite discovery, Markdown formatting,
relative/source file links, balanced fenced blocks, the JSON request example,
four user stories, unique sequential FR-001–FR-022 and T001–T031 identifiers,
nine success criteria and requirement/task coverage. Final review checks the
same semantics across artifacts; a passing identifier count alone is
insufficient.

**Validation result**: Passed on 2026-09-13. SpecKit discovered the package;
formatting passed for all eight documents; 36 local/source links and the JSON
example resolved; story, requirement, success-criterion and task counts matched;
all 22 functional requirements have task mappings. Manual consistency review
reconciled shared definitions versus private files, source-specific dates,
default versus selectable statuses, layout-specific row counts and inline
delivery.

T002 must prove Sample & Testing values, components and instance catalog
changes. T020 must prove referral/event identities and dates, rejection links
and another configuration over an existing source. All application tasks remain
unchecked. Completing this document checklist means the package can guide
implementation; it does not claim application tests, CI, deployment or user
acceptance have passed.
