# 008-patient-merge - Patient Merge Feature

**Complete full-stack patient merge functionality for OpenELIS Global**

---

## 📁 Documentation Structure

### Core Specification

- **[spec.md](./spec.md)** - Complete feature specification with functional
  requirements

### Implementation Plans

- **[plan.md](./plan.md)** - Backend implementation plan (M1-M3)
- **[tasks.md](./tasks.md)** - Task breakdown and progress tracking

### Developer Guides

#### Backend (✅ COMPLETE)

- **[BACKEND_COMPLETE.md](./BACKEND_COMPLETE.md)** - Full backend implementation
  summary

  - REST API documentation
  - Architecture details
  - Test coverage (44/44 tests passing)
  - Database schema
  - Git history

- **[quickstart.md](./quickstart.md)** - Step-by-step backend development guide

#### Frontend (🚧 PENDING)

- **[FRONTEND_QUICKSTART.md](./FRONTEND_QUICKSTART.md)** - Quick start guide for
  frontend devs
  - API integration examples
  - Component structure suggestions
  - Carbon Design System patterns
  - State management examples

---

## 🎯 Current Status

### Backend: ✅ COMPLETE

- **Branch:** `feat/008-m3-rest-controller`
- **Tests:** 44/44 passing (100%)
- **Commits:** 6 commits
- **Status:** Production ready

**Milestones Completed:**

- ✅ M1: Database Layer (audit table, migrations, DAO)
- ✅ M2: Service Layer (validation, consolidation, FHIR)
- ✅ M3: REST API (3 endpoints with security)
- ✅ Code Quality (all TODOs resolved, FIXME fixed)

### Frontend: 🚧 PENDING

- **Branch:** To be created (`feat/008-frontend`)
- **Components:** To be implemented
- **Status:** Ready to start

**Required Components:**

- Patient search/selection
- Side-by-side comparison view
- Validation dialog
- Confirmation modal
- Result display

---

## 🚀 Quick Start

### For Backend Developers

1. Read [BACKEND_COMPLETE.md](./BACKEND_COMPLETE.md) for full API documentation
2. Review [spec.md](./spec.md) for functional requirements
3. Check [quickstart.md](./quickstart.md) for implementation details
4. Run tests: `mvn test -Dtest="*PatientMerge*Test"`

### For Frontend Developers

