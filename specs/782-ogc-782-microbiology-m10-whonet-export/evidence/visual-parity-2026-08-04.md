# OGC-782 M4 Visual Parity - 2026-08-04

## Reference

- [M-09 feature intent](https://github.com/DIGI-UW/openelis-work/blob/main/designs/microbiology/m-09-whonet-export.md)
- [M-09 painless workflow mock](https://digi-uw.github.io/openelis-work/designs/microbiology/m-09-whonet-painless-prototype.html)

## Preserved Intent

The implementation preserves the mock's compact Configure, Preview, and Generate
sequence; explicit period and surveillance policy; pre-download counts; mapping
readiness; in-context repair; eligible-row preview; and clear generation state.
It uses the repository's current Carbon components, page grid, breadcrumb,
progress indicator, tiles, notification, data table, pagination, and config-backed
sidenav rather than copying prototype-specific styling.

## Intentional Differences

| Mock | M4 implementation | Reason |
| --- | --- | --- |
| “WHO GLASS standard” seven-day label | “First patient-organism isolate in 7 days” | The local deterministic rule has not been certified as the complete current WHO/CLSI policy. |
| Broad mapping coverage and auto-map actions | Only organism/antibiotic gaps used by the selected export, with exact M3 repair links | Other vocabularies and automated mapping ownership are not authoritative yet. |
| Scheduling, SFTP/email, FHIR, and profile controls | Manual download only | Explicitly outside the accepted M4 slice. |
| WHONET compatibility language | OpenELIS surveillance CSV candidate for WHONET validation | A real current importer/profile has not yet validated the long-format file. |

## Inspected Evidence

The six generated screenshots cover desktop Configure, desktop Preview, exact
organism repair, generated success, mobile Configure, and mobile Preview. Visual
inspection confirmed no overlapping controls or page-level horizontal overflow;
the mobile AST table remains an explicitly labeled keyboard-focusable horizontal
region. The refreshed mobile Preview starts at the full breadcrumb context, and
the generated screenshot no longer contains the previous scene label. Binary
files remain outside git under `frontend/e2e-evidence/` until they are packaged
and uploaded with the exact deployed-SHA walkthrough.
