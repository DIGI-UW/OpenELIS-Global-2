# Microbiology Spec Health Cleanup List

**Last updated:** 2026-06-27
**Scope:** OGC-782 microbiology parent and MVP bacteriology path.

This list is for product artifact cleanup only. It should not decide the final
microbiology implementation architecture.

## Cleanup Rows

| Artifact | Classification | Problematic wording | Why it is risky | Product-safe rewrite | Engineering note to carry separately |
| --- | --- | --- | --- | --- | --- |
| OGC-782 M-00 parent | Implementation leakage | "the ordered test carries a `workflow_type`" | The product need is automatic routing. Field names belong in the engineering crosswalk. | "The ordered test determines which microbiology workflow is created, such as routine bacteriology or TB." | Engineering currently maps this to Test Catalog `culture_workflow_type` and Case `workflow_type`, but product artifacts should not force names. |
| OGC-782 M-00 parent | Implementation leakage | Data model sketch names `micro_case`, `sample_item_id`, `workflow_type`, `method_id`, and many stage enum values | Helpful as engineering context, but risky inside the product spine because it makes the schema feel pre-approved. | "A microbiology case represents one workflow for one physical specimen and tracks progress through received, incubating, growth/ID, AST, review, and reporting states." | Engineering crosswalk owns table names, keys, stage enum values, and migration order. |
| OGC-786 M-01 epic | Real contradiction | "Culture Protocol masters" and Test Catalog integration via `default_culture_protocol_id` | This conflicts with later reconciliation tickets that favor Method reuse and no `default_culture_protocol_id`. | "Users can manage culture setup recipes and assign a default recipe to relevant microbiology tests." | Engineering currently favors default Method plus micro-specific method metadata; reconcile OGC-786 wording with OGC-841/925. |
| OGC-789 M-03 epic | Implementation leakage | "Culture Protocol (ComboBox, defaulted from selected test's `default_culture_protocol_id`)" | It binds UI and schema to an older model. | "When a microbiology order is placed, users can confirm or adjust the culture setup defaults for the selected test." | Engineering decides whether this reads the test default Method, a Method extension, or another resolved default. |
| OGC-789 M-03 epic | Engineering decision needed | "Sample post-save hook triggers `MicroCaseService.createCaseForSample(sample_id)`" | Product should require automatic case creation, not a hook/service signature or sample-level key. | "Saving a qualifying microbiology order creates the appropriate microbiology case without duplicate cases on resave." | Engineering decides hook location, resolver method, Sample versus SampleItem anchor, and idempotency strategy. |
| OGC-820 M-04 story | Implementation leakage | Title: "Case foundation - `micro_case` schema (SampleItem x workflow) + stage machine + createCaseForSample + workbench shell..." | The title is mostly implementation. It obscures the user outcome and packs too many concerns into one story. | "Create and open a microbiology case for one specimen workflow." | Engineering crosswalk should retain `sample_item_id + workflow_type`, stage audit, uniqueness, and service details. |
| OGC-820 M-04 story | Implementation leakage | Acceptance criteria require `UNIQUE (sample_item_id, workflow_type)`, nullable `workflow_type`, and Envers | These are technical constraints, not product acceptance. They should be tested in engineering tasks, not Casey-owned product criteria. | "A specimen can have separate bacteriology and TB workups without duplicate accessioning, and each case's stage history is auditable." | Engineering tasks should implement and test uniqueness, nullable workflow classification, and audit mechanism. |
| OGC-791 M-05 epic | Implementation leakage | "`micro_ast_run` schema + service", "multi-reading extension on result", "`micro_ast_override` table" | Product should say what AST entry/review must do; storage shape belongs in engineering. | "Users can enter AST readings for an isolate, see interpreted S/I/R results, override with justification, and preserve the original reading." | Engineering currently favors run/header plus existing result rows and override audit, pending implementation proof. |
| OGC-792 M-07 epic | Product gap plus implementation leakage | Lists routes, shells, cards, polling interval, table columns, and "my-cases" filter | The workflow need is clear, but the ticket mixes route/page implementation with some possibly stale product behavior like "my-cases." | "Users can see cultures and AST work that need action, filter by operational state, and open the relevant case." | Engineering/UX decides route structure, polling versus push, query shape, and whether ownership filters exist. |
| OGC-793 M-06 epic | Implementation leakage | "`expert_rule_definition` + version schema", JSON evaluator, phenotype table | The product need is expert review and rule-driven flags; rule storage/evaluator format belongs in engineering. | "Supervisors can review rule-driven AST concerns, apply or reject recommendations, and record justification." | Engineering decides rule persistence, evaluator format, versioning, and phenotype export integration. |
| OGC-886 M-11 story | Implementation leakage | "Replace the narrow result-only critical table with a polymorphic `critical_notification`" | The clinical need is a call/read-back log; table replacement is engineering. | "Users can log, acknowledge, and close critical microbiology communications for cases, isolates, samples, or results." | Engineering currently separates clinical critical log from the existing alerts dashboard view. |
| OGC-889 M-11 story | Implementation leakage | "Alerts Dashboard at `/alerts/criticals`" | Product should require criticals to be visible in existing alert workflows, not mandate a route. | "Critical microbiology communications are visible in the existing alerts workflow with status and follow-up actions." | Engineering/UX decides route, filter view, and integration with existing `Alert`/notification services. |
| OGC-794 M-09 epic | Implementation leakage risk | `whonet_export_run`, mapping admin sub-pages, output details mixed with product scope | WHONET export needs detailed format rules, but product specs should not mandate internal table names or page partitioning. | "Users can validate and export finalized microbiology cases to WHONET-compatible files for surveillance submission." | Engineering crosswalk decides whether to extend existing WHONET services, add run audit tables, and how to map codes. |

## Product-Safe Microbiology Acceptance Pattern

Use acceptance criteria like:

- A qualifying microbiology order creates the correct case workflow.
- A physical specimen can support separate bacteriology and TB workups without
  duplicate accessioning.
- A technologist can record culture setup, readings, organism identification,
  AST readings, interpretations, and review decisions.
- A supervisor can see what changed and who made each clinical decision.
- Critical communications can be logged and followed up through the existing
  operational alert workflow.
- Finalized cases can feed surveillance export.

Avoid acceptance criteria like:

- Creates `micro_case` table.
- Calls `MicroCaseService.createCaseForSample`.
- Adds `default_culture_protocol_id`.
- Stores AST rows in `micro_ast_result`.
- Uses `/microbiology/case/:caseId`.
- Implements a JSON evaluator.
- Adds a particular polling interval.
