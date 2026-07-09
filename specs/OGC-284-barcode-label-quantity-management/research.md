# Research: OGC-284 Barcode Label Quantity Management

**Feature**: OGC-284  
**Date**: 2026-03-11  
**Purpose**: Reframe planning around the clarified requirement that OGC-284 must
fully cover the Jira/design intent, while preserving already-completed
remediation work as baseline rather than treating it as the full feature.

---

## Decision 1: Treat current M1-M4 work as completed baseline, not final feature scope

### Decision

The barcode remediation already merged into the working branch should be treated
as **completed baseline work** for OGC-284:

- admin config/i18n hardening,
- persistence and upsert reliability,
- pathology quantity persistence support,
- label resilience and max-limit enforcement,
- CI/review stabilization.

Remaining planning must focus on the Jira/design workflows that are still
missing.

### Rationale

- The clarified spec now requires full Jira/design parity.
- Existing PR work materially reduces risk and should not be re-planned as if it
  were undone.
- Reviewers need a clear distinction between delivered baseline capabilities and
  remaining implementation scope.

### Alternatives Considered

1. **Re-plan everything from zero**
   - Rejected: obscures completed work and makes tasking noisy.
2. **Treat current branch as complete OGC-284 delivery**
   - Rejected: contradicts Jira/design scope.
3. **Use completed work as baseline and plan only remaining scope (chosen)**
   - Selected: best for truthful planning and manageable milestones.

---

## Decision 2: Full parity with Jira/design is mandatory for this planning pass

### Decision

The planning artifacts must assume OGC-284 includes:

- pre-save labels UI in applicable sample-creation workflows,
- post-save print dialog after accession assignment,
- per-label-type print selection,
- separate print jobs per selected label type,
- `Skip - Print Later`,
- print-later entry from the appropriate order/sample view.

### Rationale

- The user explicitly clarified that the spec must require full Jira/design
  parity.
- Prior artifacts drifted by narrowing scope to persistence/resilience only.
- Continuing with a reduced scope would recreate the same misalignment.

### Alternatives Considered

1. **Allow explicit follow-up tickets**
   - Rejected: conflicts with clarified direction.
2. **Document the gap but keep remediation-only plan**
   - Rejected: preserves known drift.
3. **Plan for full parity now (chosen)**
   - Selected: aligns spec and planning with stakeholder intent.

---

## Decision 3: Model the implementation as shared workflow foundation plus rollout

### Decision

Remaining implementation should be broken into small milestones:

1. shared workflow/domain foundation,
2. pre-save labels UI,
3. post-save print dialog and print-later behavior,
4. rollout across all supported sample-creation workflows and integration
   validation.

### Rationale

- The repo contains multiple candidate workflow surfaces (`addOrder`,
  `genericSample`, `notebook`, `batchOrderEntry`, pathology-related views, and
  existing print/reprint surfaces).
- A shared foundation avoids copy/paste divergence across workflow families.
- Small milestones improve reviewability under Constitution Principle IX.

### Alternatives Considered

1. **One large UX milestone**
   - Rejected: too much merge/review risk.
2. **Workflow-by-workflow implementation without shared foundation**
   - Rejected: high duplication risk and inconsistent behavior.
3. **Shared foundation + staged rollout (chosen)**
   - Selected: balances consistency and milestone size.

---

## Decision 4: Reuse existing print infrastructure, but add explicit post-save orchestration

### Decision

The plan should reuse existing barcode printing integration points
(`LabelMakerServlet`, existing print barcode entrypoints, and current
sample/order success surfaces) rather than invent a new print subsystem.
However, new orchestration is required so save responses and/or follow-up
workflow state can drive:

- available label types,
- selected quantities,
- separate print-job requests,
- deferred print-later behavior.

### Rationale

- Existing printing routes already exist and are field-proven.
- The missing gap is workflow orchestration and UX, not low-level PDF/label
  generation.
- Reuse reduces risk while still meeting the clarified feature scope.

### Alternatives Considered

1. **Create a new print service stack**
   - Rejected: too much churn for limited value.
2. **Reuse existing print endpoints with no new orchestration**
   - Rejected: insufficient to satisfy Jira/design UX requirements.
3. **Reuse printing endpoints and add workflow orchestration (chosen)**
   - Selected: best fit for the current codebase.

---

## Decision 5: Contracts and data model must expand beyond generic sample persistence

### Decision

Planning artifacts must now document:

- labels-section row model,
- post-save print dialog state,
- workflow applicability rules,
- save-response or follow-up print-context data needed to launch printing,
- reprint/deferred-print entry expectations.

### Rationale

- Previous contracts were intentionally remediation-scoped and generic-sample
  centric.
- The clarified spec now includes UX flows that need explicit runtime/data
  modeling.
- Without contract updates, implementation would drift again.

### Alternatives Considered

1. **Keep contracts persistence-only**
   - Rejected: no longer matches spec.
2. **Specify only UI behavior with no data contracts**
   - Rejected: too vague for backend/frontend coordination.
3. **Expand contracts/data model to cover workflow UX (chosen)**
   - Selected: reduces downstream rework.

---

## Open Questions Resolved

- **Is remediation-only scope sufficient?** No. Full Jira/design parity is
  required.
- **Should already-completed work be reimplemented?** No. Treat it as completed
  baseline and plan only the missing scope.
- **Do we need a brand-new print engine?** No. Reuse existing print
  infrastructure and add workflow orchestration.
- **Do we need new milestones?** Yes. New post-remediation milestones are needed
  to deliver the missing workflow UX in manageable slices.

---

## Research Outcome

The updated planning direction is:

1. preserve M1-M4 as completed enabling work,
2. add small remaining milestones for labels UI, post-save dialog, and
   print-later/reprint behavior,
3. expand design artifacts so they describe workflow UX, not only persistence,
4. validate all supported barcode-printing sample-creation workflows before
   declaring OGC-284 complete.
