# Feature 011: Artifacts Index

**Feature:** Madagascar Analyzer Integration  
**Last Updated:** 2026-02-02  
**Purpose:** Comprehensive index of all feature artifacts and documentation

---

## Core Specification Documents

| Document                       | Purpose                                    | Status                  |
| ------------------------------ | ------------------------------------------ | ----------------------- |
| [spec.md](spec.md)             | Feature specification with user stories    | ✅ v1.2 (Generic-First) |
| [plan.md](plan.md)             | Implementation plan with M0-M21 milestones | ✅ Updated with M19-M21 |
| [tasks.md](tasks.md)           | Detailed task breakdown (T001-T314+)       | ✅ T200-T239 added      |
| [data-model.md](data-model.md) | Entity relationship documentation          | ✅ Current              |

---

## Plans & Status Reports

### plans/ Directory

| Document                                                                                         | Date       | Status         |
| ------------------------------------------------------------------------------------------------ | ---------- | -------------- |
| [architecture-remediation-2026-02-02.plan.md](plans/architecture-remediation-2026-02-02.plan.md) | 2026-02-02 | ✅ Implemented |
| [IMPLEMENTATION-STATUS.md](plans/IMPLEMENTATION-STATUS.md)                                       | 2026-02-02 | ✅ Complete    |
| [m9-m10-dashboard-plan.md](plans/m9-m10-dashboard-plan.md)                                       | 2026-01-27 | ✅ Complete    |

**Latest Plan:** Architecture Remediation (2026-02-02)

- Contract corrections via internet research
- Generic-First architecture with default configs
- BC2000 promotion, global inventory (36 plugins)
- Phases 0-3, 5, 6 complete | Phase 4 architecture documented

---

## Contracts & Agreements

### contracts/ Directory

| Document                                                             | Version | Purpose                                                       |
| -------------------------------------------------------------------- | ------- | ------------------------------------------------------------- |
| [supported-analyzers.md](contracts/supported-analyzers.md)           | v1.1.0  | Authoritative analyzer inventory (13 required + 23 supported) |
| [order-export-api.yaml](contracts/order-export-api.yaml)             | v1.0    | Order export REST API specification                           |
| [template-fixture-mapping.md](contracts/template-fixture-mapping.md) | v1.0    | Mock template to fixture mapping                              |

---

## Verification & Testing

| Document                                               | Purpose                        | For             |
| ------------------------------------------------------ | ------------------------------ | --------------- |
| [VERIFICATION-CHECKLIST.md](VERIFICATION-CHECKLIST.md) | Field verification procedures  | Deployment team |
| [VERIFICATION-GUIDE.md](VERIFICATION-GUIDE.md)         | General verification guide     | All             |
| [GENERIC-TEST-RECIPE.md](GENERIC-TEST-RECIPE.md)       | Generic analyzer testing steps | QA/Testing      |
| [testing-matrix.md](testing-matrix.md)                 | Test coverage matrix           | All             |

---

## Changelogs

| Document                                           | Date       | Changes                                                       |
| -------------------------------------------------- | ---------- | ------------------------------------------------------------- |
| [CHANGELOG-2026-02-02.md](CHANGELOG-2026-02-02.md) | 2026-02-02 | Architecture remediation, Generic-First, contract corrections |

---

## Research & Analysis

### research/ Directory

| Document                                                                | Purpose                           |
| ----------------------------------------------------------------------- | --------------------------------- |
| [hibernate-mapping-analysis.md](research/hibernate-mapping-analysis.md) | ORM mapping analysis              |
| [xml-migration-scope.md](research/xml-migration-scope.md)               | XML to annotation migration scope |
| [xml-to-annotations-guide.md](research/xml-to-annotations-guide.md)     | Migration guide                   |

### Root Analysis Documents

| Document                                                         | Purpose                   |
| ---------------------------------------------------------------- | ------------------------- |
| [research.md](research.md)                                       | General research findings |
| [pre-implementation-analysis.md](pre-implementation-analysis.md) | Initial analysis          |
| [REMEDIATION-REPORT.md](REMEDIATION-REPORT.md)                   | Remediation report        |

---

## Milestone Documentation

### milestones/ Directory

| Document                                                                                | Milestone | Purpose                     |
| --------------------------------------------------------------------------------------- | --------- | --------------------------- |
| [current-state-analysis-2026-02-02.md](milestones/current-state-analysis-2026-02-02.md) | General   | Current state analysis      |
| [IMPLEMENTATION-SUMMARY.md](milestones/IMPLEMENTATION-SUMMARY.md)                       | General   | Implementation summary      |
| [m9-m10-manual-testing-guide.md](milestones/m9-m10-manual-testing-guide.md)             | M9-M10    | Horiba testing guide        |
| [m9-m10-testing-infrastructure.md](milestones/m9-m10-testing-infrastructure.md)         | M9-M10    | Testing infrastructure      |
| [m11-simulator-dashboard-analysis.md](milestones/m11-simulator-dashboard-analysis.md)   | M11       | Stago simulator analysis    |
| [m11-stago-implementation-analysis.md](milestones/m11-stago-implementation-analysis.md) | M11       | Stago implementation        |
| [m11-stago-testing/](milestones/m11-stago-testing/)                                     | M11       | Stago testing documentation |

---

## Checklists

### checklists/ Directory

| Document                                      | Purpose                |
| --------------------------------------------- | ---------------------- |
| [requirements.md](checklists/requirements.md) | Requirements checklist |

---

## External Artifacts

### analyzer-defaults/ (Root Directory)

