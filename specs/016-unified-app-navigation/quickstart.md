# Quickstart: Unified Application Navigation

**Feature**: 016-unified-app-navigation **Date**: 2026-04-06

## Prerequisites

- Java 21 (use `sdk env` for SDKMAN auto-switch)
- Node.js 20+
- Docker + Docker Compose
- PostgreSQL 14+ (runs in Docker)
- Git with submodules: `git submodule update --init --recursive`

## Development Setup

### 1. Start from the spec branch

```bash
git checkout spec/016-unified-app-navigation
```

### 2. Backend

```bash
# Build (skip tests for speed)
mvn clean install -DskipTests -Dmaven.test.skip=true

# Start Docker environment
cp .env.example .env  # if not already done
docker compose up -d

# Run backend tests
mvn test -Dtest=MenuServiceTest,MenuConfigurationHandlerTest
```

### 3. Frontend

```bash
cd frontend

# Install dependencies
npm install

# Start dev server
npm start

# Run unit tests
npm test

# Run E2E tests (Playwright)
npx playwright test
```

## Key Files to Know

### Backend

| File                                                                                             | Purpose                              |
| ------------------------------------------------------------------------------------------------ | ------------------------------------ |
| `src/main/java/org/openelisglobal/menu/valueholder/Menu.java`                                    | Menu entity (extend with new fields) |
| `src/main/java/org/openelisglobal/menu/controller/MenuController.java`                           | REST endpoints (add role filtering)  |
| `src/main/java/org/openelisglobal/menu/service/MenuServiceImpl.java`                             | Business logic (add provenance)      |
| `src/main/java/org/openelisglobal/menu/util/MenuUtil.java`                                       | Menu tree builder + filtering        |
| `src/main/java/org/openelisglobal/configuration/service/ConfigurationInitializationService.java` | Config loading pattern to follow     |
| `src/main/java/org/openelisglobal/role/service/RolesConfigurationHandler.java`                   | Example DomainConfigurationHandler   |

### Frontend

| File                                               | Purpose                                  |
| -------------------------------------------------- | ---------------------------------------- |
| `frontend/src/components/admin/Admin.js`           | Hardcoded SideNav (~45 items) to replace |
| `frontend/src/components/validation/Validation.js` | Hardcoded SideNav to replace             |
| `frontend/src/components/coldStorage/Reports.js`   | Hardcoded SideNav to replace             |
| `frontend/package.json`                            | Dependencies to upgrade (React, Carbon)  |

### Configuration

| File                                                    | Purpose                        |
| ------------------------------------------------------- | ------------------------------ |
| `src/main/resources/configuration/menus/menus.json`     | Base menu config (NEW)         |
| `/var/lib/openelis-global/configuration/backend/menus/` | Distribution override location |

## Milestone Branches

```bash
# M1: Frontend modernization
git checkout -b feat/016-unified-app-navigation-m1-frontend-modern

# M2: Backend config
git checkout -b feat/016-unified-app-navigation-m2-backend-config

# M3: GlobalSidebar component
git checkout -b feat/016-unified-app-navigation-m3-global-sidebar

# M4: Migration & cleanup
git checkout -b feat/016-unified-app-navigation-m4-migration
```

## Verification Commands

### M1 Checkpoint

```bash
cd frontend
node -e "console.log('React:', require('./package.json').dependencies.react)"
npm list @carbon/react @carbon/icons-react
npm list carbon-components  # Should show empty/not found
npm start  # Verify no console errors
npm test
```

### M2 Checkpoint

```bash
mvn test -Dtest=MenuConfigurationHandlerTest,MenuServiceTest
curl -s http://localhost:8080/openelis-global/rest/menu | jq '.[0].menu.configSource'
```

### M3 Checkpoint

```bash
cd frontend
npm test -- --testPathPattern=GlobalSidebar
npm run build  # Check bundle size
```

### M4 Checkpoint

```bash
cd frontend
npx playwright test tests/navigation.spec.ts
npm run build  # Final bundle size check
```
