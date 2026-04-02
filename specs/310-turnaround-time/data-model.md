# Data Model: 310 Turn Around Time

**Date**: 2026-04-02

---

## New Entities

### PublicHoliday

| Field        | Type                     | Required | Description                                                 |
| ------------ | ------------------------ | -------- | ----------------------------------------------------------- |
| id           | Integer (PK, sequence)   | Yes      | Auto-generated primary key                                  |
| holiday_date | DATE                     | Yes      | The holiday date                                            |
| holiday_name | VARCHAR(100)             | Yes      | Descriptive name                                            |
| is_recurring | BOOLEAN                  | No       | If true, repeats annually on same month/day. Default: false |
| is_active    | BOOLEAN                  | No       | If false, excluded from TAT calculations. Default: true     |
| lastupdated  | TIMESTAMP                | Yes      | Audit: last modification timestamp                          |
| sys_user_id  | INTEGER (FK system_user) | Yes      | Audit: user who last modified                               |

**Constraints**:

- UNIQUE(holiday_date) per non-recurring entries within same year
- holiday_name max 100 characters
- FK sys_user_id references system_user(id)

**Lifecycle**: Created > Active > Inactive (soft) > Deleted (hard)

---

### WeekendConfig

| Field       | Type                     | Required | Description                                       |
| ----------- | ------------------------ | -------- | ------------------------------------------------- |
| id          | Integer (PK, sequence)   | Yes      | Auto-generated primary key                        |
| day_of_week | INTEGER                  | Yes      | 0=Sunday, 1=Monday, ..., 6=Saturday               |
| is_weekend  | BOOLEAN                  | No       | Whether this day is a weekend day. Default: false |
| lastupdated | TIMESTAMP                | Yes      | Audit: last modification timestamp                |
| sys_user_id | INTEGER (FK system_user) | Yes      | Audit: user who last modified                     |

**Constraints**:

- UNIQUE(day_of_week) — exactly 7 rows, one per day
- day_of_week range: 0-6
- Seeded with Saturday(6)=true, Sunday(0)=true; all others false

**Lifecycle**: Rows are seeded on creation. Only `is_weekend` field is updated,
never inserted/deleted.

---

## Existing Entities (Read-Only for TAT)

### Sample (timestamps used)

| Field              | Used For Segment                        |
| ------------------ | --------------------------------------- |
| entered_date       | Order Created (segments 1, 7)           |
| collection_date    | Specimen Collected (segments 1, 2)      |
| received_timestamp | Specimen Received (segments 2, 3, 4, 5) |

### Analysis (timestamps used)

| Field          | Used For Segment                      |
| -------------- | ------------------------------------- |
| started_date   | Testing Started (segment 3)           |
| completed_date | Result Entered (segments 4, 6)        |
| released_date  | Validated/Released (segments 5, 6, 7) |

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
