# Feature Specification: Inventory Management Reporting Backend

**Feature Branch**: `001-inventory-reporting`
**Created**: 2025-12-14
**Status**: Draft
**Input**: User description: "so the inventory mgt feature seems to be done with but pending only reporting. i have added the initial ui work for that, i  need to add the backend for that work..... and make sure they integrate well... use existing reporting framework"

## User Scenarios & Testing _(mandatory)_

### User Story 1 - Stock Levels Report Generation (Priority: P1)

Laboratory staff need to generate inventory stock level reports to monitor current inventory status, identify items that need reordering, and maintain adequate stock levels for laboratory operations.

**Why this priority**: This is the most fundamental inventory report - knowing what's in stock is essential for daily operations and prevents stockouts that could halt laboratory testing.

**Independent Test**: Can be fully tested by selecting "Stock Levels" report type, choosing PDF format, and verifying the downloaded report contains accurate current stock quantities for all active inventory items. Delivers immediate visibility into inventory status.

**Acceptance Scenarios**:

1. **Given** the user is on the Inventory Reports page, **When** they select "Stock Levels" report type and click Generate, **Then** a PDF report downloads showing all active items with current quantities, storage locations, and reorder levels
2. **Given** the user selects "Stock Levels" with "Group by Type" enabled, **When** they generate the report, **Then** items are organized by inventory item type (Reagent, Consumable, etc.)
3. **Given** the user selects "Stock Levels" with "Group by Location" enabled, **When** they generate the report, **Then** items are organized by storage location hierarchy
4. **Given** the user selects "Stock Levels" with date range filters, **When** they generate the report, **Then** the report shows stock levels as of the end date specified

---

### User Story 2 - Low Stock Alert Reports (Priority: P1)

Laboratory managers need to quickly identify items that are below their reorder threshold to proactively prevent stockouts and maintain uninterrupted testing operations.

**Why this priority**: Equally critical as stock levels - this report enables proactive inventory management and prevents testing delays due to insufficient supplies.

**Independent Test**: Can be tested independently by creating inventory items with quantities below reorder thresholds, generating a Low Stock report, and verifying only those items appear with clear indication of shortage severity.

**Acceptance Scenarios**:

1. **Given** inventory items exist with quantities below reorder levels, **When** the user generates a "Low Stock" report, **Then** the report lists only items below threshold with current quantity, reorder level, and recommended order quantity
2. **Given** the user includes inactive items in the Low Stock report, **When** they generate the report, **Then** both active and inactive items below threshold are shown with status indicator
3. **Given** multiple lots exist for an item below threshold, **When** generating the Low Stock report, **Then** the report aggregates quantities across lots and shows total available quantity

---

### User Story 3 - Expiration Forecast Reports (Priority: P2)

Laboratory quality assurance staff need to identify items approaching expiration to minimize waste through timely usage planning and prevent use of expired materials that could compromise test results.

**Why this priority**: Important for quality control and cost management, but less urgent than stock availability. Can be run weekly/monthly rather than daily.

**Independent Test**: Can be tested by creating inventory lots with various expiration dates, generating an Expiration Forecast report with 30-day lookback, and verifying items expiring within that window are listed with days remaining.

**Acceptance Scenarios**:

1. **Given** the user selects "Expiration Forecast" report with a 30-day window, **When** they generate the report, **Then** all items with lots expiring within 30 days are listed with expiration dates, quantities, and days until expiration
2. **Given** the user includes already-expired items, **When** they generate the report, **Then** expired items appear with negative days value or "EXPIRED" indicator
3. **Given** the user groups by location, **When** generating an Expiration Forecast, **Then** items are organized by storage location to facilitate physical inventory checks

---

### User Story 4 - Transaction History Reports (Priority: P2)

Laboratory supervisors and auditors need to review inventory transaction history (receipts, consumption, adjustments, disposals) for a specified date range to track inventory movements, investigate discrepancies, and support audit requirements.

**Why this priority**: Essential for accountability and auditing but not needed for daily operations. Typically used for monthly reviews or specific investigations.

**Independent Test**: Can be tested by performing various inventory transactions (receive, consume, adjust), then generating a Transaction History report for that date range and verifying all transactions appear with correct details.

**Acceptance Scenarios**:

