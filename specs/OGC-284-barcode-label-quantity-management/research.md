# Research: OGC-284 Barcode Label Quantity Management (Assessment Follow-up)

**Feature**: OGC-284  
**Date**: 2026-02-14  
**Purpose**: Resolve assessment findings and remove implementation risk before
merge

---

## Decision 1: Remove unscoped per-label FHIR lookup from `BlockLabel`

### Decision

`BlockLabel` should not execute a broad FHIR `QuestionnaireResponse` search
during label construction. Instead, specimen-type data should be resolved in a
service-layer step and passed into the label object.

### Rationale

- Current pattern performs an unscoped search and takes the first result.
- This introduces correctness risk (wrong specimen type in multi-response
  environments).
- It also increases latency during bulk label generation.
- It violates the spirit of keeping data compilation out of low-level rendering
  classes.

### Alternatives Considered

1. **Keep search in label class and cache locally**
   - Rejected: still couples rendering class to data lookup and is difficult to
     scope safely.
2. **Constrain search query but keep in label class**
   - Rejected: improves accuracy but keeps architecture concern unresolved.
3. **Service-layer context assembly + label constructor input (chosen)**
   - Selected: clearer responsibility boundaries and easier unit testing.

---

## Decision 2: Align admin-configured fields with rendered output behavior

### Decision

Slide and freezer label field toggles must either:

1. be fully implemented in rendered labels, or
2. be removed/hidden if unsupported.

For OGC-284 remediation, the preferred direction is to implement currently
exposed options so configuration remains trustworthy.

### Rationale

- Presenting toggles that do nothing creates operational confusion.
- Review feedback identified this as an unresolved behavior mismatch.
- Implementing field mapping preserves intent of expanded barcode configuration.

### Alternatives Considered

1. **Leave toggles as-is and document limitations**
   - Rejected: ongoing mismatch between UI and behavior.
2. **Remove all non-working toggles**
   - Rejected: loses feature intent and can be seen as functional regression.
3. **Implement missing field rendering (chosen)**
   - Selected: aligns configuration contract with runtime behavior.

---

## Decision 3: Complete backend localization key set for new label fields

### Decision

Add and verify backend message keys in `message_en.properties` and
`message_fr.properties` for every new `barcode.label.info.*` key referenced by
label classes.

### Rationale

- Missing backend keys can render raw message IDs in printed output.
- Frontend translations do not cover backend print label message resolution.
- This is a low-cost, high-confidence fix.

### Alternatives Considered

1. **Rely on existing generic keys only**
   - Rejected: does not cover newly introduced key names.
2. **Fallback to English literals in Java**
   - Rejected: violates i18n-first requirement.
3. **Add proper keys in backend bundles (chosen)**
   - Selected: constitution-aligned and explicit.

---

## Decision 4: Preserve existing label-count persistence model; focus on hardening

### Decision

Keep `SampleBarcodeInfo` and `SampleItemBarcodeInfo` persistence approach
(including upsert behavior) and target remediation to correctness/performance
gaps rather than schema redesign.

### Rationale

- Core OGC-284 value (config + persisted counts) is already implemented.
- Tests already cover critical upsert behavior for default and pathology paths.
- Current unresolved risks are primarily behavior alignment and data-enrichment
  strategy, not schema structure.

### Alternatives Considered

1. **Refactor persistence model**
   - Rejected: high churn, low incremental value for current risks.
2. **Incremental hardening (chosen)**
   - Selected: faster route to merge-ready quality.

---

## Decision 5: Treat CI and review-thread closure as explicit deliverables

### Decision

The remediation scope includes:

- rerunning relevant CI checks after fixes,
- confirming failing job impact to OGC-284 paths,
- explicitly resolving review threads with code references.

### Rationale

- Technical fixes without thread closure leave PR in ambiguous state.
- CI failure currently blocks merge readiness regardless of code quality.
- Explicit closure improves traceability for reviewers.

### Alternatives Considered

1. **Code-only fixes, defer thread closure**
   - Rejected: delays review completion.
2. **Close threads without evidence**
   - Rejected: weak review hygiene.
3. **Fix + verify + resolve with evidence (chosen)**
   - Selected: best alignment with quality gates.

---

## Open Questions Resolved

- **Do we need new external APIs?** No new endpoints; existing endpoints are
  sufficient, but contracts are documented with updated payload expectations.
- **Do we need new schema migration?** Not for planned remediation; existing
  OGC-284 schema additions remain valid.
- **Is effort >3 days requiring multiple milestones?** Expected effort is within
  a single remediation milestone.

---

## Research Outcome

All assessment-driven technical unknowns are resolved. Design can proceed with:

1. service-layer pathology context assembly,
2. label field behavior alignment with configuration,
3. backend i18n completion,
4. CI/review closure workflow.
