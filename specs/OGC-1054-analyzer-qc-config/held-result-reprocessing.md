# Held-result reprocessing proposal

This follow-up proposes an explicit operator action that applies current local
mapping decisions to one previously held normalized analyzer result. It is
separate from transport retry and does not remove or bypass the delivery receipt.

The current approved workflow says a mapping correction leaves the original held
result unchanged and applies to the next matching message. Therefore this proposal
requires an explicit product decision before it can be accepted.

## Proposed behavior

An authorized analyzer user selects **Reprocess held result** in the results
worklist. OpenELIS locks the selected row and analyzer, verifies their Bridge
connection and acknowledged profile revision, reconstructs the stored Observation,
and applies the current local mapping. A still-unresolved row records its next hold
reason; an excluded row is removed; a resolved row becomes actionable. Control
processing occurs in the same transaction.

## Review gates

- Decide whether this intentionally supersedes the current next-message-only rule.
- Prevent reprocessing from discarding unsaved edits elsewhere in the worklist.
- Report control processing as successful only when a quality-control result was
  actually created.
- Complete focused service, authorization, frontend, and assembled workflow tests.
