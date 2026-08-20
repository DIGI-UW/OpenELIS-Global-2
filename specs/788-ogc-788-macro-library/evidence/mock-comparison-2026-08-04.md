# OGC-788 M1 M-08 Mock Comparison

## Authority

The OGC-788 product spec is the M1 behavior contract. The
[M-08 design](https://github.com/DIGI-UW/openelis-work/blob/main/designs/microbiology/m-08-macro-library.md)
and [rendered mock](https://digi-uw.github.io/openelis-work/designs/microbiology/m-08-macro-library.html)
provide workflow and visual intent; their route, schema, service, cache, and
component suggestions are not implementation requirements.

## Comparison

| Area                    | M-08 intent                                                                        | M1 implementation                                                                                                                        | Ruling                                                                                         |
| ----------------------- | ---------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------- |
| Administration          | Search, filter, inspect, add, edit, and control phrase availability.               | Carbon DataTable, toolbar search, context/status/sort controls, pagination, status tags, overflow actions, and a Carbon editor.          | Parity for M1.                                                                                 |
| Orientation             | Provide an administration destination with recognizable hierarchy.                 | Configuration-based Admin sidenav plus Dashboard/Admin/Macro Library breadcrumbs.                                                        | Parity using the native OpenELIS shell.                                                        |
| Stable state            | Preserve useful list and editor state.                                             | `/admin/MacroLibrary` canonically carries `q`, `context`, `status`, `sort`, `page`, `pageSize`, and `edit`.                              | Parity; the mock's `/admin/macros` route is non-binding.                                       |
| Runtime expansion       | Suggest relevant dot codes and leave editable expanded text in the clinical field. | One shared `MacroTextArea` integrates culture activity, clinical history, and antibiotic exposure; existing payloads persist plain text. | Parity and cross-field reuse proved.                                                           |
| Responsive behavior     | Keep administration and runtime usable on mobile.                                  | Essential columns fit without horizontal overflow; the suggestion list remains readable and does not cover the primary action.           | Intent preserved with a denser responsive table.                                               |
| Default content         | Illustrates 85 defaults while also saying the final list is TBD.                   | M1 ships only a property-gated, non-clinical UAT phrase.                                                                                 | Required safety deviation; reviewed source/content/version/approver block M2 package delivery. |
| Import/export/bulk      | Shows broader package administration.                                              | Not implemented in M1.                                                                                                                   | Correctly assigned to M2.                                                                      |
| Categories and metadata | Shows fixed draft categories and implementation-specific source details.           | Controlled translatable field contexts and provenance support future consumers without schema churn.                                     | Product intent retained; technical prescription rejected.                                      |

## Visual Review

The final desktop and mobile screenshots were checked for shell consistency,
breadcrumb context, table density, text wrapping, action visibility, clipping,
overlap, stale loading state, and primary-action reachability. Desktop uses the
same OpenELIS header, sidenav, Carbon toolbar, DataTable, pagination, tags, and
modal patterns as other administration pages. Mobile intentionally hides the
context and source columns while retaining those values in filters and the
editor.

The authoritative standalone HTML itself contains a narrow/clipped runtime
dropdown and overlapping text near the bottom. Those defects and its internal
technical prescriptions are not copied. M1 matches the workflow intent while
remaining consistent with the current OpenELIS Carbon application.

Verdict: no blocking visual or product contradiction remains for M1. Human UAT
is still required, and the reviewed default-package decision remains the M2
gate.
