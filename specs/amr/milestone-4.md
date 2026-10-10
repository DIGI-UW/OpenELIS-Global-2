# Milestone 4: Culture rows and media tracking

This slice extends the existing inoculation row and hosts culture work in the
Milestone 3 case shell. Choose Culture rows in Testing to inoculate, append
readings, extend incubation, record outcomes, attach catalog tests, and create
subcultures. Tests render beneath their culture before nested subcultures. The
Gram shortcut preselects the lab unit's configured active test only for source
specimen types that allow it. Ordinary result entry and validation remain the
Milestone 3 pipeline.

## Service and HTTP contract

| Endpoint                                                           | Behavior                                                                                                                                                                                                           |
| ------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `GET /rest/microbiology/culture-catalog`                           | Active coded choices, tagged media and currently usable lots                                                                                                                                                       |
| `GET /rest/microbiology/cases/{caseId}/cultures`                   | Culture tree rows with immutable reading/extension histories, proposals and calculated timing                                                                                                                      |
| `GET /rest/microbiology/cases/{caseId}/cultures/options`           | Case-member specimens and descendant aliquots, media-link defaults, lab tracking policy and source-compatible Gram configuration                                                                                   |
| `POST /rest/microbiology/cases/{caseId}/cultures`                  | Inoculate with `sourceSampleItemId`, optional `parentId`/`subculturePurpose`, container, medium/lot or `notTracked`, atmosphere, duration/unit, optional temperature/check interval/loop volume and `inoculatedAt` |
| `POST /rest/microbiology/cases/{caseId}/cultures/{rowId}/{action}` | Append `reading` or `extension`; record `outcome`; edit `inoculated-time`/`positive-time`; append `negative-proposal`                                                                                              |
| `POST /rest/microbiology/cases/{caseId}/analyses`                  | Existing test chooser additionally accepts `placement=CULTURE` and `cultureId`; source must match that row                                                                                                         |

Reading inputs are `readingId`, optional `quantityId` and `note`. Extension
inputs are positive `extendBy`, `unit`, required coded `reasonId` and optional
`note`. The service records actor, time and incubation day. Outcome inputs are
`GROWTH`, `NO_GROWTH` or `CONTAMINATED`, with optional `positiveAt` and a pending
`proposalId` when confirming an instrument-negative proposal. A proposal stores
its signal and receipt attribution without changing the outcome.

Every mutation locks the current case, checks Results rights in its current lab
unit and rejects terminal/final locks. Direct links retain the M2 read-only
exception. Controllers compile no entity relationships and define no
transactions. The service builds DTOs inside its transaction; separate HQL
collection fetches initialize histories without multiple-bag/cartesian joins.
Foreign sources, rows, parent links and culture-test source mismatches are
rejected. Two cultures may independently carry the same catalog test.

The inoculation clock must be between source receipt and now, remains editable
until the first reading, and respects parent/child and positivity chronology.
Positivity must be between inoculation and now; manual changes record USER as
source. Original inoculation time anchors both extensions and check intervals.
Growth can continue incubating and receiving readings/extensions. No Growth
and Contamination close the row; late growth can reopen No Growth while the
case remains mutable. Other rows retain their own histories and outcomes.

## Configuration and persistence

Inventory items gain a Microbiology-medium tag, lot-tracking setting and usual
atmosphere/temperature fields. The inventory item form exposes the two flags.
The existing test-catalog reagent link supports optional incubation defaults;
its media-defaults modal edits timing/environment values while preserving usage
metadata. Changing medium or source refreshes matching defaults; a specific
specimen type takes precedence over a generic link. Selecting culture media
never decrements stock.

The lab-unit editor configures `requireTrackedMedia` and its Gram stain test.
The engineering spec scopes mandatory tracking to the lab unit, default off.
A tracked item or a unit requiring tracking forbids Not tracked; otherwise the
user can choose it. Tracked selections require a matching active/in-use,
QC-passed, unexpired lot with positive current quantity. Decimal timing and
physical values are validated before persistence to prevent silent rounding.

Liquibase migration 121 adds nullable fields for historic rows and three
append-only history tables, foreign keys/indexes and coded choice seeds. Both
application and test persistence units register all new mappings. Structural
rollback removes M4 additions while retaining prior inoculation history. Older
inoculation-table rollback tests unwind M4 dependencies before dropping that
table and reapply them afterward.

## Evidence and remaining work

Focused tests cover real PostgreSQL migrations and persisted histories,
source/lot/permission/final-lock rejection, temporal and precision boundaries,
aliquots, culture tests, growth continuation, proposals and eager loading.
Component tests exercise actual Carbon controls, source-scoped Gram selection,
media prefill, failed drafts, read-only controls and configuration payloads.
The property-gated CULTURE_WORKSPACE browser scenario uses a unique case, lab
unit, tests, medium and lot for its inoculate/read/extend/Gram/subculture/outcome
journey and reload checks. Executed local checks: 58 frontend tests across seven files; 42 backend culture,
ORM, migration, rollback and scenario tests across six files; and 36 existing
case/result/access regression tests across three files. The browser journey
passed in 8.1 seconds with desktop and narrow-viewport screenshots reviewed.
Removing the duration-precision guard and reverting source-scoped Gram selection
each caused the intended regression test to fail; the restored guards passed.
Spotless, Prettier and focused lint checks passed. Full local CI and GitHub
checkpoint results remain separate gates, reported in the PR. Owner acceptance
is pending.

Incoming instrument adapters, isolates and AST remain later roadmap slices.
The retained workbench mutation path is scheduled for priority removal in
[issue #4675](https://github.com/DIGI-UW/OpenELIS-Global-2/issues/4675), including
its query routes and migration of existing behavior tests. M4 adds no features
to that old path. Historic incomplete rows remain readable with unavailable
metrics; they are not automatically invented or rewritten by this migration.
