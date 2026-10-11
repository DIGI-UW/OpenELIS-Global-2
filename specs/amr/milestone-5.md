# Milestone 5: Isolates and referral

The case Testing chooser now includes Isolates. A technician can pick numbered
isolates from a culture with a formal Growth outcome, record preliminary or final
identification, create an isolate specimen when needed, and refer it through the
existing Refer Out panel. Sender identification remains separate from local
identification for received isolates.

## Contract and permission boundary

| Case endpoint under `/rest/microbiology/cases/{caseId}` | Behavior |
| --- | --- |
| `GET /isolates` | Isolates, eager origin/history, permission flags, linked referrals and badge |
| `GET /isolates/options` | Active organisms/methods, configured isolate types, sources, allowed tests and referral choices |
| `POST /isolates` | Pick from `cultureId`; optional Gram/morphology; coded significance |
| `POST /isolates/{isolateId}/identify` | Organism, PRELIMINARY/FINAL, catalog method, performed timestamp, optional confidence/note and current version |
| `POST /isolates/{isolateId}/reidentify` | Same fields, mandatory reason and current lab-unit Validation rights |
| `POST /isolates/{isolateId}/received-origin` | Sending organization and reported coded organism or text before local identification |
| `POST /isolates/{isolateId}/acknowledge` | Validator acknowledgment with reason and current version |
| `POST /isolates/{isolateId}/sample-item` | Idempotent demand creation for LABEL, TESTING or REFERRAL |
| `POST /isolates/{isolateId}/test` | Add an allowed clinical test to the isolate item |
| `POST /isolates/{isolateId}/referral` | Demand-create the isolate item and create the existing referral workflow records |
| `POST /isolates/referrals` | Refer a linked specimen or aliquot |

All writes lock the case, recheck current lab-unit rights and require a mutable
case. Ordinary writes require Results; re-identification and mismatch
acknowledgment require Validation. Direct links preserve the V2 read-only access
rule. Case-scoped source queries exclude descendants actively owned by another
case. A missing role lookup cannot confer permissions.

History records retain organism, method, confidence, performed/recorded times,
actor, reason and amendment reference. A per-isolate sequence orders changes even
when timestamps coincide. Re-identification retains existing susceptibility
readings and run selection. Final-release locks require the existing official
amendment workflow; cancellation restores identification date and acknowledgment
as well as organism/status, and appends reversal history.

## Configuration and persistence

In Sample Types, mark an active type **Use for microbiology isolates**. This is a
configuration flag, not a match on an English type name. Incoming root items of
that type automatically receive an ISO label and a Growth (received isolate)
origin row during V2 routing. Retries and additional analyses reuse that isolate.
Demand-created child items reuse the original order and accession, reference the
original specimen/aliquot, and join the same case. Their external container label
uses the existing accession plus isolate label. Existing order barcode workflows
can use those items; this slice adds no separate label printer.

The initial sender information is entered in Record sender identification. Local
identification requires it for received isolates. A coded or text mismatch is
flagged until a validator acknowledges it; another identification resets that
acknowledgment and final-release readiness remains blocked while it is pending.

Referral creation delegates to the shared referral service with the exact
analysis, including its draft history, result link and transport metadata. Two
isolates can carry the same catalog test independently. The Referred badge covers
nonterminal referrals on linked specimens, aliquots and isolate items. Dispatch,
return and completion continue through the existing referral workflow.

Migration 122 adds origin/sample/sender metadata, identification timestamps and
restoration fields, event sequence, and the configured sample-type flag. It also
permits COLONIZER significance. Its rollback restores the preceding schema and
constraint while retaining earlier isolates/history. Downgrade is rejected if
new COLONIZER rows conflict with the preceding constraint; it does not silently
rewrite those observations.

The obsolete unlinked isolate mutation controller is replaced by these
case-scoped DTO routes. Archived workbench cleanup remains tracked separately.

## Validation

Database regressions exercise growth eligibility, numbering, demand item reuse,
same accession/parent, source ownership, role/reason/stale-write rejection,
append-only history, sender mismatch/readiness, official amendment restoration,
received-isolate routing, detached eager access and exact referral associations.
The migration test upgrades retained isolate/history rows, verifies that an
incompatible COLONIZER downgrade leaves the schema and record intact, rolls back
structural additions with compatible data and reapplies them. Component tests use real Carbon controls and check
payloads, read-only actions, and required acknowledgment reasons.

Property-gated ISOLATE_WORKSPACE and RECEIVED_ISOLATE scenarios provide uniquely
owned browser fixtures. Playwright journeys cover growth-to-pick-to-identify-to-
referral with reload, plus sender mismatch acknowledgment. Browser and full local/
GitHub validation are pending until their results are recorded.

Focused checks passed: 16 isolate database tests; 59 culture, result-entry,
permission, identification and readiness regressions; 28 schema, mapping,
architecture, controller and history checks; and 31 affected component tests.
Removing the sample-item association condition makes the two-isolate referral
regression fail, and restoring it makes the full isolate suite pass.

The optional repository-wide TypeScript check reports 2,524 errors on both the
parent revision and this milestone; no additional errors were found in the new
isolate browser specification. This is recorded separately from required CI.
