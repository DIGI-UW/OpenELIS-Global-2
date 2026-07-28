# Analyzer Mapping API Contract

**Base path:** `/rest/analyzer`

This contract covers the current configuration MVP. Result import and
Results/Validation v4 endpoints are deferred to a separate milestone.

## Test Mappings and Pending Codes

- `GET /analyzers/{id}/fields`
- `GET /analyzers/{id}/mappings`
- `GET /analyzers/{id}/pending-codes`
- `GET /analyzers/{id}/test-mapping-options`
- `PUT /analyzers/{id}/pending-codes/{pendingCodeId}/status`
- `POST /analyzers/{id}/pending-codes/{pendingCodeId}/resolve`

Pending-code resolution validates the selected OpenELIS test in the service
layer and updates the analyzer-specific mapping state.

## Result-Value Mappings

- `GET /analyzers/{id}/result-value-mappings`
- `PUT /analyzers/{id}/result-value-mappings`
- `GET /analyzers/{id}/result-value-options?testCode={code}`
- `GET /analyzers/{id}/pending-result-values`
- `POST /analyzers/{id}/pending-result-values/{pendingId}/resolve`

Resolution request:

```json
{
  "openelisResultOptionId": "123"
}
```

The server derives the option value and display label. It rejects inactive
options and options that do not belong to the pending value's mapped test.

Mapping response entries include:

```json
{
  "analyzerValue": "POS",
  "analyzerTestCode": "HIV",
  "openelisResultOptionId": "123",
  "openelisValue": "Positive",
  "openelisLabel": "Positive",
  "bindingStatus": "BOUND"
}
```

Older free-text entries are returned with `bindingStatus: "LEGACY_UNBOUND"`.

## Setup Verification

- `GET /analyzers/{id}/setup-verification`
- `POST /analyzers/{id}/setup-verification`

Verification records confirmed mapping/QC identifiers, computed fingerprints,
actor, and time. A mapping or QC fingerprint mismatch makes that verification
stale. The service also records the action through the existing durable analyzer
audit mechanism.

## Authorization and Errors

Endpoints require the repository's analyzer administration authority. Validation
failures return `400`; missing analyzer/pending/option resources return `404`;
authorization failures follow the shared Spring Security contract. Controllers
delegate validation and persistence to transactional services.
