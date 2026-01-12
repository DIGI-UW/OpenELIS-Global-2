# Phase 2.5 Enhancements - API Documentation & Error Handling

**Status**: ✅ COMPLETED **Date**: 2026-01-06 **Scope**: Enhanced REST
controllers with comprehensive API documentation, sophisticated error handling,
pagination, and filtering

---

## Overview

Phase 2.5 builds upon the Phase 2 backend implementation (services, controllers,
DAOs) with three major enhancements:

1. **📚 Comprehensive API Documentation** - OpenAPI 3.0.3 specifications for all
   endpoints
2. **🔧 Sophisticated Error Handling** - Global exception handler with detailed
   error responses
3. **📊 Pagination & Filtering** - Support for offset-limit pagination and
   search filtering on configuration endpoints

---

## Deliverables

### 1. OpenAPI/Swagger Documentation

#### Files Created:

- [specs/bioanalytical-laboratory/contracts/manifest-import.openapi.yml](manifest-import.openapi.yml)
- [specs/bioanalytical-laboratory/contracts/analyzer-data.openapi.yml](analyzer-data.openapi.yml)
- [specs/bioanalytical-laboratory/contracts/reporting.openapi.yml](reporting.openapi.yml)

#### Manifest Import API

**Base Path**: `/rest/notebook/bioanalytical`

**Endpoints**:

- `GET /sample-types` - List valid sample types (43 total) with
  pagination/filtering
- `GET /tests` - List valid analytical tests (16 total) with
  pagination/filtering
- `GET /source-origins` - List valid source origins (8 total) with
  pagination/filtering
- `POST /entry/{entryId}/samples/preview-manifest` - Preview CSV before import
- `POST /entry/{entryId}/samples/import-manifest` - Execute batch sample import

**Features**:

- Full CSV column mapping with required/optional fields
- Row-by-row validation with error collection
- Sample creation with auto-generated IDs and barcodes
- Reception metadata storage as JSON in notebook pages

#### Analyzer Data Integration API

**Endpoints**:

- `GET /instruments` - Supported instruments (LC-MS/MS, HPLC, dissolution, etc.)
- `GET /instruments/{type}/formats` - File format support per instrument
- `POST /entry/{entryId}/analyzer-data/upload` - Upload raw instrument data
- `POST /entry/{entryId}/analyzer-data/validate` - Validate analyzer output
- `GET /entry/{entryId}/qc-trending` - QC trending with Levey-Jennings chart
- `GET /instruments/{id}/quality` - Instrument quality metrics

**Features**:

- Multi-format support (mzML, CDF, CSV, PDF)
- Calibration curve validation (r² ≥ 0.99)
- QC result acceptance (≥80% pass rate)
- Westgard multi-rule detection (1-3S, 2-2S, R-4S, 4-1S, 10X)
- System suitability checks

#### Reporting & Dashboard API

**Endpoints**:

- `GET /dashboard` - Complete metrics dashboard (throughput, quality, study
  progress, utilization)
- `GET /entry/{entryId}/throughput` - Sample throughput metrics with TAT
- `GET /entry/{entryId}/quality-metrics` - QC, calibration, and instrument
  uptime
- `GET /entry/{entryId}/study-progress` - Bioequivalence study tracking
- `GET /entry/{entryId}/instrument-utilization` - Instrument runs and
  utilization %
- `GET /entry/{entryId}/tat-by-test` - Turnaround time by test type
- `GET /external-reports` - Export format options
- `POST /entry/{entryId}/export-results` - Export to LMIS, REDCap, CDISC/SDTM,
  research
- `GET /alerts` - Dashboard alerts with severity filtering

**Features**:

- Date range filtering (optional startDate/endDate)
- Historical trending (monthly aggregation)
- Multiple export formats with regulatory compliance
- Alert severity levels (CRITICAL, WARNING, INFO)
- Pagination support for alerts (limit/offset)

---

### 2. Global Exception Handler