1. **Given** the user selects "Transaction History" with a date range (required), **When** they generate the report, **Then** all inventory transactions within that date range are listed with transaction type, item, lot, quantity, user, and timestamp
2. **Given** the user exports to Excel format, **When** they generate the Transaction History report, **Then** an Excel file downloads with sortable/filterable columns for transaction analysis
3. **Given** the user exports to CSV format, **When** they generate the report, **Then** a CSV file downloads suitable for import into external analysis tools

---

### User Story 5 - Usage Trends Reports (Priority: P3)

Laboratory management needs to analyze inventory consumption patterns over time to optimize purchasing decisions, budget planning, and identify unusual consumption that might indicate process issues or waste.

**Why this priority**: Strategic planning feature - valuable for long-term optimization but not critical for daily operations. Can be generated monthly/quarterly.

**Independent Test**: Can be tested by recording inventory consumption over a period, generating a Usage Trends report for that date range, and verifying consumption totals and trends are accurately calculated and visualized.

**Acceptance Scenarios**:

1. **Given** the user selects "Usage Trends" with a date range (required), **When** they generate the report, **Then** the report shows consumption totals by item with average usage rates and trend indicators
2. **Given** the user groups by test type or department, **When** generating Usage Trends, **Then** consumption is broken down by the selected grouping to identify usage patterns
3. **Given** the date range spans multiple months, **When** generating the report, **Then** monthly consumption totals are shown to identify seasonal patterns or anomalies

---

### User Story 6 - Lot Traceability Reports (Priority: P3)

Quality assurance staff need to trace the usage of specific inventory lots to identify which test results were produced using materials from a particular lot, supporting recall investigations and quality control.

**Why this priority**: Important for quality incidents but used infrequently. Required for regulatory compliance and investigations but not routine operations.

**Independent Test**: Can be tested by recording test results that consumed specific lots, generating a Lot Traceability report for those lots, and verifying all associated test results are linked correctly.

**Acceptance Scenarios**:

1. **Given** the user selects "Lot Traceability" report, **When** they specify a lot number and generate the report, **Then** all test results and analyses using that lot are listed with patient identifiers, test dates, and results
2. **Given** a lot is recalled or suspect, **When** generating the Lot Traceability report, **Then** the report includes sufficient information to identify affected patients and determine if retesting is needed
3. **Given** the user exports to PDF, **When** generating Lot Traceability, **Then** the report includes lot details (manufacturer, expiration, receipt date, storage conditions) for documentation

---

### Edge Cases

- What happens when no inventory items match the selected report criteria (e.g., no items below reorder level)? System should generate a report with a message indicating "No items found matching criteria" rather than failing.
- How does the system handle reports with extremely large datasets (e.g., 5 years of transaction history)? System should implement pagination limits or warn users when selecting date ranges likely to produce very large reports, suggesting they narrow the criteria.
- What happens if a user generates a report while inventory data is being modified by another user? Reports should generate from a consistent snapshot to avoid inconsistent data within a single report.
- How does the system handle special characters or very long item names in exported CSV/Excel files? System should properly escape special characters and truncate or wrap long text to prevent CSV parsing errors.
- What happens when a report generation takes longer than expected (e.g., complex grouping calculations)? System should show progress indicator and allow user to cancel long-running report generation.
- How does the system handle reports when storage locations are in hierarchical structures? Reports grouped by location should correctly aggregate child location quantities into parent totals.

## Requirements _(mandatory)_

### Functional Requirements

