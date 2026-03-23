# Feature 011: Artifacts Index

**Feature:** Madagascar Analyzer Integration **Last Updated:** 2026-02-08
**Purpose:** Comprehensive index of all feature artifacts and documentation

---

## Core Specification Documents

| Document                          | Purpose                                    | Status                  |
| --------------------------------- | ------------------------------------------ | ----------------------- |
| [spec.md](../spec.md)             | Feature specification with user stories    | ✅ v1.2 (Generic-First) |
| [plan.md](../plan.md)             | Implementation plan with M0-M21 milestones | ✅ Updated with M19-M21 |
| [tasks.md](../tasks.md)           | Detailed task breakdown (T001-T314+)       | ✅ T200-T239 added      |
| [data-model.md](../data-model.md) | Entity relationship documentation          | ✅ Current              |
| [quickstart.md](../quickstart.md) | Developer onboarding guide                 | ✅ Current              |
| [research.md](../research.md)     | Research findings                          | ✅ Current              |

---

## Plans & Reference

### plans/ Directory (this directory)

| Document                                                           | Purpose                       |
| ------------------------------------------------------------------ | ----------------------------- |
| [ARTIFACTS-INDEX.md](ARTIFACTS-INDEX.md)                           | This navigation index         |
| [universal-analyzer-bridge-v2.md](universal-analyzer-bridge-v2.md) | Bridge architecture reference |
| [astm-flows-audit-report.md](astm-flows-audit-report.md)           | ASTM flow audit findings      |

---

## Contracts & Agreements

### contracts/ Directory

| Document                                                                | Version | Purpose                                                       |
| ----------------------------------------------------------------------- | ------- | ------------------------------------------------------------- |
| [supported-analyzers.md](../contracts/supported-analyzers.md)           | v1.1.0  | Authoritative analyzer inventory (13 required + 23 supported) |
| [order-export-api.yaml](../contracts/order-export-api.yaml)             | v1.0    | Order export REST API specification                           |
| [template-fixture-mapping.md](../contracts/template-fixture-mapping.md) | v1.0    | Mock template to fixture mapping                              |

---

## Verification & Testing

| Document                                                            | Purpose                        | For             |
| ------------------------------------------------------------------- | ------------------------------ | --------------- |
| [VERIFICATION-CHECKLIST.md](../templates/VERIFICATION-CHECKLIST.md) | Field verification procedures  | Deployment team |
| [VERIFICATION-GUIDE.md](../research/VERIFICATION-GUIDE.md)          | General verification guide     | Developers      |
| [GENERIC-TEST-RECIPE.md](../templates/GENERIC-TEST-RECIPE.md)       | Generic analyzer testing steps | QA/Testing      |
| [testing-matrix.md](../checklists/testing-matrix.md)                | Test coverage matrix           | All             |

---

## Research & Analysis

### research/ Directory

| Document                                                                                     | Purpose                               |
| -------------------------------------------------------------------------------------------- | ------------------------------------- |
| [analyzer-plugin-architecture-report.md](../research/analyzer-plugin-architecture-report.md) | Plugin architecture analysis          |
| [hl7-analyzer-messaging-validation.md](../research/hl7-analyzer-messaging-validation.md)     | HL7 protocol validation (OBX-3 bug)   |
| [pre-implementation-analysis.md](../research/pre-implementation-analysis.md)                 | Architecture decision record (D1, D2) |
| [metadata-management-analysis-report.md](../research/metadata-management-analysis-report.md) | 6-tier metadata ownership model       |
| [xml-hibernate-migration.md](../research/xml-hibernate-migration.md)                         | XML → JPA annotation migration guide  |
| [VERIFICATION-GUIDE.md](../research/VERIFICATION-GUIDE.md)                                   | Developer verification procedures     |

---

## Checklists

### checklists/ Directory

| Document                                                               | Purpose                 |
| ---------------------------------------------------------------------- | ----------------------- |
| [requirements.md](../checklists/requirements.md)                       | Requirements checklist  |
| [CONSTITUTION-COMPLIANCE.md](../checklists/CONSTITUTION-COMPLIANCE.md) | Constitution compliance |
| [testing-matrix.md](../checklists/testing-matrix.md)                   | Test coverage matrix    |

---

## External Artifacts

### Analyzer Harness

**Location:** `projects/analyzer-harness/`

| Document                                                              | Purpose                          |
| --------------------------------------------------------------------- | -------------------------------- |
| [README.md](../../../projects/analyzer-harness/README.md)             | Harness overview and dev setup   |
| [SETUP-REPORT.md](../../../projects/analyzer-harness/SETUP-REPORT.md) | Architecture gaps & setup report |
| [analyzer-defaults/](../../../projects/analyzer-defaults/README.md)   | 11 default config templates      |

### Test Fixtures

**Location:** `src/test/resources/testdata/`

| Fixture                             | IDs       | Analyzers | Purpose                           |
| ----------------------------------- | --------- | --------- | --------------------------------- |
| `madagascar-analyzer-test-data.xml` | 2000-2012 | 13        | Madagascar contract (Feature 011) |
| `global-analyzer-inventory.xml`     | 3000-3035 | 36        | All supported plugins             |
| `analyzer-mapping-test-data.xml`    | 1000-1004 | 5         | Feature 004 (ASTM mapping)        |

---

## Navigation Guide

### For Developers

1. Start with [spec.md](../spec.md) for feature overview
2. Read [plan.md](../plan.md) for milestone structure
3. Check [tasks.md](../tasks.md) for specific tasks
4. Review
   [contracts/supported-analyzers.md](../contracts/supported-analyzers.md) for
   analyzer details

### For Deployment Teams

1. Read [VERIFICATION-CHECKLIST.md](../templates/VERIFICATION-CHECKLIST.md) for
   field procedures
2. Use [contracts/supported-analyzers.md](../contracts/supported-analyzers.md)
   for analyzer specs
3. Follow [GENERIC-TEST-RECIPE.md](../templates/GENERIC-TEST-RECIPE.md) for
   testing

### For Architecture Review

1. Read
   [pre-implementation-analysis.md](../research/pre-implementation-analysis.md)
   for decisions
2. Review [universal-analyzer-bridge-v2.md](universal-analyzer-bridge-v2.md) for
   bridge architecture
3. Check
   [analyzer-defaults/README.md](../../../projects/analyzer-defaults/README.md)
   for config templates

---

**Maintained By:** OpenELIS Global Feature 011 Team **Repository:**
`DIGI-UW/OpenELIS-Global-2`
