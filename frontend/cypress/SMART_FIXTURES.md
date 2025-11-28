# Smart Fixture Management for Cypress Tests

The storage E2E tests use intelligent fixture management to optimize test execution speed while maintaining flexibility.

## How It Works

The fixture management system automatically:

1. **Checks if fixtures exist** before loading
2. **Only loads fixtures if missing** (unless overridden)
3. **Preserves fixtures between runs** by default (for fast iteration)
4. **Allows manual control** via environment variables

## Environment Variables

All environment variables use the `CYPRESS_` prefix (Cypress automatically strips this prefix):

### `CYPRESS_SKIP_FIXTURES`

- **Default**: `false`
- **Purpose**: Skip fixture loading entirely (assumes fixtures already exist)
- **Use case**: Fastest iteration when fixtures are already loaded
- **Example**: `CYPRESS_SKIP_FIXTURES=true npm run cy:run -- --spec "cypress/e2e/storage*.cy.js"`

### `CYPRESS_FORCE_FIXTURES`

- **Default**: `false`
- **Purpose**: Force reload fixtures even if they already exist
- **Use case**: When you need fresh test data or suspect fixture corruption
- **Example**: `CYPRESS_FORCE_FIXTURES=true npm run cy:run -- --spec "cypress/e2e/storage*.cy.js"`

### `CYPRESS_CLEANUP_FIXTURES`

- **Default**: `false` (changed from `true` for faster iteration)
- **Purpose**: Clean up fixtures after tests complete
- **Use case**: Clean database state between test runs or CI/CD pipelines
- **Example**: `CYPRESS_CLEANUP_FIXTURES=true npm run cy:run -- --spec "cypress/e2e/storage*.cy.js"`

## Behavior Matrix

| SKIP_FIXTURES | FORCE_FIXTURES | Fixtures Exist | Action        |
| ------------- | -------------- | -------------- | ------------- |
| `true`        | any            | any            | Skip loading  |
| `false`       | `true`         | any            | Always load   |
| `false`       | `false`        | `true`         | Skip loading  |
| `false`       | `false`        | `false`        | Load fixtures |

## Common Workflows

### Fast Iteration (Default)

```bash
# Run tests - automatically checks and skips loading if fixtures exist
npm run cy:run -- --spec "cypress/e2e/storage*.cy.js"
```

- Checks if fixtures exist
- Skips loading if they exist
- Skips cleanup (preserves fixtures for next run)
- **Fastest option for development**

### First Time Setup

```bash
# First run - fixtures don't exist, so they'll be loaded automatically
npm run cy:run -- --spec "cypress/e2e/storage*.cy.js"
```

- Fixtures don't exist
- Automatically loads fixtures
- Preserves fixtures for next run

### Force Fresh Data

```bash
# Force reload fixtures (useful after database changes or fixture updates)
CYPRESS_FORCE_FIXTURES=true npm run cy:run -- --spec "cypress/e2e/storage*.cy.js"
```

- Always loads fixtures (reloads even if exist)
- Preserves fixtures for next run (unless CLEANUP_FIXTURES=true)

### CI/CD Pipeline

```bash
# Clean state for CI/CD
CYPRESS_CLEANUP_FIXTURES=true npm run cy:run -- --spec "cypress/e2e/storage*.cy.js"
```

- Loads fixtures if missing
- Cleans up after tests complete
- Ensures clean database state

### Maximum Speed (Skip Everything)

```bash
# Skip loading and cleanup (assumes fixtures already exist)
CYPRESS_SKIP_FIXTURES=true npm run cy:run -- --spec "cypress/e2e/storage*.cy.js"
```

- Skips fixture loading entirely
- Skips cleanup
- **Fastest possible execution** (assumes fixtures exist)

## Implementation Details

### Fixture Existence Check

The system checks for fixture existence by querying for known test rooms:

```sql
SELECT COUNT(*) FROM storage_room WHERE code IN ('MAIN', 'SEC', 'INACTIVE');
```

If at least 2 test rooms exist, fixtures are considered present.

### Commands

- `cy.setupStorageTests()` - Smart setup with fixture management
- `cy.cleanupStorageTests()` - Conditional cleanup based on env var
- `cy.checkStorageFixturesExist()` - Check if fixtures exist
- `cy.loadStorageFixtures()` - Load fixtures
- `cy.cleanStorageFixtures()` - Clean fixtures

## Benefits

1. **Faster Development**: Skip loading/cleanup during iteration
2. **Flexibility**: Control behavior via environment variables
3. **Reliability**: Automatic existence checks prevent unnecessary operations
4. **CI/CD Ready**: Enable cleanup for clean test runs
5. **Developer Friendly**: Sensible defaults for common workflows

## Migration Notes

**Breaking Change**: `CLEANUP_FIXTURES` default changed from `true` to `false`

- **Old behavior**: Fixtures cleaned up by default
- **New behavior**: Fixtures preserved by default (faster iteration)
- **Migration**: Set `CYPRESS_CLEANUP_FIXTURES=true` if you need cleanup