- **FR-001**: System MUST provide a REST endpoint that accepts report generation requests with parameters: reportType, exportFormat, startDate, endDate, includeInactive, includeExpired, groupByType, groupByLocation
- **FR-002**: System MUST support six report types: STOCK_LEVELS, LOW_STOCK, EXPIRATION_FORECAST, TRANSACTION_HISTORY, USAGE_TRENDS, LOT_TRACEABILITY
- **FR-003**: System MUST support three export formats: PDF, Excel (XLSX), and CSV
- **FR-004**: System MUST validate that date range is required for USAGE_TRENDS and TRANSACTION_HISTORY report types, returning appropriate error if missing
- **FR-005**: System MUST validate that startDate is not after endDate, returning appropriate error if invalid
- **FR-006**: System MUST generate Stock Levels reports showing all inventory items with current quantities, storage locations, reorder levels, and last updated timestamps
- **FR-007**: System MUST generate Low Stock reports showing only items where total available quantity is below the reorder threshold
- **FR-008**: System MUST generate Expiration Forecast reports showing lots expiring within the specified date range (default 30 days) with days until expiration
- **FR-009**: System MUST generate Transaction History reports showing all inventory transactions within the specified date range with transaction type, item, lot, quantity, user, timestamp, and reason
- **FR-010**: System MUST generate Usage Trends reports showing consumption totals and average usage rates for the specified date range with trend indicators
- **FR-011**: System MUST generate Lot Traceability reports linking specific lots to test results, analyses, and patient identifiers
- **FR-012**: System MUST apply the includeInactive filter to include or exclude inactive inventory items from reports (default: exclude)
- **FR-013**: System MUST apply the includeExpired filter to include or exclude expired lots from reports (default: include for historical reports, exclude for operational reports)
- **FR-014**: System MUST apply groupByType filter to organize report data by inventory item type when enabled
- **FR-015**: System MUST apply groupByLocation filter to organize report data by storage location hierarchy when enabled
- **FR-016**: System MUST set appropriate HTTP response headers including Content-Type and Content-Disposition with filename
- **FR-017**: System MUST return report data as binary stream (Blob) for PDF/Excel, or text stream for CSV
- **FR-018**: System MUST generate unique filenames for downloaded reports including report type and generation timestamp
- **FR-019**: System MUST handle errors gracefully and return appropriate HTTP status codes (400 for validation errors, 500 for generation errors)
- **FR-020**: System MUST log all report generation requests with user, report type, parameters, and timestamp for audit purposes

### Constitution Compliance Requirements (OpenELIS Global)

- **CR-001**: UI components already implemented using Carbon Design System (@carbon/react) - backend integration MUST preserve this
- **CR-002**: All UI strings already internationalized via React Intl - backend MUST return data in format compatible with frontend expectations
- **CR-003**: Backend MUST follow 5-layer architecture (Valueholder→DAO→Service→Controller→Form)
  - REST Controller handles HTTP request/response
  - Service layer contains business logic for report generation
  - DAO layer queries inventory data
  - Report beans (Valueholders) structure data for export
  - Form objects capture request parameters
- **CR-004**: Database queries MUST use existing inventory tables (inventory_item, inventory_lot, inventory_transaction, inventory_usage, inventory_audit_log) - NO new tables required for reporting
- **CR-005**: Report generation MUST integrate with existing reporting framework (JasperReports for PDF, Apache POI for Excel, custom CSV writer)
- **CR-006**: All report parameters MUST be passed via query parameters or request body - NO hardcoded configuration
- **CR-007**: Security: Reports MUST respect user permissions and include audit logging of report generation events (user_id + timestamp)
- **CR-008**: Tests MUST be included:
  - Unit tests for service layer report logic
  - Integration tests for REST endpoints
  - E2E tests verifying frontend-to-backend integration for each report type

### Key Entities _(include if feature involves data)_

- **InventoryReportRequest**: Captures user's report generation request including reportType, exportFormat, date range filters, and grouping options
- **StockLevelReportData**: Aggregated view of current inventory stock levels including item details, quantities across lots, storage locations, and reorder status
- **TransactionReportData**: Historical record of inventory transactions including transaction type, item/lot details, quantities, user, timestamp, and reason
- **ExpirationReportData**: View of inventory lots with upcoming expirations including item details, lot information, expiration dates, quantities, and storage locations
- **UsageTrendReportData**: Aggregated consumption data over time including item, consumption totals, average usage rates, and trend calculations
- **LotTraceabilityReportData**: Linkage between inventory lots and test results including lot details, test result IDs, patient identifiers, and test dates

## Success Criteria _(mandatory)_

### Measurable Outcomes

