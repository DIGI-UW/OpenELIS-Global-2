# API Contracts: 310 Turn Around Time

**Date**: 2026-04-02 **Base Path**: `/rest`

---

## Calendar Management Endpoints

### GET /rest/calendar/holidays

List public holidays for a given year.

**Query Parameters**: | Param | Type | Required | Default | Description |
|-------|------|----------|---------|-------------| | year | integer | No |
current year | Filter by year | | includeInactive | boolean | No | false |
Include inactive holidays |

**Response 200**:

```json
{
  "year": 2026,
  "holidays": [
    {
      "id": 1,
      "date": "2026-01-01",
      "name": "New Year's Day",
      "isRecurring": true,
      "isActive": true,
      "dayOfWeek": "Thursday",
      "isWeekendDay": false,
      "lastUpdated": "2026-01-15T10:30:00Z"
    }
  ]
}
```

**Permission**: `calendar-management` module (read)

---

### POST /rest/calendar/holidays

Create a new public holiday.

**Request Body**:

```json
{
  "date": "2026-01-01",
  "name": "New Year's Day",
  "isRecurring": true
}
```

**Response 201**: Created holiday object (same shape as GET list item)

**Validation**:

- date: required, valid ISO date
- name: required, max 100 chars, non-blank
- Duplicate date in same year: 409 Conflict

**Permission**: `calendar-management` module (write)

---

### PUT /rest/calendar/holidays/{id}

Update an existing holiday.

**Request Body**:

```json
{
  "date": "2026-01-01",
  "name": "New Year's Day",
  "isRecurring": true,
  "isActive": true
}
```

**Response 200**: Updated holiday object **Response 404**: Holiday not found

**Permission**: `calendar-management` module (write)

---

### DELETE /rest/calendar/holidays/{id}

Delete a holiday.

**Response 204**: No Content **Response 404**: Holiday not found

**Permission**: `calendar-management` module (write)

---

### POST /rest/calendar/holidays/import

Import holidays from CSV file.

**Request**: `multipart/form-data` with CSV file **CSV Columns**:
`date,name,recurring` (recurring: true/false)

**Response 200**:

```json
{
  "imported": 8,
  "skipped": 2,
  "errors": [
    { "row": 3, "reason": "Duplicate date: 2026-01-01" },
    { "row": 7, "reason": "Invalid date format" }
  ]
}
```

**Permission**: `calendar-management` module (write)

---

### GET /rest/calendar/holidays/export

Export holidays as CSV download.

**Query Parameters**: | Param | Type | Required | Default |
|-------|------|----------|---------| | year | integer | No | current year |

**Response 200**: `text/csv` file download **Headers**:
`Content-Disposition: attachment; filename="holidays-2026.csv"`

**Permission**: `calendar-management` module (read)

---

### GET /rest/calendar/weekends

Get weekend day configuration.

**Response 200**:

```json
{
  "weekendDays": [0, 6]
}
```

(0=Sunday, 6=Saturday)

**Permission**: `calendar-management` module (read)

---

### PUT /rest/calendar/weekends

Update weekend day configuration.

**Request Body**:

```json
{
  "weekendDays": [5, 6]
}
```

**Response 200**: Updated weekend config **Validation**: Array of integers 0-6,
no duplicates

**Permission**: `calendar-management` module (write)

---

## TAT Report Endpoints

### GET /rest/reports/tat/summary

Get TAT summary statistics, histogram, and breakdown for the selected filters.

**Query Parameters**: | Param | Type | Required | Default | Description |
|-------|------|----------|---------|-------------| | fromDate | ISO date | Yes
| - | Start of date range | | toDate | ISO date | Yes | - | End of date range |
| segment | enum | Yes | - | TAT segment (see below) | | calculationMode | enum
| No | CALENDAR | CALENDAR or WORKING_TIME | | labUnitIds | comma-sep integers |
No | all | Filter by lab unit IDs | | testIds | comma-sep integers | No | all |
Filter by test IDs | | panelIds | comma-sep integers | No | all | Filter by
panel IDs | | priority | enum | No | all | ROUTINE, STAT, ASAP | | sampleTypeId
| integer | No | all | Filter by sample type | | orderingSiteId | integer | No |
all | Filter by ordering site | | includeCancelled | boolean | No | false |
Include cancelled/rejected | | breakdownBy | enum | No | LAB_UNIT | Breakdown
dimension |

