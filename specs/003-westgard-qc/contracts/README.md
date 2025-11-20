# API Contracts: Westgard QC Feature

This directory contains OpenAPI 3.0.3 specifications for the Westgard Rules Quality Control compliance feature REST APIs.

## Contract Files

### 1. qc-api.yaml
**Endpoints for QC dashboard, result entry, and control lot management**

- `GET /dashboard` - Get QC dashboard overview for all analyzers
- `GET /dashboard/{analyzerId}` - Get analyzer-specific dashboard data
- `POST /results` - Create QC result (manual entry or from ASTM interface via Feature 004)
- `GET /control-lots` - List control lots with filtering
- `POST /control-lots` - Create new control lot
- `GET /control-lots/{lotId}` - Get control lot details
- `PUT /control-lots/{lotId}` - Update control lot configuration
- `POST /control-lots/{lotId}/deactivate` - Deactivate control lot
- `GET /charts/{lotId}` - Get Levey-Jennings chart data

**Key Schemas:**
- `DashboardResponse` - Dashboard overview data
- `QCResultCreateRequest` - QC result entry payload
- `ControlLot` - Control lot entity
- `ChartDataResponse` - Levey-Jennings chart data points

### 2. violation-api.yaml
**Endpoints for violation management and alerts**

- `GET /violations` - List violations with filtering (analyzer, severity, status, date range)
- `GET /violations/{violationId}` - Get violation details
- `POST /violations/{violationId}/acknowledge` - Acknowledge violation
- `POST /violations/{violationId}/resolve` - Resolve violation (warning-level only)
- `GET /alerts/recent` - Get recent alerts for current user
- `POST /alerts/{alertId}/mark-read` - Mark alert as read

**Key Schemas:**
- `ViolationSummary` - Violation list item
- `ViolationDetail` - Full violation details with related results
- `Alert` - Notification entity

### 3. corrective-action-api.yaml
**Endpoints for corrective action workflow**

- `GET /corrective-actions` - List corrective actions with filtering
- `POST /corrective-actions` - Create corrective action for violation
- `GET /corrective-actions/{actionId}` - Get corrective action details
- `PUT /corrective-actions/{actionId}` - Update corrective action
- `POST /corrective-actions/{actionId}/assign` - Assign action to user
- `POST /corrective-actions/{actionId}/start` - Mark action as in progress
- `POST /corrective-actions/{actionId}/complete` - Complete action with resolution notes
- `GET /corrective-actions/my-tasks` - Get current user's assigned tasks

**Key Schemas:**
- `CorrectiveActionSummary` - Action list item
- `CorrectiveActionDetail` - Full action details
- `CorrectiveActionCreateRequest` - Action creation payload

## Authentication

All endpoints use Spring Security session-based authentication (`JSESSIONID` cookie).

**Required Roles:**
- **Results role**: View QC results, create corrective actions
- **Biologist role**: All Results permissions + configure rules, manage control lots, resolve violations
- **Global Admin role**: Full access

## Common Response Codes

- `200 OK` - Successful request
- `201 Created` - Resource created successfully
- `400 Bad Request` - Invalid request (validation failed, business rule violation)
- `401 Unauthorized` - User not authenticated or lacks required role
- `403 Forbidden` - User authenticated but lacks permission for specific resource
- `404 Not Found` - Resource not found
- `500 Internal Server Error` - Server error

## Internationalization

All error responses include an `i18nKey` field for client-side localization:

```json
{
  "error": "Control lot is not active",
  "errorCode": "CONTROL_LOT_INACTIVE",
  "i18nKey": "qc.error.control_lot_inactive"
}
```

Frontend must use React Intl to resolve `i18nKey` to localized message.

## Pagination

List endpoints support pagination via query parameters:

- `page` - Page number (0-based, default: 0)
- `size` - Items per page (1-100, default: 20)
- `sort` - Sort field and direction (e.g., `violationDateTime,desc`)

Response includes `pagination` object:

```json
{
  "violations": [...],
  "pagination": {
    "currentPage": 0,
    "pageSize": 20,
    "totalItems": 45,
    "totalPages": 3
  }
}
```

## Testing

Use these contracts with:

- **Backend Testing**: Spring MVC Test with MockMvc
- **Integration Testing**: RestAssured
- **Frontend Testing**: Cypress intercepts with contract schemas
- **API Documentation**: Swagger UI or Redoc

## Cross-References

- **Feature Spec**: `../spec.md`
- **Implementation Plan**: `../plan.md`
- **Data Model**: `../data-model.md`
- **Quickstart Guide**: `../quickstart.md`
- **Feature 004 Integration**: QC result capture from ASTM interface (FR-008 in spec.md)