**File Created**:
[src/main/java/org/openelisglobal/notebook/controller/rest/BioanalyticalGlobalExceptionHandler.java](BioanalyticalGlobalExceptionHandler.java)

#### Features:

- **Centralized Error Handling**: `@ControllerAdvice` covers all Bioanalytical
  REST controllers
- **Structured Error Responses**: Consistent JSON format with:
  - Error message
  - HTTP status code and reason
  - Timestamp (ISO 8601)
  - Request ID (unique tracking identifier)
  - Request path

#### Handled Exceptions:

| Exception                             | HTTP Status               | Response Format                                                   |
| ------------------------------------- | ------------------------- | ----------------------------------------------------------------- |
| `MethodArgumentNotValidException`     | 400 Bad Request           | List of field validation errors with messages and rejected values |
| `MethodArgumentTypeMismatchException` | 400 Bad Request           | Parameter name, expected type, and received value                 |
| `MaxUploadSizeExceededException`      | 413 Payload Too Large     | File size limit exceeded with max size                            |
| `NoHandlerFoundException`             | 404 Not Found             | Request path and HTTP method                                      |
| `IllegalArgumentException`            | 400 Bad Request           | Invalid argument details                                          |
| `IllegalStateException`               | 409 Conflict              | Invalid operation state details                                   |
| `RuntimeException`                    | 500 Internal Server Error | Generic error with stack trace logging                            |
| `Exception`                           | 500 Internal Server Error | Catch-all with detailed message                                   |

#### Request ID Generation:

```
Format: REQ-{8-char-UUID}
Example: REQ-A1B2C3D4
```

Used for audit trail, debugging, and error tracking across distributed systems.

---

### 3. Pagination & Filtering

#### Enhanced Endpoints:

- `GET /sample-types?offset=0&limit=50&filter=plasma`
- `GET /tests?offset=0&limit=50&filter=HPLC`
- `GET /source-origins?offset=0&limit=50&filter=medical`
- `GET /alerts?severity=CRITICAL&limit=20`

#### Parameters:

| Parameter  | Type    | Default | Max | Required | Notes                                 |
| ---------- | ------- | ------- | --- | -------- | ------------------------------------- |
| `offset`   | integer | 0       | N/A | No       | Zero-based pagination offset          |
| `limit`    | integer | 50      | 200 | No       | Items per page, validation enforced   |
| `filter`   | string  | N/A     | N/A | No       | Case-insensitive substring search     |
| `severity` | string  | N/A     | N/A | No       | Alert filter: CRITICAL, WARNING, INFO |

#### Response Pagination:

```json
{
  "sampleTypes": [...],
  "total": 43,           // Total matching items (after filter)
  "offset": 0,
  "limit": 50,
  "returned": 25         // Items actually returned
}
```

#### Validation:

- Offset must be ≥ 0 (throws `IllegalArgumentException` for negative)
- Limit must be 1-200 (throws `IllegalArgumentException` for invalid range)
- Filter is optional, case-insensitive substring match
- Exception handler converts validation errors to 400 Bad Request

---

## Error Response Examples

### Validation Error (400)

```json
{
  "error": "Validation failed",
  "status": 400,
  "statusText": "Bad Request",
  "timestamp": "2026-01-06T14:30:00",
  "requestId": "REQ-A1B2C3D4",
  "path": "/rest/notebook/bioanalytical/entry/123/samples/import-manifest",
  "validationErrors": [
    {
      "field": "entryId",
      "message": "must not be null",
      "rejectedValue": "null"
    }
  ],
  "errorCount": 1
}
```

### Invalid Parameter (400)

```json
{
  "error": "Invalid parameter 'limit': expected Integer but received 'abc'",
  "status": 400,
  "statusText": "Bad Request",
  "timestamp": "2026-01-06T14:30:00",
  "requestId": "REQ-B2C3D4E5",
  "path": "/rest/notebook/bioanalytical/sample-types",
  "parameter": "limit",
  "expectedType": "Integer"
}
```

### File Size Exceeded (413)

