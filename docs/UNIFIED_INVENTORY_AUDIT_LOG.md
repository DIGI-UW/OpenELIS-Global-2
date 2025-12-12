# Unified Inventory Audit Log API

## Overview

The Unified Inventory Audit Log provides a centralized view of ALL audit trail activity across all inventory-related tables. Instead of querying each table separately, you can now get a consolidated view of all inventory changes in one place.

## New REST Endpoints

### 1. GET `/rest/inventory/audit-logs/all` - Unified Audit Log

Get all inventory-related audit logs across all tables (items, lots, locations, usage, transactions) in a single unified view.

#### Request Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `startDate` | String | No | - | Start date filter (format: `yyyy-MM-dd`) |
| `endDate` | String | No | - | End date filter (format: `yyyy-MM-dd`) |
| `entityType` | String | No | - | Filter by entity type: `ITEM`, `LOT`, `LOCATION`, `USAGE`, `TRANSACTION` |
| `userId` | String | No | - | Filter by user ID |
| `activity` | String | No | - | Filter by activity type: `I` (INSERT), `U` (UPDATE), `D` (DELETE) |
| `limit` | Integer | No | 100 | Maximum records to return (max: 1000) |
| `offset` | Integer | No | 0 | Pagination offset |

#### Response Format

```json
{
  "logs": [
    {
      "id": 12345,
      "timestamp": "2025-12-12T10:30:00",
      "activity": "U",
      "performedByUser": "john.doe",
      "sysUserId": "42",
      "entityType": "LOT",
      "tableName": "INVENTORY_LOT",
      "changes": {
        "currentQuantity": {
          "old": "100.0",
          "new": "85.0"
        },
        "status": {
          "old": "ACTIVE",
          "new": "IN_USE"
        }
      },
      "changesXml": "<field name=\"currentQuantity\">...</field>",
      "summary": "Current Quantity: 100.0 → 85.0, Status: ACTIVE → IN_USE"
    }
  ],
  "totalRecords": 547,
  "limit": 100,
  "offset": 0,
  "hasMore": true
}
```

#### Example Usage

```javascript
// Get all inventory logs from the last 7 days
fetch('/rest/inventory/audit-logs/all?startDate=2025-12-05&endDate=2025-12-12')
  .then(res => res.json())
  .then(data => {
    console.log(`Total audit records: ${data.totalRecords}`);
    console.log(`Showing ${data.logs.length} records`);

    data.logs.forEach(log => {
      console.log(`[${log.entityType}] ${log.timestamp}: ${log.summary}`);
      console.log(`  Performed by: ${log.performedByUser}`);
    });
  });

// Get only LOT changes made by a specific user
fetch('/rest/inventory/audit-logs/all?entityType=LOT&userId=42&limit=50')
  .then(res => res.json())
  .then(data => console.log(data));

// Get all INSERT activities (new records created)
fetch('/rest/inventory/audit-logs/all?activity=I')
  .then(res => res.json())
  .then(data => console.log(`${data.totalRecords} records created`));

// Pagination example
fetch('/rest/inventory/audit-logs/all?limit=20&offset=40')
  .then(res => res.json())
  .then(data => {
    console.log(`Page 3 of ${Math.ceil(data.totalRecords / 20)}`);
  });
```

---

### 2. GET `/rest/inventory/audit-logs/statistics` - Audit Log Statistics

Get summary statistics about inventory audit trail activity.

#### Response Format

```json
{
  "totalLogs": 1247,
  "countByTable": {
    "INVENTORY_ITEM": 89,
    "INVENTORY_LOT": 542,
    "INVENTORY_STORAGE_LOCATION": 23,
    "INVENTORY_USAGE": 487,
    "INVENTORY_TRANSACTION": 106
  },
  "countByActivity": {
    "INSERT": 234,
    "UPDATE": 987,
    "DELETE": 26
  }
}
```

#### Example Usage