**Segment Values**: `ORDER_TO_COLLECTION`, `COLLECTION_TO_RECEIPT`,
`RECEIPT_TO_TESTING`, `RECEIPT_TO_RESULT`, `RECEIPT_TO_VALIDATION`,
`RESULT_TO_VALIDATION`, `OVERALL`

**breakdownBy Values**: `LAB_UNIT`, `TEST`, `PRIORITY`, `SAMPLE_TYPE`,
`ORDERING_SITE`

**Response 200**:

```json
{
  "calculationMode": "CALENDAR",
  "excludedDaysCount": 0,
  "totalCount": 1247,
  "mean": 3.7,
  "median": 2.97,
  "percentile90": 6.25,
  "min": 0.37,
  "max": 48.25,
  "stdDeviation": 2.17,
  "histogram": [{ "binLabel": "0-1h", "binMin": 0, "binMax": 1, "count": 142 }],
  "breakdown": [
    {
      "dimensionValue": "Hematology",
      "dimensionId": 5,
      "count": 412,
      "mean": 2.25,
      "median": 1.8,
      "percentile90": 4.5,
      "max": 18.37
    }
  ]
}
```

**Permission**: `tat-report` module (read)

---

### GET /rest/reports/tat/detail

Get paginated detail list of individual TAT results.

**Query Parameters**: Same filters as summary, plus: | Param | Type | Required |
Default | |-------|------|----------|---------| | page | integer | No | 1 | |
pageSize | integer | No | 25 (max 100) | | sortField | string | No | selectedTat
| | sortOrder | enum | No | desc | | breakdownFilter | string | No | - |

**Response 200**:

```json
{
  "totalCount": 1247,
  "page": 1,
  "pageSize": 25,
  "calculationMode": "CALENDAR",
  "results": [
    {
      "labNumber": "DEV012500001",
      "patientName": "Doe, John",
      "testName": "Hemoglobin",
      "labUnit": "Hematology",
      "priority": "Routine",
      "sampleType": "Whole Blood",
      "orderingSite": "District Hospital",
      "orderCreated": "2026-01-15T08:30:00Z",
      "collected": "2026-01-15T08:45:00Z",
      "received": "2026-01-15T09:00:00Z",
      "testingStarted": "2026-01-15T09:15:00Z",
      "resultEntered": "2026-01-15T09:30:00Z",
      "validated": "2026-01-15T10:05:00Z",
      "selectedSegmentTat": 1.08,
      "overallTat": 1.58
    }
  ]
}
```

**Permission**: `tat-report` module (read). Patient name requires patient data
permission.

---

### GET /rest/reports/tat/trend

Get time series trend data.

**Query Parameters**: Same filters as summary, plus: | Param | Type | Required |
Default | |-------|------|----------|---------| | interval | enum | No | DAILY |
| compareBy | enum | No | - |

**interval Values**: `DAILY`, `WEEKLY`, `MONTHLY` **compareBy Values**:
`LAB_UNIT`, `PRIORITY`, `SAMPLE_TYPE`, `ORDERING_SITE`

**Response 200**:

```json
{
  "calculationMode": "CALENDAR",
  "series": [
    {
      "label": "All",
      "dataPoints": [
        {
          "period": "2026-01-15",
          "mean": 3.5,
          "median": 2.8,
          "percentile90": 6.0,
          "count": 45
        }
      ]
    }
  ]
}
```

**Permission**: `tat-report` module (read)

---

### GET /rest/reports/tat/export

Export TAT report data.

**Query Parameters**: Same filters as summary, plus: | Param | Type | Required |
Default | |-------|------|----------|---------| | format | enum | Yes | - |

**format Values**: `CSV`, `PDF`

**Response**:

- CSV: `text/csv` download with all detail rows, all segments, both Calendar and
  Working Time values
- PDF: `application/pdf` download with summary, histogram, breakdown, trend
  chart, up to 1000 detail rows

**Permission**: `tat-report` module (read)
