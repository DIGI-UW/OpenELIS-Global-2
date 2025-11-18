# API Contracts: ASTM Analyzer Field Mapping

**Feature**: 004-astm-analyzer-mapping  
**Date**: 2025-11-14  
**Status**: Draft

This document defines REST API contracts for the ASTM analyzer field mapping
feature.

## Base URL

All endpoints are prefixed with `/rest/analyzer` (or `/rest/analyzer-mapping` if
namespace separation needed).

## Authentication

All endpoints require authentication via Spring Security session management.
User must have appropriate role:

- `LAB_USER`: View-only access
- `LAB_SUPERVISOR`: View + acknowledge errors
- `SYSTEM_ADMINISTRATOR`: Full CRUD access

## Common Response Formats

### Success Response

```json
{
  "data": { ... },
  "status": "success"
}
```

### Error Response

```json
{
  "error": "Error message",
  "status": "error",
  "code": "ERROR_CODE"
}
```

## Endpoints

### 1. Analyzer Management

#### GET /rest/analyzer

List all analyzers with pagination and filtering.

**Query Parameters**:

- `page` (integer, default: 0): Page number
- `size` (integer, default: 25): Page size (25, 50, 100)
- `search` (string): Search term (name, type)
- `status` (string): Filter by status (ACTIVE, INACTIVE)
- `analyzerType` (string): Filter by analyzer type
- `sort` (string, default: "lastModified,desc"): Sort field and direction

**Response**:

```json
{
  "data": {
    "content": [
      {
        "id": "uuid",
        "name": "Hematology Analyzer 1",
        "type": "HEMATOLOGY",
        "ipAddress": "192.168.1.10",
        "port": 5000,
        "protocolVersion": "ASTM LIS2-A2",
        "isActive": true,
        "testUnitIds": ["unit1", "unit2"],
        "lastModified": "2025-11-14T10:00:00Z",
        "statistics": {
          "totalMappings": 50,
          "requiredMappings": 3,
          "unmappedFields": 5
        }
      }
    ],
    "totalElements": 100,
    "totalPages": 4,
    "page": 0,
    "size": 25
  },
  "status": "success"
}
```

#### POST /rest/analyzer

Create new analyzer.

**Request Body**:

```json
{
  "name": "Hematology Analyzer 1",
  "type": "HEMATOLOGY",
  "ipAddress": "192.168.1.10",
  "port": 5000,
  "protocolVersion": "ASTM LIS2-A2",
  "testUnitIds": ["unit1", "unit2"],
  "isActive": false
}
```

**Response**: 201 Created with analyzer object

#### PUT /rest/analyzer/{id}

Update analyzer.

**Request Body**: Same as POST

**Response**: 200 OK with updated analyzer object

#### DELETE /rest/analyzer/{id}

Delete analyzer (soft delete if recent results exist).

**Response**: 204 No Content

#### GET /rest/analyzer/{id}/test-connection

Test TCP connection to analyzer.

**Response**:

```json
{
  "data": {
    "success": true,
    "latency": 45,
    "timestamp": "2025-11-14T10:00:00Z",
    "logs": [
      {
        "timestamp": "10:00:00.000",
        "level": "INFO",
        "message": "Starting connection test"
      }
    ]
  },
  "status": "success"
}
```

### 2. Analyzer Field Management

#### GET /rest/analyzer/{analyzerId}/fields

List analyzer fields with pagination.

**Query Parameters**:

- `page`, `size`, `search`, `fieldType`, `sort`

**Response**:

```json
{
  "data": {
    "content": [
      {
        "id": "uuid",
        "fieldName": "GLUCOSE",
        "astmRef": "O|1|GLUCOSE",
        "fieldType": "NUMERIC",
        "unit": "mg/dL",
        "isActive": true,
        "mappingStatus": "MAPPED"
      }
    ],
    "totalElements": 50
  },
  "status": "success"
}
```

#### POST /rest/analyzer/{analyzerId}/query

Query analyzer to retrieve available fields.

**Response**:

```json
{
  "data": {
    "fields": [
      {
        "fieldName": "GLUCOSE",
        "astmRef": "O|1|GLUCOSE",
        "fieldType": "NUMERIC",
        "unit": "mg/dL"
      }
    ],
    "totalFields": 50,
    "queryTime": 2.5
  },
  "status": "success"
}
```

### 3. Field Mapping Management

#### GET /rest/analyzer/{analyzerId}/mappings

List field mappings for analyzer.

**Response**:

