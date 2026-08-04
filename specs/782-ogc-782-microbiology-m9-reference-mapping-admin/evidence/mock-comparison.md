# M3 Mock and Product-Spec Comparison

## Authority

The M3 behavior contract in `spec.md` is authoritative for this milestone. The
OpenELIS Work M-01/M-02 artifacts and Confluence workflow narrative provide
product and interaction intent; they do not prescribe routes, schema, services,
or React component structure.

Compared sources:

- [M-01 Organism Master](https://digi-uw.github.io/openelis-work/designs/microbiology/m-01-organism-master.html)
- [M-02 Breakpoint Catalog](https://digi-uw.github.io/openelis-work/designs/microbiology/m-02-breakpoint-catalog.html)
- [Microbiology workflow narrative](https://uwdigi.atlassian.net/wiki/spaces/oeg/pages/1315209256)
- M3 `spec.md`, `research.md`, and live AMR evidence at runtime SHA
  `7416a1626ccd0dade98aa6b010f91b56c226e4f4`

## Comparison

| Area | Product/mock intent | Implemented M3 behavior | Ruling |
| --- | --- | --- | --- |
| Admin entry and orientation | Reach microbiology reference administration from Admin with a clear breadcrumb and local navigation. | Uses the configuration-driven OpenELIS Admin sidenav, `PageBreadCrumb`, and Carbon tabs for Organisms, Antibiotics, AST panels, Culture methods, and Breakpoint standards. | Parity. Native OpenELIS shell is intentionally retained. |
| Organism vocabulary | Search organisms; show WHONET identity, group, and clinically useful defaults; edit records safely. | Server-backed Carbon table shows name, WHONET code, group, initial significance, default panel, and status. Search/filter/page state is bookmarkable; edit and guarded deactivation are available. | Parity. The denser standard table replaces the prototype's oversized master layout. |
| Culture defaults | Maintain the culture behavior used by ordered work. | Culture defaults are maintained on the existing Method vocabulary and Test Catalog relationship. The UI says `Culture methods`, not `Culture Protocols`. | Intentional engineering/product clarification; avoids a competing protocol identity. |
| AST panels | Maintain ordered antibiotics and publish a new version without rewriting historical runs. | Carbon editor captures tier/report behavior and publishes an immutable new current version; existing runs retain their recorded version. | Parity. |
| Breakpoint lifecycle | Distinguish Active, Loaded, and Archived standards; inspect versioned rules; activate a version without recalculating history. | Standard list/detail routes expose lifecycle, activation/effective date, filters, thresholds, and historical preservation. | Parity. Separate linkable list/detail pages replace the mock's same-page drilldown. |
| Breakpoint import | Preview before apply, show row errors, apply valid rows, and protect local changes. | Import dialog reports valid/skipped/unchanged/imported counts, supports rejected-row download, is idempotent, and refuses to overwrite local corrections. | Parity and stronger explicit error feedback. |
| Reference content | Mock illustrates recognizable CLSI/EUCAST examples. | Demo and UAT content is clearly synthetic. | Required deviation: proprietary clinical breakpoint content is not distributed. |
| Future navigation | Mock includes Macro Library, Hub Subscription, and WHONET Mapping destinations. | Those destinations are absent from M3. | Correctly excluded from this milestone. |

## Visual Validation

The reviewed desktop/mobile evidence uses the existing OpenELIS header and
sidenav, Carbon tabs, toolbar search, DataTable, pagination, status tags,
dialogs, and inline notifications. Screenshots were checked for readable table
density, clipping, overlap, stale loading state, modal action visibility, and
mobile tooltip contrast. Automated desktop/mobile accessibility passed 9/9
after the Carbon tooltip selector was corrected.

The implementation is intentionally quieter and denser than the standalone
mock pages. It preserves the mock's information hierarchy and user actions
without reproducing the prototype's custom shell, combined list/detail layout,
or future-module navigation.

## Clarification To Carry Forward

M-02 visibly offers `Export CSV`, but export is not included in the M3 product
spec, acceptance criteria, or live UAT. This is not a contradiction inside the
accepted M3 boundary. Product should rule whether breakpoint-catalog export is
a later administration story or only a mock convenience before a future slice
claims complete M-02 coverage.

## Evidence

- [Paced MP4 walkthrough](https://amr.openelis-global.org/__review/evidence/ogc-782/m3/7416a162/walkthrough.mp4)
- [Reviewed screenshot contact sheet](https://amr.openelis-global.org/__review/evidence/ogc-782/m3/7416a162/contact-sheet.png)
- [Video-frame contact sheet](https://amr.openelis-global.org/__review/evidence/ogc-782/m3/7416a162/video-contact-sheet.png)

Verdict: no blocking product or visual contradiction remains for the M3
acceptance boundary. Human UAT is still required.
