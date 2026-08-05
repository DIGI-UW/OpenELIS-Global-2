# OGC-788 M2 M-08 Mock Comparison

## Authority

The OGC-788 product spec defines behavior. The
[M-08 design](https://github.com/DIGI-UW/openelis-work/blob/main/designs/microbiology/m-08-macro-library.md)
and [rendered mock](https://digi-uw.github.io/openelis-work/designs/microbiology/m-08-macro-library.html)
define workflow and visual intent, not routes, schemas, APIs, component trees,
or package ownership.

## Comparison

| Area              | M-08 intent                                       | M2 implementation                                                                                               | Ruling                                                   |
| ----------------- | ------------------------------------------------- | --------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------- |
| Selection         | Administer several phrases efficiently.           | Carbon row selection and a selection-scoped batch toolbar.                                                      | Parity.                                                  |
| Safety            | Make broad changes deliberate and understandable. | Named Carbon confirmation with cancel, reversible availability changes, and danger treatment for local removal. | Parity with stronger action boundaries.                  |
| Export            | Produce a portable/reviewable library.            | Deterministic UTF-8 CSV with code, phrase, contexts, state, and provenance; no database or audit IDs.           | Parity for export.                                       |
| Responsive use    | Keep core administration usable at narrow widths. | Compact selected-count state, wrapped phrase text, visible status tag, and no page overflow.                    | Parity using current OpenELIS Carbon patterns.           |
| Stable state      | Preserve useful administration state.             | Search/filter/sort/page query state survives selection, actions, export, navigation, and reload.                | Parity; row selection itself is intentionally transient. |
| Reviewed package  | Show broader package/default management.          | Not implemented without approved clinical source/content/version/approver/collision policy.                     | Explicit product gate, not a visual defect.              |
| Additional fields | Reuse phrases across approved narrative surfaces. | M1 consumers remain unchanged; no new consumer was guessed from the mock.                                       | Pending product decision.                                |

## Visual Review

The seven final desktop/mobile checkpoints and both contact sheets were inspected
for Carbon shell consistency, breadcrumb hierarchy, table density, selection
state, confirmation context, phrase wrapping, tag visibility, clipping,
overflow, and hidden focusable controls. The final recording contains no setup
dialogs and uses the repository's standard title-card, evidence, and outcome
helpers.

The implementation deliberately follows the current OpenELIS shell and Carbon
components rather than reproducing standalone mock chrome. No blocking behavior
or visual contradiction remains in the delivered broader-administration slice.