```json
{
  "data": {
    "mappings": [
      {
        "id": "uuid",
        "analyzerField": {
          "id": "uuid",
          "fieldName": "GLUCOSE",
          "fieldType": "NUMERIC"
        },
        "openelisField": {
          "id": "uuid",
          "name": "Glucose Test",
          "type": "TEST",
          "loincCode": "2339-0"
        },
        "mappingType": "TEST_LEVEL",
        "isRequired": true,
        "isActive": true
      }
    ],
    "statistics": {
      "totalMappings": 50,
      "requiredMappings": 3,
      "unmappedFields": 5
    }
  },
  "status": "success"
}
```

#### POST /rest/analyzer/{analyzerId}/mappings

Create field mapping.

**Request Body**:

```json
{
  "analyzerFieldId": "uuid",
  "openelisFieldId": "uuid",
  "openelisFieldType": "TEST",
  "mappingType": "TEST_LEVEL",
  "isRequired": false,
  "isActive": false
}
```

**Response**: 201 Created with mapping object

#### PUT /rest/analyzer/mappings/{mappingId}

Update field mapping.

**Request Body**: Same as POST

**Response**: 200 OK with updated mapping object

#### DELETE /rest/analyzer/mappings/{mappingId}

Delete field mapping.

**Response**: 204 No Content

### 4. Qualitative Result Mapping

#### GET /rest/analyzer/fields/{fieldId}/qualitative-mappings

List qualitative value mappings for field.

**Response**:

```json
{
  "data": {
    "mappings": [
      {
        "id": "uuid",
        "analyzerValue": "POS",
        "openelisCode": "POSITIVE",
        "isDefault": false
      }
    ]
  },
  "status": "success"
}
```

#### POST /rest/analyzer/fields/{fieldId}/qualitative-mappings

Create qualitative value mapping.

**Request Body**:

```json
{
  "analyzerValue": "POS",
  "openelisCode": "POSITIVE",
  "isDefault": false
}
```

### 5. Unit Mapping

#### GET /rest/analyzer/fields/{fieldId}/unit-mapping

Get unit mapping for field.

**Response**:

```json
{
  "data": {
    "id": "uuid",
    "analyzerUnit": "mg/dL",
    "openelisUnit": "mmol/L",
    "conversionFactor": 0.0555,
    "rejectIfMismatch": false
  },
  "status": "success"
}
```

#### POST /rest/analyzer/fields/{fieldId}/unit-mapping

Create or update unit mapping.

**Request Body**:

```json
{
  "analyzerUnit": "mg/dL",
  "openelisUnit": "mmol/L",
  "conversionFactor": 0.0555,
  "rejectIfMismatch": false
}
```

### 6. Error Dashboard

#### GET /rest/analyzer/errors

List analyzer errors with filtering.

**Query Parameters**:

- `page`, `size`, `search`, `errorType`, `severity`, `status`, `analyzerId`,
  `startDate`, `endDate`, `sort`

**Response**:

```json
{
  "data": {
    "content": [
      {
        "id": "uuid",
        "analyzer": {
          "id": "uuid",
          "name": "Hematology Analyzer 1"
        },
        "errorType": "MAPPING",
        "severity": "ERROR",
        "errorMessage": "No mapping found for test code: GLUCOSE",
        "status": "UNACKNOWLEDGED",
        "timestamp": "2025-11-14T10:00:00Z"
      }
    ],
    "totalElements": 100,
    "statistics": {
      "totalErrors": 100,
      "unacknowledged": 50,
      "critical": 10,
      "last24Hours": 25
    }
  },
  "status": "success"
}
```

#### POST /rest/analyzer/errors/{errorId}/acknowledge

Acknowledge error.

**Response**: 200 OK

#### POST /rest/analyzer/errors/{errorId}/reprocess

Reprocess error message after mapping created.

**Response**:

```json
{
  "data": {
    "success": true,
    "message": "Message reprocessed successfully"
  },
  "status": "success"
}
```

### 7. Copy Mappings

#### POST /rest/analyzer/{sourceId}/copy-mappings/{targetId}

Copy all mappings from source analyzer to target analyzer.

**Request Body**:

```json
{
  "overwriteExisting": true
}
```

**Response**: 200 OK with copy summary

## Error Codes

- `ANALYZER_NOT_FOUND`: Analyzer with given ID not found
- `FIELD_NOT_FOUND`: Analyzer field with given ID not found
- `MAPPING_NOT_FOUND`: Mapping with given ID not found
- `DUPLICATE_MAPPING`: Mapping already exists
- `TYPE_INCOMPATIBLE`: Field types are incompatible for mapping
- `REQUIRED_MAPPING_MISSING`: Required mapping not found
- `CONNECTION_FAILED`: TCP connection to analyzer failed
- `QUERY_TIMEOUT`: Analyzer query timed out
- `VALIDATION_ERROR`: Request validation failed
