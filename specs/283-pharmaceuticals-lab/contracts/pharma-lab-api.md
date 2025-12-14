# API Contracts - Pharmaceuticals Laboratory Workflow

> All endpoints are prefixed with `/rest/pharmaceutical/`. Reuses OGC-51
> Notebook/Page and existing Storage APIs where applicable.

## Sample Management

### Base Path: `/rest/pharmaceutical/samples`

- `GET /` — List all samples
- `GET /{id}` — Get sample by ID
- `GET /{id}/details` — Get comprehensive sample details
- `GET /barcode/{barcode}` — Find sample by barcode
- `GET /unique-id/{uniqueSampleId}` — Find sample by unique ID
- `GET /status/{status}` — Filter samples by status
- `GET /lab-type/{labType}` — Filter samples by lab type
- `GET /expiring` — Get samples expiring within 30 days
- `GET /search?term={term}` — Search samples
- `POST /register` — Register new sample
- `PUT /{id}` — Update sample
- `PUT /{id}/status?status={status}` — Update sample status
- `DELETE /{id}` — Delete sample

## Aliquot Management

### Base Path: `/rest/pharmaceutical/aliquots`

- `GET /` — List all aliquots
- `GET /{id}` — Get aliquot by ID
- `GET /barcode/{barcode}` — Find aliquot by barcode
- `GET /sample/{sampleId}` — Get aliquots for a sample
- `GET /sample/{sampleId}/available` — Get available aliquots for a sample
- `GET /status/{status}` — Filter aliquots by status
- `GET /storage/{storageLocationId}` — Get aliquots at storage location
- `GET /freeze-thaw-exceeded` — Get aliquots that have exceeded freeze-thaw limits
- `GET /{id}/freeze-thaw-status` — Get freeze-thaw status for aliquot
- `POST /sample/{sampleId}` — Create aliquot for sample
- `POST /{id}/freeze-thaw` — Record freeze-thaw cycle
- `PUT /{id}/status?status={status}` — Update aliquot status
- `DELETE /{id}` — Delete aliquot

## Assay Run Management

### Base Path: `/rest/pharmaceutical/assay-runs`

- `GET /` — List all assay runs
- `GET /{id}` — Get assay run by ID
- `GET /sample/{sampleId}` — Get assay runs for a sample
- `GET /status/{status}` — Filter by status
- `GET /pending-review` — Get runs pending review
- `GET /oos` — Get out-of-specification runs
- `GET /notebook/{notebookPageId}` — Get run by notebook page
- `GET /{id}/can-approve` — Check if run can be approved
- `POST /sample/{sampleId}` — Initiate assay run for sample
- `PUT /{id}/results` — Record assay results
- `POST /{id}/submit-review` — Submit for review
- `POST /{id}/approve` — Approve assay run
- `POST /{id}/reject?reason={reason}` — Reject assay run
- `PUT /{id}/link-notebook?notebookPageId={id}` — Link to notebook page
- `DELETE /{id}` — Delete assay run

## Disposal Workflow

### Base Path: `/rest/pharmaceutical/disposal`

- `GET /` — List all disposal records
- `GET /{id}` — Get disposal record by ID
- `GET /sample/{sampleId}` — Get disposal records for a sample
- `GET /status/{status}` — Filter by status
- `GET /pending-approvals` — Get pending approval requests
- `GET /sample/{sampleId}/can-dispose` — Check if sample can be disposed
- `POST /request` — Request disposal (body: sampleId, reason, method, justification)
- `POST /{id}/approve` — Approve disposal request
- `POST /{id}/reject?rejectionReason={reason}` — Reject disposal request
- `POST /{id}/execute` — Execute disposal (body: witnessId, disposalNotes)
- `PUT /{id}/schedule?scheduledTimestamp={timestamp}` — Schedule disposal
- `GET /{id}/certificate` — Generate disposal certificate

## Environmental Excursion Management

### Base Path: `/rest/pharmaceutical/excursions`

- `GET /` — List all excursions
- `GET /{id}` — Get excursion by ID
- `GET /device/{deviceId}` — Get excursions for a device
- `GET /status/{status}` — Filter by status
- `GET /active` — Get active excursions
- `GET /unacknowledged` — Get unacknowledged excursions
- `GET /alert-type/{alertType}` — Filter by alert type
- `GET /device/{deviceId}/has-active` — Check if device has active excursion
- `GET /{id}/affected-samples` — Get samples affected by excursion
- `POST /record` — Record new excursion
- `POST /{id}/acknowledge?notes={notes}` — Acknowledge excursion
- `POST /{id}/resolve?notes={notes}` — Resolve excursion
- `POST /{id}/escalate?reason={reason}` — Escalate excursion
- `DELETE /{id}` — Delete excursion

## Reporting & Dashboard

### Base Path: `/rest/pharmaceutical/reports`

- `GET /dashboard` — Dashboard summary metrics
- `GET /intake?startDate={date}&endDate={date}` — Intake volume report
- `GET /qc?startDate={date}&endDate={date}` — QC pass rate report
- `GET /assays?startDate={date}&endDate={date}` — Assay metrics report
- `GET /oos?startDate={date}&endDate={date}` — Out-of-spec report
- `GET /tat?startDate={date}&endDate={date}` — Turnaround time report
- `GET /storage` — Storage metrics report
- `GET /disposal?startDate={date}&endDate={date}` — Disposal summary report
- `GET /excursions?startDate={date}&endDate={date}` — Excursion summary report
- `GET /sample-status-distribution` — Sample status distribution
- `GET /assay-type-distribution?startDate={date}&endDate={date}` — Assay type distribution
- `GET /excursion-history?startDate={date}&endDate={date}` — Excursion history
- `GET /disposal-history?startDate={date}&endDate={date}` — Disposal history
- `GET /export/{reportType}/csv?startDate={date}&endDate={date}` — Export CSV
- `GET /export/{reportType}/pdf?startDate={date}&endDate={date}` — Export PDF

## Notes

- Authentication via existing Spring Security session management
- All dates in ISO 8601 format (YYYY-MM-DD)
- Status enums are case-sensitive (e.g., PENDING, APPROVED, REJECTED)
- Frontend uses Carbon Design System components and React Intl for i18n