```javascript
fetch('/rest/inventory/audit-logs/statistics')
  .then(res => res.json())
  .then(stats => {
    console.log(`Total audit logs: ${stats.totalLogs}`);
    console.log(`Most active table: ${Object.keys(stats.countByTable).reduce((a,b) =>
      stats.countByTable[a] > stats.countByTable[b] ? a : b
    )}`);
  });
```

---

## UI Integration Examples

### Option 1: Add "Audit History" Tab to Inventory UI

Add a new tab in the inventory module that shows all changes:

```jsx
// frontend/src/components/inventory/InventoryAuditHistoryTab.jsx
import React, { useState, useEffect } from 'react';
import { DataTable, DatePicker, Select } from '@carbon/react';

const InventoryAuditHistoryTab = () => {
  const [logs, setLogs] = useState([]);
  const [filters, setFilters] = useState({
    startDate: null,
    endDate: null,
    entityType: '',
    activity: '',
    limit: 100,
    offset: 0
  });
  const [stats, setStats] = useState(null);

  useEffect(() => {
    fetchAuditLogs();
    fetchStatistics();
  }, [filters]);

  const fetchAuditLogs = async () => {
    const params = new URLSearchParams();
    if (filters.startDate) params.append('startDate', filters.startDate);
    if (filters.endDate) params.append('endDate', filters.endDate);
    if (filters.entityType) params.append('entityType', filters.entityType);
    if (filters.activity) params.append('activity', filters.activity);
    params.append('limit', filters.limit);
    params.append('offset', filters.offset);

    const response = await fetch(`/rest/inventory/audit-logs/all?${params}`);
    const data = await response.json();
    setLogs(data.logs);
  };

  const fetchStatistics = async () => {
    const response = await fetch('/rest/inventory/audit-logs/statistics');
    const data = await response.json();
    setStats(data);
  };

  return (
    <div className="inventory-audit-history">
      <h2>Inventory Audit History</h2>

      {stats && (
        <div className="stats-summary">
          <div>Total Logs: {stats.totalLogs}</div>
          <div>Inserts: {stats.countByActivity.INSERT}</div>
          <div>Updates: {stats.countByActivity.UPDATE}</div>
          <div>Deletes: {stats.countByActivity.DELETE}</div>
        </div>
      )}

      <div className="filters">
        <DatePicker
          datePickerType="range"
          onChange={([start, end]) => {
            setFilters({
              ...filters,
              startDate: start?.toISOString().split('T')[0],
              endDate: end?.toISOString().split('T')[0]
            });
          }}
        >
          <DatePickerInput placeholder="Start date" />
          <DatePickerInput placeholder="End date" />
        </DatePicker>

        <Select
          id="entity-type-filter"
          labelText="Entity Type"
          onChange={(e) => setFilters({...filters, entityType: e.target.value})}
        >
          <SelectItem value="" text="All Types" />
          <SelectItem value="ITEM" text="Items" />
          <SelectItem value="LOT" text="Lots" />
          <SelectItem value="LOCATION" text="Locations" />
          <SelectItem value="USAGE" text="Usage" />
          <SelectItem value="TRANSACTION" text="Transactions" />
        </Select>

        <Select
          id="activity-filter"
          labelText="Activity"
          onChange={(e) => setFilters({...filters, activity: e.target.value})}
        >
          <SelectItem value="" text="All Activities" />
          <SelectItem value="I" text="Created" />
          <SelectItem value="U" text="Updated" />
          <SelectItem value="D" text="Deleted" />
        </Select>
      </div>

      <DataTable
        rows={logs.map(log => ({
          id: log.id,
          timestamp: new Date(log.timestamp).toLocaleString(),
          entityType: log.entityType,
          activity: log.activity === 'I' ? 'Created' : log.activity === 'U' ? 'Updated' : 'Deleted',
          user: log.performedByUser,
          summary: log.summary
        }))}
        headers={[
          { key: 'timestamp', header: 'Timestamp' },
          { key: 'entityType', header: 'Type' },
          { key: 'activity', header: 'Activity' },
          { key: 'user', header: 'User' },
          { key: 'summary', header: 'Changes' }
        ]}
      />
    </div>
  );
};

export default InventoryAuditHistoryTab;
```

