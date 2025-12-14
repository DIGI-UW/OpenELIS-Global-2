# API Contract: Viral and Vaccine Laboratory Workflow

**Feature**: `282-viral-vaccine-workflow`  
**Date**: 2025-12-14  
**Version**: 1.0.0

## Base URL

```
/rest/viral
```

## Authentication

All endpoints require authentication via existing OpenELIS session management. Requests must include:
- `Cookie: JSESSIONID=<session-id>` (HTTP-only secure cookie)

## Common Headers

**Request**:
```
Content-Type: application/json
Accept: application/json
Accept-Language: en-US, fr-FR
```

**Response**:
```
Content-Type: application/json
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
```

## Common Response Codes

| Code | Description                                       |
| ---- | ------------------------------------------------- |
| 200  | Success                                           |
| 201  | Resource created                                  |
| 400  | Bad request (validation error)                    |
| 401  | Unauthorized (not authenticated)                  |
| 403  | Forbidden (insufficient permissions)              |
| 404  | Resource not found                                |
| 409  | Conflict (e.g., duplicate biosafety clearance)    |
| 422  | Unprocessable entity (business logic error)       |
| 500  | Internal server error                             |

## Endpoints

### 1. Sample Reception

#### POST /samples

Register a new viral sample with cold chain documentation.

**Request Body**:
```json
{
  "sampleId": "VS-2025-001",
  "specimenType": "NP_SWAB",
  "biosafety Level": "BSL-2",
  "collectionSite": "District Hospital",
  "collectorId": "user-123",
  "studyProtocolNumber": "PROTO-2025-01",
  "consentStatus": "OBTAINED",
  "coldChain": {
    "dispatchTemperature": -80.0,
    "arrivalTemperature": -78.5,
    "transportMethod": "DRY_ICE",
    "transportDuration": 120,
    "excursionFlag": false
  }
}
```

**Response** (201 Created):
```json
{
  "id": "uuid-123",
  "sampleId": "VS-2025-001",
  "coldChainStatus": "COMPLIANT",
  "createdAt": "2025-12-14T10:30:00Z",
  "createdBy": "user-123"
}
```

---

#### GET /samples/{id}

Retrieve viral sample details.

**Response** (200 OK):
```json
{
  "id": "uuid-123",
  "sampleId": "VS-2025-001",
  "specimenType": "NP_SWAB",
  "biosafety Level": "BSL-2",
  "coldChainStatus": "COMPLIANT",
  "coldChainLogs": [
    {
      "eventType": "DISPATCH",
      "temperature": -80.0,
      "timestamp": "2025-12-14T08:00:00Z"
    },
    {
      "eventType": "ARRIVAL",
      "temperature": -78.5,
      "timestamp": "2025-12-14T10:00:00Z"
    }
  ],
  "biosafetyClearance": {
    "checklistComplete": true,
    "approvedBy": "biosafety-officer-001",
    "approvalDate": "2025-12-14T10:15:00Z"
  }
}
```

---

### 2. Biosafety Clearance

#### POST /biosafety-clearance

Assign biosafety level and complete checklist.

**Request Body**:
```json
{
  "viralSampleId": "uuid-123",
  "biosafety Level": "BSL-3",
  "requiredPPE": ["N95 respirator", "Double gloves", "Gown", "Face shield"],
  "checklistItems": [
    {"item": "Appropriate PPE worn", "checked": true},
    {"item": "BSL-3 hood operational", "checked": true},
    {"item": "Spill kit available", "checked": true},
    {"item": "Trained personnel only", "checked": true}
  ]
}
```

**Response** (201 Created):
```json
{
  "id": "clearance-001",
  "viralSampleId": "uuid-123",
  "checklistComplete": true,
  "approvedBy": "biosafety-officer-001",
  "approvalDate": "2025-12-14T10:15:00Z"
}
```

---

### 3. Sample Aliquoting

#### POST /samples/{id}/aliquots

Create child aliquots from parent sample.

**Request Body**:
```json
{
  "numberOfAliquots": 4,
  "volumePerAliquot": 0.5,
  "aliquotPurpose": "BIOBANK"
}
```