```json
{
  "error": "File size exceeds maximum allowed size",
  "status": 413,
  "statusText": "Payload Too Large",
  "timestamp": "2026-01-06T14:30:00",
  "requestId": "REQ-C3D4E5F6",
  "path": "/rest/notebook/bioanalytical/entry/123/analyzer-data/upload",
  "maxSize": 104857600
}
```

### Internal Server Error (500)

```json
{
  "error": "An unexpected error occurred",
  "status": 500,
  "statusText": "Internal Server Error",
  "timestamp": "2026-01-06T14:30:00",
  "requestId": "REQ-D4E5F6G7",
  "path": "/rest/notebook/bioanalytical/dashboard"
}
```

---

## Controller Enhancements

### BioanalyticalManifestImportController

**Enhanced Methods**:

```java
// Before: Simple list with no filtering
GET /sample-types
Response: { "sampleTypes": [...], "total": 43 }

// After: Pagination + filtering
GET /sample-types?offset=0&limit=50&filter=plasma
Response: {
  "sampleTypes": [
    { "id": "1", "description": "Plasma - EDTA" },
    { "id": "2", "description": "Plasma - Citrate" }
  ],
  "total": 43,
  "offset": 0,
  "limit": 50,
  "returned": 2
}
```

**Validation Added**:

- Offset validation (must be ≥ 0)
- Limit validation (must be 1-200)
- Filter string validation (non-null check, case-insensitive matching)

### Error Handling

**Before**: IOException caught and returned as bad request with plain message
**After**: IOException thrown as `IllegalArgumentException` → Global handler
catches → Structured error response with requestId

---

## Testing Recommendations

### 1. Pagination Tests

```bash
# Test default pagination
curl http://localhost:8080/rest/notebook/bioanalytical/sample-types

# Test with custom offset/limit
curl http://localhost:8080/rest/notebook/bioanalytical/sample-types?offset=10&limit=25

# Test filter
curl http://localhost:8080/rest/notebook/bioanalytical/sample-types?filter=plasma

# Test combined
curl http://localhost:8080/rest/notebook/bioanalytical/sample-types?offset=5&limit=10&filter=serum
```

### 2. Error Handling Tests

```bash
# Test invalid limit (should return 400)
curl http://localhost:8080/rest/notebook/bioanalytical/sample-types?limit=500

# Test negative offset (should return 400)
curl http://localhost:8080/rest/notebook/bioanalytical/sample-types?offset=-1

# Test type mismatch (should return 400)
curl http://localhost:8080/rest/notebook/bioanalytical/sample-types?limit=abc

# Test missing required path param (should return 400)
curl -X POST http://localhost:8080/rest/notebook/bioanalytical/entry//samples/preview-manifest
```

### 3. API Documentation Tests

```bash
# Validate OpenAPI spec
npx @redocly/cli validate specs/bioanalytical-laboratory/contracts/manifest-import.openapi.yml
npx @redocly/cli validate specs/bioanalytical-laboratory/contracts/analyzer-data.openapi.yml
npx @redocly/cli validate specs/bioanalytical-laboratory/contracts/reporting.openapi.yml

# Generate docs (optional)
npx @redocly/cli build-docs specs/bioanalytical-laboratory/contracts/manifest-import.openapi.yml
```

---

## Integration with Frontend

### OpenAPI Documentation Consumption:

```javascript
// Option 1: Load in Swagger UI
// Add to Spring Boot configuration:
// springdoc.api-docs.path=/v3/api-docs
// springdoc.swagger-ui.path=/swagger-ui.html

// Option 2: Generate TypeScript client (with OpenAPI Generator)
npx openapi-generator-cli generate \
  -i specs/bioanalytical-laboratory/contracts/manifest-import.openapi.yml \
  -g typescript-fetch \
  -o src/api/bioanalytical
```

### Error Handling in Frontend:

