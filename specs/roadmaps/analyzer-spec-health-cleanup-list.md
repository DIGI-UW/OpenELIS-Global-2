# Analyzer Spec Health Cleanup List

**Last updated:** 2026-06-27
**Scope:** OGC-1054 analyzer profile/mapping work and related design language.

This list is for product artifact cleanup only. It should not decide the final
Analyzer/Bridge/OpenELIS architecture.

## Cleanup Rows

| Artifact | Classification | Problematic wording | Why it is risky | Product-safe rewrite | Engineering note to carry separately |
| --- | --- | --- | --- | --- | --- |
| OGC-1054 epic | Implementation leakage | "adding reusable Analyzer Types (forkable configurations for a kind of instrument)" and "Terminology: user-facing name is Analyzer Type; profile is the same object" | It collapses a product concept, an existing OpenELIS plugin capability object, and JSON profile files into one presumed implementation. | "Users can manage reusable analyzer setup profiles for ASTM, HL7, and File analyzers, then use those profiles to verify mappings and set up instruments." | Decide whether profiles remain JSON files, become OpenELIS-managed records, become Bridge-owned runtime config, or use a hybrid. |
| OGC-1054 epic | Engineering decision needed | "replacing today's developer-facing Analyzer Types plugin-registry page" | Product can say the existing experience is not sufficient, but should not decide whether replacement is a repurposed route, a new route, or a Bridge UI. | "The user-facing analyzer setup experience should not expose developer/plugin-registry details." | Engineering decides whether to repurpose `/analyzers/types`, add a profile page, or move some workflow into Bridge. |
| OGC-1055 story | Implementation leakage | "Analyzer-type (profile) entity: protocol, mappings container, `parent_profile_id`; analyzer-type association; `@Audited`" | This makes entity shape, lineage model, and auditing technology product requirements before architecture is settled. | "Users can see available analyzer setup profiles, their protocol, status, mapping completeness, and where they are used." | Engineering crosswalk should evaluate JSON/git history, database audit, Envers, and Bridge-owned profile state. |
| OGC-1055 story | Implementation leakage | "`Route: /analyzers/types` (repurposed)" | Route choice is an implementation and migration decision, not a product requirement. | "Users can access analyzer profile management from the Analyzers area." | Engineering may keep `/analyzers/types` for compatibility, rename it, or route users to a Bridge/admin surface. |
| OGC-1056 story | Implementation leakage | "Edit a type's mappings" and "Save as a new analyzer type / Update this type" | The product need is safe mapping reuse and change control, not a specific type/profile persistence model. | "Users can edit reusable mapping defaults, preserve existing analyzer behavior, and understand when a change affects other analyzers." | Engineering decides whether edits fork JSON files, copy profile versions, update DB rows, or update Bridge-managed mappings. |
| OGC-1056 story | Implementation leakage | "linked-test = Carbon ComboBox typeahead" | Product should require searchable catalog mapping, not a specific component implementation inside the story. | "Users can search and select existing catalog tests by name, code, or LOINC." | Frontend implementation should follow OpenELIS Carbon standards, but component details belong in engineering tasks. |
| OGC-1057 story | Implementation leakage | "Inline Add-Analyzer in the Analyzers List" and specific section structure | It may constrain UX before Bridge/OpenELIS ownership and diagnostics surface are decided. | "Users can add an analyzer through a guided setup that identifies the instrument, verifies mappings, and establishes a connection." | Engineering/UX decides whether this is OpenELIS-only, Bridge-assisted, or split between OpenELIS and Bridge UI. |
| OGC-1057 story | Engineering decision needed | "Connect: network address; data flow one-way default; two-way offered only if the type supports it" | Product should define safe setup and fallback behavior, but support/probe logic may belong in Bridge. | "Users can connect the analyzer safely, verify communication when supported, and proceed with one-way operation when two-way setup is unavailable." | Decide Bridge versus OpenELIS responsibility for probes, live diagnostics, connection state, and failure modes. |
| OGC-1058 story | Engineering decision needed | "Extend the existing pending codes store to cover result values" | This chooses OpenELIS storage before deciding whether Bridge should own traffic learning and diagnostics. | "Unmapped incoming codes or values are held for review and can be resolved without blocking future analyzer traffic." | Engineering decides source of truth: OpenELIS pending-code tables, Bridge event store, shared log, or staged result pipeline. |
| OGC-1058 story | Implementation leakage | "Alerts page, red flag on Analyzers List row and analyzer type" | It mixes product notification needs with a specific UI/data location. | "Users are visibly alerted when analyzer traffic needs mapping review, and can navigate to resolve it." | Engineering/UX decides alert surface, Bridge diagnostics surface, and OpenELIS list indicators. |
| Analyzer profile FRS/mockup | Engineering decision needed | Any language implying OpenELIS must own profile authoring, git commits, runtime config, or FILE watching | It can prevent the better architecture where Bridge owns more runtime/profile behavior or exposes its own UI. | "Users can create, duplicate, update, version, and apply analyzer profiles with a clear history and safe rollback behavior." | Engineering decides profile authority, git/file strategy, Bridge UI need, and FILE ownership boundaries. |
| Analyzer FILE-mode specs | Real contradiction risk | Any requirement that OpenELIS watches folders, polls files, or transports files | This conflicts with current remediation guidance and may duplicate Bridge runtime behavior. | "Users can configure FILE analyzer intake and see whether the file-based connection is healthy." | Engineering decides whether Bridge owns all watcher/transport behavior, and whether Bridge needs UI for watcher status. |

## Product-Safe Analyzer Acceptance Pattern

Use acceptance criteria like:

- A lab admin can choose ASTM, HL7, or File setup.
- A lab admin can select a reusable profile or create a new one from an
  existing profile.
- A lab admin can verify mapped tests, result values, and QC behavior before
  enabling an analyzer.
- Existing analyzer setups are not silently changed when a reusable profile is
  edited.
- Unmapped incoming traffic is held for review and does not block the analyzer
  connection.

Avoid acceptance criteria like:

- Creates `AnalyzerTypeProfile` table.
- Stores mappings in `parent_profile_id` lineage.
- Uses `/analyzers/types`.
- Extends pending-code store.
- OpenELIS commits profile JSON.
- Bridge posts to a specific endpoint unless that endpoint is already part of
  an accepted engineering contract.
