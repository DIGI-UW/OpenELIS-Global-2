# Research And Artifact Crosswalk: OGC-788

## Sources Reviewed

- OpenELIS Work M-08 Markdown, rendered mock, and archived FRS.
- OGC-782 R1 spec, roadmap, implementation, tests, and deployed UAT state.
- Current `Dictionary`, `Note`, admin routing, Carbon forms, and microbiology
  narrative fields in the M4 repository.

## Product-Safe Feature Intent

- Staff can type a short dot code or choose a suggestion to insert a shared,
  approved phrase.
- Suggestions are relevant to the current field and fully keyboard accessible.
- Expanded text remains editable and is what the clinical workflow saves.
- Authorized administrators can manage, find, activate, and deactivate shared
  phrases.
- A reviewed default package can be adopted without destroying local content.
- The feature is cross-cutting, with microbiology as its first consumer.

## Repository Findings

| Area                       | Current state                                                                                               | Consequence                                                                               |
| -------------------------- | ----------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------- |
| `Dictionary`               | Stores coded result choices and category/localization data.                                                 | Do not overload it with free-text shortcut and provenance semantics.                      |
| `Note`                     | Stores authored notes attached to clinical records and users.                                               | Do not use persisted clinical observations as reusable definitions.                       |
| Microbiology activity      | `CaseTimelinePanel` owns a controlled Carbon activity-note text area.                                       | Suitable first runtime consumer without changing the case payload.                        |
| Microbiology order details | Clinical history is narrative; Antibiotic Exposure is an authoritative binary choice.                       | Integrate the reusable runtime with clinical history without changing the order contract. |
| Admin UI                   | React Router 5, Carbon side navigation, tables, breadcrumbs, and server-backed query state are established. | Reuse these patterns; exact M-08 routes and modal dimensions are non-binding.             |
| Test data                  | OGC-782 uses a property-gated UAT scenario service.                                                         | Extend it through the service layer only; no SQL or fixed database IDs.                   |

## Spec Health Cleanup

| Artifact  | Problematic wording                                                                                | Risk                                                                                | Product-safe rewrite                                                                        | Engineering note                                                                      |
| --------- | -------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------- |
| OGC-788   | Names a table, service, cache, React components, routes, and permission constants as requirements. | Locks design before repo analysis and makes product acceptance depend on internals. | Users insert, discover, and manage approved phrases with appropriate access and freshness.  | Select entities, endpoints, cache, components, and routes in the implementation plan. |
| M-08 FRS  | Requires fixed columns, modal sizes, component names, and invalidation mechanics.                  | Treats a visual aid as an API and component contract.                               | Preserve the management and expansion workflow, keyboard behavior, and observable outcomes. | Use repo Carbon/router/service patterns and document visual deviations.               |
| M-08 FRS  | Requires 85 defaults while stating the full list is TBD at implementation.                         | Could ship clinically unsafe or unreviewed report text.                             | Ship only a versioned phrase set with named clinical review.                                | M1 uses safe demonstration fixtures; package delivery is gated in M2.                 |
| M-08 FRS  | Fixed categories are presented as both UX labels and technical storage.                            | Makes future non-microbiology use require schema or API churn.                      | Phrases apply to one or more user-visible field contexts.                                   | Use controlled extensible context values and translatable labels.                     |
| M-03/M-08 | Antibiotic Exposure is described as both a binary choice and a macro-enabled narrative.            | Implementations could silently change clinical meaning while reconciling the mocks. | Keep the binary choice; require a separately defined narrative before adding a consumer.    | Do not mount the macro control on the existing checkbox.                              |
| OGC-788   | References fifteen sub-stories that do not exist in Jira.                                          | Creates false traceability and completion claims.                                   | Use this spec's tested user stories and milestone ledger.                                   | Reconcile Jira after spec publication.                                                |

## Open Product Input

Only one input blocks the follow-on default-package milestone: the exact phrase
list, source/version, clinical approver, and collision policy. It does not block
M1's managed-library and runtime behavior.