**Response** (201 Created):
```json
{
  "parentSampleId": "uuid-123",
  "aliquots": [
    {
      "aliquotId": "VS-2025-001-A1",
      "volumeMl": 0.5,
      "status": "AVAILABLE",
      "createdAt": "2025-12-14T11:00:00Z"
    },
    {
      "aliquotId": "VS-2025-001-A2",
      "volumeMl": 0.5,
      "status": "AVAILABLE",
      "createdAt": "2025-12-14T11:00:00Z"
    },
    {
      "aliquotId": "VS-2025-001-A3",
      "volumeMl": 0.5,
      "status": "AVAILABLE",
      "createdAt": "2025-12-14T11:00:00Z"
    },
    {
      "aliquotId": "VS-2025-001-A4",
      "volumeMl": 0.5,
      "status": "AVAILABLE",
      "createdAt": "2025-12-14T11:00:00Z"
    }
  ]
}
```

---

### 4. PCR Testing

#### POST /pcr/runs

Create a new PCR run.

**Request Body**:
```json
{
  "runId": "PCR-2025-001",
  "runDate": "2025-12-14T14:00:00Z",
  "instrumentId": "QPCR-01",
  "assayType": "COVID19",
  "kitLotNumber": "KIT-2025-01",
  "samples": [
    {"sampleId": "uuid-123", "wellPosition": "A1"},
    {"sampleId": "uuid-456", "wellPosition": "A2"}
  ],
  "controls": [
    {"controlType": "POSITIVE", "wellPosition": "H11"},
    {"controlType": "NEGATIVE", "wellPosition": "H12"}
  ]
}
```

**Response** (201 Created):
```json
{
  "id": "run-uuid-001",
  "runId": "PCR-2025-001",
  "status": "PENDING_RESULTS",
  "createdAt": "2025-12-14T14:00:00Z"
}
```

---

#### POST /pcr/runs/{runId}/results

Submit PCR results for a run.

**Request Body**:
```json
{
  "results": [
    {
      "sampleId": "uuid-123",
      "targets": [
        {"gene": "N", "ctValue": 18.5, "interpretation": "DETECTED"},
        {"gene": "ORF1AB", "ctValue": 19.2, "interpretation": "DETECTED"},
        {"gene": "S", "ctValue": 20.1, "interpretation": "DETECTED"}
      ]
    },
    {
      "sampleId": "uuid-456",
      "targets": [
        {"gene": "N", "ctValue": null, "interpretation": "NOT_DETECTED"},
        {"gene": "ORF1AB", "ctValue": null, "interpretation": "NOT_DETECTED"},
        {"gene": "S", "ctValue": null, "interpretation": "NOT_DETECTED"}
      ]
    }
  ],
  "controlResults": [
    {"controlType": "POSITIVE", "ctValue": 22.3, "valid": true},
    {"controlType": "NEGATIVE", "ctValue": null, "valid": true}
  ]
}
```

**Response** (200 OK):
```json
{
  "runId": "PCR-2025-001",
  "controlStatus": "PASS",
  "resultsSubmitted": 2,
  "validationWarnings": []
}
```

---

### 5. ELISA Testing

#### POST /elisa/runs

Create a new ELISA run.

**Request Body**:
```json
{
  "runId": "ELISA-2025-001",
  "runDate": "2025-12-14T15:00:00Z",
  "plateId": "PLATE-001",
  "readerInstrument": "READER-01",
  "assayType": "ANTI_SPIKE_IGG",
  "kitLotNumber": "KIT-ELISA-01",
  "kitExpiryDate": "2026-12-31",
  "calibrators": [
    {"concentration": 0, "od450": 0.05},
    {"concentration": 10, "od450": 0.25},
    {"concentration": 50, "od450": 0.85},
    {"concentration": 100, "od450": 1.50},
    {"concentration": 250, "od450": 2.80},
    {"concentration": 500, "od450": 3.20}
  ],
  "samples": [
    {"sampleId": "uuid-789", "wellPosition": "A1"},
    {"sampleId": "uuid-101", "wellPosition": "A2"}
  ]
}
```

