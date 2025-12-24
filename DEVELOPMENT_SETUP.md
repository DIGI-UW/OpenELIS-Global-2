# OpenELIS Global 2 - Daily Development Setup

> **Quick Reference Guide for Daily Development Workflow**
>
> This guide provides essential commands and workflows for efficient daily development on OpenELIS Global 2.

---

## 🚀 **Quick Start Commands**

### **Start Development Environment**

```bash
# Start all containers
docker compose -f dev.docker-compose.yml up -d

# Verify all containers are running
docker compose -f dev.docker-compose.yml ps
```

### **Access Application**

| **Interface**          | **URL**                                | **Credentials**     |
| ---------------------- | -------------------------------------- | ------------------- |
| **React UI** (Primary) | https://localhost/                     | admin : adminADMIN! |
| **Legacy UI**          | https://localhost/api/OpenELIS-Global/ | admin : adminADMIN! |
| **Backend API**        | https://localhost:8443/                | Direct access       |
| **FHIR Server**        | https://localhost:8444/fhir/           | HAPI FHIR R4        |

---

## 💻 **Daily Development Workflow**

### **Frontend Development (React + Carbon Design System)**

#### Hot Reload Development

```bash
# Navigate to frontend
cd frontend

# Frontend hot-reload is automatic via Docker container
# Edit files in frontend/src/** and see changes instantly at https://localhost/
```

#### Code Quality & Testing

```bash
# Format code (MANDATORY before commit)
npm run format

# Check formatting
npm run check-format

# Run unit tests
npm test

# Run individual E2E test (recommended during development)
npm run cy:single cypress/e2e/login.cy.js

# Run storage-related E2E tests
npm run cy:single cypress/e2e/storage*.cy.js
```

### **Backend Development (Java + Spring Framework)**

#### Code Changes & Hot Reload

```bash
# After making Java code changes:
mvn clean install -DskipTests -Dmaven.test.skip=true

# Hot reload backend (recreate webapp container)
docker compose -f dev.docker-compose.yml up -d --no-deps --force-recreate oe.openelis.org
```

#### Code Quality

```bash
# Format Java code (MANDATORY before commit)
mvn spotless:apply

# Check formatting
mvn spotless:check

# Build verification
mvn clean install -DskipTests -Dmaven.test.skip=true
```

### **Combined Frontend + Backend Changes**

```bash
# Format all code before commit
mvn spotless:apply && cd frontend && npm run format && cd ..

# Full rebuild and restart
mvn clean install -DskipTests -Dmaven.test.skip=true
docker compose -f dev.docker-compose.yml up -d --no-deps --force-recreate oe.openelis.org
```

---

## 🧪 **Testing Environment**

### **Test Data Management**

```bash
# Load test fixtures (run from project root)
./src/test/resources/load-test-fixtures.sh

# Reset database and reload fixtures
./src/test/resources/load-test-fixtures.sh --reset

# Load without verification (faster)
./src/test/resources/load-test-fixtures.sh --no-verify
```

### **Available Test Data**

- **3 Test Patients**: John E2E-Smith, Jane E2E-Jones, Bob E2E-Williams
- **10 Test Samples**: DEV01000000000000001-010 (year 2000 format)
- **20+ Sample Items**: Various types (blood, serum, urine) with storage assignments
- **Storage Hierarchy**: 3 rooms, 5 devices, 100+ positions

### **E2E Testing (Cypress)**

```bash
cd frontend

# Run individual test (fast feedback - recommended during development)
npm run cy:single cypress/e2e/login.cy.js
npm run cy:single cypress/e2e/storageAssignment.cy.js

# Debug mode with detailed logging
npm run cy:debug cypress/e2e/login.cy.js

# Quick run (optimized for speed)
npm run cy:quick

# Full E2E suite (CI/CD only - NOT for development)
npm run cy:run
```

---

## 🔧 **Container Management**

### **Basic Commands**

```bash
# Start development environment
docker compose -f dev.docker-compose.yml up -d

# Stop all containers
docker compose -f dev.docker-compose.yml down

# View container status
docker compose -f dev.docker-compose.yml ps

# View logs (specific container)
docker compose -f dev.docker-compose.yml logs -f oe.openelis.org
docker compose -f dev.docker-compose.yml logs -f frontend.openelis.org

# Restart specific container (after backend changes)
docker compose -f dev.docker-compose.yml up -d --no-deps --force-recreate oe.openelis.org

# Restart frontend container (if needed)
docker compose -f dev.docker-compose.yml up -d --no-deps --force-recreate frontend.openelis.org
```

### **Database Management**

```bash
# Reset database volume (clean slate)
docker compose -f dev.docker-compose.yml down
docker volume rm openelis-global-2_db-data
docker compose -f dev.docker-compose.yml up -d

# Access PostgreSQL directly (if needed)
docker exec -it openelisglobal-database psql -U clinlims -d clinlims
```

---

## 📋 **Pre-Commit Checklist**

### **MANDATORY Steps (Every Commit)**