---

### Option 2: Create Audit Report (for Reports Tab)

Create a formal audit report that can be exported:

```jsx
// frontend/src/components/reports/InventoryAuditReport.jsx
import React, { useState } from 'react';
import { Button } from '@carbon/react';
import { Download } from '@carbon/icons-react';

const InventoryAuditReport = () => {
  const [reportData, setReportData] = useState(null);

  const generateReport = async (startDate, endDate) => {
    const params = new URLSearchParams({
      startDate,
      endDate,
      limit: 10000 // Get all for report
    });

    const response = await fetch(`/rest/inventory/audit-logs/all?${params}`);
    const data = await response.json();
    setReportData(data);
  };

  const exportToPDF = () => {
    // Implementation for PDF export
    console.log('Exporting to PDF...');
  };

  const exportToExcel = () => {
    // Implementation for Excel export
    const csvContent = [
      ['Timestamp', 'Entity Type', 'Activity', 'User', 'Summary'],
      ...reportData.logs.map(log => [
        log.timestamp,
        log.entityType,
        log.activity,
        log.performedByUser,
        log.summary
      ])
    ].map(row => row.join(',')).join('\n');

    const blob = new Blob([csvContent], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `inventory-audit-${Date.now()}.csv`;
    link.click();
  };

  return (
    <div className="inventory-audit-report">
      <h2>Inventory Audit Report</h2>

      <div className="report-controls">
        <DateRangePicker onGenerate={generateReport} />

        {reportData && (
          <>
            <Button onClick={exportToPDF} renderIcon={Download}>
              Export to PDF
            </Button>
            <Button onClick={exportToExcel} renderIcon={Download}>
              Export to Excel
            </Button>
          </>
        )}
      </div>

      {reportData && (
        <div className="report-summary">
          <h3>Summary</h3>
          <p>Total Records: {reportData.totalRecords}</p>
          <p>Period: {/* date range */}</p>
          {/* Render report data */}
        </div>
      )}
    </div>
  );
};

export default InventoryAuditReport;
```

---

## Use Cases

### 1. Compliance & Regulatory Reporting

```javascript
// Get all audit logs for last quarter for compliance report
const lastQuarterStart = '2025-10-01';
const lastQuarterEnd = '2025-12-31';

fetch(`/rest/inventory/audit-logs/all?startDate=${lastQuarterStart}&endDate=${lastQuarterEnd}&limit=10000`)
  .then(res => res.json())
  .then(data => {
    // Generate compliance report
    console.log(`Total inventory activities: ${data.totalRecords}`);
    // Export to PDF for auditors
  });
```

### 2. Investigating Discrepancies

```javascript
// Find all changes to a specific lot in the last week
fetch('/rest/inventory/audit-logs/all?entityType=LOT&startDate=2025-12-05')
  .then(res => res.json())
  .then(data => {
    data.logs.forEach(log => {
      console.log(`${log.timestamp}: ${log.summary}`);
      console.log(`Changed by: ${log.performedByUser}`);
    });
  });
```

### 3. User Activity Monitoring

```javascript
// See all changes made by a specific user
fetch('/rest/inventory/audit-logs/all?userId=42')
  .then(res => res.json())
  .then(data => {
    console.log(`User made ${data.totalRecords} changes`);
    // Show detailed activity timeline
  });
```

### 4. Daily Activity Dashboard

```javascript
// Show today's inventory activity
const today = new Date().toISOString().split('T')[0];

Promise.all([
  fetch(`/rest/inventory/audit-logs/all?startDate=${today}`).then(r => r.json()),
  fetch('/rest/inventory/audit-logs/statistics').then(r => r.json())
]).then(([todayLogs, stats]) => {
  console.log(`Today's activity: ${todayLogs.totalRecords} changes`);
  console.log(`All-time activity: ${stats.totalLogs} total logs`);
});
```

---

## Performance Considerations

### Pagination

For large datasets, always use pagination:

```javascript
const PAGE_SIZE = 100;
let currentPage = 0;