**Response** (201 Created):
```json
{
  "id": "run-uuid-002",
  "runId": "ELISA-2025-001",
  "standardCurve": {
    "parameters": {"a": 0.05, "b": 1.2, "c": 50.0, "d": 3.5},
    "rSquared": 0.998
  },
  "status": "PENDING_RESULTS",
  "createdAt": "2025-12-14T15:00:00Z"
}
```

---

### 6. Cryogenic Storage

#### GET /storage/units

List all storage units (dewars and freezers).

**Query Parameters**:
- `unitType` (optional): DEWAR or FREEZER
- `location` (optional): Filter by location

**Response** (200 OK):
```json
{
  "units": [
    {
      "id": "unit-001",
      "unitId": "LN2-01",
      "unitType": "DEWAR",
      "name": "Main Dewar",
      "targetTemperature": -196.0,
      "location": "Lab Building A, Room 101",
      "currentTemperature": -196.2,
      "lastTemperatureReading": "2025-12-14T16:00:00Z",
      "alarmStatus": "NORMAL"
    },
    {
      "id": "unit-002",
      "unitId": "ULT-01",
      "unitType": "FREEZER",
      "name": "Ultra-Cold Freezer 1",
      "targetTemperature": -80.0,
      "location": "Lab Building A, Room 102",
      "currentTemperature": -80.5,
      "lastTemperatureReading": "2025-12-14T16:00:00Z",
      "alarmStatus": "NORMAL"
    }
  ]
}
```

---

#### GET /storage/units/{unitId}/positions

Get storage hierarchy for a unit.

**Response** (200 OK):
```json
{
  "unitId": "LN2-01",
  "hierarchy": [
    {
      "level": "CANISTER",
      "name": "C1",
      "children": [
        {
          "level": "RACK",
          "name": "R1",
          "children": [
            {
              "level": "BOX",
              "name": "BOX-A",
              "capacity": 81,
              "occupied": 45,
              "positions": [
                {"name": "A1", "status": "OCCUPIED", "sampleId": "uuid-123"},
                {"name": "A2", "status": "EMPTY"},
                // ... 79 more positions
              ]
            }
          ]
        }
      ]
    }
  ]
}
```

---

#### POST /storage/assign

Assign sample to storage position.

**Request Body**:
```json
{
  "sampleId": "uuid-123",
  "positionId": "position-uuid-001"
}
```

**Response** (200 OK):
```json
{
  "sampleId": "uuid-123",
  "positionPath": ["LN2-01", "C2", "R3", "BOX-A", "D5"],
  "assignedAt": "2025-12-14T16:30:00Z"
}
```

---

### 7. Temperature Monitoring

#### GET /monitoring/logs

Get temperature logs for a storage unit.

**Query Parameters**:
- `unitId` (required): Storage unit ID
- `startDate` (required): ISO 8601 datetime
- `endDate` (required): ISO 8601 datetime

**Response** (200 OK):
```json
{
  "unitId": "LN2-01",
  "logs": [
    {"timestamp": "2025-12-14T00:00:00Z", "temperature": -196.0, "withinRange": true},
    {"timestamp": "2025-12-14T01:00:00Z", "temperature": -196.2, "withinRange": true},
    // ... hourly readings
  ],
  "statistics": {
    "minTemperature": -196.5,
    "maxTemperature": -195.5,
    "avgTemperature": -196.1,
    "readingsInRange": 24,
    "readingsOutOfRange": 0,
    "compliancePercentage": 100.0
  }
}
```

---

#### GET /monitoring/excursions

Get temperature excursion events.

**Query Parameters**:
- `unitId` (optional): Filter by storage unit
- `startDate` (optional): ISO 8601 datetime
- `endDate` (optional): ISO 8601 datetime
- `resolved` (optional): true/false/all (default: all)

