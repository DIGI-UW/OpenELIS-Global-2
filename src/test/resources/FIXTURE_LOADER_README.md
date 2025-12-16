# Test Fixture Loader - Unified Script

## Overview

This directory contains a **single unified script** for loading all test
fixtures:

- Storage hierarchy (rooms, devices, shelves, racks, positions)
- E2E test data (patients, samples, sample items, storage assignments)

## Usage

### Method 1: Direct Script Execution (Recommended)

```bash
# From project root
./src/test/resources/load-test-fixtures.sh
```

### Method 2: Via Cypress Convenience Wrapper

```bash
# From project root
./frontend/cypress/support/load-storage-fixtures.sh
```

### Method 3: Via Cypress Tests (Automatic)

Cypress tests automatically use the unified loader via
`cy.loadStorageFixtures()`.

## Features

✅ **Unified Script**: Single script replaces 3 duplicate scripts  
✅ **Smart Detection**: Automatically detects Docker vs direct psql  
✅ **Verification**: Verifies data was loaded correctly  
✅ **Docker Support**: Works with `openelisglobal-database` container  
✅ **Direct psql Support**: Works without Docker (configurable via env vars)  
✅ **Clear Output**: Shows what was loaded and verification results

## Environment Variables (for direct psql)

When not using Docker, configure connection via:

```bash
export DB_USER=clinlims
export DB_NAME=clinlims
export DB_HOST=localhost
export DB_PORT=5432

./src/test/resources/load-test-fixtures.sh
```

## Cypress Integration

Cypress tests are **smart** about fixture management:

### Automatic Behavior

1. **Checks if fixtures exist** before loading
2. **Only loads if missing** (unless overridden)
3. **Preserves fixtures between runs** by default

### Environment Variables

Control Cypress fixture behavior via `CYPRESS_*` env vars:

- `CYPRESS_SKIP_FIXTURES=true` - Skip loading entirely (assumes fixtures exist)
- `CYPRESS_FORCE_FIXTURES=true` - Force reload even if fixtures exist
- `CYPRESS_CLEANUP_FIXTURES=true` - Clean up after tests (default: false)

### Examples

```bash
# Fast iteration (skip if fixtures exist)
npm run cy:run -- --spec "cypress/e2e/storage*.cy.js"

# Force reload fixtures
CYPRESS_FORCE_FIXTURES=true npm run cy:run -- --spec "cypress/e2e/storage*.cy.js"

# Skip loading entirely (fastest)
CYPRESS_SKIP_FIXTURES=true npm run cy:run -- --spec "cypress/e2e/storage*.cy.js"
```

## What Gets Loaded

### Storage Hierarchy

- 3 Rooms (Main Laboratory, Secondary Laboratory, Inactive Room)
- 5 Devices (Freezers, Refrigerators, Cabinets)
- 6 Shelves
- 6 Racks
- 100+ Positions (mix of occupied/unoccupied)

### E2E Test Data

- 3 Test Patients (John E2E-Smith, Jane E2E-Jones, Bob E2E-Williams)
- 10 Test Samples (E2E-001 through E2E-010)
- 20+ Test Sample Items
- 15+ Storage Assignments
- 5 Test Analyses (orders for E2E sample items)
- 2 Test Results (for finalized analyses)

## Verification

The script automatically verifies data was loaded:

```
Storage Hierarchy | Rooms     |     3
                  | Devices   |     5
                  | Shelves   |     6
                  | Racks     |     6
                  | Positions |   106

E2E Test Data     | Patients            |     3
                  | Samples             |    10
                  | Sample Items        |    20+
                  | Storage Assignments |    15+
                  | Analyses            |     5
                  | Results             |     2
```

## Troubleshooting

### Script Not Found

Ensure you're running from project root:

```bash
cd /path/to/OpenELIS-Global-2
./src/test/resources/load-test-fixtures.sh
```

### Docker Container Not Found

Ensure the database container is running:

```bash
docker compose -f dev.docker-compose.yml up -d database
```

### Direct psql Connection Issues

Check PostgreSQL is running and credentials are correct:

```bash
psql -U clinlims -d clinlims -h localhost -p 5432 -c "SELECT 1;"
```

## Migration from Old Scripts

The following scripts have been **consolidated** into `load-test-fixtures.sh`:

- ❌ `load-e2e-fixtures.sh` (removed)
- ❌ `load-storage-test-data.sh` (removed)
- ✅ `load-test-fixtures.sh` (unified replacement)

The Cypress convenience wrapper
(`frontend/cypress/support/load-storage-fixtures.sh`) still exists but now calls
the unified script.