**Location:** `/analyzer-defaults/`

**Contents:** 11 default configuration templates for GenericASTM and GenericHL7
plugins

| Protocol | Templates                                                                                |
| -------- | ---------------------------------------------------------------------------------------- |
| ASTM (6) | mindray-ba88a, horiba-pentra60, horiba-micros60, stago-start4, sysmex-xn, genexpert-astm |
| HL7 (5)  | mindray-bc2000, mindray-bc5380, mindray-bs360e, abbott-architect, genexpert-hl7          |

**Index:** [analyzer-defaults/README.md](../../../analyzer-defaults/README.md)

---

### plugins/analyzers/GenericHL7/ (Plugins Directory)

**Location:** `/plugins/analyzers/GenericHL7/`

**Document:**
[ARCHITECTURE.md](../../../plugins/analyzers/GenericHL7/ARCHITECTURE.md)

**Status:** 🚧 Architecture documented, Java implementation pending (M19 - 3
days)

**Purpose:** Database-driven HL7 analyzer plugin specification

---

### Test Fixtures (Test Resources)

**Location:** `/src/test/resources/testdata/`

| Fixture                             | IDs       | Analyzers | Purpose                           |
| ----------------------------------- | --------- | --------- | --------------------------------- |
| `madagascar-analyzer-test-data.xml` | 2000-2012 | 13        | Madagascar contract (Feature 011) |
| `global-analyzer-inventory.xml`     | 3000-3035 | 36        | All supported plugins             |
| `analyzer-mapping-test-data.xml`    | 1000-1004 | 5         | Feature 004 (ASTM mapping)        |

**Loader Script:** `/src/test/resources/load-analyzer-test-data.sh`

**Usage:**

```bash
# Madagascar fixtures (13 analyzers)
./load-analyzer-test-data.sh --dataset-011

# Global inventory (36 plugins)
./load-analyzer-test-data.sh --all-plugins

# Both Feature 004 and 011
./load-analyzer-test-data.sh --all
```

---

## Document Relationships

```
specs/011-madagascar-analyzer-integration/
│
├── spec.md ────────────────► Core specification
│   └── References contracts/supported-analyzers.md
│
├── plan.md ────────────────► Implementation plan (M0-M21)
│   └── Referenced by tasks.md
│
├── tasks.md ───────────────► Task breakdown (T001-T314+)
│   └── Implements plan.md milestones
│
├── CHANGELOG-2026-02-02.md ► Recent changes summary
│   └── Links to plans/architecture-remediation-2026-02-02.plan.md
│
├── contracts/
│   └── supported-analyzers.md ► Authoritative inventory (v1.1.0)
│       └── References ../../analyzer-defaults/
│
├── plans/
│   ├── architecture-remediation-2026-02-02.plan.md ► Remediation plan
│   ├── IMPLEMENTATION-STATUS.md ► Status tracking
│   └── m9-m10-dashboard-plan.md ► M9-M10 plan
│
└── VERIFICATION-CHECKLIST.md ► Deployment verification
    └── Used with supported-analyzers.md
```

---

## Key Decision Records

### ADR-001: Generic-First Architecture (2026-02-02)

**Decision:** Use GenericASTM/GenericHL7 with loadable default configs as
PRIMARY integration method.

**Files:**

- `analyzer-defaults/` (templates)
- `plugins/analyzers/GenericHL7/ARCHITECTURE.md` (spec)
- `contracts/supported-analyzers.md` (updated)

**Impact:** New analyzers require zero Java code changes

---

### ADR-002: GenericFile Deferral (2026-02-02)

**Decision:** Defer GenericFile plugin pending file format abstraction research.

**Rationale:** Complex abstraction problem, existing FILE plugins work well

**Timeline:** Future milestone (post-M21)

---

### ADR-003: BC2000 Promotion (2026-02-02)

**Decision:** Promote BC2000 from P2 to P1 priority.

**Rationale:** Plugin exists (Mindray), minimal effort, demonstrates GenericHL7

**Impact:** 12→13 required analyzers

---

## Navigation Guide

### For Developers

1. Start with [spec.md](spec.md) for feature overview
2. Read [plan.md](plan.md) for milestone structure
3. Check [tasks.md](tasks.md) for specific tasks
4. Review [contracts/supported-analyzers.md](contracts/supported-analyzers.md)
   for analyzer details

### For Deployment Teams

1. Read [VERIFICATION-CHECKLIST.md](VERIFICATION-CHECKLIST.md) for field
   procedures
2. Use [contracts/supported-analyzers.md](contracts/supported-analyzers.md) for
   analyzer specs
3. Follow [GENERIC-TEST-RECIPE.md](GENERIC-TEST-RECIPE.md) for testing

### For QA/Testing

1. Use [testing-matrix.md](testing-matrix.md) for coverage tracking
2. Follow [GENERIC-TEST-RECIPE.md](GENERIC-TEST-RECIPE.md) for test procedures
3. Check milestone docs in [milestones/](milestones/) for specific milestone
   testing

### For Architecture Review

1. Read
   [plans/architecture-remediation-2026-02-02.plan.md](plans/architecture-remediation-2026-02-02.plan.md)
2. Review
   [plugins/analyzers/GenericHL7/ARCHITECTURE.md](../../../plugins/analyzers/GenericHL7/ARCHITECTURE.md)
3. Check [analyzer-defaults/README.md](../../../analyzer-defaults/README.md)

---

**Maintained By:** OpenELIS Global Feature 011 Team  
**Repository:** `DIGI-UW/OpenELIS-Global-2`