**Response** (200 OK):
```json
{
  "excursions": [
    {
      "id": "excursion-001",
      "unitId": "ULT-01",
      "alarmStartTime": "2025-12-13T02:15:00Z",
      "alarmEndTime": "2025-12-13T04:30:00Z",
      "durationMinutes": 135,
      "minTemperature": -75.0,
      "maxTemperature": -70.0,
      "affectedSamples": ["uuid-123", "uuid-456"],
      "correctiveAction": "Service technician called, compressor repaired",
      "alertSent": true,
      "acknowledgedBy": "cold-chain-coordinator-001"
    }
  ]
}
```

---

### 8. Result Review

#### GET /results/pending-review

Get results awaiting review.

**Query Parameters**:
- `resultType` (optional): PCR/ELISA/CULTURE/VACCINE_LOT

**Response** (200 OK):
```json
{
  "pendingResults": [
    {
      "resultId": "result-uuid-001",
      "resultType": "PCR",
      "sampleId": "uuid-123",
      "testDate": "2025-12-14T14:00:00Z",
      "interpretation": "DETECTED",
      "enteredBy": "technician-001",
      "entryDate": "2025-12-14T15:00:00Z"
    },
    {
      "resultId": "result-uuid-002",
      "resultType": "ELISA",
      "sampleId": "uuid-789",
      "testDate": "2025-12-14T15:00:00Z",
      "interpretation": "POSITIVE",
      "titer": 250.5,
      "enteredBy": "technician-002",
      "entryDate": "2025-12-14T16:00:00Z"
    }
  ]
}
```

---

#### POST /results/{id}/review

Approve, reject, or request repeat of a result.

**Request Body**:
```json
{
  "action": "APPROVED",
  "reviewerComments": "Results consistent with clinical picture",
  "digitalSignature": "signature-hash-123"
}
```

**Response** (200 OK):
```json
{
  "resultId": "result-uuid-001",
  "reviewStatus": "APPROVED",
  "reviewedBy": "senior-virologist-001",
  "reviewDate": "2025-12-14T17:00:00Z",
  "resultLocked": true
}
```

---

### 9. Cold Chain Reports

#### POST /reports/cold-chain

Generate cold chain compliance report.

**Request Body**:
```json
{
  "startDate": "2025-01-01T00:00:00Z",
  "endDate": "2025-12-31T23:59:59Z",
  "storageUnitIds": ["unit-001", "unit-002"],
  "includeExcursionsOnly": false,
  "exportFormat": "PDF"
}
```

**Response** (200 OK):
```json
{
  "reportId": "report-uuid-001",
  "reportUrl": "/reports/download/report-uuid-001",
  "generatedAt": "2025-12-14T17:30:00Z",
  "expiresAt": "2025-12-21T17:30:00Z"
}
```

---

## Error Responses

All error responses follow this structure:

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Validation failed",
    "details": [
      {
        "field": "biosafety Level",
        "message": "biosafety Level must be BSL-1, BSL-2, BSL-3, or BSL-4"
      }
    ]
  },
  "timestamp": "2025-12-14T10:00:00Z",
  "path": "/rest/viral/biosafety-clearance"
}
```

### Error Codes

| Code | Description |
| ---- | ----------- |
| VALIDATION_ERROR | Request validation failed |
| BIOSAFETY_CLEARANCE_INCOMPLETE | Biosafety checklist not complete |
| CONTROL_OUT_OF_RANGE | PCR/ELISA controls failed validation |
| POSITION_OCCUPIED | Storage position already has a sample |
| TEMPERATURE_EXCURSION_ONGOING | Cannot proceed while temperature out of range |
| RESULT_ALREADY_APPROVED | Cannot modify locked result |
| INSUFFICIENT_VOLUME | Not enough sample volume for aliquoting |

---

## Pagination

List endpoints support pagination:

**Query Parameters**:
- `page` (default: 0): Page number (0-indexed)
- `size` (default: 20): Items per page
- `sort` (optional): Sort field and direction (e.g., `createdAt,desc`)

**Response** includes pagination metadata:
```json
{
  "content": [...],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "totalPages": 5,
    "totalElements": 95
  }
}
```

---

## Versioning

API version is included in all responses:

```
X-API-Version: 1.0.0
```

Breaking changes will increment the major version. Backward-compatible additions increment the minor version.
