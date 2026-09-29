# Native Reporting Application Contract — Version 1

**Status**: Repository-owned implementation contract, reconciled with
clarification. Paths are relative to the existing OpenELIS API application
context. Use existing session authentication, request protections and Reports
access. The client never supplies an owner, permission claim, query text or
filesystem path.

## Common Operations

| Method and path                                       | Request                                 | Success                                                                               |
| ----------------------------------------------------- | --------------------------------------- | ------------------------------------------------------------------------------------- |
| `GET /rest/reports/data-export/report-types`          | None                                    | 200: instance-enabled source definitions, labels, layout/filter metadata and defaults |
| `GET /rest/reports/data-export/variables`             | Source definition and layout            | 200: common/configured catalog, labels, types and effective limits                    |
| `POST /rest/reports/data-export/jobs`                 | Submission below                        | 202: persisted job reference, including idempotent replay                             |
| `GET /rest/reports/data-export/jobs`                  | Bounded page and optional status filter | 200: current user's jobs, pagination and owner-wide active-work indicator             |
| `GET /rest/reports/data-export/jobs/{id}`             | Identifier                              | 200: owner-visible detail and available actions                                       |
| `GET /rest/reports/data-export/jobs/{id}/download`    | Identifier                              | 200: complete authorized CSV attachment                                               |
| `POST /rest/reports/data-export/jobs/{id}/retry`      | New client request identifier           | 202: linked child of a failed job                                                     |
| `POST /rest/reports/data-export/jobs/{id}/cancel`     | Identifier                              | 200: CANCELLED; repeated cancellation may return the same state                       |
| `GET /rest/reports/data-export/saved-configs`         | Bounded page/search                     | 200: instance-wide shared definitions                                                 |
| `POST /rest/reports/data-export/saved-configs`        | Name and non-date report definition     | 201: shared definition                                                                |
| `GET /rest/reports/data-export/saved-configs/{id}`    | Identifier                              | 200: shared definition/version                                                        |
| `PUT /rest/reports/data-export/saved-configs/{id}`    | Definition and expected version         | 200: updated definition; 409 for a stale version                                      |
| `DELETE /rest/reports/data-export/saved-configs/{id}` | Identifier and expected version         | 204: removed definition; past jobs unaffected                                         |

These operations serve every supported report source. No separate Referral or
Non-Conformance job/queue API is introduced. Existing lab/test lookups may be
reused where they provide the required instance-aware choices.

## Catalog and Submission

Each source definition exposes a stable identifier/version, label, relevant date
meaning, supported filters and layouts. Each field exposes stable identity,
readable label/header, type, group, source identity and layout applicability.
Return effective date-range, active-job and retention limits from configuration.
The catalog varies with instance configuration; there is no fixed field count.

```json
{
  "schemaVersion": 1,
  "reportType": "SAMPLE_TESTING",
  "layout": "SPREADSHEET",
  "clientRequestId": "unique-identifier-for-this-submission",
  "selectedVariables": [
    "accessionNumber",
    "collectionDate",
    "configured-test-field-reference"
  ],
  "filterSpec": {
    "dateFrom": "2026-08-01",
    "dateTo": "2026-08-31",
    "labSectionIds": ["configured-section-id"],
    "testIds": ["configured-test-id"],
    "resultStatuses": ["FINALIZED"]
  }
}
```

The configured identifiers are examples, not production constants. The opaque
field-reference representation must distinguish source and configured identity;
finalize its serialization with the catalog implementation. Requests and saved
definitions use that same representation.

Per-test spreadsheet intervals use `test:<id>:<interval>` and
`component:<id>:<interval>`, where the interval is `orderToResultMinutes`,
`receivedToValidatedMinutes` or `resultedToValidatedMinutes`. They are numeric
measurements tied to the corresponding result, with readable test/component
labels. The detailed list uses the existing generic interval identities.
Generic result-dependent intervals are unavailable in the spreadsheet and old
requests selecting them receive the existing stale-column error. Both layouts
retain common specimen collection-to-receipt and order-to-collection fields.