```bash
# 1. Format all code
mvn spotless:apply
cd frontend && npm run format && cd ..

# 2. Verify build passes
mvn clean install -DskipTests -Dmaven.test.skip=true

# 3. Verify formatting is correct
mvn spotless:check
cd frontend && npm run check-format && cd ..

# 4. Test relevant functionality
npm run cy:single cypress/e2e/{relevant-test}.cy.js
```

### **Constitution Compliance Check**

- ✅ **Carbon Design System** used exclusively (NO Bootstrap/Tailwind)
- ✅ **React Intl** for all user-facing strings (NO hardcoded text)
- ✅ **5-Layer Architecture** respected (Controller → Service → DAO → Entity → DB)
- ✅ **FHIR compliance** for external data (if applicable)
- ✅ **Java 21** + **Spring Framework 6.2.2** (NOT Spring Boot)

---

## 🏗️ **Architecture Quick Reference**

### **5-Layer Pattern (MANDATORY)**

```
Controller (REST endpoints)
    ↓ delegates to
Service (Business logic + @Transactional)
    ↓ calls
DAO (Data access + HQL queries)
    ↓ uses
Valueholder (JPA Entity)
    ↓ maps to
Database (PostgreSQL)
```

### **Frontend Stack**

- **React 17** + **Carbon Design System v1.15**
- **React Intl** for i18n (MANDATORY for all text)
- **SWR 2.0.3** for data fetching
- **Formik 2.2.9** + **Yup 0.29.2** for forms

### **Backend Stack**

- **Java 21** + **Spring Framework 6.2.2** (Traditional MVC, NOT Spring Boot)
- **Hibernate 6.x** + **PostgreSQL 14+**
- **HAPI FHIR R4** for interoperability
- **Liquibase 4.8.0** for schema migrations

---

## 🎯 **Key Features to Develop/Test**

### **Sample Storage Management** (Reference Implementation)

- **Location**: Storage → Storage Management
- **Features**: Sample assignment, movement, search, hierarchy management
- **Test Data**: Ready-to-use storage hierarchy with test samples
- **E2E Tests**: `cypress/e2e/storage*.cy.js`

### **Patient Management**

- **Location**: Patient → Patient Management
- **Test Patients**: Search "Smith", "Jones", or "Williams"
- **Features**: Patient registration, search, demographics

### **Laboratory Workflow**

- **Order Entry**: Create laboratory orders
- **Result Entry**: Enter and validate test results
- **Report Generation**: Laboratory reports and analytics

---

## 📚 **Essential Resources**

### **Key Documentation**

- **`AGENTS.md`** - Comprehensive development guide (1147 lines)
- **Constitution**: `.specify/memory/constitution.md` - Governance principles
- **Testing Guide**: `.specify/guides/testing-roadmap.md` - Testing strategy
- **Sample Feature**: `specs/001-sample-storage/quickstart.md` - Complete feature example

### **External References**

- **Carbon Design System**: https://carbondesignsystem.com/
- **Spring Framework**: https://docs.spring.io/spring-framework/docs/6.2.2/reference/html/
- **HAPI FHIR**: https://hapifhir.io/hapi-fhir/docs/
- **OpenELIS Docs**: https://docs.openelis-global.org/

---

## ⚠️ **Common Issues & Solutions**

### **Backend Hot Reload Not Working**

```bash
# Rebuild and restart webapp container
mvn clean install -DskipTests -Dmaven.test.skip=true
docker compose -f dev.docker-compose.yml up -d --no-deps --force-recreate oe.openelis.org
```

### **Frontend Changes Not Reflecting**

```bash
# Check frontend container logs
docker compose -f dev.docker-compose.yml logs -f frontend.openelis.org

# Restart if needed
docker compose -f dev.docker-compose.yml up -d --no-deps --force-recreate frontend.openelis.org
```

### **Database Issues**

```bash
# Reset database completely
docker compose -f dev.docker-compose.yml down
docker volume rm openelis-global-2_db-data
docker compose -f dev.docker-compose.yml up -d
./src/test/resources/load-test-fixtures.sh
```

### **SSL Certificate Warnings**

- **Expected in development** - Use self-signed certificates
- **Browser Solution**: Click "Advanced" → "Proceed to localhost"
- **curl Solution**: Use `-k` flag: `curl -k https://localhost/`

---

## 🏃‍♂️ **Quick Development Iterations**

### **Frontend-Only Changes**

```bash
# Edit files in frontend/src/**
# Changes auto-reload via Docker container hot-reload
# No rebuild needed!
```

### **Backend-Only Changes**

```bash
# Edit Java files
mvn clean install -DskipTests -Dmaven.test.skip=true
docker compose -f dev.docker-compose.yml up -d --no-deps --force-recreate oe.openelis.org
```

### **Full Stack Changes**

```bash
# Edit both frontend and backend
mvn clean install -DskipTests -Dmaven.test.skip=true
docker compose -f dev.docker-compose.yml up -d --no-deps --force-recreate oe.openelis.org
# Frontend changes are automatic
```

---

**💡 Pro Tips:**

- Keep Docker containers running during development for fastest iteration
- Use individual E2E tests during development (NOT full suite)
- Format code frequently to avoid large formatting commits
- Check constitution compliance early and often
- Use test data extensively - it's designed for comprehensive testing

**🎯 Happy developing on OpenELIS Global 2!** 🧪💻✨
