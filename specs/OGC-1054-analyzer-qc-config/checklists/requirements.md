# OGC-1054 Requirements Quality Checklist

Run this checklist during R0 review and whenever scope or acceptance changes.
It validates the specification, not implementation progress.

## Source And Authority

- [x] The roadmap identifies repository specs and `openelis-work` functional/
      visual artifacts as the product sources.
- [x] Jira is traceability only and supplies no scope, status, acceptance,
      architecture, implementation, or testing decision.
- [x] Every `openelis-work` reference is used only for visible behavior,
      terminology, workflow, or visual intent.
- [x] Engineering decisions are grounded in repository code, engineering
      specifications, ADRs, or versioned contracts.

## Scope

- [x] The full MVP is stated as an end-to-end lab workflow.
- [x] Foundation behavior is not labeled MVP.
- [x] Post-MVP OGC-1054 work and unrelated analyzer-program work are distinct.
- [x] Multi-component and Results/Validation work remain outside this train.

## Architecture

- [x] Bridge exclusively owns profiles and analyzer-facing runtime.
- [x] The established working profile system is the implementation baseline;
      revision/catalog work evolves it rather than introducing a second model.
- [x] A profile's two jobs are explicit: analyzer-type communication/runtime
      behavior and defaults for a new OpenELIS analyzer instance.
- [x] GeneXpert ASTM and FluoroCycler are blocking, unabridged compatibility
      fixtures for profile-contract changes.
- [x] Bridge profiles explicitly own control-result recognition with
      deterministic `RULES`/affirmed-`NONE` semantics, no undocumented-as-`NONE`
      shortcut, and no hidden fallback.
- [x] Profile revisions are immutable/retained while referenced; Update shared
      and Duplicate Profile never move a pinned analyzer implicitly, and
      OpenELIS does not preserve an authoritative copied-profile snapshot.
- [x] OpenELIS owns local bindings, audit, operational QC, activation, held
      results, alerts, and review.
- [x] Operational QC is separate from analyzer activation; `AnalyzerQcRule` is
      classified for removal rather than retained as operational QC.
- [x] Analyzer mock proves real ASTM, HL7, and FILE transport through Bridge.
- [x] No OpenELIS FILE poller, raw protocol parser, `QcRun`, dual writer,
      duplicate editor, or duplicate pending queue is permitted.
- [x] Existing profile content is curated by evidence; current rows and equal
      LOINCs create no preserve-every-row or `LEGACY_UNBOUND` requirement.
- [x] Profile-specific values exist only in profile data and parameterized test
      fixtures; production validators, consumers, runtime handlers, and UI have
      no hard-coded profile/model/manufacturer/code special case or copied
      profile default. Generic data/pin lookup remains valid.

## Acceptance

- [x] Every `MVP-*` criterion has a precondition, action, observable outcome,
      owning iteration, automated layer, and UAT mapping.
- [x] Criteria test user-visible outcomes where appropriate and do not accept
      route/API/database existence as feature proof.
- [x] The 17 UAT steps cover the complete MVP without API-driven user actions,
      including live reconciliation and draft-type learning.
- [x] G0 binds human UAT and MP4 to one exact deployment and checklist.
- [x] Schema/catalog existence cannot prove profile compatibility; the matrix
      requires OE defaults, Bridge runtime, mock transport, and assembled parity.

## Iterations

- [x] The roadmap uses only `[✓]`, `[x]`, `[*]`, and `[ ]` markers.
- [x] Exactly one iteration is `[*]`.
- [x] Marker transitions occur only for formal start, review-ready exit, or
      completed merge.
- [x] `[x]` requires the full implementation and automated exit gate; `[✓]`
      requires every checkpoint PR to be merged on its canonical target.
- [x] The immediate successor of `[x]` may start while predecessor review
      continues, but no checkpoint merges before every predecessor is `[✓]`.
- [x] No future iteration begins production work.

## TDD And Test Levels

- [x] Each behavior starts with a failing test at the layer owning the rule.
- [x] Cross-repository behavior has producer/consumer and real-transport proof.
- [x] RTL uses a real router for URL, breadcrumb, reload, and history behavior.
- [x] Playwright uses visible controls and user-facing assertions only.
- [x] The final remote acceptance package is created only after non-video
      output, console, trace, runtime, and screenshots are inspected.

## UX And Safety

- [x] Lab-facing flows contain no developer-only fields.
- [x] Current Carbon patterns and reusable components are required.
- [x] Desktop and mobile viewports, localization, accessibility, heading, URL,
      and breadcrumb behavior are deterministic.
- [x] Unknown traffic is held and visible, never silently dropped or posted.
- [x] The exact activation predicate excludes operational-QC and connection-test
      state, applies on every transition into `ACTIVE`, leaves the last active
      candidate unchanged while edits are pending, and requires an exact Bridge
      acknowledgment of the pinned desired-state fingerprint.
- [x] Analyzer Types owns the sole reusable mapping/recognition authoring
      surface; Verify links to it and no per-analyzer editor remains.
- [x] Legacy gates remove the complete OpenELIS `AnalyzerQcRule` path, its table,
      Bridge-pushed classifier fields, and Bridge hard-coded recognition
      fallbacks before G0.
