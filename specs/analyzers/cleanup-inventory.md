# Analyzer cleanup inventory

Updated 22 October 2026. This replaces earlier completion claims. The boundary
re-audit found decisions recreated after the original classes were deleted.
The current candidate removes those decisions and replaces their tests. Final
publication and full CI results must be recorded against the resulting commit.

| Item | Responsibility and change | Validation / remaining work |
| --- | --- | --- |
| A01 units | Remove OE display-label equality at import. Preserve delivered quantity, comparator and units; local binding approval owns the chosen catalog target. No unit conversion. | Regression uses equivalent display spellings without changing the value. |
| A02 canonical answers | OE maps only the delivered canonical answer. Raw text cannot change an answer or exclude a number. Editor uses normalized evidence; obsolete spelling rows may be deleted. | Import, editor and deletion tests. |
| A03 versioned decoding | 1.1 requires explicit roles and supported typed values. Only 1.0 decodes the old role vocabulary. Recovery reads the accepted receipt version. | Missing-role, unsupported-value and retained-observation tests. |
| A04 source scope | No rejection based on today's profile pin. A different source profile is retained as unresolved local work; equal codes from unrelated profiles do not share a mapping. | Profile-change delivery test and receipts. |
| A05 mapping history | Delete historical target-equality gates. Stable profile-scoped canonical identities use current approved local mappings, including deliberate corrections. History remains audit. | Changed or missing old-target tests now require successful approved mapping. |
| A06 tabular interpretation | FILE/HTTP extraction preserves fields. Bridge interprets them once using retained decimal/failure settings; restart preserves the receipt decision. | Complete Bridge suite, receipt restart and HTTP regressions. |
| A07 distinct runs | Delete timestamp/value suppression and raw-field held upsert. Receipt identity owns replay; separate runs remain visible and linked for review. Recovery updates the retained row by ID. | Equal-value/time, held-row independence and importer replay tests. |
| A08 provenance | Result decoding does not require an opaque recognition hash to understand explicit classification. Consistency checks remain. | Missing-hash contract test. |
| A09 local repair availability | Mapping repair reads saved canonical output declarations and held normalized results. Defaults/adoption still consult Bridge. New confirmations require the reviewer, timestamp, local decisions and audit event; no recognition hash. | View/edit/confirm tests assert no Bridge calls; all 11 database persistence tests and 30 editor tests pass. Final full CI pending. |
| A10 tests and claims | Remove obsolete expectations and unused setup helper; seed no longer sends the deleted confirmation field. Replace claims with exact evidence. | Focused checks pass; final full CI and review reconciliation pending. |

## Manual boundary review

Trace receipt → transport parser → normalized bundle → contract decoder → local
mapping → placement/QC/review. For each failure, identify the owning decision
before changing code or a test. A test preserving a retired decision is removed
or rewritten; it is not authority to restore that decision.

Checks retained: authenticated source association, supported contract structure,
active local tests/components/answers, explicit mapping approval/exclusion,
concurrent-editor protection, local specimen/order placement, operational QC,
audit and delivery identity. No local catalog IDs are copied into Bridge.

The canonical source tuple is profile ID, test code, component sub-identity and
canonical answer. A parser revision may change extraction but cannot repurpose
that tuple to mean another clinical result. New meanings need new identities.
Unit display spelling is not a clinical identifier; mapping approval reviews the
declared quantity and local target. Neither side silently converts units.

All fixable checks must pass on the final candidate. The trusted submodule pin
check remains blocked until the user merges Bridge/mock dependencies; do not
weaken it or merge anything as part of this work.
