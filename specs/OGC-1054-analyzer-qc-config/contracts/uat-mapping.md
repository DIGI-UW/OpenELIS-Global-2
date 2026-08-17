# OGC-1054 Remote UAT Mapping

These are the stable required steps for the full MVP story. They are authored
in Grist only when the G0 release candidate is deployed. The old `AN-QC-*`
checklist remains historical and unpublished for final acceptance.

| Step key   | Route/state                                  | Reviewer action                                                          | Expected visible result                                                                                                                           | Acceptance IDs                     |
| ---------- | -------------------------------------------- | ------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------- |
| AN-MVP-001 | `/analyzers/types` with search/filter query  | Read the explainer/counts, then find and inspect a shipped Analyzer Type | Purpose, aggregate state, source, protocol, lifecycle, mapping completeness, use, and attention are understandable; reload/history preserve state | MVP-001, MVP-002                   |
| AN-MVP-002 | Analyzer Type create/fork flow               | Create a site type or fork a shared type                                 | Unique lab-facing name and lineage persist; deactivate/reactivate replaces delete; no developer identity field appears                            | MVP-003, MVP-004                   |
| AN-MVP-003 | Type/analyzer mapping route                  | Resolve one unmatched test and explicitly exclude another                | Every source row remains visible; complete active catalog search works; selected Test and explicit exclusion persist independently                | MVP-005, MVP-006                   |
| AN-MVP-004 | Result-value mapping section                 | Map a qualitative analyzer value                                         | Only active options belonging to the mapped Test are offered; save and reload preserve selection                                                  | MVP-007                            |
| AN-MVP-005 | QC-identification and save-scope state       | Confirm QC identifiers and save a shared change                          | Identification is distinct from operational QC; fork/update scope and affected analyzers are explicit                                             | MVP-008, MVP-009                   |
| AN-MVP-006 | `/analyzers` inline setup at Instrument      | Choose type, name, and lab units; navigate forward/back and reload       | List remains available; URL, breadcrumb, section, selected type, name, and lab units stay coherent                                                | MVP-010, MVP-011                   |
| AN-MVP-007 | Inline Verify state                          | Confirm mappings, inspect signer metadata, then create a stale change    | Sign-off records actor/time/revision and stale state becomes an explicit blocker                                                                  | MVP-012                            |
| AN-MVP-008 | Inline Connect state                         | Configure supported settings and run connection fixtures                 | Endpoint and success/failure/timeout are clear; direction choices follow capability and safe degradation                                          | MVP-013, MVP-014                   |
| AN-MVP-009 | Operational QC from setup                    | Enter invalid then valid required rule/lot data                          | Exact validation appears; valid save updates QC readiness without changing mapping sign-off                                                       | MVP-015                            |
| AN-MVP-010 | Completion summary and activation state      | Attempt activation before and after completing blockers                  | Complete blocker list appears; incomplete activation fails; complete activation succeeds after runtime sync                                       | MVP-016, MVP-017                   |
| AN-MVP-011 | Inline Verify live-capture state             | Request a known result and inspect reconciliation                        | Seen items verify, absent items remain not seen, and new items remain explicit without data loss                                                  | MVP-018                            |
| AN-MVP-012 | Blank site type in Inline Verify             | Request live traffic and bind the populated rows                         | Received rows populate visibly, remain held, and require explicit valid catalog choices before use                                                | MVP-019, MVP-020                   |
| AN-MVP-013 | Visible demo control and result/QC workflow  | Emit a known patient result and QC result                                | Both travel through Bridge and appear in their correct OpenELIS workflows                                                                         | MVP-018                            |
| AN-MVP-014 | Visible demo control, analyzer row, Alerts   | Emit an unknown test/value                                               | Item is held, not posted or lost; analyzer and Alerts visibly require attention                                                                   | MVP-019                            |
| AN-MVP-015 | Held-item resolution flow                    | Resolve with catalog choice and emit the same message again              | Resolution is safe and audited; next message maps without another unknown alert                                                                   | MVP-020                            |
| AN-MVP-016 | FILE type/setup and visible demo control     | Configure and exercise a FILE scenario                                   | Lab-facing behavior matches other protocols while automated contracts prove Bridge owns watching/transport                                        | MVP-021                            |
| AN-MVP-017 | Bookmarked analyzer routes on desktop/mobile | Revisit completed configuration and primary routes                       | Carbon layout, headings, breadcrumbs, query state, actions, and responsive behavior remain coherent                                               | MVP-002, MVP-011, MVP-022, MVP-023 |

## Review Rules

- Fixtures establish preconditions separately; the reviewer performs the story
  through visible controls.
- Playwright cannot mark Grist steps or replace human acceptance.
- A required Fail blocks G0. N/A is invalid for a required step unless this
  contract is amended before deployment.
- The report must identify the exact deployed target and checklist revision.