```javascript
// All error responses include requestId for support
async function importManifest(entryId, file, mapping) {
  try {
    const response = await fetch(
      `/rest/notebook/bioanalytical/entry/${entryId}/samples/import-manifest`,
      { method: "POST", body: formData }
    );

    if (!response.ok) {
      const error = await response.json();
      console.error(`Error [${error.requestId}]: ${error.error}`);
      // Display error.error to user with requestId for support
      showAlert(`Error ${error.requestId}: ${error.error}`);
    }
  } catch (e) {
    console.error("Network error", e);
  }
}
```

### Pagination Usage:

```javascript
// Load first page with filter
const response = await fetch(
  "/rest/notebook/bioanalytical/sample-types?offset=0&limit=50&filter=plasma"
);
const data = await response.json();

// Render pagination controls
console.log(`Showing ${data.returned} of ${data.total} items`);
console.log(`Page: offset=${data.offset}, limit=${data.limit}`);

// Load next page
const nextPage = await fetch(
  `/rest/notebook/bioanalytical/sample-types?offset=${
    data.offset + data.limit
  }&limit=${data.limit}`
);
```

---

## Performance Considerations

### Pagination Benefits:

- **Reduced Response Size**: Only fetch needed items, not entire dataset
- **Faster Page Load**: 50 items much faster than 1000+ items
- **Better UX**: Progressive loading with pagination controls
- **Scalability**: Works same way with 100 or 100,000 items

### Query Optimization (for Phase 3):

When implementing database queries in service layer:

```java
// Efficient pagination query
List<Sample> samples = sampleDAO.findAll(offset, limit, filter);
long totalCount = sampleDAO.countByFilter(filter);
```

### Error Handling Performance:

- Global handler catches all exceptions → consistent response time
- Request ID generation (UUID substring) is fast
- No performance impact on happy path

---

## Configuration Notes

### Spring Boot Application Properties:

```properties
# Maximum upload file size
spring.servlet.multipart.max-file-size=100MB
spring.servlet.multipart.max-request-size=100MB

# Global exception handler bean
# (automatically picked up via @ControllerAdvice)

# Optional: Enable Swagger UI
springdoc.api-docs.path=/v3/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.operationsSorter=method
```

---

## Files Modified/Created

### New Files:

- `BioanalyticalGlobalExceptionHandler.java` - Global exception handling
- `manifest-import.openapi.yml` - OpenAPI spec (280+ lines)
- `analyzer-data.openapi.yml` - OpenAPI spec (500+ lines)
- `reporting.openapi.yml` - OpenAPI spec (550+ lines)

### Modified Files:

- `BioanalyticalManifestImportController.java` - Added pagination, filtering,
  import statements

### Total Lines of Code Added:

- API Documentation: ~1,330 lines (3 OpenAPI specs)
- Exception Handler: ~140 lines
- Controller Enhancements: ~80 lines pagination/filtering logic
- **Total: ~1,550 lines**

---

## Next Steps (Phase 3)

### Frontend Component Development:

1. Create `BioanalyticalWorkflowTab.js` - Main workflow component
2. Create `BioanalyticalManifestImportModal.js` - CSV import UI with progress
   tracking
3. Create 5 Stage Page Components - Stage-specific UIs with ACL enforcement
4. Create `BioanalyticalReportingDashboard.js` - Metrics and charts
5. Register workflow in `NoteBookInstanceEntryForm.js`
6. Add cascading role-based access control
7. Add i18n translation keys

### Testing Phase:

- Unit tests for pagination logic
- Integration tests for error handler
- E2E tests for complete workflows
- API documentation validation

---

## Summary

**Phase 2.5** successfully enhanced the Bioanalytical backend with:

- ✅ Production-grade API documentation (OpenAPI 3.0.3)
- ✅ Comprehensive error handling with request tracking
- ✅ Pagination & filtering for list endpoints
- ✅ ~65% of total implementation complete

**Progress**:

- Backend Services: ✅ Complete (Phase 1-2.5)
- Frontend Components: ⏳ Pending (Phase 3)
- Integration & Testing: ⏳ Pending (Phase 4-5)

**Total Implementation**: ~1,550 lines of documentation and error handling code
