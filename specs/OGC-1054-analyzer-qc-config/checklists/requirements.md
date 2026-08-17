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
- [x] OpenELIS owns local bindings, audit, operational QC, activation, held
      results, alerts, and review.
- [x] Analyzer mock proves real ASTM, HL7, and FILE transport through Bridge.
- [x] No OpenELIS FILE poller, raw protocol parser, `QcRun`, dual writer,
      duplicate editor, or duplicate pending queue is permitted.

## Acceptance

- [x] Every `MVP-*` criterion has a precondition, action, observable outcome,
      owning iteration, automated layer, and UAT mapping.
- [x] Criteria test user-visible outcomes where appropriate and do not accept
      route/API/database existence as feature proof.
- [x] The 17 UAT steps cover the complete MVP without API-driven user actions,
      including live reconciliation and blank-type learning.
- [x] G0 binds human UAT and MP4 to one exact deployment and checklist.

## Iterations

- [x] The roadmap uses only `[x]`, `[*]`, and `[ ]` markers.
- [x] Exactly one iteration is `[*]`.
- [x] Marker transitions occur only when an iteration starts or finishes.
- [x] A finished iteration requires its full exit gate, review, and merge.
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
- [x] Operational QC applicability remains blocked until `AMB-M3-001` is
      resolved in a repository specification/contract.
