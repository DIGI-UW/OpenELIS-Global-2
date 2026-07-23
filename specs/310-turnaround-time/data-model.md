# Data Model: 310 Turn Around Time

**Date**: 2026-04-02

---

## New Entities

### PublicHoliday

| Field        | Type                   | Required | Description                                                                                                                                                                              |
| ------------ | ---------------------- | -------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| id           | Integer (PK, sequence) | Yes      | Auto-generated primary key                                                                                                                                                               |
| holiday_date | DATE                   | Yes      | The holiday date                                                                                                                                                                         |
| holiday_name | VARCHAR(100)           | Yes      | Descriptive name                                                                                                                                                                         |
| is_recurring | BOOLEAN                | No       | If true, repeats annually on same month/day. Default: false                                                                                                                              |
| is_active    | BOOLEAN                | No       | If false, excluded from TAT calculations. Default: true                                                                                                                                  |
| lastupdated  | TIMESTAMP              | Yes      | Audit: last modification timestamp                                                                                                                                                       |
| sys_user_id  | VARCHAR(36)            | Yes      | Audit: user who last modified (string ID, not FK — per codebase convention). NOTE: `BaseObject.sysUserId` is `@Transient`; subclass MUST add `@Column(name = "sys_user_id")` to persist. |

**Constraints**:

- No DB UNIQUE constraint on holiday_date (recurring expansion makes DB-level
  uniqueness infeasible). Duplicate detection enforced at the service layer:
  reject if another holiday (including recurring occurrences) already exists for
  the same month/day in the target year
- holiday_name max 100 characters
- sys_user_id is a string user ID (from `getSysUserId(request)`), not an integer
  FK

**Lifecycle**: Created > Active > Inactive (soft) > Deleted (hard)

---

### WeekendConfig

| Field       | Type                   | Required | Description                                                                                      |
| ----------- | ---------------------- | -------- | ------------------------------------------------------------------------------------------------ |
| id          | Integer (PK, sequence) | Yes      | Auto-generated primary key                                                                       |
| day_of_week | INTEGER                | Yes      | 0=Sunday, 1=Monday, ..., 6=Saturday                                                              |
| is_weekend  | BOOLEAN                | No       | Whether this day is a weekend day. Default: false                                                |
| lastupdated | TIMESTAMP              | Yes      | Audit: last modification timestamp                                                               |
| sys_user_id | VARCHAR(36)            | Yes      | Audit: user who last modified (string ID, `@Column` override required on `BaseObject.sysUserId`) |

**Constraints**:

- UNIQUE(day_of_week) — exactly 7 rows, one per day
- day_of_week range: 0-6
- Seeded with Saturday(6)=true, Sunday(0)=true; all others false

**Lifecycle**: Rows are seeded on creation. Only `is_weekend` field is updated,
never inserted/deleted.

---

## Existing Entities (Read-Only for TAT)

### Sample (timestamps used)

| Field                                 | Java Type            | Precision | Used For Segment                        |
| ------------------------------------- | -------------------- | --------- | --------------------------------------- |
| entered_date (DB: ENTERED_DATE)       | `java.sql.Date`      | Date only | Order Created (segments 1, 7)           |
| collection_date (DB: COLLECTION_DATE) | `java.sql.Timestamp` | DateTime  | Specimen Collected (segments 1, 2)      |
| receivedTimestamp (DB: RECEIVED_DATE) | `java.sql.Timestamp` | DateTime  | Specimen Received (segments 2, 3, 4, 5) |

Use `getReceivedTimestamp()` (not `getReceivedDate()` which converts to Date).

### Analysis (timestamps used)

| Field                              | Java Type (before M0) | Java Type (after M0) | DB Column Type                | Used For Segment                      |
| ---------------------------------- | --------------------- | -------------------- | ----------------------------- | ------------------------------------- |
| startedDate (DB: STARTED_DATE)     | `java.sql.Date` (bug) | `java.sql.Timestamp` | `TIMESTAMP WITHOUT TIME ZONE` | Testing Started (segment 3)           |
| completedDate (DB: COMPLETED_DATE) | `java.sql.Date` (bug) | `java.sql.Timestamp` | `TIMESTAMP WITHOUT TIME ZONE` | Result Entered (segments 4, 6)        |
| releasedDate (DB: RELEASED_DATE)   | `java.sql.Date` (bug) | `java.sql.Timestamp` | `TIMESTAMP WITHOUT TIME ZONE` | Validated/Released (segments 5, 6, 7) |

**M0 fixes the Hibernate HBM mapping** from `type="java.sql.Date"` to
`type="java.sql.Timestamp"` in `Analysis.hbm.xml`. The DB columns are already
TIMESTAMP — no schema migration needed. After M0, all segments have hour-level
precision.

**IMPORTANT**: Use `Analysis.releasedDate` (not `Sample.releasedDate`) for the
validation timestamp — both entities have a `releasedDate` field but they
represent different things.

---

## Computed (Not Persisted)

### TATResult

Represents a single TAT calculation for one order/test. Computed on-demand by
the TAT calculation service.

| Field            | Type      | Description                                                 |
| ---------------- | --------- | ----------------------------------------------------------- |
| labNumber        | String    | Order accession number                                      |
| testName         | String    | Test name (for per-test segments)                           |
| labUnit          | String    | Laboratory section                                          |
| priority         | String    | Routine/STAT/ASAP                                           |
| sampleType       | String    | Specimen type                                               |
| orderingSite     | String    | Referring organization                                      |
| orderCreated     | Timestamp | Order entry timestamp                                       |
| collected        | Timestamp | Collection timestamp (nullable)                             |
| received         | Timestamp | Receipt timestamp (nullable)                                |
| testingStarted   | Timestamp | Testing started timestamp (nullable)                        |
| resultEntered    | Timestamp | Result entry timestamp (nullable)                           |
| validated        | Timestamp | Validation/release timestamp (nullable)                     |
| calendarTatHours | Decimal   | Calendar Time TAT in hours (nullable if timestamps missing) |
| workingTatHours  | Decimal   | Working Time TAT in hours (nullable if timestamps missing)  |

### TATSummary

Aggregate statistics for a set of TATResults.

| Field        | Type    | Description                           |
| ------------ | ------- | ------------------------------------- |
| totalCount   | Integer | Number of results with calculable TAT |
| mean         | Decimal | Arithmetic mean (hours)               |
| median       | Decimal | 50th percentile (hours)               |
| percentile90 | Decimal | 90th percentile (hours)               |
| min          | Decimal | Minimum TAT (hours)                   |
| max          | Decimal | Maximum TAT (hours)                   |
| stdDeviation | Decimal | Standard deviation (hours)            |
| histogram    | List    | Bin labels, min, max, count           |
| breakdown    | List    | Per-dimension aggregated stats        |

---

## Relationships

```
PublicHoliday --audit--> SystemUser
WeekendConfig --audit--> SystemUser
Sample --has-many--> Analysis
Analysis --has--> Test (test_id FK)
Analysis --has--> TestSection (test_sect_id FK, = Lab Unit)
Sample --has--> SampleHuman (patient link)
Sample --has--> SampleOrganization (ordering site link)
```