1. **START HERE:** [FRONTEND_QUICKSTART.md](./FRONTEND_QUICKSTART.md)
2. Read API endpoints in
   [BACKEND_COMPLETE.md](./BACKEND_COMPLETE.md#rest-api-endpoints)
3. Review UI flow in [spec.md](./spec.md#user-scenarios--testing-mandatory)
4. Check Carbon Design System patterns

---

## 📊 Implementation Progress

### Backend Implementation

```
Database Layer    ████████████████████ 100% ✅
Service Layer     ████████████████████ 100% ✅
REST API Layer    ████████████████████ 100% ✅
Code Quality      ████████████████████ 100% ✅
Documentation     ████████████████████ 100% ✅
```

### Frontend Implementation

```
Component Design  ░░░░░░░░░░░░░░░░░░░░   0% 🚧
API Integration   ░░░░░░░░░░░░░░░░░░░░   0% 🚧
UI/UX Polish      ░░░░░░░░░░░░░░░░░░░░   0% 🚧
E2E Tests         ░░░░░░░░░░░░░░░░░░░░   0% 🚧
Documentation     ░░░░░░░░░░░░░░░░░░░░   0% 🚧
```

---

## 🏗️ Architecture Overview

### Backend Stack

- **Database:** PostgreSQL with Liquibase migrations
- **ORM:** Hibernate/JPA with native SQL for performance
- **Service Layer:** Spring @Transactional services
- **REST API:** Spring MVC with @RestController
- **Security:** Role-based (ROLE_GLOBAL_ADMIN)
- **FHIR:** R4 Patient.link compliance

### Frontend Stack (Planned)

- **Framework:** React 18+
- **UI Library:** Carbon Design System
- **i18n:** React Intl
- **State:** React Hooks + Context
- **Testing:** Cypress E2E tests

---

## 🔌 REST API Endpoints

All endpoints require `ROLE_GLOBAL_ADMIN` authentication.

**Base URL:** `/rest/patient/merge`

### 1. GET `/details/{patientId}`

Get patient merge preview details

**Response:** Patient demographics + data summary

### 2. POST `/validate`

Validate merge request without executing

**Request:** Patient IDs + primary selection + reason **Response:** Validation
result with warnings/errors

### 3. POST `/execute`

Execute the patient merge

**Request:** Same as validate with `confirmed: true` **Response:** Execution
result with audit ID

📖 **Full API docs:**
[BACKEND_COMPLETE.md](./BACKEND_COMPLETE.md#rest-api-endpoints)

---

## 🧪 Testing

### Backend Tests (All Passing ✅)

```bash
# Run all patient merge tests
mvn test -Dtest="*PatientMerge*Test"

# Run specific test class
mvn test -Dtest="PatientMergeRestControllerTest"

# Coverage breakdown:
# - PatientMergeAuditDAOTest: 3/3
# - PatientMergeRestControllerTest: 9/9
# - PatientMergeServiceIntegrationTest: 9/9
# - PatientMergeConsolidationServiceIntegrationTest: 8/8
# - PatientMergeServiceImplTest: 6/6
# - PatientMergeExecutionTest: 6/6
# - FhirPatientLinkServiceImplTest: 3/3
```

### Frontend Tests (Pending)

```bash
# E2E tests (to be implemented)
npm run cy:run -- --spec "cypress/e2e/patient-merge.cy.js"

# Component tests (to be implemented)
npm test -- PatientMerge
```

---

## 📝 Key Design Decisions

### 1. Patient ID Parameters

**Why patient1Id + patient2Id + primaryPatientId?**

- Frontend shows both patients side-by-side
- User selects which should be primary
- Backend validates primary matches one of the two
- Better UX than requiring frontend to determine roles

### 2. Confirmation Flag

**Why in API request?**

- Defense-in-depth: prevents accidental execution
- Provides audit trail of user confirmation
- Backend enforces `confirmed: true`

### 3. Names Not Merged

**Why keep primary patient's name?**

- Names are core identifiers
- User explicitly chose which patient to keep
- Avoids confusion post-merge
- Only non-identifying data merged (address, phone, email)

### 4. Native SQL for Bulk Updates

**Why not JPQL?**

- Hibernate issues with String ID → BIGINT conversion
- Performance: single UPDATE vs entity loading
- Still fully @Transactional (ACID maintained)

📖 **Full design rationale:**
[BACKEND_COMPLETE.md](./BACKEND_COMPLETE.md#key-design-decisions)

---

## 🎨 UI/UX Workflow (Planned)

```
┌─────────────────────────────────────────────────────────┐
│ 1. Patient Search & Selection                          │
│    • Search for patients (existing search component)   │
│    • Select two patients to compare                    │
│    • Load details for each patient                     │
└─────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────┐
│ 2. Side-by-Side Comparison                             │
│    • Display demographics and data summary             │
│    • Highlight conflicting fields                      │
│    • User selects primary patient (to keep)            │
└─────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────┐
│ 3. Validation                                           │
│    • Call POST /validate endpoint                      │
│    • Show warnings (e.g., "5 samples will be moved")   │
│    • Show errors if merge not allowed                  │
└─────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────┐
│ 4. Confirmation                                         │
│    • User enters merge reason                          │
│    • Review final summary                              │
│    • Confirm destructive operation                     │
└─────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────┐
│ 5. Execution & Results                                  │
│    • Call POST /execute endpoint                       │
│    • Show loading indicator (1-3 seconds)              │
│    • Display success with audit ID                     │
│    • Provide link to view primary patient              │
└─────────────────────────────────────────────────────────┘
```

---

## 📚 Additional Resources

### OpenELIS Documentation

- [AGENTS.md](../../AGENTS.md) - Comprehensive agent onboarding
- [CLAUDE.md](../../CLAUDE.md) - Claude Code specific instructions
- [Constitution](./../.specify/memory/constitution.md) - Project governance

### External Standards

- [FHIR R4 Patient.link](https://www.hl7.org/fhir/patient-definitions.html#Patient.link)
- [Carbon Design System](https://carbondesignsystem.com/)
- [React Intl](https://formatjs.io/docs/react-intl/)

### Example Code

- Patient Search: `frontend/src/components/patient/PatientSearch.js`
- Patient Display: `frontend/src/components/patient/PatientInfo.js`
- Base REST Controller: `src/main/java/.../common/rest/BaseRestController.java`

---

## 🤝 Contributing

### Backend Changes

- Branch: `feat/008-m3-rest-controller`
- Status: **Locked** (complete, do not modify without consultation)
- Tests: All 44 tests must pass
- Format: `mvn spotless:apply` before commit

### Frontend Changes

- Branch: Create new `feat/008-frontend`
- Base: Latest `develop`
- Tests: Add E2E tests for all workflows
- Format: `npm run format` before commit
- i18n: All strings must use React Intl

---

## 📞 Support

### Questions About Backend?

- Read [BACKEND_COMPLETE.md](./BACKEND_COMPLETE.md)
- Check test files for usage examples
- Review git commits on `feat/008-m3-rest-controller`

### Questions About Frontend?

- Start with [FRONTEND_QUICKSTART.md](./FRONTEND_QUICKSTART.md)
- Review Carbon Design System docs
- Check existing OpenELIS components for patterns

### Found a Bug?

- Backend: Run tests, check logs, review audit trail
- Frontend: Check browser console, network tab
- Create issue with reproduction steps

---

## ✅ Checklist for Frontend Development

- [ ] Read FRONTEND_QUICKSTART.md
- [ ] Review REST API endpoints
- [ ] Set up component structure
- [ ] Implement patient search/selection
- [ ] Build comparison view (Carbon DataTable)
- [ ] Add validation dialog (Carbon Modal)
- [ ] Create confirmation modal
- [ ] Implement error handling
- [ ] Add internationalization (React Intl)
- [ ] Write E2E tests (Cypress)
- [ ] Test all HTTP status codes (200, 400, 401, 403, 404)
- [ ] Accessibility audit (WCAG 2.1 AA)
- [ ] User documentation

---

**Status Summary:**

- ✅ Backend: Production ready (44/44 tests)
- 🚧 Frontend: Ready to start
- 📖 Documentation: Complete

**Last Updated:** 2025-12-11