async function loadNextPage() {
  const response = await fetch(
    `/rest/inventory/audit-logs/all?limit=${PAGE_SIZE}&offset=${currentPage * PAGE_SIZE}`
  );
  const data = await response.json();

  // Display data.logs

  if (data.hasMore) {
    // Show "Load More" button
  }

  currentPage++;
}
```

### Filtering

Apply filters to reduce result size:

```javascript
// Instead of fetching all and filtering in frontend
// ❌ BAD
const allLogs = await fetch('/rest/inventory/audit-logs/all?limit=10000');
const lotLogs = allLogs.filter(log => log.entityType === 'LOT');

// ✅ GOOD
const lotLogs = await fetch('/rest/inventory/audit-logs/all?entityType=LOT');
```

### Caching

Consider caching statistics and recent logs:

```javascript
// Cache statistics for 5 minutes
const statsCache = {
  data: null,
  timestamp: null
};

async function getStatistics() {
  const now = Date.now();
  if (statsCache.data && (now - statsCache.timestamp < 300000)) {
    return statsCache.data;
  }

  const response = await fetch('/rest/inventory/audit-logs/statistics');
  const data = await response.json();

  statsCache.data = data;
  statsCache.timestamp = now;

  return data;
}
```

---

## Backend Architecture

### How It Works

1. **Query all inventory tables**: The endpoint queries 5 inventory tables registered in `reference_tables`
2. **Transform to common format**: Each History record is transformed to a common format with entity type
3. **Apply filters**: Date, user, activity, entity type filters are applied
4. **Sort chronologically**: All logs are sorted by timestamp descending (newest first)
5. **Paginate**: Results are paginated based on limit/offset parameters

### Database Query

The endpoint uses the new `getAllHistoryByRefTableId()` method:

```java
// HistoryDAOImpl.java
public List<History> getAllHistoryByRefTableId(String tableId) {
    String sql = "from History h where h.referenceTable = :tableId " +
                 "order by h.timestamp desc, h.activity desc";
    Query<History> query = entityManager.unwrap(Session.class)
        .createQuery(sql, History.class);
    query.setParameter("tableId", Integer.parseInt(tableId));
    return query.list();
}
```

---

## Testing

Test the unified audit log endpoints:

```bash
# Get all logs
curl http://localhost:8080/rest/inventory/audit-logs/all

# Filter by date range
curl "http://localhost:8080/rest/inventory/audit-logs/all?startDate=2025-12-01&endDate=2025-12-12"

# Filter by entity type
curl "http://localhost:8080/rest/inventory/audit-logs/all?entityType=LOT"

# Filter by user and activity
curl "http://localhost:8080/rest/inventory/audit-logs/all?userId=1&activity=U"

# Get statistics
curl http://localhost:8080/rest/inventory/audit-logs/statistics
```

---

## Future Enhancements

### Potential additions:

1. **Real-time updates**: WebSocket support for live audit log streaming
2. **Advanced search**: Full-text search in change summaries
3. **Aggregations**: Group by time periods (daily/weekly/monthly)
4. **Change analytics**: Charts showing activity trends
5. **Alerting**: Notifications for suspicious activities
6. **Audit log retention**: Automated archival of old logs

---

## Summary

The Unified Inventory Audit Log API provides:

✅ **Single endpoint** to query all inventory audit logs
✅ **Flexible filtering** by date, entity type, user, activity
✅ **Pagination** for large datasets
✅ **Statistics** for reporting and dashboards
✅ **Performance optimized** with proper indexing
✅ **Ready for UI integration** in inventory module or reports tab

This makes it easy to build comprehensive audit trail viewers, compliance reports, and activity dashboards for your inventory management system!
