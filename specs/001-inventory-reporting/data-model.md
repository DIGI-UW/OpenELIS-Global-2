# Data Model: Inventory Management Reporting Backend

**Feature**: 001-inventory-reporting
**Date**: 2025-12-14

## Overview

This document defines the report bean DTOs (valueholders) used to structure data for inventory reports. These are simple POJOs used with JRBeanCollectionDataSource for JasperReports integration and CSV/Excel export.

## Report Data Beans

### StockLevelReportData

Represents current inventory stock levels for a single item.

**Fields**:
- `String itemId` - Inventory item ID
- `String itemName` - Item name (e.g., "Reagent XYZ")
- `String itemType` - Item type (REAGENT, CONSUMABLE, etc.)
- `String categoryName` - Category name (if groupByType enabled)
- `String storageLocationPath` - Full storage path (e.g., "Lab A > Freezer 1 > Shelf 2")
- `String storageLocationId` - Storage location ID (if groupByLocation enabled)
- `BigDecimal totalQuantity` - Total quantity across all lots
- `String unitOfMeasure` - Unit (mL, units, boxes, etc.)
- `BigDecimal reorderLevel` - Reorder threshold quantity
- `Boolean isBelowReorderLevel` - True if totalQuantity < reorderLevel
- `Integer activeLotCount` - Number of active lots for this item
- `LocalDateTime lastUpdated` - Last inventory update timestamp

**Getter Methods** (for JasperReports):
- All fields have standard getters (e.g., `getItemName()`, `getTotalQuantity()`)

---

### LowStockReportData

Represents items currently below their reorder threshold.

**Fields**:
- `String itemId`
- `String itemName`
- `String itemType`
- `String storageLocationPath`
- `BigDecimal currentQuantity` - Total available quantity
- `String unitOfMeasure`
- `BigDecimal reorderLevel` - Threshold
- `BigDecimal shortfall` - Calculated as (reorderLevel - currentQuantity)
- `BigDecimal recommendedOrderQuantity` - Suggested order amount
- `Integer daysUntilStockout` - Estimated days based on usage trends (nullable)
- `LocalDateTime lastUpdated`

---

### ExpirationForecastReportData

Represents inventory lots expiring within the forecast window.

**Fields**:
- `String lotId`
- `String lotNumber`
- `String itemName`
- `String itemType`
- `String storageLocationPath`
- `BigDecimal quantity`
- `String unitOfMeasure`
- `LocalDate expirationDate`
- `Integer daysUntilExpiration` - Calculated as (expirationDate - currentDate)
- `String expirationStatus` - "EXPIRED", "CRITICAL" (<7 days), "WARNING" (<30 days)
- `String manufacturer` - Lot manufacturer (optional)
- `LocalDate receivedDate` - Date lot was received

---

### TransactionHistoryReportData

Represents a single inventory transaction within the date range.

**Fields**:
- `String transactionId`
- `LocalDateTime transactionTimestamp`
- `String transactionType` - "RECEIVE", "CONSUME", "ADJUST", "DISPOSE", "TRANSFER"
- `String itemName`
- `String lotNumber`
- `String storageLocationPath`
- `BigDecimal quantityChange` - Positive for receipt, negative for consumption/disposal
- `String unitOfMeasure`
- `BigDecimal quantityAfterTransaction` - Resulting quantity
- `String performedBy` - User who performed transaction
- `String reason` - Transaction reason/notes (optional)
- `String referenceType` - "TEST_RESULT", "PURCHASE_ORDER", "MANUAL", etc.
- `String referenceId` - ID of referenced entity (optional)

---

### UsageTrendReportData

Represents aggregated consumption data for an item over the date range.

**Fields**:
- `String itemId`
- `String itemName`
- `String itemType`
- `BigDecimal totalConsumed` - Total quantity consumed in date range
- `String unitOfMeasure`
- `BigDecimal averageDailyUsage` - totalConsumed / days in range
- `BigDecimal averageWeeklyUsage` - totalConsumed / weeks in range
- `BigDecimal averageMonthlyUsage` - totalConsumed / months in range
- `Integer transactionCount` - Number of consumption transactions
- `String trendIndicator` - "INCREASING", "STABLE", "DECREASING" (based on week-over-week comparison)
- `BigDecimal peakUsageMonth` - Highest monthly consumption
- `String peakUsageMonthName` - Month name of peak (e.g., "June 2024")

---

### LotTraceabilityReportData

Represents linkage between a lot and test results/analyses that used it.

**Fields**:
- `String lotId`
- `String lotNumber`
- `String itemName`
- `String manufacturer`
- `LocalDate expirationDate`
- `LocalDate receivedDate`
- `String testResultId` - Test result that consumed this lot
- `String accessionNumber` - Sample accession number
- `String patientId` - Patient identifier (de-identified if needed)
- `String testName` - Test/analysis name
- `LocalDateTime testDate` - Date test was performed
- `String performedBy` - Technician who performed test
- `BigDecimal quantityUsed` - Amount consumed for this test
- `String unitOfMeasure`

---

## Relationship to Existing Entities

These report beans aggregate data from existing inventory entities:

**Source Tables** (via DAOs):
- `inventory_item` - Item master data
- `inventory_lot` - Lot/batch information
- `inventory_transaction` - Transaction history
- `inventory_usage` - Test result usage tracking
- `inventory_storage_location` - Storage hierarchy
- `inventory_audit_log` - Audit trail (optional)

**Data Aggregation**:
- Service layer performs JOIN FETCH queries to eagerly load relationships
- Beans are populated within service transaction (prevents LazyInitializationException)
- All calculated fields (shortfall, daysUntilExpiration, etc.) computed in service layer

## CSV/Excel Export Considerations

**Field Ordering** (for CSV headers):
- Order fields logically (ID, name, type, location, quantity fields, dates, status)
- Use `getAsCSVString()` method pattern for row serialization

**Date Formatting**:
- LocalDate: "yyyy-MM-dd" (ISO 8601)
- LocalDateTime: "yyyy-MM-dd HH:mm:ss"
- Timezone: Server timezone (consistent with existing OpenELIS behavior)

**Number Formatting**:
- BigDecimal: Use `setScale(2, RoundingMode.HALF_UP)` for quantities
- Integer: Plain integer representation

**Null Handling**:
- Null strings: Return empty string ""
- Null numbers: Return "0" or empty string (context-dependent)
- Null dates: Return empty string ""

## JasperReports Integration

**Field Definitions in JRXML**:
```xml
<field name="itemName" class="java.lang.String"/>
<field name="totalQuantity" class="java.math.BigDecimal"/>
<field name="storageLocationPath" class="java.lang.String"/>
```

**Date/Number Formatting**:
- Use Jasper formatters in JRXML, NOT pre-formatted strings in beans
- Keeps beans simple and reusable across formats

**Grouping Support**:
- Use `categoryName` or `storageLocationId` as group expression
- JasperReports Group bands handle subtotals automatically