- **SC-001**: Users can generate any of the six report types and receive downloaded files in under 5 seconds for datasets under 1000 records
- **SC-002**: Reports accurately reflect real-time inventory data with less than 1-minute lag from transaction to report visibility
- **SC-003**: 100% of report generation requests complete successfully or return clear error messages indicating the specific validation issue
- **SC-004**: All three export formats (PDF, Excel, CSV) produce correctly formatted files that open without errors in standard applications (Adobe Reader, Microsoft Excel, text editors)
- **SC-005**: Reports include all required data fields as specified in the UI design with no missing columns or incomplete information
- **SC-006**: The frontend UI successfully integrates with the backend with zero JavaScript errors and downloads files with correct Content-Type headers
- **SC-007**: Report generation performs efficiently even with large datasets, handling up to 10,000 inventory items and 100,000 transactions without timeout or memory errors
- **SC-008**: Grouping options (by type, by location) produce correctly organized reports with accurate subtotals and hierarchical structure
- **SC-009**: Date range filters accurately include only transactions/data within the specified boundaries with correct timezone handling
- **SC-010**: All report generation events are logged to audit trail with user identification, enabling compliance with regulatory requirements

### Technical Quality Criteria

- **TQ-001**: Code coverage exceeds 70% for report service layer
- **TQ-002**: All REST endpoints return appropriate HTTP status codes (200 for success, 400 for validation errors, 500 for server errors)
- **TQ-003**: Report generation uses existing reporting framework patterns (JasperReports/POI/CSV) consistently with other OpenELIS reports
- **TQ-004**: No SQL injection vulnerabilities in report queries (use parameterized queries)
- **TQ-005**: All backend code passes Spotless formatting checks
- **TQ-006**: Integration tests verify each report type with various parameter combinations
- **TQ-007**: E2E tests verify complete user workflow from selecting report options to downloading files

## Assumptions

- **A-001**: The existing inventory management feature is complete and functional with tables for inventory_item, inventory_lot, inventory_transaction, inventory_usage, and inventory_audit_log
- **A-002**: The frontend UI work referenced is the InventoryReports.jsx and InventoryReportsModal.jsx components already implemented
- **A-003**: The OpenELIS reporting framework (JasperReports for PDF, Apache POI for Excel) is already configured and available
- **A-004**: Report generation will be synchronous (user waits for report) rather than asynchronous (queued background job) based on existing OpenELIS patterns
- **A-005**: Reports do not require complex visualizations (charts/graphs) - primarily tabular data with grouping and aggregation
- **A-006**: Users generating reports have appropriate permissions to view inventory data (permission checks will use existing RBAC system)
- **A-007**: Report file sizes will typically be under 10MB (no special handling for very large reports beyond warning users)
- **A-008**: Timezone handling will use server timezone for date range queries (consistent with existing OpenELIS behavior)

## Out of Scope

- Real-time report previews in the browser (reports are download-only)
- Scheduled/automated report generation (future enhancement)
- Custom report templates or user-defined report layouts
- Email delivery of reports
- Report history or saved report configurations
- Interactive charts or data visualizations within reports
- Report sharing or collaborative features
- Mobile-optimized report viewing
- Integration with external business intelligence tools

## Dependencies

- **D-001**: Existing inventory management backend (DAOs, Services, Controllers for inventory_item, inventory_lot, inventory_transaction, inventory_usage)
- **D-002**: OpenELIS reporting framework (JasperReports library for PDF generation)
- **D-003**: Apache POI library for Excel (XLSX) generation
- **D-004**: Existing authentication and authorization system for user identification and permission checks
- **D-005**: Existing audit logging infrastructure to record report generation events
- **D-006**: Frontend components: InventoryReports.jsx, InventoryReportsModal.jsx, InventoryService.js (ReportsAPI)
- **D-007**: Internationalization (i18n) strings already defined in frontend language files

## References

- Existing UI Implementation:
  - `/frontend/src/components/inventory/InventoryReports.jsx`
  - `/frontend/src/components/inventory/InventoryReportsModal.jsx`
  - `/frontend/src/components/inventory/InventoryService.js` (ReportsAPI.generate method)
- OpenELIS Reporting Framework:
  - `/src/main/java/org/openelisglobal/reports/controller/rest/ReportRestController.java`
  - `/src/main/java/org/openelisglobal/reports/action/implementation/Report.java`
  - Example: `/src/main/java/org/openelisglobal/coldstorage/controller/rest/FreezerReportDataController.java`
- Constitution: `.specify/memory/constitution.md` (v1.7.0)