Sample & Testing supports `SPREADSHEET` (default) and `RESULT_LIST`. Other
source definitions advertise meaningful layouts; a flat event report uses the
common tabular projection. Both Sample & Testing layouts preserve every included
result; the spreadsheet uses extra rows for repeats. A layout does not change
the requested record filter. Preserve draft filter/test choices across layout
changes.

Dates are required ISO dates interpreted by the source's declared anchor.
Omitted lab-section selection resolves to all currently accessible sections at
submission; explicit empty scope is invalid. Empty/omitted test selection means
all eligible tests. Result statuses default to finalized for Sample & Testing;
validate supported choices using existing status authority. Reject unsupported
filters/layouts and unknown/retyped fields explicitly.

Freeze source/version, layout, ordered fields/labels and resolved lab scope on
acceptance. A later configured rename does not retarget or relabel a stored
file. If an accepted request can no longer be executed against its source
definition, fail with a clear retry/review explanation rather than silently
reinterpret it.

Bind idempotency to owner plus client request identifier and normalized request.
Replay returns the same job; changed parameters under the same identifier
conflict. Use the same rule for retry creation. A deliberate new run receives a
new identifier.

## Delivery and Job State

One persisted job path handles generation. The builder observes its submitted
job and presents the ready download in place; longer-running work also appears
in the queue. A separate estimate or synchronous-generation response is not
required to deliver this low-friction behavior.

Return job identifier, state, frozen request, named filters, timestamps, safe
error code, completed output row count/file size, retry parent and available
actions. Do not return query text, result values or storage paths in job
metadata. Default lists to 20 records, enforce a bounded maximum, and keep
active-work metadata independent of pagination. Refresh known active IDs to
observe terminal transitions; filtering only to active statuses loses completed
jobs.

Only QUEUED can be cancelled. FAILED can create a retry child with the same
request and current-access checks. EXPIRED opens a draft with fresh dates
required. The download reuses the complete stored artifact; it never regenerates
from data.

For successful downloads use a safe server-generated attachment filename,
`Content-Type: text/csv; charset=UTF-8` and `Cache-Control: no-store`. Include a
UTF-8 BOM, standard comma/quote escaping, ISO dates and empty null cells. Common
field headers use canonical labels; configured fields use labels captured at
submission. Check current access and expiry before sending bytes.

## Shared Report Definitions

Store name, source definition, layout, ordered field identities and non-date
filters. Store creator/change metadata and concurrency version. The list is
shared across the instance's report users; no invitations or private-library
ownership are introduced. The client confirms overwrite/delete, and the server
rejects stale versions. Save-as-copy creates a new definition.

Opening a shared definition creates a draft and requires a new period. Running
it creates a job for the current user and applies that user's existing access.
Definitions contain configuration, not clinical result rows. Changing/deleting
one does not mutate prior jobs or files.

## Existing Access and Errors

Use existing Reports, patient-data and lab-section authority. If a base export
capability is needed in the role-module tree, seed it for existing
report-enabled roles with rollback; do not add a user setup journey or widen
their existing data access. No new identifying-field permission tiers are
required by this MVP.

Jobs/files remain scoped to the runner. Shared saved definitions do not use
owner-only lookup. Recheck current access before execution and file release.
Retain the draft after errors and explain genuine access failures without
changing the requested columns/scope.

| Status | Meaning                                                                                             |
| ------ | --------------------------------------------------------------------------------------------------- |
| 401    | No authenticated session                                                                            |
| 403    | Existing access does not permit the requested operation/data scope                                  |
| 404    | Resource absent; another user's job/file is undisclosed                                             |
| 409    | Invalid lifecycle action, changed request under reused submission ID, or concurrent definition edit |
| 410    | Owned file expired; job detail remains available                                                    |
| 422    | Invalid dates, unsupported source/layout/filter, or unknown/invalid field identities                |
| 429    | Configured active-job limit reached                                                                 |

Resolve job ownership before reporting its lifecycle. Use localizable error
codes and field associations, omitting clinical values from diagnostics.
Configuration validation and database/service tests enforce source/field
compatibility.
