# Engineering Crosswalk and Ambiguity Log

## Current Repository State

- M1 already introduced `MicroOrganism`, `MicroAntibiotic`, `MicroAstPanel`,
  `MicroAstPanelAntibiotic`, `MicroBreakpointStandard`, and
  `MicroBreakpointRule` plus service-created fixtures.
- Existing APIs are read-only option endpoints for the case/AST workflow. There
  is no administration API or React administration surface.
- Breakpoint lookup precedence is implemented and covered by tests.
- AST runs reference concrete panel and breakpoint-standard records. Historical
  safety therefore depends on treating published versions as immutable.
- Culture defaults are stored by `MicroCultureSetup` and keyed to an existing
  Method. Order routing already resolves culture-capable Tests through Method.
- WHONET readiness checks organism and antibiotic codes, but no mapping admin or
  actual export is implemented.

## Artifact Health Pass

| Artifact/topic | Classification | Problem | Product-safe ruling | Engineering note |
|---|---|---|---|---|
| OGC-858 / OGC-859 marked Done | Real contradiction | Repo has no specimen/department WHONET administration or the described complete legibility behavior. | Do not claim those broader outcomes from Jira status alone. | M3 covers reference surfaces it can verify; broader WHONET vocabulary mapping remains tracked for export work. |
| M-01 route and table wording | Implementation leakage | The FRS names routes, columns, tables, permissions, and modal structure as requirements. | Preserve workflow behavior and acceptance outcomes only. | Routes, DTOs, entities, and security expressions follow current repo patterns. |
| Culture Protocol master | Real contradiction | Older mock/Jira wording describes a master; current FRS says reuse Method; repo has a Method-keyed extension record. | Users manage culture instructions associated with Methods. | Keep `MicroCultureSetup` as the Method extension; do not create another Method vocabulary. |
| AST panel versioning | Engineering decision needed | Current panel rows are mutable and unversioned, while the product requires historical runs to remain unaffected. | Published edits create a new visible version. | Add immutable panel versions and point new setup to the current version. |
| Breakpoint status | Engineering decision needed | Current `is_active` cannot distinguish Loaded from Archived. | Show Active, Loaded, and Archived with one active version per publisher. | Add explicit lifecycle state and an activation audit event. |
| Breakpoint datasets | Product gap / external constraint | Specs say load CLSI and EUCAST but do not address distribution rights or source validation. | Sites import licensed/current data; demos use synthetic labeled rows. | Do not commit publisher breakpoint content. |
| Partial CSV import | Engineering decision needed | Valid rows must persist while invalid rows are reported, without leaving an internally inconsistent standard. | Validate all rows, then atomically apply the valid set selected by the administrator. | Parse into typed rows, resolve references through services, apply valid rows in one service transaction. |
| Local customization | Product gap | “Preserve” is stated without defining collision identity. | A correction protects the exact publisher/version + organism/group + antibiotic + method + specimen rule. | Use the rule's normalized natural key for idempotency and overwrite protection. |
| WHO-TB additions in M-02 | Scope contradiction | M-02 now mixes routine bacteriology and later operational TB behavior. | Routine CLSI/EUCAST administration ships first. | OGC-916 remains outside M3. |

## Engineering Decisions

1. Build administration on the existing microbiology entities and service layer;
   do not create parallel reference stores.
2. Add schema only for fields required by accepted behavior: reference metadata,
   immutable panel versioning, explicit breakpoint lifecycle/import metadata,
   and activation audit.
3. Keep current read-only workflow endpoints stable. Add admin-specific contracts
   that return compiled DTOs from service transactions.
4. Use server-side search/filter/sort/page contracts. Canonical React Router query
   utilities are the single URL-state implementation.
5. Use the authenticated OpenELIS user for audit actors. Request bodies contain no
   authoritative actor field.
6. Reuse Carbon DataTable, toolbar search, Select/ComboBox/MultiSelect,
   Pagination, ComposedModal, InlineNotification, Tag, and PageBreadCrumb.
7. Playwright interacts through labels, roles, and Carbon's public behavior. No
   arbitrary waits, forced clicks, or private Carbon DOM selectors.

## Deferred Questions

- Whether specimen, origin, patient type, and department WHONET mapping should
  live in their owning admin vocabularies or a consolidated WHONET mapping page.
  This does not block M3 because export generation is excluded.
- Whether Catalog Subscription should later own remote organism/antibiotic and
  breakpoint updates. M3 preserves import provenance so that integration can be
  added without replacing local administration.
