# API Contracts: Analytical Laboratory Workflow

## Reception

### POST `/rest/analytical/reception/manifest/validate`

- **Purpose**: Validate manifest CSV before import.
- **Request**: multipart file + `client_source`
  (MEDICAL_LAB|EXTERNAL_CLIENT|RESEARCH).
- **Response**: `{ valid: boolean, errors: [{row, message}], preview: [rows] }`.

### POST `/rest/analytical/reception/manifest/import`

- **Purpose**: Create SampleItems + NotebookPageSample entries.
- **Body (JSON after CSV mapping)**:

```json
{
  "rows": [
    {
      "sample_type": "TABLET",
      "requested_tests": ["ASSAY", "DISSOLUTION"],
      "storage_condition": "REFRIGERATED",
      "client_name": "ACME Pharma",
      "project_reference": "R&D-2025-12",
      "external_id": "EXT-123",
      "submission_date": "2025-12-14"
    }
  ],
  "notebook_id": "NB-123",
  "page_type": "RECEPTION"
}
```

- **Response**: `{ created: int, errors: [{row, message}] }`.

## Test Assignment

### GET `/rest/analytical/assignments`

- **Filters**: `status`, `test_type`, `assigned_to`, `notebook_id`, `page_id`.
- **Response**: paged list of assignments with sample metadata and requested
  tests.

### POST `/rest/analytical/assignments`

- **Purpose**: Create assignments for selected samples/tests.
- **Body**:

```json
{
  "assignments": [
    {
      "sample_item_id": "SMP-1",
      "test_type": "ASSAY",
      "methodology": "HPLC",
      "assigned_to_user_id": "user-123",
      "expected_completion_date": "2025-12-20"
    }
  ],
  "page_id": "PAGE-TEST-ASSIGNMENT"
}
```

- **Response**: `{ created_ids: ["ASN-1", "ASN-2"] }`.

### PATCH `/rest/analytical/assignments/{id}`

- **Purpose**: Update status or reassign.
- **Body**:
  `{ status, assigned_to_user_id?, methodology?, reassignment_reason? }`.
- **Response**: updated assignment.

## Analysis Execution & Results

### GET `/rest/analytical/results`

- **Filters**: `assignment_id`, `test_type`, `analyst`, `status`, `notebook_id`.
- **Response**: paged results with OOS flags and approval state.

### POST `/rest/analytical/results`

- **Purpose**: Manual result entry.
- **Body**:

```json
{
  "test_assignment_id": "ASN-1",
  "result_value": 98.4,
  "result_unit": "%",
  "specification_limit_low": 95,
  "specification_limit_high": 105,
  "pass_fail_status": "PASS",
  "oos_flag": false,
  "result_date": "2025-12-18",
  "analyte": "API-123",
  "remarks": "Within spec"
}
```

### POST `/rest/analytical/results/import`

- **Purpose**: Bulk HPLC/LC-MS CSV import with mapping.
- **Body**:

```json
{
  "mapping": {
    "sample_identifier": "Sample ID",
    "sequence_id": "SeqNo",
    "result_value": "Conc",
    "result_unit": "Unit",
    "specification_limit_low": "SpecLow",
    "specification_limit_high": "SpecHigh"
  },
  "rows": [{ "SeqNo": "1", "Sample ID": "SMP-1", "Conc": "98.1", "Unit": "%" }],
  "assignment_lookup": "sequence_id" // or "sample_identifier"
}
```

- **Response**: `{ imported: int, errors: [{row, message}] }`.

### POST `/rest/analytical/results/{id}/approval`

- **Purpose**: Approve/reject result.
- **Body**: `{ decision: "APPROVE"|"REJECT", "reason"?: "text" }`.

### POST `/rest/analytical/results/{id}/oos`

- **Purpose**: Mark/resolve OOS.
- **Body**:
  `{ oos_flag: true, oos_investigation_id, root_cause?, disposition? }`.

## Reporting & Release

### GET `/rest/analytical/reports`

- **Filters**: `notebook_id`, `status`, `version`.

### POST `/rest/analytical/reports`

- **Purpose**: Generate report and create new version.
- **Body**:
  `{ "notebook_id": "NB-123", "include_samples": ["SMP-1"], "format": "PDF" }`.
- **Response**: `{ report_id, version_number, status: "DRAFT", attachment_id }`.

### POST `/rest/analytical/reports/{id}/approve`

- **Purpose**: Approve report (sets status APPROVED).

### POST `/rest/analytical/reports/{id}/release`

- **Purpose**: Release approved report to recipient.
- **Body**:
  `{ "recipient": "ACME Pharma", "delivery_method": "EMAIL", "release_notes": "Released to sponsor" }`.

## Post-Test Handling / Retention

### GET `/rest/analytical/retention`

- **Filters**: `status`, `retention_end_before`, `notebook_id`,
  `sample_item_id`.

### POST `/rest/analytical/retention/transfer`

- **Purpose**: Move samples to biorepository with retention metadata.
- **Body**:

```json
{
  "sample_item_id": "SMP-1",
  "retention_quantity": 5,
  "retention_unit": "mL",
  "retention_reason": "STABILITY",
  "retention_end_date": "2027-12-14",
  "storage_location_id": "LOC-123",
  "storage_condition": "FROZEN"
}
```

- **Response**: `{ retention_id, storage_assignment_id }`.

### POST `/rest/analytical/retention/{id}/disposition`

- **Purpose**: Set disposition after review/expiry.
- **Body**:
  `{ disposition_status: "DISPOSED"|"EXTENDED", "retention_end_date"?: "YYYY-MM-DD", "notes"?: "text" }`.

## Security & Audit

- All endpoints require RBAC permissions; audit trail logs user, timestamp,
  before/after values for assignments, results, reports, and retention
  operations.
- Controllers remain stateless; transactions start in services, eager fetch to
  avoid lazy load in controllers.
